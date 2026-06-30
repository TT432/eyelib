# Eyelib 系统架构

> 项目实际架构：六边形(Ports & Adapters) + 包边界模块化单体 + ECS + Stonecutter 多版本。
> **不是 DDD** —— 没有 Aggregate / Repository / Domain Event 等战术模式。Eyelib 是 Bedrock 渲染规范的 Java 复刻,战略 DDD 的「限界上下文」思想体现在包边界上。

## 系统上下文

```
┌──────────────────────────────────────────────────────────┐
│  Minecraft: Java Edition (Forge)                          │
│  ┌──────────────────────────────────────────────────┐    │
│  │  eyelib (Bedrock 渲染引擎复刻)                    │    │
│  │  · 解析 .mcpack Bedrock addon                    │    │
│  │  · Molang 表达式引擎                              │    │
│  │  · 材质 / 模型 / 动画 / 粒子 运行时               │    │
│  │  · 渲染管线(VAO → VBO → Shader → Draw Call)       │    │
│  └──────────────────────────────────────────────────┘    │
│         │                                                  │
│         ▼                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │ OpenGL 3.2   │  │ 纹理管理     │  │ 实体系统     │    │
│  │ (LWJGL)      │  │ (NativeImage)│  │ (Entity)     │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
└──────────────────────────────────────────────────────────┘
```

## 构建与源码布局

**单 Gradle project + Stonecutter 多 version node**(见 [ADR-0014](../decisions/0014-flat-merge.md)、[ADR-0015](../decisions/0015-stonecutter-multi-version.md))：

```
qylEyelib/                                    ← 单 Gradle project(centralScript = build.gradle)
├── src/main/java/io/github/tt432/eyelib/    ← 全部主源码,包边界 = 架构边界
│   ├── <module>/                            ← 顶层模块包(共 18 个,见 MODULES.md)
│   └── <module>/<sub>/                      ← 二级子包
├── versions/1.20.1/                          ← Stonecutter node(legacyforge)
├── versions/1.21.1/                          ← Stonecutter node(neoforge)
├── clientsmoke/                              ← composite build(烟雾测试框架)
├── build.gradle                              ← centralScript,每个 node 跑一次
├── settings.gradle                           ← Stonecutter tree 定义
└── stonecutter.gradle                        ← active version + plugin 声明
```

版本特定代码用 `//?` 注释切分,放在 `versions/<mc-version>/` 下。当前 active version 是 `1.20.1`。

## 模块分层(包边界 = 物理边界)

ADR-0014 之后,模块边界由 **Java 包**(`io.github.tt432.eyelib.<module>`)定义,不再由 Gradle subproject 定义。详见 [:MODULES.md:](../../MODULES.md) 完整清单。

```
┌──────────────────────────────────────────────────────────┐
│  Root 编排包(Forge 生命周期)                              │
│  · io.github.tt432.eyelib.client / .common               │
│  · bootstrap、EntityRenderSystem、loader 注册            │
│  · 依赖所有模块(编排者,不被模块依赖)                     │
├──────────────────────────────────────────────────────────┤
│  bridge (Adapter 层)                                      │
│  · io.github.tt432.eyelib.bridge                          │
│  · 实现 domain 层定义的 Port 接口                         │
│  · 集中所有 Minecraft API 依赖(目前 13 个类)             │
│  · Stonecutter `//?` 注释的主要栖息地                     │
├──────────────────────────────────────────────────────────┤
│  Domain 模块包(目标:零 MC import)                        │
│  · animation / behavior / material / model / particle    │
│  · molang / importer                                      │
│  · Port 由 domain 定义,bridge 实现                        │
├──────────────────────────────────────────────────────────┤
│  基础设施模块包                                           │
│  · attachment / capability / event / mixin / network     │
│  · track / util / debug / smoke                           │
└──────────────────────────────────────────────────────────┘
```

## 核心依赖方向

- **Domain 包 → bridge 包**: ❌ 禁止(循环依赖)
- **bridge → Domain**: ✅ 单向(Port 由 domain 定义,bridge 实现)
- **Domain → util**: ✅
- **Root 编排包 → 所有模块**: ✅(反向被禁止,模块不得依赖 root 编排包)
- **模块间非编排依赖**: 通过 ADR-0002 的依赖白名单约束

详见 [ADR-0002](../decisions/0002-module-boundaries.md)。

## 六边形架构约束

1. **Domain 模块包不 import `net.minecraft.*` / `net.minecraftforge.*`**。
   - 历史状态:ADR-0010 原本由 ArchUnit 强制;ADR-0014 因收益不抵成本删除;ADR-0015 计划以 freeze 模式恢复。
   - 当前状态:**文档约定 + PR review 把关**(ArchUnit 骨架尚未落地,Phase 2+ pending 见 [ADR-0015](../decisions/0015-stonecutter-multi-version.md))。
2. **Domain 测试 oracle 来自 Bedrock 规范** —— 不来自当前实现输出。
3. **Port 由 domain 定义,bridge 实现** —— 依赖方向不可逆。
4. **`//?` 版本切分注释** —— 集中在 bridge 是目标态,当前 L1 散布 + L2/L3 集中(见 [ADR-0015](../decisions/0015-stonecutter-multi-version.md) §3)。

## 提取进度

Domain 模块的 Port 接口清单、MC import 渗透情况和剩余工作记录在 [:docs/architecture/domain-module-map.md:](../architecture/domain-module-map.md)。该文件是单一事实源,本文件不再维护进度表。

验收闸门流程:G1(ArchUnit 隔离) → G2(spec-based 测试) → G3(RenderDoc 集成)。详见 Skill `eyelib-hexagonal-gates`。

## 设计决策(ADR 索引)

所有跨模块设计决策以 ADR 形式记录在 [decisions/](../decisions/)：

| 编号 | 内容 |
|------|------|
| [0001](../decisions/0001-modular-architecture-control-spec.md) | 模块架构控制规范 |
| [0002](../decisions/0002-module-boundaries.md) | 模块边界(核心依赖规则) |
| [0003](../decisions/0003-side-boundaries.md) | Side 边界(client/server) |
| [0004](../decisions/0004-generated-code-policy.md) | 生成代码隔离策略 |
| [0005](../decisions/0005-mc-debt-ledger.md) | MC 功能债务台账 |
| [0006](../decisions/0006-key-architecture-decisions.md) | 关键架构决策历史 |
| [0007](../decisions/0007-known-pitfalls.md) | 已知陷阱与反模式 |
| [0008](../decisions/0008-item-track-design.md) | Item Track 设计 |
| [0009](../decisions/0009-domain-events-particle-interaction.md) | Domain 事件 — 粒子交互 |
| [0010](../decisions/0010-hexagonal-architecture.md) | 六边形架构(Domain/Bridge 分层) |
| [0011](../decisions/0011-documentation-design-baseline.md) | 文档设计基线 |
| [0012](../decisions/0012-system-testing-strategy.md) | 系统测试策略 |
| [0013](../decisions/0013-bedrock-animation-controller-and-calculation.md) | 基岩版动画控制器与计算逻辑 |
| [0013 附录 A](../decisions/0013a-bedrock-animation-query-functions.md) | 基岩版 `query.*` 函数清单(316 个) |
| [0014](../decisions/0014-flat-merge.md) | 子项目 flat-merge 为单 project |
| [0015](../decisions/0015-stonecutter-multi-version.md) | Stonecutter 多版本 + ArchUnit 恢复 |
| [0016](../decisions/0016-bridge-extraction-standard.md) | 库隔离标准 — DDD 分层与 ACL 职责边界 |
| [0017](../decisions/0017-remove-migrate26renames.md) | 移除 migrate26Renames 文本替换，统一用 `//?` 条件注释 |
| [0018](../decisions/0018-isolated-quiescent-fragments.md) | 孤立静止片段架构（IQF）—— Application 层形状判据 + ArchUnit 扩展 |

> 注:0013 主文档与 [0013a 附录 A](../decisions/0013a-bedrock-animation-query-functions.md)(基岩版 query.* 函数清单)共享同一 ADR 编号,0013a 是 0013 的附录而非独立 ADR。
