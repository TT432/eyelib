# Domain 模块映射：Port 清单、提取状态

> 配合 ADR-0010 使用。此文件记录当前 Port 接口的位置和提取进度。

## 模块总览

```
                   ┌──────────────────────────┐
                   │      eyelib-bridge        │ ← 已创建
                   │  所有 MC import 集中在此   │
                   │  实现各 domain 的 Port      │
                   └────┬────┬────┬────┬───────┘
                        │    │    │    │
        ┌───────────────┤    │    │    └──────────────┐
        ▼               ▼    ▼    ▼                   ▼
  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐
  │ material │  │  molang  │  │  model   │  │  animation   │
  │ Port: 1  │  │ Port: 3  │  │ Port: 0  │  │  Port: 0     │
  └──────────┘  └──────────┘  └──────────┘  └──────────────┘
  ┌──────────┐  ┌──────────┐  ┌──────────────┐  ┌──────────────┐
  │ behavior │  │ particle │  │  importer    │  │  eyelib-util │
  │ Port: 0  │  │ Port: 0  │  │  已是 domain │  │   Port: 3    │
  └──────────┘  └──────────┘  └──────────────┘  └──────────────┘
```

> Port 数量指**该模块自己定义的 Port 接口**。共享 Port（多模块使用）放 `eyelib-util`。

## 提取状态（2026-06-08 更新）

| 模块 | MC 文件数 | ArchUnit | Spec 测试 | 剩余工作 |
|------|----------|---------|----------|---------|
| eyelib-material | 3 | ✅ | 28 tests | BrShaderMapping + BrMaterialEntry 迁移至 bridge |
| eyelib-molang | 4 | ✅ | 21 tests | platform/ 按 ROADMAP 保留 |
| eyelib-model | 2 | ✅ | — | FriendlyByteBuf 硬依赖（StreamCodec） |
| eyelib-animation | 4 | ✅ | 7 tests | Entity 引用 → PortEntity |
| eyelib-behavior | 1 | ✅ | 9 tests | — |
| eyelib-particle | 8 | ✅ | 6 tests | client/ 渲染桥迁移至 bridge |

## 已创建 Port 接口

| Port | 位置 | 说明 |
|------|------|------|
| `PortStringRepresentable` | eyelib-util | 替代 MC StringRepresentable，含 `fromEnum()` |
| `PortResourceLocation` | eyelib-util | 纯数据 record（`of()`, `parse()`, `toString()`） |
| `PortFriendlyByteBuf` | eyelib-util | 接口保留（暂未使用，StreamCodec 要求 MC 类型） |
| `PortRenderPass` | src/main/java/io/github/tt432/eyelib/material/port/ | 渲染语义（`transparency()`, `disableCulling()`, `of()` 工厂） |
| `PortEntity` | src/main/java/io/github/tt432/eyelib/molang/port/ | 实体属性查询 `Map<String, Object>` |
| `PortLevel` | src/main/java/io/github/tt432/eyelib/molang/port/ | 世界属性（dayTime, gameTime, playerCount, moonPhase） |
| `PortItemStack` | src/main/java/io/github/tt432/eyelib/molang/port/ | 物品栈属性（count, maxStackSize） |

## Port 复用矩阵

| Port 接口 | 定义在 | 复用模块 |
|-----------|--------|----------|
| `PortStringRepresentable` | eyelib-util | material, animation, behavior |
| `PortResourceLocation` | eyelib-util | behavior, particle, molang, material |
| `PortFriendlyByteBuf` | eyelib-util | —（预留） |
| `PortEntity` | eyelib-molang | animation, particle |
| `PortLevel` | eyelib-molang | particle |
| `PortRenderPass` | eyelib-material | bridge (RenderPassAdapter) |
| `PortItemStack` | eyelib-molang | — |

## 依赖方向

- Domain 模块 ≠→ bridge（禁止循环依赖）
- eyelib-bridge → domain 模块 ✅（单向）
- root → bridge ✅（通过 build.gradle 显式依赖）
- Port 共享规则：多个 domain 模块共用 → 放 eyelib-util；单 domain 使用 → 放各自 port/ 包
