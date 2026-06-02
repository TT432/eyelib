# eyelib-particle 测试重写计划

## 审计摘要

**文件总数**: 18 个测试文件
**评估标准**: C1-Name(命名清晰) / C2-AAA(三段分明) / C3-Single concern(单一职责) / C4-No external deps(无外部依赖) / C5-Quality assertion(断言语义明确) / C6-Low coupling(低耦合) / C7-Speed(<100ms)

---

## 保留列表（Keep）

以下文件质量良好，无需改动：

| 文件 | 理由 |
|------|------|
| `api/ParticleSpawnRequestTest.java` | 3 个方法各测单一职责：spawn ID 与 particle ID 保持字符串、null 字段拒绝、position 防御性拷贝。AAA 清晰，无外部依赖 |
| `api/ParticlePublisherTest.java` | 2 个方法分别测试 `replaceParticles` 与 `publishParticle`。内联 `MemoryParticleStore` 无全局状态 |
| `api/ParticleStoreContractTest.java` | 契约测试，覆盖 put → replaceAll → get → names → clear 完整生命周期 |
| `runtime/ParticleCommandRuntimeTest.java` | 3 个方法各测单一行为：suggest 过滤、suggest 空前缀、buildRequest 和 spawnSuccessMessage。命名尚可（含少量 'And' 但范围明确） |
| `runtime/ParticleRuntimeSupportTest.java` | 4 个方法分别测试：ParticleRuntimeDefinition、ParticleTimer、ParticleBlackboard、ParticleRuntimeContext。职责隔离良好 |
| `runtime/ParticleDefinitionAdapterTest.java` | 8 个方法覆盖 witchspell 完整 fixture、events 保留、null/blank 输入拒绝。AAA 清晰，无文件系统依赖（仅 classpath `getResourceAsStream`） |
| `client/ParticleRenderManagerLifecycleTest.java` | 3 个方法各测单一流程：spawn/remove idempotency、render tick 清理、client tick + clear。内联 FakeEnvironment |

---

## 删除列表（Delete）

以下文件属于**源头扫描/文档断言**测试，违反 C4（读源码文件）且脆弱——改动注释或重构就会红：

| 文件 | 理由 |
|------|------|
| `runtime/ParticleRuntimeBoundaryTest.java` | `Files.walk()` 扫描 runtime 源码，断言无 MC/Forge import。C4 违规、脆弱。直接删除 |
| `runtime/ParticleDefinitionBoundaryTest.java` | 同上，扫描 `eyelib-particle/src/main/java` 下的 import。C4 违规 |
| `runtime/ParticleDefinitionDocumentationTest.java` | `Files.readString()` 读 MODULES.md + 4 个决策文档，断言含特定文本。C4 违规、数据脆弱 |
| `loading/ParticleLoadingBoundaryTest.java` | `Files.walk()` 扫描 loading 包 import。C4 违规 |
| `client/ParticleClientIntegrationBoundaryTest.java` | `Files.readString()` 读 6 个源码文件，断言特定字符串。C4 违规。如果保留，应改为编译期架构测试 |
| `ParticleModuleFinalBoundaryTest.java` | 扫描模块全部源码 + 交叉引用其他测试文件。C4 违规、脆弱、维护成本高 |

**建议替代方案**: 架构边界验证改用 **Gradle 模块隔离**（模块间依赖约束）即可，无需引入额外测试依赖。这些边界验证不是行为测试，不适合放在 jUnit 测试套件中。

---

## 拆分/重写列表（Rewrite）

### 1. `api/ParticlePublisherAndSpawnApiTest.java`
- **问题**: 3 个方法中，前 2 个 (`publisherFlattens...` 和 `publisherPublishes...`) 完全重复 `ParticlePublisherTest.java` 的测试逻辑；第 3 个 (`spawnRequestRequiresStringIdsAndDefensivelyCopiesPosition`) 完全重复 `ParticleSpawnRequestTest.java`。此外包含重复的 `MemoryParticleStore` 内部类。
- **目标**: 删除此文件，将真正需要集成测试的场景移到集成测试目录（如 `src/test-integration/`）。

### 2. `runtime/bedrock/component/ParticleComponentRuntimeTest.java`
- **问题**: 仅 2 个测试方法，覆盖 **10+ 个 particle 组件**（billboard、lighting、tinting、initialSpeed、initialSpin、lifetimeExpression、killPlane、motionDynamic、expireIfInBlocks、expireIfNotInBlocks、motionParametric）。C3 严重违规（Eager Test）。
- **目标**: 拆分为每个组件的独立测试类：
  - `ParticleAppearanceBillboardTest` — size/UV 计算
  - `ParticleAppearanceTintingTest` — 静态颜色 + 渐变色
  - `ParticleInitialSpeedSpinTest` — onStart 速度/旋转/旋转速率
  - `ParticleLifetimeExpressionTest` — expiration/maxLifetime 行为
  - `ParticleLifetimeKillPlaneTest` — kill plane 裁剪
  - `ParticleMotionDynamicTest` — 加速度、阻力、旋转
  - `ParticleMotionParametricTest` — 参数位置/速度
  - `ParticleExpireBlocksTest` — expire_if_in_blocks / expire_if_not_in_blocks
- 保留 `FakeParticle` 作为共享测试夹具

### 3. `runtime/bedrock/component/EmitterComponentRuntimeTest.java`
- **问题**: 4 个方法每个覆盖多个 emitter 组件。`instantManualAndSteadyRateComponentsPreserveEmissionGating` 一个方法测试 3 个 rate 组件。`onceLoopingAndExpressionLifetimeComponentsPreserveLifecycleEffects` 测试 3 个 lifetime 组件。`localSpaceAndShapeComponentsPreservePositionEvaluationAndDirection` 测试 localSpace + shape 两类组件。C3 违规。
- **目标**: 拆分为：
  - `EmitterRateInstantTest` — onLoop 发射粒子数
  - `EmitterRateManualTest` — canEmit 门控逻辑
  - `EmitterRateSteadyTest` — onTick 逐步发射 + onLoop 重置
  - `EmitterLifetimeOnceTest` — active_time 到期移除
  - `EmitterLifetimeLoopingTest` — looping/wait 周期
  - `EmitterLifetimeExpressionTest` — 表达式门控
  - `EmitterLocalSpaceTest` — position/rotation/velocity 标记
  - `EmitterShapePointTest` — emit position 精确值
  - `EmitterShapeBoxTest` — emit position 随机范围
  - `DirectionTest` — outwards 方向计算
- `componentManagerDecodesEmitterRateAndLifetimeFromRawComponents` 可保留为简短集成测试
- 共享 `FakeEmitter`

### 4. `runtime/bedrock/ParticleRuntimeLifecycleTest.java`
- **问题**: `emitterRegistersMolangStateAndSpawnsModuleParticles` 在一个方法中测：molang 状态注册、粒子生成 idempotency、remove idempotency、position 验证。`particleRegistersMolangStateDispatchesComponentsAndRemovesIdempotently` 一处测：molang、初始速度、lifetime、render frame tick、remove idempotency。C3 违规。
- **目标**: 拆分为：
  - `EmitterMolangStateTest` — parentScope 继承、emitter 变量
  - `EmitterSpawnParticlesTest` — 生成粒子数量/位置
  - `ParticleRemoveIdempotencyTest` — 重复 remove
  - `ParticleInitializationTest` — speed/lifetime/particle 变量
  - `ParticleExpirationByMaxLifetimeTest` — 超 max_lifetime 移除
  - `ParticleExpirationByExpressionTest` — expiration_expression 移除
  - `EmitterRemoveTest` — emitter.removed() 标记
  - `ParticleRemoveCallbackIdempotentTest` — 回调幂等性

### 5. `loading/ParticleResourcePublicationTest.java`
- **问题**: 依赖全局状态 `ParticleDefinitionRegistry.store()`。虽然 `@BeforeEach` 清理，但测试间通过共享状态耦合（C6 违规）。同时测试类内部 `ParticleDefinition` 使用全局 registry。
- **目标**: 重构 `ParticleResourcePublication` 以接受 `ParticleStore` 接口而非直接操作全局 registry。然后测试可传内存 store。5 个测试方法本身结构良好，保持拆分即可。
  - 或：至少在测试前通过 `store().clear()` 隔离。但长期建议改为依赖注入。

---

## 新测试建议（New）

| 优先级 | 测试描述 | 测试类名（建议） |
|--------|---------|-----------------|
| P1 | **ParticleSpawnRequest 序列化/反序列化** — `ParticleSpawnRequest` 的 JSON/network serialization 保留所有字段 | `ParticleSpawnRequestSerializationTest` |
| P1 | **ParticlePublisher 空列表替换** — `replaceParticles(List.of())` 应清空 store | `ParticlePublisherEmptyReplaceTest` |
| P1 | **ParticlePublisher 重复 publish 覆盖** — 同一 ID publish 两次，后者覆盖前者 | `ParticlePublisherOverwriteTest` |
| P2 | **ParticleStore 并发安全** — 多线程并发 put/replaceAll/clear 不抛异常（如果 Store 实现有并发保证） | `ParticleStoreConcurrencyTest` |
| P2 | **ParticleTimer 极端值** — ticks=0、large ticks、partialTick=1.0 边界 | `ParticleTimerBoundaryTest` |
| P2 | **ParticleBlackboard 类型安全** — 禁止的类型转换错误消息清晰 | `ParticleBlackboardTypeSafetyTest` |
| P2 | **EmitterComponentManager 错误回退** — 无法识别的 component 不抛异常 | `EmitterComponentManagerFallbackTest` |

---

## 优先级

### P0 — 必须处理
1. **ParticleComponentRuntimeTest** 拆分为 8 个单组件测试
2. **EmitterComponentRuntimeTest** 拆分为 10 个单组件测试
3. **ParticleRuntimeLifecycleTest** 拆分为 8 个场景测试

### P1 — 建议处理
4. **ParticlePublisherAndSpawnApiTest** 删除（重复内容）
5. **ParticleResourcePublicationTest** 解耦全局状态（依赖注入 store）
6. 删除 6 个边界源扫描测试，边界验证改用 Gradle 模块隔离

### P2 — 可后续优化
7. 新增序列化、并发、边界值测试
8. `ParticleDefinitionAdapterTest` 可增加更多 fixture 变体（缺失字段、非法值等）

---

## 统计

| 动作 | 文件数 | 占比 |
|------|--------|------|
| 保留（Keep） | 7 | 38.9% |
| 删除（Delete） | 6 | 33.3% |
| 拆分/重写（Rewrite） | 5 | 27.8% |
| **待处理** | **11** | **61.1%** |
