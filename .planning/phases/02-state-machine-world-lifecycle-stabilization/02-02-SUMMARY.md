---
phase: 02-state-machine-world-lifecycle-stabilization
plan: 02
subsystem: runtime
tags: [forge-1.20.1, world-creation, state-machine, stabilization, WorldOpenFlows, superflat]

# Dependency graph
requires:
  - phase: 02-01
    provides: "ClientSmokeStateMachine skeleton with placeholder handlers, ClientSmokeState enum"
provides:
  - "World auto-creation via WorldOpenFlows.createFreshLevel() with creative superflat world"
  - "World reuse via loadLevel() when ClientSmokeTest already exists"
  - "Multi-stage readiness: player spawn check (WORLD_WAIT) + configurable stabilization delay (STABILIZE)"
  - "One-shot Phase 2 completion log with test count"
affects: ["02-03-screenshot-exit", "Phase 3 test execution"]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "WorldOpenFlows.createFreshLevel() for programmatic world creation (Forge 1.20.1)"
    - "WorldPresets.FLAT registry lookup for superflat dimension generation"
    - "Fixed seed (12345L) + WorldOptions for deterministic world generation"
    - "Multi-stage tick-driven readiness: levelExists → player spawn → stabilize ticks"
    - "One-shot log guard (stabilizeCompleteLogged) to prevent log spam in stable state"

key-files:
  created:
    - "eyelib-clientsmoke/src/test/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachineWorldTest.java"
  modified:
    - "eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachine.java"

key-decisions:
  - "Used Forge 1.20.1 WorldOpenFlows API (not NeoForge 1.21.1 WorldOpenFlows) — createFreshLevel(String, LevelSettings, WorldOptions, Function<RegistryAccess, WorldDimensions>) with 4 params"
  - "Used loadLevel(null, WORLD_NAME) for world reuse — null Screen parameter OK for automation (screen cleared if load fails)"
  - "Used WorldPresets.FLAT via registry lookup (not WorldPresets::createNormalWorldDimensions) — matches behavioral contract of 'creative superflat world'"
  - "Added stabilizeCompleteLogged guard field — prevents per-tick log spam after stabilization completes (deviation from plan which omitted this guard)"
  - "Used method reference ClientSmokeStateMachine::createFlatWorldDimensions as dimensions getter — cleaner than inline lambda"

patterns-established:
  - "Pattern 1: WorldOpenFlows pipeline for programmatic world creation on Forge 1.20.1"
  - "Pattern 2: RegistryAccess-based dimension preset lookup (FLAT via Registries.WORLD_PRESET)"
  - "Pattern 3: One-shot log guards for terminal states in tick-driven state machines"

requirements-completed: ["ENG-03", "ENG-04"]

# Metrics
duration: 26min
completed: 2026-05-06
---

# Phase 2 Plan 02: 世界创建 + 多阶段就绪 + 稳定化 Summary

**通过 Forge 1.20.1 WorldOpenFlows API 实现全自动创意超平坦世界创建/复用、玩家生成轮询、可配置稳定化延迟 — 全程零用户交互**

## Performance

- **Duration:** 26 min
- **Started:** 2026-05-06T11:58:35Z
- **Completed:** 2026-05-06T12:25:27Z
- **Tasks:** 2
- **Files modified:** 2 (1 source + 1 test)

## Accomplishments

- 将 `handleWorldCreate()` 占位符替换为使用 Forge 1.20.1 `WorldOpenFlows` API 的完整世界创建逻辑
- 实现世界复用：若 `ClientSmokeTest` 已存在则直接加载，不重新创建
- 超平坦创意世界（`WorldPresets.FLAT`），固定种子 `12345L`，普通难度，允许命令
- `handleWorldWait()`：每 tick 轮询 `mc.player != null && mc.level != null`，无日志刷屏
- `handleStabilize()`：在玩家生成后等待 `RELOAD_STABILIZE_TICKS`（默认 40）tick
- 一次性稳定化完成日志（`stabilizeCompleteLogged` 守卫），输出 "Ready for test execution" 和 "Phase 2 complete" 及测试计数
- 状态机保持在 `STABILIZE`（Phase 3 从该状态接管）
- 完整错误处理：创建/加载失败时记录完整堆栈跟踪后转入 `ERROR` 状态

## Task Commits

每个任务按 TDD 流程（RED → GREEN）原子提交：

1. **Task 1: 添加世界创建字段并实现 handleWorldCreate()** 
   - `963e41c` (test) — RED: 字段和方法的反射测试
   - `d4842b5` (feat) — GREEN: 实现 handleWorldCreate(), createFlatWorldDimensions(), 三个新字段

2. **Task 2: 实现 handleWorldWait() 和 handleStabilize() 及多阶段就绪**
   - `76303e1` (test) — RED: 测试占位符模式已移除、含 mc.player 和 RELOAD_STABILIZE_TICKS
   - `d59bab9` (feat) — GREEN: 实现 handleWorldWait() 和 handleStabilize()

**计划元数据:** (待提交)

## Files Created/Modified

- `eyelib-clientsmoke/src/main/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachine.java` — 添加 WORLD_NAME/WORLD_SEED/stabilizeStartTick 字段，实现完整世界创建/等待/稳定化处理器（238 行，从 130 行增加）
- `eyelib-clientsmoke/src/test/java/io/github/tt432/clientsmoke/runtime/ClientSmokeStateMachineWorldTest.java` — 反射型单元测试，验证字段存在性、方法签名、行为契约

## Decisions Made

1. **Forge 1.20.1 API 适配：** 计划的 TARGET API 是 `mc.getLevelSource().createLevel()`，实际 API 是 `mc.createWorldOpenFlows().createFreshLevel()`（4 个参数，含 `Function<RegistryAccess, WorldDimensions>`）。通过读取编译后的 Forge jar 验证了精确签名。

2. **世界类型：** 计划代码使用 `WorldPresets::createNormalWorldDimensions`，但行为契约要求"超平坦创意世界"。改用 `WorldPresets.FLAT` 通过注册表查找生成平坦维度。

3. **世界复用：** 计划使用 `mc.loadLevel(WORLD_NAME)`，实际 API 是 `mc.createWorldOpenFlows().loadLevel(Screen, String)`。传入 `null` 作为 Screen 参数（自动化场景，加载失败时清除屏幕）。

4. **防止日志刷屏：** 添加 `stabilizeCompleteLogged` 守卫字段（计划未包含，但每 tick 重复记录"稳定化完成"会造成日志刷屏）。

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] 稳定化完成后每 tick 日志刷屏**
- **Found during:** Task 2 (handleStabilize 实现)
- **Issue:** 计划代码在完成稳定化后未设置一次性守卫，导致后续每个 tick 都输出 "Stabilization complete" 和 "Phase 2 complete"
- **Fix:** 添加 `stabilizeCompleteLogged` 布尔守卫字段，确保完成日志仅记录一次
- **Files modified:** `ClientSmokeStateMachine.java`
- **Verification:** `stabilizeCompleteLogged` 在字段声明中出现 3 次（声明 + guard 检查 + one-shot 设置）
- **Committed in:** `d59bab9` (Task 2 GREEN commit)

**2. [Rule 1 - Bug] 世界类型不匹配：NORMAL 而非 FLAT**
- **Found during:** Task 1 (handleWorldCreate 实现)
- **Issue:** 计划代码注释使用 `WorldPresets::createNormalWorldDimensions`，但行为契约要求超平坦创意世界以消除地形干扰
- **Fix:** 使用 `WorldPresets.FLAT` 通过 `Registries.WORLD_PRESET` 注册表查找创建平坦维度
- **Files modified:** `ClientSmokeStateMachine.java`
- **Verification:** `createFlatWorldDimensions()` 方法调用 `registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)`
- **Committed in:** `d4842b5` (Task 1 GREEN commit)

**3. [Rule 1 - Bug] Forge 1.20.1 API 签名：loadLevel 需要 Screen 参数**
- **Found during:** Task 1 (API 验证阶段)
- **Issue:** 计划使用 `mc.loadLevel(WORLD_NAME)` 但 Forge 1.20.1 的实际签名为 `WorldOpenFlows.loadLevel(Screen, String)`
- **Fix:** 使用 `mc.createWorldOpenFlows().loadLevel(null, WORLD_NAME)` — 自动化场景传入 null Screen 符合需求
- **Files modified:** `ClientSmokeStateMachine.java`
- **Verification:** 编译通过，`loadLevel(null, WORLD_NAME)` 签名与反编译的 `WorldOpenFlows.class` 匹配
- **Committed in:** `d4842b5` (Task 1 GREEN commit)

---

**Total deviations:** 3 auto-fixed (3 Rule 1 bugs)
**Impact on plan:** 全部自动修复均为正确性和可用性所必需。无范围蔓延。

## Issues Encountered

- **Forge 1.20.1 world creation API discovery:** `WorldOpenFlows` 存在于 Forge 1.20.1 中（与研究文档声称的不同），但方法签名与 1.21.1 不同（4 个参数，无 Screen 参数）。通过 IDE 反编译验证。
- **无法通过 shell 运行 Gradle（AGENTS.md 约束）：** 用 IDE 诊断（`problemCount: 0`）和 grep 验证替代了 `./gradlew build`。

## User Setup Required

无 — 无需外部服务配置。

## Next Phase Readiness

- 状态机现在完全自动化地创建/复用超平坦测试世界、等待玩家生成、执行稳定化延迟
- 完成时记录 "Ready for test execution"（无日志刷屏），状态保持在 `STABILIZE`
- **准备就绪，进入 Phase 3** —— 下一个计划（02-03）可从 `STABILIZE` 状态接管，开始测试执行（截图/退出）

---

*Phase: 02-state-machine-world-lifecycle-stabilization*
*Completed: 2026-05-06*
