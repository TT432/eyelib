# Phase 28: ANIM — 动画 Port 层删除与接口简化

**Gathered:** 2026-05-12
**Status:** Ready for planning
**Mode:** Smart Discuss (autonomous — ROADMAP as spec)

## Phase Boundary
`client/animation/` 下无效的 Port 接口层全部清除，Animation 默认方法不经过 Port 环形委托，调用方直接使用 Animation 自身方法。

### Files to delete
- `AnimationIdentityPort.java`
- `AnimationStatePort.java`
- `AnimationExecutionPort.java`
- `AnimationRuntimePortSet.java`
- `LegacyAnimationRuntimeAdapter.java`
- `AnimationRuntimes.java`

### Files to modify
- `Animation.java` — 移除环形委托，Untyped方法直接调用抽象方法
- `BrAnimator.java` — `identityPort().name()` → `name()`
- `AnimationComponent.java` — `identityPort().name()` → `name()`
- `BrControllerStateOwner.java` — `identityPort().name()` → `name()`, `statePort().createData()` → `createData()`
- `AnimationRuntimePortsTest.java` — 重写为不依赖ports()端口API
