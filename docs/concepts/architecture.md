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
│   ├── <module>/                            ← 顶层模块包(清单见 MODULES.md)
│   └── <module>/<sub>/                      ← 二级子包
├── versions/1.20.1/                          ← Stonecutter node(legacyforge)
├── versions/1.21.1/                          ← Stonecutter node(neoforge)
├── clientsmoke/                              ← 独立 Stonecutter 子模块(烟雾测试框架),经 mavenLocal 产物消费
├── archunit_store/                           ← ArchUnit 冻结基线(随仓库提交,逐 node 一份)
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
│  · 集中所有 Minecraft API 依赖                            │
│  · Stonecutter `//?` 注释的主要栖息地                     │
├──────────────────────────────────────────────────────────┤
│  Domain 模块包(目标:零 MC import)                        │
│  · animation / behavior / material / model / particle    │
│  · molang / importer                                      │
│  · Port 由 domain 定义,bridge 实现                        │
├──────────────────────────────────────────────────────────┤
│  基础设施模块包                                           │
│  · capability / mixin / network / track / ui             │
│  · util / debug / smoke / uitest / wintersky / snowstorm │
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
   - 历史状态:ADR-0010 原本由 ArchUnit 强制;ADR-0014 因收益不抵成本删除;ADR-0015 以 freeze 模式恢复。
   - 当前状态:**ArchUnit 已落地**（freeze 模式）。`ArchitectureTest.java` 按 ADR-0016 四层模型约束 domain 包零 MC import;`IqfSourceScanRulesTest.java` 按 ADR-0018 约束 IQF 规则。基线随仓库提交在 `archunit_store/<node>/`,只允许缩小;基线增长由 `test` 任务闸门拦截。**三节点基线当前为空**——存量违规已全部还清,新违规即失败(见 [ADR-0015](../decisions/0015-stonecutter-multi-version.md) §4)。
2. **Domain 测试 oracle 来自 Bedrock 规范** —— 不来自当前实现输出。
3. **Port 由 domain 定义,bridge 实现** —— 依赖方向不可逆。
4. **`//?` 版本切分注释** —— 集中在 bridge 是目标态,当前 L1 散布 + L2/L3 集中(见 [ADR-0015](../decisions/0015-stonecutter-multi-version.md) §3)。

## 提取进度

Domain 模块的 Port 接口清单、MC import 渗透情况和剩余工作记录在 [:docs/architecture/domain-module-map.md:](../architecture/domain-module-map.md)。该文件是单一事实源,本文件不再维护进度表。

验收闸门流程:G1(ArchUnit 隔离) → G2(spec-based 测试) → G3(RenderDoc 集成)。详见 Skill `eyelib-hexagonal-gates`。

## 设计决策(ADR 索引)

所有跨模块设计决策以 ADR 形式记录在 [decisions/](../decisions/),**编号索引只在 [:docs/README.md:](../README.md) 维护一份**(含状态与附录说明)。本文件不再复制该表。
