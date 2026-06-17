# Spec-Based 测试方法论

> 从 2026-06-08 hexagonal architecture 重构中验证的测试编写模式。

## 核心原则

测试的 oracle 来自 **Bedrock 原始规范**，不来自当前代码的输出。一个测试失败意味着"代码偏离了 Bedrock 规范"，不是"代码行为变了"。

## Oracle 优先级

1. **Mojang Creator 文档**（`/mnt/e/_____基岩版文档/minecraft-creator/creator/Documents/`）— 权威
2. **真实 .mcpack 数据**（`run/resourcepacks/*.mcpack`）或 Microsoft 官方示例 fixture — 权威
3. **Bedrock Wiki**（`/mnt/e/_____基岩版文档/bedrock-wiki/docs/`）
4. **项目内部 pitfall/ADR/reference** — 二次加工，可能滞后，仅作参考不作出处

## 已在项目中建立的 spec-based 测试

### material: BrMaterialResolverSpecTest (28 tests) + BrRenderStateSpecTest (10 tests)

Oracle: Mojang `material-files.md`。

**BrMaterialResolverSpecTest** — 材质继承链语义验证（28 tests）：
- 继承、+defines/-defines、+states/-states、别名、beam additive 模式、循环检测

**BrRenderStateSpecTest** — 渲染状态管道验证（11 tests，新增 2026-06-09）：
- 核心洞察：Bridge 模块测试无法在 plain JUnit 中运行（MC `RenderType`/`ResourceLocation` 类需要 Forge 类加载环境）。
- 解决方案：**在 domain 模块中测试完整纯逻辑管道** `BrMaterialEntry → BrMaterialResolver → BrRenderStateFactory → BrRenderState`
- BrRenderState 的 `transparency/cull/surfaceClass/writeMask` 字段直接决定最终 PortRenderPass，`BrRenderState → PortRenderPass` 转换是纯 switch 语句不引入新语义
- 覆盖：entity SOLID → alphablend BLEND → 真实 entity_nocull (仅 DisableCulling) → 假想 ALPHA_TEST+DisableCulling → beam_additive ADDITIVE → alphatest → emissive CUTOUT → glint → standalone → needsCustomRenderType

### behavior: BehaviorEntitySpecTest（5 tests）

Oracle: Mojang `EntityBehaviorIntroduction.md` — 实体 JSON 结构、components、component_groups、events。

使用人工构造的 JSON（匹配 Bedrock 格式）做 CODEC 往返测试。

### particle: BrParticleSpecTest（4 tests）

Oracle: Microsoft shapeshifter 官方示例 `witchspell.json` — 真实 Bedrock 粒子 JSON。

```java
@Test
@DisplayName("Mojang §ParticleEffects: 真实 witchspell 粒子解析")
void realWitchspellParses() throws Exception {
    String json = loadFixture("witchspell.json");
    var result = BrParticle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
    assertTrue(result.result().isPresent());
}
```

## 测试命名规范

- 类名: `<Subject>SpecTest`
- 测试名: `Mojang §<章节>: <语义描述>`
- 断言注释解释**为什么**期望值正确（引用 Mojang 文档原文）

## 反模式（禁止）

- 从项目内部 pitfall 记录中提取"规范"写测试
- 测试名不含 Mojang 文档引用
- 人工构造的 JSON 与 Bedrock 格式不匹配（缺少 `minecraft:entity` 包装等）
- 测试 pin 当前实现行为（如 `assertThrows(LinkageError.class, ...)` 在非 MC 环境中测试 MC 类加载失败）
- **在 Bridge 模块中写引用 MC 类型的 JUnit 测试** — `RenderType`/`ResourceLocation` 等 MC 类的静态初始化器在 plain JUnit 中失败（`ExceptionInInitializerError`）。Bridge 适配器的正确性通过 domain 管道测试 + RenderDoc 截帧验证

## Bridge 测试边界（2026-06-09 实验确认）

| 测试类型 | 运行环境 | 适用场景 |
|---------|---------|---------|
| domain 纯逻辑管道 | 纯 JUnit (Gradle `:test`) | BrMaterialResolver, BrRenderStateFactory, Molang 求值 |
| Port 契约测试 | 纯 JUnit | PortEntity, PortRenderPass 接口行为 |
| Bridge 适配器 | clientsmoke (MC 进程内) | RenderPassAdapter, EntityPortAdapter, ResourceLocationBridge |
| 组件接线测试 | clientsmoke (MC 进程内) | EntityRenderSystem.setupClientEntity, renderComponents |

**clientsmoke 模式**：注解 `@ClientSmoke(description="...", priority=N)`，在无参构造器中执行所有断言逻辑（throw = fail）。通过 `eyelib_debug_clientsmoke()`（MCP 工具）一键运行——不要手动调用 `eyelib_debug_enter_world`（clientsmoke 自己创建世界）。参照 `src/main/java/io/github/tt432/eyelib/smoke/RenderTypeBridgeSmoke.java`。

## 人工构造数据 vs 真实 .mcpack 数据

2026-06-09 教训：`BrRenderStateSpecTest` 中为 `entity_nocull` 人工加了 `ALPHA_TEST` define，但真实 `.mcpack` 中 `entity_nocull:entity` 只有 `DisableCulling`。这导致 clientsmoke 运行时测试失败（`got SOLID`，因为无 ALPHA_TEST）。

**规则**：
- spec 测试的人工构造数据应**明确标注为 hypothetical**
- 人工数据与真实数据不一致时，以真实 `.mcpack` 数据为准
- 如果真实 data 不支持某个场景但逻辑是正确的，写一个独立的 "hypothetical" 测试并标注
