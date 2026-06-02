# eyelib-animation 测试重写计划

## 审计摘要

**文件总数**: 9 个测试文件
**评估标准**: C1-Name(命名清晰) / C2-AAA(三段分明) / C3-Single concern(单一职责) / C4-No external deps(无外部依赖) / C5-Quality assertion(断言语义明确) / C6-Low coupling(低耦合) / C7-Speed(<100ms)

---

## 保留列表（Keep）

以下文件质量良好，无需改动：

| 文件 | 理由 |
|------|------|
| `bedrock/controller/BrAnimationControllersCodecTest.java` | 单一测试方法，验证 codec 从 JSON 解析后的完整数据结构。无外部依赖，fast |
| `bedrock/BrAnimationCodecTest.java` | 单一测试方法，验证 `BrAnimation.CODEC` 解析完整 animation JSON。断言语义良好 |
| `bedrock/BrBoneAnimationChannelTest.java` | 4 个方法分别测试：channel 构建、linear 插值采样、missing channel fallback、单 keyframe 行为、Catmull-Rom → Linear 回退。结构清晰 |
| `bedrock/BrAnimationPlaybackStateTest.java` | 3 个方法分别测试：LOOP 模式 tick/wrap/restart、HOLD_ON_LAST_FRAME 钳位、reset 清空计数器。AAA 良好 |

---

## 删除列表（Delete）

无文件需要完全删除。所有 9 个文件测试的是**运行时行为**而非源文件结构。

---

## 拆分/重写列表（Rewrite）

### 1. `bedrock/controller/BrAnimationControllerStateOwnerTest.java`

- **问题**:
  - **全局状态依赖**（C6 违规）：方法 `tickAnimationInitializesOwnerBackedControllerStateAndCachesChildStateByAnimationName` 使用 `AnimationManager.INSTANCE.put()` 注入子动画——测试间通过单例 `AnimationManager.INSTANCE` 共享状态。
  - **Eager Test**（C3 违规）：方法名含 "And" ——同时验证了 tick 初始化 controller state **AND** caching child state。
  - `getDataUsesDirectAnimationMethodsAndCachesResultViaComputeIfAbsent` 同样含 "And" ——同时验证 createData() 委托 **AND** computeIfAbsent 缓存。
  - `@AfterEach tearDown()` 执行 `AnimationManager.INSTANCE.clear()`——测试清理依赖全局状态重置。

- **目标**:
  - **解耦全局状态**：`BrAnimationController.tickAnimation` 目前通过 `AnimationManager.INSTANCE.get(name)` 查找子动画。先重构生产代码，使其接受 `Function<String, Animation>` 查找函数（或 `AnimationLookup` 接口），然后在测试中传 mock。
  - **拆分方法**：
    - `tickAnimationInitializesControllerState()` — 仅验证 tick 后 startTick、currState 正确设置
    - `tickAnimationCachesChildAnimationState()` — 仅验证子动画 createData 只调用一次
  - **拆分 getData 测试**：
    - `getDataDelegatesToAnimationCreateData()` — 验证首次调用调用 createData
    - `getDataCachesResultByAnimationName()` — 验证第二次调用返回缓存值
    - `getDataInvokesNameOnEachCall()` — 验证 name() 作为 map key 每次调用都会调

### 2. `bedrock/controller/BrAnimationControllerBehaviorTest.java`

- **问题**:
  - **全局状态依赖**（C6 违规）：`allAnimationFinishedDelegatesToChildAllAnimationFinished` 使用 `AnimationManager.INSTANCE.put()`。`@AfterEach` 清空。
  - 命名含 "And" (轻微)。

- **目标**:
  - 同上，解耦 `AnimationManager.INSTANCE` 依赖。
  - 方法可保持原位（单一职责），仅去除全局状态依赖。
  - `fromSchemaFallsBackToDefaultStateWhenInitialStateIsMissing` 本身良好，无需改动。

### 3. `bedrock/BrAnimationEntryCharacterizationTest.java`

- **问题**:
  - **全局状态依赖**（C6 违规）：`BrAnimationEntry.fromSchema` 内部调用 `GlobalBoneIdHandler.get(\"body\")`。`GlobalBoneIdHandler` 是静态单例，注册骨头 ID 的副作用可能影响其他测试。
  - **测试不可重复**：如果其他测试在之前调用了 `GlobalBoneIdHandler.get("body")`，这里的断言依赖其全局注册行为。运行顺序敏感。

- **目标**:
  - 重构生产代码：使 `GlobalBoneIdHandler` 可注入（如通过接口传入）、或 `BrAnimationEntry.fromSchema` 接受 `Function<String, Integer>` 参数。
  - 测试方法名 `fromSchemaCompilesNamedBonesIntoRuntimeBoneIdsAndChannels` 含 "And" ——可拆分为两个测试：
    - `fromSchemaCompilesNamedBonesIntoRuntimeBoneIds()` — 验证 "Body" → boneId 映射
    - `fromSchemaBuildsChannelsFromBoneAnimations()` — 验证 channels 包含 ROTATION
  - **清理 @AfterEach**: 如果 `GlobalBoneIdHandler` 无法完全解耦，应添加 `@AfterEach` 重置其状态。

### 4. `bedrock/BrAnimationEntryLifecycleTest.java`

- **问题**:
  - 轻微 Eager：`onFinishResetsPlaybackFieldsAndClearsRuntimeParticles` 一个断言同时检查 playback field reset 和 particles/effects 清理。
  - 结构尚可，方法内按功能分组清晰（fields→particles→effects）。

- **目标**:
  - 可拆分为：
    - `onFinishResetsPlaybackTimeFields()` — lastTicks/animTime/deltaTime/loopedTimes
    - `onFinishClearsRuntimeParticles()` — particles 清空
    - `onFinishPreservesEffects()` — effects 保留 3 个（sound/particle/timeline）
  - 或保持原位并在方法内添加 `@Nested` 分组。

### 5. `AnimationRuntimePortsTest.java`

- **问题**:
  - **全局状态依赖**（C6 违规）：`brAnimatorDispatchesThroughDirectAnimationMethods` 使用 `AnimationManager.INSTANCE.put()`。`@AfterEach tearDown()` 清空。
  - **Eager Test**（C3 违规）：`animationDirectMethodsWorkWithoutBridgeLayer` 同时测试 Animation 接口的 6 个方法（name/createData/onFinish/anyFinished/allFinished/tick）。每个方法出一个断言，组合在同一个测试方法中。
  - `brAnimatorDispatchesThroughDirectAnimationMethods` 名称含 "And"。

- **目标**:
  - 解耦 `AnimationManager.INSTANCE`（同前）。
  - **拆分 animationDirectMethodsWorkWithoutBridgeLayer**：
    - `animationNameReturnsGivenName()`
    - `animationCreateDataReturnsNewStateObject()`
    - `animationOnFinishDispatchesToImplementation()`
    - `animationAnyAllFinishedDispatchToImplementation()`
    - `animationTickAnimationDispatchesToImplementation()`
  - brAnimatorDispatcher 测试保持独立，仅解耦全局状态。

---

## 新测试建议（New）

| 优先级 | 测试描述 | 测试类名（建议） |
|--------|---------|-----------------|
| P1 | **BrAnimationEntry 边界值** — `animationLength = 0` 或负值时的 tick 行为 | `BrAnimationEntryBoundaryTest` |
| P1 | **BrAnimationController 空状态集** — `states` 为空 map 时的初始化行为 | `BrAnimationControllerEmptyStatesTest` |
| P1 | **BrAnimationPlaybackState 负时间 tick** — 传入负 ticks/delta 时的行为 | `BrAnimationPlaybackStateNegativeTimeTest` |
| P2 | **BrBoneAnimationChannel 空 keyframe** — 空 keyframe map 的 sample 返回 null | `BrBoneAnimationChannelEmptyKeyframeTest` |
| P2 | **BrAnimationEntry Timeline 空** — 空 timeline 的 onFinish 行为 | `BrAnimationEntryEmptyTimelineTest` |
| P2 | **BrAnimationController 状态转换阻塞** — transitions 阻止状态切换（如果 controller 有 transition 逻辑） | `BrAnimationControllerTransitionBlockingTest` |
| P2 | **AnimationComponent 多 controller 并发** — 多个 controller 注册不同 slot 时的协调 | `AnimationComponentMultiControllerTest` |

---

## 核心改善模式：全局状态解耦

`eyelib-animation` 测试的最大问题不是 Eager Test，而是 **全局状态依赖**。以下是统一修复策略：

```
// 生产代码现状
class BrAnimationController {
    void tickAnimation(...) {
        Animation child = AnimationManager.INSTANCE.get(name);  // 静态单例
    }
}

// 目标：可注入依赖
class BrAnimationController {
    void tickAnimation(..., AnimationLookup lookup) {
        Animation child = lookup.find(name);  // 接口
    }
}
```

**需解耦的全局状态**:
1. `AnimationManager.INSTANCE` — 在 `BrAnimationControllerStateOwnerTest`、`BrAnimationControllerBehaviorTest`、`AnimationRuntimePortsTest` 中使用
2. `GlobalBoneIdHandler` — 在 `BrAnimationEntryCharacterizationTest` 中使用，`get("body")` 返回 int 但依赖静态注册

---

## 优先级

### P0 — 必须处理
1. **BrAnimationControllerStateOwnerTest** — 全局状态解耦 + 拆分 5 个方法
2. **BrAnimationControllerBehaviorTest** — 全局状态解耦（`allAnimationFinishedDelegatesToChild`）
3. **BrAnimationEntryCharacterizationTest** — `GlobalBoneIdHandler` 解耦 + 方法拆分
4. **AnimationRuntimePortsTest** — 6 个 Animation 接口方法拆分 + 全局状态解耦

### P1 — 建议处理
5. **BrAnimationEntryLifecycleTest** — 拆分为 3 个更细的方法（可选）
6. 新增边界值测试（animationLength=0、负时间 tick 等）

### P2 — 可后续优化
7. 新增多 controller 协调测试
8. 新增 transition 阻塞测试

---

## 统计

| 动作 | 文件数 | 占比 |
|------|--------|------|
| 保留（Keep） | 4 | 44.4% |
| 删除（Delete） | 0 | 0% |
| 拆分/重写（Rewrite） | 5 | 55.6% |
| **待处理** | **5** | **55.6%** |
