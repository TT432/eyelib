# ADR-0014: 模块扁平合并 — 取消 Gradle 子项目，统一包命名空间

**Status:** Proposed
**Date:** 2026-06-16
**Author:** @TT432
**Supersedes:** ADR-0006 中「Independent Gradle subproject for each seam | Build isolation | v1.0」决策行
**Amends:** ADR-0002（模块边界由 Gradle project 边界改为包边界）、ADR-0010（ArchUnit 强制改为文档约定）

## Context

### 问题

当前 eyelib 用 12 个 Gradle 子项目隔离模块（eyelib-util、eyelib-molang、eyelib-animation 等）。这套设计的代价越来越高：

1. **样板爆炸**：每个子项目 build.gradle 是 ~95 行模板复制（legacyForge / parchment / mods.toml / publishing / signing），只有 dependencies 块不同。root build.gradle 的 `subprojects {}` 块又重复一遍 publishing/signing。
2. **依赖图复杂**：root 用 `api` + `modImplementation` + `jarJar` 三件套引每个子项目，子项目间还有 project() 边。一次小改动要触发多模块编译。
3. **Publishing 畸形**：每个子项目各自 mavenJava publication + 签名，外加 root 的 `mavenCentralLibrary` 手动收集所有子项目依赖。维护成本高，实际收益低——最终用户只装一个 mod jar。
4. **ArchUnit 强制失灵**：六边形架构的 domain 隔离靠 ArchUnit 测试，但 6 个 domain 模块中只有部分真正零 MC import，且排除规则越来越复杂（@Mod bootstrap、platform/、FriendlyByteBuf）。工具强制的收益已不抵维护成本。

### 目标

> 把 12 个子项目合并到 root 单 Gradle 项目，所有源码统一到 `io.github.tt432.eyelib.<module>` 命名空间。Publishing 只保留 mod jar。

## Decision

### 1. 包重命名映射

所有子项目源码重命名到 `io.github.tt432.eyelib.<module>`：

| 原 | 新 |
|---|---|
| `io.github.tt432.eyelibutil` | `io.github.tt432.eyelib.util` |
| `io.github.tt432.eyelibnetwork` | `io.github.tt432.eyelib.network` |
| `io.github.tt432.eyelibtrack` | `io.github.tt432.eyelib.track` |
| `io.github.tt432.eyelibmodel` | `io.github.tt432.eyelib.model` |
| `io.github.tt432.eyelibmolang` | `io.github.tt432.eyelib.molang` |
| `io.github.tt432.eyelibmaterial` | `io.github.tt432.eyelib.material` |
| `io.github.tt432.eyelibattachment` | `io.github.tt432.eyelib.attachment` |
| `io.github.tt432.eyelibanimation` | `io.github.tt432.eyelib.animation` |
| `io.github.tt432.eyelibbehavior` | `io.github.tt432.eyelib.behavior` |
| `io.github.tt432.eyelibimporter` | `io.github.tt432.eyelib.importer` |
| `io.github.tt432.eyelibparticle` | `io.github.tt432.eyelib.particle` |
| `io.github.tt432.eyelibbridge` | `io.github.tt432.eyelib.bridge` |

root 原有 `io.github.tt432.eyelib.*` 保持不变。

### 2. 命名冲突处理

两处冲突，都是 root 单文件 vs 子模块目录：

- **`eyelib.molang.mapping`**：root 有 `MolangQuery.java`（root-coupled 查询函数，不可下放）；子模块 `eyelibmolang.mapping.*` 重命名后落入同包。处理：合并到同一包，`MolangQuery` 保留，子模块类按原名共存。实施时需检查类名碰撞。
- **`eyelib.network`**：root 有 `EyelibNetworkManager`、`NetClientHandlers`、`package-info.java`；子模块 `eyelibnetwork.*`（含 `EyelibNetworkTransport`，以及将被删除的 @Mod bootstrap `EyelibNetworkMod`）重命名后落入同包。处理：合并到同一包，类名不冲突。

### 3. Mixin 合并

3 个 mixin json 合并为单一 `eyelib.mixins.json`，统一管 `io.github.tt432.eyelib.mixin` 包。track 和 attachment 的 mixin 类物理移到 `io.github.tt432.eyelib.mixin.track.*` 和 `io.github.tt432.eyelib.mixin.attachment.*`。

jar manifest 的 `MixinConfigs` 从 3 条减为 1 条。

### 4. @Mod bootstrap 统一

11 个子项目的 `@Mod` bootstrap 类（`EyelibUtilMod`、`EyelibMolangMod`、`EyelibNetworkMod` 等）全部删除。合并后只有 root `Eyelib.java` 持有 `@Mod("eyelib")`。

每个 bootstrap 类里的注册逻辑（`DeferredRegister`、`@Mod.EventBusSubscriber`、事件订阅）必须迁移到 root 的相应初始化路径或模块内的非 @Mod 入口类。**这是合并中风险最高的环节**，实施时逐个审查并测试。

### 5. mods.toml 合并

删除所有子项目的 `META-INF/mods.toml`。root 的 `mods.toml` 保持单一 mod id `eyelib`，`[[dependencies.<modid>]]` 只保留 forge/minecraft 版本约束一份。

### 6. ArchUnit 删除

6 个 domain 模块的 `src/test/java/.../archunit/ArchitectureRules.java` 及相关测试依赖全部删除。六边形架构的 domain 隔离约束改为文档约定（见 ADR-0010 修订）。

### 7. Publishing 简化

删除：
- `mavenCentralLibrary` publication 及配套 task（`libraryJar`、`librarySourcesJar`、`libraryJavadocJar`、`centralCompileDeps`、`centralRuntimeDeps`）
- `subprojects {}` 块里所有 publishing/signing 配置
- 每个子项目的 `mavenJava` publication

保留：
- root 的 `mavenJava`（mod jar + jarJar embedding）+ signing
- `nexusPublishing`（OSSRH）

### 8. build.gradle 简化

root `dependencies {}` 块删除所有 `api project(:eyelib-xxx)` / `modImplementation project(:eyelib-xxx)` / `jarJar project(:eyelib-xxx)`。`legacyForge.mods {}` 只绑定 root sourceSet。`processResources` 的 mods.toml expand 保留 root 一份。

### 9. settings.gradle 简化

删除所有 `include("eyelib-xxx", ...)`。保留 `includeBuild("clientsmoke")` 和 plugin 管理。

### 10. clientsmoke

`includeBuild("clientsmoke")` 保持不动。若 clientsmoke 有 import 引用旧包名（`io.github.tt432.eyelibmolang.*` 等），同步更新。

### 11. 文档同步

- **MODULES.md**：从「12 子项目」改为「单项目 + 按包划分的模块清单」
- **ADR-0002**：标注「Gradle project 边界已取消，改为包边界」
- **ADR-0006**：标记「Independent Gradle subproject」决策行为 Superseded by ADR-0014
- **ADR-0010**：修订 ArchUnit 强制条款为文档约定
- **AGENTS.md**：「Repository Shape」段重写
- **docs/README.md**：若有子项目引用需更新

### 12. 合并后目录结构

```
eyelib/
├── src/main/java/io/github/tt432/eyelib/
│   ├── capability/        client/         common/         debug/
│   ├── event/             smoke/          # root 原有
│   ├── mixin/                                # root + track + attachment 合并
│   ├── network/                              # root + eyelib-network 合并
│   ├── util/             track/          model/         molang/        # + root MolangQuery
│   ├── material/         attachment/     animation/     behavior/
│   ├── importer/         particle/       bridge/
└── src/test/java/io/github/tt432/eyelib/<module>/   # 同步重命名
```

## Consequences

### 正面

- **样板消失**：build.gradle 从 ~530 行降到 ~250 行，子项目 build.gradle 全部删除。
- **依赖图扁平**：单 project，无 project() 边，编译图简单。
- **Publishing 清晰**：只发 mod jar，POM 自动生成，不再手动收集 centralDeps。
- **IDE 体验改善**：单 project，无跨子项目跳转延迟。
- **未来包重构成本低**：单 sourceSet 内 refactor 是 IDE 一键操作，不再受 project 边界约束。

### 负面

- **失去物理隔离**：原本子项目边界能挡住「util 反向依赖 root」这类违规。合并后靠包名约定 + review，约束变弱。
- **ArchUnit 不再强制**：六边形架构 domain 隔离改为文档约定，可能随时间腐化。缓解：ADR-0010 保留约束描述，PR review 时人工把关。
- **单次迁移工作量大**：~50 个文件的 package 声明 + 所有 import + resources 反射引用。git history 跨越重命名会有 diff 噪音。
- **编译缓存失效**：合并后所有 build/ 目录需清理一次，首次编译较慢。

### 中性

- **mod id 统一为 `eyelib`**：原本 11 个子 mod id（`eyelibutil`、`eyelibmolang` 等）消失。若有外部 mod 依赖这些 mod id 作为 Forge dependency，会断裂——但 eyelib 还没发版，无外部消费者。

## Verification

1. `jetbrain_build_project` 退出码 0
2. 所有保留的单元测试全绿（ArchUnit 删除后）
3. `runClient` 启动，跑 clientsmoke 套件，确认 @Mod 迁移和 mixin 合并无回归
4. grep 检查旧包名残留：`eyelibutil`、`eyelibnetwork`、`eyelibtrack`、`eyelibmodel`、`eyelibmolang`、`eyelibmaterial`、`eyelibattachment`、`eyelibanimation`、`eyelibbehavior`、`eyelibimporter`、`eyelibparticle`、`eyelibbridge` — 在 `.java`、`mods.toml`、`mixin json`、反射字符串中应全部清零
