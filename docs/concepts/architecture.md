# Eyelib 系统架构

> 基于 [ADR-0010](../decisions/0010-hexagonal-architecture.md) 六边形架构设计。

## C4 Level 1: 系统上下文

```
┌──────────────────────────────────────────────────────────┐
│  Minecraft: Java Edition (Forge 47.1.3)                   │
│  ┌──────────────────────────────────────────────────┐    │
│  │  eyelib (Bedrock 渲染引擎)                         │    │
│  │  · 解析 .mcpack Bedrock addon                    │    │
│  │  · Molang 表达式引擎                              │    │
│  │  · 材质/模型/动画/粒子 运行时                      │    │
│  │  · 渲染管线（VAO→VBO→Shader→Draw Call）            │    │
│  └──────────────────────────────────────────────────┘    │
│         │                                                  │
│         ▼                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │ OpenGL 3.2   │  │ 纹理管理     │  │ 实体系统     │    │
│  │ (LWJGL)      │  │ (NativeImage)│  │ (Entity)     │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
└──────────────────────────────────────────────────────────┘
```

## C4 Level 2: 容器图（模块分层）

```
┌──────────────────────────────────────────────────────────┐
│  Root 模块 (Forge 生命周期)                                │
│  · bootstrap、EntityRenderSystem、loader 注册             │
│  · Manager/Registry/reload 基础设施                        │
│  · ──────────────────────────────────────────────────── │
│           │ 依赖所有子模块                                 │
├───────────┼──────────────────────────────────────────────┤
│  eyelib-bridge (Adapter 层)                               │
│  · 所有 MC import 集中于此                                 │
│  · 实现 Domain 层定义的 Port 接口                          │
│  · Entity→PortEntity 适配、RenderType 桥接                │
│  · ──────────────────────────────────────────────────── │
│           │ bridge → domain (单向)                        │
├───────────┼──────────────────────────────────────────────┤
│  Domain 模块 (零 MC import, JUnit 独立验证)               │
│  ┌──────────────────┬──────────────────┐                │
│  │ eyelib-molang    │ eyelib-particle  │                │
│  │ (Molang 引擎)    │ (粒子系统)       │                │
│  ├──────────────────┼──────────────────┤                │
│  │ eyelib-material  │ eyelib-animation │                │
│  │ (Bedrock 材质)   │ (动画运行时)     │                │
│  ├──────────────────┼──────────────────┤                │
│  │ eyelib-model     │ eyelib-behavior  │                │
│  │ (模型数据)       │ (实体行为组件)   │                │
│  └──────────────────┴──────────────────┘                │
│                                                           │
│  eyelib-importer (解析器/Codec)                           │
│  ──────────────────────────────────────────────────────  │
│  eyelib-util (共享工具，零 MC)                             │
│  · Port 接口、工具类                                      │
├──────────────────────────────────────────────────────────┤
│  Adapter 模块 (MC 胶水本胶)                                │
│  ┌──────────────────┬──────────────────┐                │
│  │ eyelib-network   │ eyelib-track     │                │
│  │ (网络同步)       │ (ItemStack 跟踪) │                │
│  ├──────────────────┼──────────────────┤                │
│  │ eyelib-attachment│ eyelib-preproc.  │                │
│  │ (数据附着)       │ (预处理)         │                │
│  └──────────────────┴──────────────────┘                │
└──────────────────────────────────────────────────────────┘
```

## 核心依赖方向

- **Domain → Bridge**: ❌ 禁止（循环依赖）
- **Bridge → Domain**: ✅ 单向
- **Domain → eyelib-util**: ✅
- **Root → 所有模块**: ✅
- **Adapter 模块间**: 通过 build.gradle 声明

## 六边形架构核心约束

1. **Domain 模块不 import `net.minecraft.*`** — 由 ArchUnit 规则强制
2. **Domain 测试 oracle 来自 Bedrock 规范** — 不来自当前实现输出
3. **Port 由 domain 定义，bridge 实现** — 依赖方向不可逆
4. **Bridge 是所有 Stonecutter `//? if` 的唯一栖息地**

## 提取进度

| 批次 | 模块 | ArchUnit | Spec-test | 状态 |
|------|------|:--------:|:---------:|------|
| 1 | eyelib-material | ✅ (3排除) | 28 tests | 就绪 |
| 1 | eyelib-molang | ✅ (4排除) | 21 tests | 就绪 |
| 2 | eyelib-model | ✅ (2排除) | — | 就绪 |
| 3 | eyelib-animation | ✅ (4排除) | 7 tests | 就绪 |
| 3 | eyelib-behavior | ✅ (1排除) | 9 tests | 就绪 |
| 4 | eyelib-particle | ✅ (8排除) | 6 tests | 就绪 |

验收闸门：G1(ArchUnit) → G2(spec-test) → G3(RenderDoc 集成)。详见 [ADR-0010](../decisions/0010-hexagonal-architecture.md) 和 [验收闸门](../architecture/acceptance-gates.md)。

## 设计决策

所有跨模块设计决策以 ADR 形式记录在 [decisions/](../decisions/)：

| 编号 | 内容 |
|------|------|
| 0001 | 模块架构控制规范 |
| 0002 | 模块边界（核心依赖规则） |
| 0003 | Side 边界（client/server） |
| 0004 | 生成代码隔离策略 |
| 0005 | MC 功能债务台账 |
| 0006 | 关键架构决策历史 |
| 0007 | 已知陷阱与反模式 |
| 0008 | Item Track 设计 |
| 0009 | Domain 事件—粒子交互 |
| 0010 | 六边形架构 (Domain/Bridge 分层) |
| 0011 | 文档设计基线 |
