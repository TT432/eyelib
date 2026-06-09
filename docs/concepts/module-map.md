# Eyelib 模块清单

> 面向阅读的模块导航。编译期依赖以各子模块 `build.gradle` 的 `project(:)` 声明为准。

## 快速索引

| 模块 | 类型 | 职责 |
|------|------|------|
| **Root** | 根 | Forge 生命周期、EntityRenderSystem、loader 注册 |
| **eyelib-bridge** | Adapter | MC import 集中地，实现 Domain Port 接口 |
| **eyelib-material** | Domain | Bedrock 材质定义、GL 状态、shader 管线 |
| **eyelib-molang** | Domain | Molang 值/编译器/映射树 |
| **eyelib-model** | Domain | 模型数据（Bone/Cube/Face/Vertex、GlobalBoneId） |
| **eyelib-animation** | Domain | 动画运行时（clip/controller/keyframe） |
| **eyelib-behavior** | Domain | Bedrock 实体行为组件 |
| **eyelib-particle** | Domain | 粒子 API、发射器、运行时 |
| **eyelib-importer** | Domain | Bedrock addon 解析、Codec/schema |
| **eyelib-util** | Domain | 共享工具（Port 接口、codec、纹理路径） |
| **eyelib-network** | Adapter | Forge SimpleChannel 传输层 |
| **eyelib-attachment** | Adapter | 数据附着、Forge capability 接线 |
| **eyelib-track** | Adapter | ItemStack 级跟踪基础设施 |
| **eyelib-preprocessing** | Adapter | 预处理 |

## 分层依赖

```
Root ──────► bridge ──────► domain 模块
  │                            │
  └──────► adapter 模块 ◄──────┘

Domain → Bridge: ❌ 禁止
Bridge → Domain: ✅ 单向
```

详见 [架构总览](architecture.md) 和 [ADR-0002](../decisions/0002-module-boundaries.md)。

## 完整清单

完整的模块清单（含详细路径、交互规则），见项目根目录的 [MODULES.md](../../MODULES.md)。
