# ADR-0012: System 层测试策略 — 三层 Fake-Contract 模型

**Status:** Proposed
**Date:** 2026-06-09
**Author:** @TT432

## Context

### 问题

eyelib 当前有 71 个 spec-based 测试覆盖 domain 纯逻辑（material 继承链、Molang 求值、CODEC 往返等），但 **System 层零覆盖**。

"System" 在 eyelib 中的定义为：**Component 在 JE 中的运行时接线**。具体是：

```
BrClientEntity（数据）
   ↓ [EntityRenderSystem.setupClientEntity]
ModelComponent + AnimationComponent + RenderControllerComponent（组件实例）
   ↓ [EntityRenderSystem.renderComponents]
RenderParams → RenderHelper → GPU draw call（渲染输出）
```

当前验证这段接线的方式完全依赖运行时：
- RenderDoc 截帧 → 人眼或 Python replay 分析
- `/eval` 在运行中查询组件状态
- 视觉确认"看起来对不对"

问题：
1. **反馈循环极慢**（修改 → 编译 → 启动 MC → 进世界 → 召唤实体 → 截帧 → 分析，10+ 分钟）
2. **只能验证最终渲染输出**，中间态（Component 是否正确创建、animation binding 是否正确、render type 路由是否正确）只能靠日志推断
3. **无法在 CI 中运行**，每次重构后需要人工验证全量实体
4. **违反 ADR-0010 的目标**："在不启动 Minecraft 的情况下，证明 eyelib 的每一层行为都正确实现了 Bedrock 规范"

### ECS 视角

Bedrock 是 ECS 架构。eyelib 复刻它：

| ECS 层 | eyelib 对应 | 需测试？ |
|--------|------------|---------|
| E (Entity) | MC LivingEntity + RenderData 能力 | ❌ 身份标识，纯 MC 类型 |
| C (Component) | ModelComponent, AnimationComponent, RenderControllerComponent 等 | ❌ 纯数据容器 |
| S (System) | EntityRenderSystem.setupClientEntity / renderComponents, BrAnimator.tickAnimation, RenderTypeResolver | **✅ 需要测试** |

对照 Bevy ECS 的 System 测试模式：`World::new() → spawn(entity, ComponentSet) → run_system(world) → assert component_state`。

eyelib 的挑战：System 不是独立函数——它依赖 `ModelManager.INSTANCE`、`MaterialManager.INSTANCE`、`RenderControllerManager.INSTANCE` 等全局单例，以及 `Minecraft.getInstance().level` 等 MC 运行时。

### 六个 System（按测试难度递增）

| # | System | 输入 | 输出 | MC 依赖 |
|---|--------|------|------|---------|
| S1 | `EntityPortAdapter.from(entity)` | MC Entity | PortEntity(Map) | MC Entity 类 |
| S2 | `RenderTypeResolver.resolve()` | PortResourceLocation / BrMaterialEntry | PortRenderPass | **无** |
| S3 | `RenderPassAdapter.toRenderType()` | PortRenderPass + PortResourceLocation | MC RenderType | MC RenderType |
| S4 | `RenderControllerRuntime.evalPartVisibility()` | part_visibility patterns + MolangScope | Int2BooleanOpenHashMap | **无** |
| S5 | `BrAnimator.tickAnimation()` | AnimationComponent + scope | ModelRuntimeData | **无**（已在 domain 层） |
| S6 | `EntityRenderSystem.setupClientEntity()` | Entity + BrClientEntity → ModelComponent[] | **大量 MC 依赖** |

S2-S5 已经是纯逻辑或在 domain 模块中，可被直接测试。S6 是真正的硬骨头——它是接线逻辑的核心，也是 MC 耦合最深的部分。

## Decision

### 三层测试架构

```
┌─────────────────────────────────────────────────────┐
│ Layer 3: 组件接线测试 (System Integration)            │
│ 需重构: 提取 PortManager 接口 → Fake 实现              │
│ 验证: BrClientEntity → ModelComponent[] 接线正确      │
├─────────────────────────────────────────────────────┤
│ Layer 2: Bridge 适配器测试 (Contract Test)            │
│ EntityPortAdapter, RenderTypeResolver,               │
│ RenderPassAdapter, ResourceLocationBridge             │
│ 验证: Port 接口 → Bridge 输出的映射正确               │
├─────────────────────────────────────────────────────┤
│ Layer 1: Domain 纯逻辑测试 (Spec Test) ← 已有 71 个    │
│ MaterialResolver, AnimationController, CODEC, Molang  │
│ Oracle: Mojang Creator 文档 + .mcpack 数据             │
└─────────────────────────────────────────────────────┘
```

### Layer 2 — RenderState 管道纯逻辑测试（已验证可行）

**核心发现（2026-06-09 实验验证）**：Bridge 模块中引用 `net.minecraft.*` 类型（`RenderType`、`ResourceLocation` 等）的测试无法在 plain JUnit 中运行——MC 类的静态初始化器需要完整的 Forge 类加载环境。因此 **Layer 2 的测试必须放在 domain 模块中，测试不引用 MC 类型的纯逻辑链路**。

**已验证的可行方案**：在 `eyelib-material:test` 中测试完整的纯逻辑管道：

```
BrMaterialEntry → BrMaterialResolver.resolve() → ResolvedBrMaterial
  → BrRenderStateFactory.from() → BrRenderState
```

这是语义映射的核心引擎。`BrRenderState` 的 `transparency/cull/surfaceClass/writeMask` 字段直接决定了最终的 `PortRenderPass`，而 `BrRenderState → PortRenderPass` 的转换是纯 switch 语句（`BrRenderTypeFactory.toPortPass`），不引入新的语义。

**已实现的测试**（`eyelib-material/src/test/.../BrRenderStateSpecTest.java`，10 tests）：

| # | 材质 | 验证 |
|---|------|------|
| 1 | entity | Transparency.NONE + cull=true + isSolid |
| 2 | entity_alphablend | Transparency.BLEND + cull=true |
| 3 | entity_nocull | Transparency.ALPHA_TEST + cull=false |
| 4 | entity_beam_additive | Transparency.ADDITIVE + cull=false + writeDepth=false + SurfaceClass.ADDITIVE |
| 5 | entity_alphatest | Transparency.ALPHA_TEST + cull=true + SurfaceClass.CUTOUT |
| 6 | emissive | ALPHA_TEST + USE_EMISSIVE → EMISSIVE_CUTOUT (非 CUTOUT) |
| 7 | entity_glint | GLINT → SurfaceClass.GLINT（优先级最高） |
| 8 | 独立材质 | NONE + cull=true + isSolid |
| 9 | entity_nocull customType | needsCustomRenderType=false（仅改 cull 不触发） |
| 10 | entity_beam_additive customType | needsCustomRenderType=true（非默认 blend） |

✅ `:eyelib-material:test` 全绿：28（已有 BrMaterialResolverSpecTest）+ 7（已有 BrRenderStateSpecTest）+ 10（新增）= 45 tests。

**不能测试的 Bridge 适配器**（需要 Forge 类加载）：

| Bridge 类 | 原因 | 纯 JUnit | clientsmoke (MC 进程内) |
|-----------|------|----------|------------------------|
| `RenderPassAdapter.toRenderType()` | 调用 MC 静态工厂 | ❌ | ✅ |
| `ResourceLocationBridge` | 转换涉及 MC 类型 | ❌ | ✅ |
| `EntityPortAdapter.from()` | 需 MC Entity instance | ❌ | ✅ |
| `BrRenderTypeFactory.create()` | 需 MC RenderType | ❌ | ✅ |

### Layer 2b — Bridge 适配器 clientsmoke 测试（MC 进程内）

Bridge 模块中引用 MC 类型的测试不能在 plain JUnit 运行，但可以在 **clientsmoke** 框架中运行——该框架在 MC 客户端加载后通过 `@ClientSmoke` 注解发现测试类，在 Phase 4（world 已加载）执行。

**模式**（参照 `AttachableSmoke`）：

```java
@ClientSmoke(description = "验证 RenderPassAdapter 全链路 → MC RenderType", priority = 10)
public class RenderPassAdapterSmoke {
    public RenderPassAdapterSmoke() {
        // 0. 加载 .mcpack 数据（数据在 eyeilib 资源路径中，MC 已加载）
        var materials = MaterialManager.INSTANCE.getAllData();
        
        // 1. 验证 entity → SOLID
        var pass1 = PortRenderPass.of(Transparency.SOLID, false);
        RenderType rt1 = RenderPassAdapter.toRenderType(pass1, 
            PortResourceLocation.of("minecraft", "textures/entity/test"));
        assertEquals(RenderType.entitySolid(ResourceLocationBridge.toMc(...)), rt1);
        
        // 2. 验证 entity_nocull → ALPHA_TEST + DisableCulling
        var entry = materials.get("entity_nocull:entity");
        assertNotNull(entry, "entity_nocull not loaded from .mcpack");
        var pass2 = RenderTypeResolver.resolve(
            PortResourceLocation.of("minecraft", "textures/entity/slime"), entry, materials);
        assertEquals(Transparency.ALPHA_TEST, pass2.transparency());
        assertTrue(pass2.disableCulling());
    }
}
```

**优势**：
- 使用真实 MC 类型，不需要 mock
- 使用真实 .mcpack 数据（MC 资源重载后已加载）
- 通过 `eyelib_debug_launch` → `eyelib_debug_enter_world` 自动执行
- 输出 JSON 报告，CI 可解析

### Layer 3 — 组件接线测试（需小幅重构）

**核心思想**：将 `EntityRenderSystem.setupClientEntity` 中的 Manager 单例引用替换为 Port 接口注入。

**当前耦合**：
```java
// EntityRenderSystem.setupClientEntity() 中的全局单例调用
BrClientEntity clientEntity = ClientEntityManager.INSTANCE.get(entityId.toString());
RenderControllerEntry rcEntry = RenderControllerManager.INSTANCE.get(rcName);
rcEntry.setupModel(scope, ce, clientEntityComponent.getModels(), slot, actions);
```

**目标**：提取 Port 接口，在测试中注入 Fake。

```java
// domain 模块定义 Port
public interface PortClientEntityStore {
    @Nullable BrClientEntity get(String entityId);
}
public interface PortRenderControllerStore {
    @Nullable RenderControllerEntry get(String name);
}
public interface PortModelStore {
    @Nullable Model get(String modelId);
}
```

**Fake 实现**（在 test scope 中）：
```java
class FakeClientEntityStore implements PortClientEntityStore {
    private final Map<String, BrClientEntity> store = new HashMap<>();
    void put(String id, BrClientEntity ce) { store.put(id, ce); }
    public BrClientEntity get(String id) { return store.get(id); }
}
```

**测试模式**：
```java
@Test
@DisplayName("System §setupClientEntity: slime → 2 ModelComponents with correct render types")
void slimeEntityCreatesCorrectModelComponents() {
    // Arrange: 加载 slime .mcpack 数据
    BrClientEntity slimeCE = parseJson("slime.client_entity.json");
    RenderControllerEntry slimeRC = parseJson("slime.render_controller.json");
    
    FakeClientEntityStore ceStore = new FakeClientEntityStore();
    ceStore.put("minecraft:slime", slimeCE);
    FakeRenderControllerStore rcStore = new FakeRenderControllerStore();
    rcStore.put("controller.render.slime", slimeRC);
    
    RenderData<LivingEntity> cap = createTestRenderData();
    cap.setScope(new MolangScope()); // 注入 scope
    
    // Act: 运行 setupClientEntity（使用注入的 Fake stores）
    EntityRenderSystem.setupClientEntity(entityId, cap, ceStore, rcStore);
    
    // Assert
    List<ModelComponent> comps = cap.getModelComponents();
    assertEquals(2, comps.size(), "slime 应有 2 层：内层 body + 外层 wool");
    
    ModelComponent inner = comps.get(0);
    assertEquals("geometry.slime", inner.getSerializableInfo().model().getPath());
    // 验证材质路由: entity_alphatest → ALPHA_TEST + DisableCulling
    assertFalse(inner.isSolid(), "slime body 应为 alpha test 半透明");
}
```

### 不做的

1. **不 Mock MC Entity/LivingEntity**——这类 mock 维护成本极高且不可靠。EntityPortAdapter 用真实 MC 类测试（它本来就在 bridge 中），或直接手动构造 PortEntity。
2. **不引入 GameTest**——GameTest 对 eyelib 的渲染验证帮助为零（无法访问 GPU 状态），对启动/事件注册验证的收益与 HeadlessMc 重叠但成本更高。
3. **不创建通用 ECS Test Framework**——Bevy 风格的 `World::run_system_once()` 在 MC 环境中过度工程。Fake + 依赖注入已经足够。
4. **不修改 domain 模块的现有 spec-based 测试**——它们按 oracle 优先级 (Mojang 文档 > .mcpack > Bedrock Wiki) 验证纯逻辑，已经正确。

## Consequences

### Positive

- **Layer 2 可立即执行**：`RenderTypeResolver`、`RenderPassAdapter`、`ResourceLocationBridge` 已有纯函数结构，写测试不需要任何重构
- **测试 oracle 正确**：Bridge 层的测试 oracle 来自 domain 层的已验证规范（如 `BrRenderState.Transparency.ALPHA_TEST` 的定义已在 spec 测试中验证）
- **Fake 可复用**：FakeClientEntityStore / FakeRenderControllerStore 同时服务 Layer 3 测试和 Layer 2 测试
- **CI 可运行**：所有 Layer 2 和 Layer 3 测试在标准 JUnit 中运行，0 秒启动时间
- **排除视觉依赖**：不需要启动 MC，不需要 RenderDoc，不需要 `/eval`

### Negative / Risk

- **Manager Port 提取涉及 Root 模块重构**——`EntityRenderSystem`、`ModelComponent` 等 Root 文件需要修改。缓解：每个 Port 提取在一个独立的 PR 中完成，编译 + 现有测试全绿后再合入。
- **Fake 与 Real 行为偏离**——缓解：Fake 必须通过与 Real 实现相同的 Contract Test。如 `PortClientEntityStore` 契约测试同时运行在 Fake 上和在真实 `ClientEntityManager` 上（后者通过 `runClient` 加载 .mcpack 验证）。

## Verification

- [x] Layer 2 管道测试：`BrRenderStateSpecTest` 10 tests — `:eyelib-material:test` ✅
- [x] 已确认 Bridge 适配器（RenderPassAdapter/ResourceLocationBridge/EntityPortAdapter）不能在 plain JUnit 中测试
- [x] 已删除 Bridge 模块中无法运行的测试文件
- [ ] Layer 3 预备：提取 `PortClientEntityStore` / `PortRenderControllerStore` / `PortModelStore` 接口
- [ ] Layer 3 第一批：`EntityRenderSystem.setupClientEntity` 接线测试（slime, vex, warden）
- [ ] ArchUnit 验证：Fake 实现在 domain 模块的 test scope 中，不 import MC
- [ ] Gradle `:eyelib-material:test` + `:eyelib-molang:test` + `:eyelib-bridge:test` 全绿

## Related

- ADR-0010: 六边形架构 — 本 ADR 的 Layer 3 正是 ADR-0010 "行为离线验证" 的缺失部分
- ADR-0011: 文档设计基线 — 本测试策略文档按 Diátaxis 归入 `docs/decisions/`
- `docs/architecture/acceptance-gates.md` — G2 (spec-test) 需更新：纳入 Bridge Contract Test 到 Gate
