# 代码审查报告：ArchUnit 违规清零 + Ports & Adapters 重构

审查日期：2026-07-08
规模：156 文件改动（+697/−4590）+ 45 新增，删除 47 个类、新增 22 个 Port/View 接口。

## 总体结论

架构方向正确、重构质量较高，**但存在 1 个被 `FreezingArchRule` 冻结掩盖的规则违规（P0）和 2 项未完成的清零任务**，不符合"全量清零"的既定目标。

## 验证方法（关键）

直接跑 `test` **无法**证明清零——`FreezingArchRule` 在 baseline 缺失时会把当前所有违规重新冻结并报"通过"。本审查采用**删除 `build/archunit_store` 后重跑**，检查重新生成的 baseline 文件大小（空=0违规）：`ArchitectureTest` 8 tests 0 failures，但 baseline 文件揭示了被冻结的真实违规。

## 成立的部分

| 维度 | 结论 | 证据 |
|---|---|---|
| 编译 | 三版本（1.20.1/1.21.1/26.1.2）BUILD SUCCESSFUL | compileJava + test |
| 规则放宽 7 项 | 全部合理，非掩盖 | 见下详述 |
| 7/8 规则清零 | I-5/规则8/规则4/I-2/规则1/3/6 baseline 均 0B | stored.rules + 文件大小 |
| 删除残留 | 47 个被删类零引用（import/FQN/反射/mixin JSON/AT 全干净） | DanglingRefCheck |
| Port 完整性 | 22 个 Port 全有实现+调用方，0 dead interface | PortAdapterCompleteness |
| 行为变更 | 网络Port化、反射接线、AR门控、mixin删除均正确 | diff 分析 |

### 规则放宽合理性（曾怀疑是"用放宽清零"，经验证否）

- `RECORD_CLASSES`：16 个全是真 DTO（6 packet + BakedModel/Bone + RenderEntityParams），record 化正确
- `FORGE_LIFECYCLE_ENTRIES`：19 个非接口类全带 `@Mod/@EventBusSubscriber`，且 **I-5 未排除它们**（兜底），自洽
- `EXTENDS_MC_CLASS`：实际匹配 `ModalWorksurfaceScreen extends Screen`（ModelPreviewScreen 必须是 Screen 子类），合理
- `allowEmptyShould`：`that()` 非空（40 接口），实际无效果，冗余但无害
- I-2 排除 record.INSTANCE：6 个全是无状态 marker（`record EmptyComponent()` 等），非服务定位器。**plan 把 4 个标记组件误判为"服务定位器型"，实际与 EmptyComponent/MolangNull 同类**

## 问题（按优先级）

### P0 — 规则 2 违规被 freeze 掩盖（主要问题）

删 store 重跑后，**规则 2 `domainMustNotDependOnOrchestration` baseline = 2630B，11 条违规，全部是 `domain → bridge.util.CodecOps`**：

```
EyelibCodec / Model$Bone / BrAnimationController / BaseFilter /
ParticleComponentManager / BBModelLoader / BedrockAddonLoader /
ImporterCodecUtil  →  CodecOps.{getOrThrow, unit, dispatchStable, dispatch, lazyCodec}
```

**根因**：`CodecOps` 从 `public final class`（仅 parseOrThrow）改成 `public interface` 并新增 7+ 方法（满足规则 8），但这些方法被 domain 层调用，引入 domain→bridge 反向依赖。这是**规则 3（`//?` 版本差异须在 bridge）与规则 2（domain 不依赖 bridge）的张力**——CodecOps 含 `//?` 必须在 bridge，但被 domain 需要。作者选择 freeze 规则 2 债务而非真正解决。

**plan.md 声称规则 2 "0 违规"与现状（11 条）不符**。plan 的 I-5 表格本就列出 "codec | CodecOps | 抽 Codec Port 或移 domain/util"，说明是已知待处理项但本次未完成。

> 解法：`CodecOps` 封装的是 DFU（`com.mojang.serialization`），DFU 在规则 1 白名单内，故 CodecOps **可整体移到 `domain/util`**；版本差异（`//?`）部分若需留在 bridge，则抽 `CodecVersionPort` 让 domain 调 Port。

### P1 — Stonecutter 23 文件未清零

`stonecutter-comment-baseline.txt` = 1469B，**23 个文件**含 `//?` 在非 ACL 包（client/ 17 + common/importer/util 各若干）。plan 目标是 48 文件全清，现残留 23。被 baseline 冻结，测试"通过"但未清零。

### P2 — Port 表面不完整（架构债务，非规则违规）

**14 个 application 文件仍直接 import `bridge..adapter` 类型**，集中在 6 个 Port 表面不完整的子系统：

| 子系统 | 缺口 | 高风险 Port |
|---|---|---|
| ui widgets | UIScreen/UIWidget/UIGraphics 直接 reach MCScreenAdapter/MCWidgetAdapter/MCGraphics | UiPort（高） |
| particle | ParticleRenderManager/ParticleSpawnRuntimeAdapter import ParticleRuntimeBridge | ParticlePort（高） |
| network send | DataAttachmentSyncPort 只覆盖 receive，send 仍直引 EyelibNetworkTransport | 中 |
| render orchestration | EntityRenderPorts/RenderPorts 无 Port | 中 |
| material / data-attach | RenderPassAdapter / DataAttachmentTypeRegistry 直引 | 中 |

这些**不违反 I-5**（adapter 类多为 `extends MC` 被排除、或接口、或 record），所以 I-5 baseline 是 0B——但架构上 application 仍耦合 bridge adapter，Port 抽象未贯彻到底。

### P3 — 代码质量问题（低）

- **Dead import**：`UIScreen.java:2`、`UIGraphics.java:2` import `bridge.ui.adapter.MCScreenAdapter/MCGraphics`，**仅 Javadoc 注释引用，代码未使用**。应删除（且与 ui"MC 无关"定位语义冲突）。
- **架构盲区**：`ui/` 包游离于 `DOMAIN_CLASSES`/`APPLICATION_CLASSES`/`ORCHESTRATION` 所有谓词之外，依赖完全不受 ArchUnit 约束（预存，非本次引入）。
- **5 个 Port 无 adapter 类**（ClientFrameTimePort/ClientTaskPort/PoseStackPort/VertexConsumerPort/ServerDirectoryPort 是自包含版本 shim），偏离 Port→adapter 分离。
- Port 实际 22 个，plan/任务称 23，差 1（文档不准）。

## 建议

1. **P0 必须处理**：要么把 CodecOps 移 domain/util 真正清零规则 2，要么在 plan/ADR 显式记录这是"已知债务"并说明 freeze 的正当理由——但不能让 plan 声称"0 违规"而 baseline 实际 11 条。
2. **P1 决策**：23 个 stonecutter 文件是继续清零，还是正式纳入 baseline 并记录（plan 目标是全清）。
3. **P2**：补全 UiPort/ParticlePort 等的 Port 表面，消除 application→adapter 直引。
4. **P3**：删 dead import；考虑把 `ui/` 纳入某个架构谓词消除盲区。

## 核心判断

这是一次高质量的重构主体，但"清零"的宣称不完整——规则 2 的 11 条违规和 stonecutter 的 23 文件被 freeze/baseline 机制掩盖成了"通过"。是否接受这些作为"已知债务"需用户决策，但当前 plan 文档与实际 baseline 状态不一致，应修正。
