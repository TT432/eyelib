# Domain 模块映射：Port 清单、提取状态

> 配合 ADR-0010 使用。此文件记录当前 Port 接口的位置和提取进度。
> 模块名为 ADR-0014 flat-merge 后的包名（`io.github.tt432.eyelib.<module>`）。

## 模块总览

```
                   ┌──────────────────────────┐
                   │        bridge             │ ← MC import 集中目标
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
  │ behavior │  │ particle │  │  importer    │  │    util       │
  │ Port: 0  │  │ Port: 0  │  │  已是 domain │  │   Port: 3     │
  └──────────┘  └──────────┘  └──────────────┘  └──────────────┘
```

> Port 数量指**该模块自己定义的 Port 接口**。共享 Port（多模块使用）放 `util`。

## 提取状态

> ArchUnit 隔离：ADR-0014 因收益不抵成本删除，ADR-0015 计划以 freeze 模式恢复（Phase 2+ pending）。当前状态为**文档约定 + PR review 把关**。

> 以下 MC 文件数与 Spec 测试数为 2026-06-08 快照，仅供横向对比参考。

| 模块 | MC 文件数 | Spec 测试 | 剩余工作 |
|------|----------|----------|---------|
| material | 3 | 28 tests | BrShaderMapping + BrMaterialEntry 迁移至 bridge |
| molang | 4 | 21 tests | platform/ 按 ROADMAP 保留 |
| model | 2 | — | FriendlyByteBuf 硬依赖（StreamCodec） |
| animation | 4 | 7 tests | Entity 引用 → PortEntity |
| behavior | 1 | 9 tests | — |
| particle | 8 | 6 tests | client/ 渲染桥迁移至 bridge |

## 已创建 Port 接口

| Port | 位置 | 说明 |
|------|------|------|
| `PortStringRepresentable` | `util` | 替代 MC StringRepresentable，含 `fromEnum()` |
| `PortResourceLocation` | `util` | 纯数据 record（`of()`, `parse()`, `toString()`） |
| `PortFriendlyByteBuf` | `util` | 接口保留（暂未使用，StreamCodec 要求 MC 类型） |
| `PortRenderPass` | `material/port/` | 渲染语义（`transparency()`, `disableCulling()`, `of()` 工厂） |
| `PortEntity` | `molang/port/` | 实体属性查询 `Map<String, Object>` |
| `PortLevel` | `molang/port/` | 世界属性（dayTime, gameTime, playerCount, moonPhase） |
| `PortItemStack` | `molang/port/` | 物品栈属性（count, maxStackSize） |

## Port 复用矩阵

| Port 接口 | 定义在 | 复用模块 |
|-----------|--------|----------|
| `PortStringRepresentable` | util | material, animation, behavior |
| `PortResourceLocation` | util | behavior, particle, molang, material |
| `PortFriendlyByteBuf` | util | —（预留） |
| `PortEntity` | molang | animation, particle |
| `PortLevel` | molang | particle |
| `PortRenderPass` | material | bridge (RenderPassAdapter) |
| `PortItemStack` | molang | — |

## 依赖方向

- Domain 模块 ≠→ bridge（禁止循环依赖）
- bridge → domain 模块 ✅（单向）
- root 编排包（client/common）→ bridge ✅
- Port 共享规则：多个 domain 模块共用 → 放 `util`；单 domain 使用 → 放各自 `port/` 包
