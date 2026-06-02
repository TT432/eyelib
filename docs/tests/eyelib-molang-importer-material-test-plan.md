# qylEyelib 测试重写计划

---

## 1. eyelib-molang（共 16 个文件）

### Keep（保留，质量好，结构清晰）

| 文件 | 理由 |
|------|------|
| **MolangTernaryConditionalTest** (81L) | 每个 case 只测一个三元表达式场景，one-assert-per-test，边界覆盖完整（true/false/undefined） |
| **MolangFullPipelineTest** (92L) | `@ParameterizedTest @CsvSource` 驱动，数据与逻辑分离，每个测试方法只测一个操作族 |
| **MolangCompilerImplHandsOnQaTest** (61L) | 每个方法独立验证一个 compiler 行为，断言少且聚焦 |
| **MolangParserFrontendDivergenceTest** (45L) | `@ParameterizedTest @ValueSource` 简洁驱动，纯粹的前端解析分歧验证 |
| **MolangCallableDiscoveryRoleContractTest** (105L) | 每个方法验证一个约定角色场景，role metadata 验证清晰，`@AfterEach` 清理状态 |
| **MolangQueryVariantSelectionMatrixContractTest** | Contract-test 风格，每个 case 测一个变体选择维度 |
| **MolangCallableVariantSelectionAmbiguityContractTest** | 同上，专注模糊性场景 |
| **MolangCallablePublicationSignatureRoleTest** | 专注签名角色发布约定 |
| **MolangHostPublicationDeterminismConflictTest** | 专注主机发布冲突检测 |
| **MolangMcAdapterSeamTest** | Seam test，验证 MC 适配边界 |
| **MolangCorpusLinterTest** | 语料库 lint 行为验证，简洁聚焦 |

### Delete（删除，测试价值低或重复）

| 文件 | 理由 |
|------|------|
| — | 无直接可删除文件 |

### Rewrite（重写）

| 文件 | 问题诊断 | 改造建议 |
|------|----------|----------|
| **MolangMathTest.trigonometryAndClampStayPlainJvmAndStable** | **Eager Test**: 一个方法测了 5 个独立函数（sin, cos, atan2, clamp×2）。失败时无法定位哪个函数出错。 | 拆成 5 个独立测试方法（`sin30`, `cos60`, `atan2`, `clampMax`, `clampMin`），每个方法只测一个函数、一个断言。保留 `duplicateClassRegistrationIsDeduped...` 方法不变（它已经是 one-concept-per-test）。 |
|| **MolangCorpusHarnessTest** | **Mystery Guest**（高）：从 classpath 读取 golden 文件做断言对比，无自动重录机制。硬编码了 36 个 case ID 到测试代码中。一旦 corpus 增加 case，测试必须同步修改。 | **(A) 短期方案**：用 `@MethodSource` 动态加载 corpus case ID，从 golden 目录自动发现，避免硬编码。 **(B) 长期方案**：实现 CI 自动重录模式。具体实现：<br>1. 在测试类中定义 `private static boolean recordMode = Boolean.getBoolean("molang.corpus.record");`<br>2. 测试执行时若 `recordMode==true`，将实际输出直接覆写 golden 文件，**不进行断言比较**（跳过 `assertEquals` 或条件分支：`if (recordMode) { writeGolden(actual); return; }`）<br>3. 在 `@AfterAll` 或 `afterAll()` 方法中遍历所有已记录的 golden 写入：将内存中收集的实际输出批量化写回 `src/test/resources/molang/corpus/` 下的对应 `.golden` 文件<br>4. 用法：`mvn test -Dmolang.corpus.record=true` 或 Gradle `-Dmolang.corpus.record=true`，运行后 Git diff 确认变更并提交<br>5. 注意写回时保留原文件目录结构，使用与读取相同的 `PathResolver` 逻辑确保一致性<br><br>**(C) 最低成本方案**：将 case ID 列表提取到独立的 `corpus-cases.json` 数据文件，用 `@MethodSource("loadCorpusCasesFromJson")` 加载，减少代码侵入。 |
| **MolangDiskCacheTest** | **Mystery Guest**: `@TempDir` 做真实文件 I/O，并发测试 `concurrentReadWrite` 用 10 线程写入真实文件系统。依赖底层文件系统行为和线程调度，CI 中可能因 I/O 竞争产生偶发失败。 | **molecule 化改造**：将文件 I/O 抽象为 `CacheStorage` 接口，测试时注入 `InMemoryCacheStorage`。保留 1-2 个集成测试验证真实文件 I/O，其余测试用内存实现。`concurrentReadWrite` 改为用 `InMemoryCacheStorage` + `ExecutorService`，避免真实文件系统竞态。 |
| **HandwrittenMolangAstParserFrontendTest** | （未完整阅读，推测是解析器 frontend 的逐 case 测试） | 确认是否与 `MolangParserFrontendDivergenceTest` 重叠。如有重复测试的逻辑，合并或删除重复场景。 |

### New（新建）

| 测试 | 理由 |
|------|------|
| **MolangExpressionEvaluatorFuzzTest** | 模糊测试：随机生成 Molang 表达式，确保 compiler 不 crash、返回有限浮点数。可沿用现有 `MolangCompilerImpl` + `MolangScope` 基础设施。 |
| **MolangBindingEdgeCaseTest** | 边界绑定场景：空字符串、超长表达式、非法 UTF-8、嵌套过多结构体等。当前测试覆盖率不够。 |
| **MolangScopeQueryResolveContractTest** | query 函数解析 + 调用约定的 contract 测试（与 `MolangCallableDiscoveryRoleContractTest` 互补但视角不同）。 |

### Priority 排序

1. **P0 - 立即做**: `MolangMathTest.trigonometryAndClampStayPlainJvmAndStable` 拆分（改动最小，风险最低，影响最直接）
2. **P1 - 本周做**: `MolangCorpusHarnessTest` 改造（移除 case ID 硬编码 + golden 重录机制）
3. **P1 - 本周做**: `MolangDiskCacheTest` molecule 化（提取 `CacheStorage` 接口 + 将大部分测试转为内存实现）
4. **P2 - 本月做**: 新增 `MolangBindingEdgeCaseTest` + `MolangExpressionEvaluatorFuzzTest`
5. **P3 - 可推迟**: 确认 `HandwrittenMolangAstParserFrontendTest` 与 divergence test 的重复情况

---

## 2. eyelib-importer（共 10 个文件）

### Keep（保留）

| 文件 | 理由 |
|------|------|
| **ImportedImageDataTest** (201L) | **最佳实践标杆**：每个方法只测一种 TGA 格式变体（24bpp B2T, 32bpp alpha, RLE, RLE run, T2B, null, tooShort, unsupportedType）。数据驱动、边界覆盖全、helper 函数复用良好。 |
| **BedrockModelLoaderTest** | （推测与模型加载器配合良好，保留确认） |
| **BedrockImportedModelDataTest** | 保留确认 |
| **BrClientEntityCodecTest** | 实体编解码测试，保留确认 |
| **BlockbenchModelCompatibilityTest** | Blockbench 兼容性测试，保留确认 |
| **ImportedModelTextureRepackerTest** | 纹理重组测试，保留确认 |
| **BedrockAddonActionsAndStuffCoverageTest** | 动作覆盖测试，保留确认 |

### Delete（删除）

| 文件 | 理由 |
|------|------|
| — | 无直接可删除文件（BedrockAddonLoaderTest 虽有大量断言但仍有测试价值，需重构而非删除） |

### Rewrite（重写）

| 文件 | 问题诊断 | 改造建议 |
|------|----------|----------|
|| **BedrockAddonLoaderTest** (813L) | **Eager Test + Assertion Roulette 最严重**：`loadsFolderAddonWithResourceAndBehaviorPacks` 单方法包含 ~30 个断言，分别验证 models, clientEntities, attachables, animations, animationControllers, renderControllers, particles, textures, materials, textureIndexFiles 等。失败时完全无法定位哪个资源未加载。同样问题存在于 `loadsMcaddonContainingNestedMcpacks`(~20断言)、`keepsTextureIndexFilesManagedInsteadOfUnmanaged`(~10断言)、`loadsManagedParticleWithoutUnmanagedFallback`(~10断言) 等方法。 | **分拆策略**（保持单文件，拆成 12 个独立方法，每类资源一个）：<br>1. **`loadsFolderAddonWithResourcePacks`** — 只验证 pack 计数 (2 packs, 1 resource, 1 data)<br>2. **`folderAddonMapsEntityFiles`** — 只验证 entity 相关资源加载<br>3. **`folderAddonMapsAnimationFiles`** — 只验证 animation + animation_controllers<br>4. **`folderAddonMapsRenderControllers`** — 只验证 render_controllers<br>5. **`folderAddonMapsParticleFiles`** — 只验证 particles + particlesByIdentifier<br>6. **`folderAddonMapsTextureFiles`** — 只验证 textures<br>7. **`folderAddonMapsMaterialFiles`** — 只验证 materials + materialEntries<br>8. **`folderAddonMapsTextureIndexFiles`** — 只验证 textureIndexFiles (6个子项各一个断言)<br>9. **`folderAddonLoadsPackIcon`** — 单独验证 packIcon<br>10. **`loadsMcaddonContainingNestedMcpacks`** — 仅验证 mcaddon 结构加载<br>11. **`keepsTextureIndexFilesManagedInsteadOfUnmanaged`** — 仅验证 textureIndex 管理<br>12. **`loadsManagedParticleWithoutUnmanagedFallback`** — 仅验证 particle 无回退<br><br>**Helper 提取策略**（同一文件内部共享）：<br>- 将 `writeResourcePack()`, `writeBehaviorPack()`, `writeString()`, `writePng()`, `writeBrarchive()` 抽为内部私有方法或 `@Nested` 内部类的 helper<br>- 将 `zipDirectory()`, `addFileToZip()` 抽为 `ZipTestHelper`（如果被其他测试文件共用）<br>- JSON 生成方法 (`resourceManifestJson()`, `animationJson()` 等) 抽为常量类 `BedrockAddonFixtureJson`<br><br>**不拆成多个文件**：单文件 12 个独立方法（每类资源一个）比 6 个文件各 2 个方法更易维护。改用一个文件内的多个方法而非多个文件。 |
| **BedrockAddonRealFixtureIntegrationTest** (120L) | **Eager Test + Assertion Roulette**: `assertOfficialFixtureLoaded` 包含 ~35 个断言，验证 pack 计数、clientEntities, attachables, animationControllers, renderControllers, animations, models, particles, textures, sounds, languages, flipbook 参数等。测试方法两个（folder + mcaddon 两种加载方式）共用同一个断言方法。 | 1. 将 `assertOfficialFixtureLoaded` 拆成多个独立断言方法：`assertPackCounts`, `assertEntitiesLoaded`, `assertAnimationsLoaded`, `assertRenderControllersLoaded`, `assertParticlesLoaded` 等<br>2. 或者直接将 fixture 的资源列举改为 keyCount 验证（数据驱动方式）<br>3. mcaddon 和 folder 两种加载方式各保留一个测试方法，调用同样的拆分断言序列 |
| **ImporterModuleIdentityTest** (86L) | **Fragile Test**: 通过相对路径读取 `MODULES.md`, `docs/decisions/0003-side-boundaries.md`, `package-info.java`, `build.gradle`, `mods.toml`, Java 源文件等内容，并断言包含特定字符串。一旦修改文档/构建配置，测试立即失败。 | 1. **移除 Markdown 文档断言**（`moduleDocsDeclareImporterSchemaForgeFunctionalIdentity`）：文档内容不属于测试契约，用 `@link` 或 `@see` 替代<br>2. **构建配置断言保留但简化**（`buildAndBootstrapKeepForgeModShapeExplicit`）：只验证 modId 和 build 插件 ID，不验证具体版本或描述<br>3. **依赖隔离检查保留**（`importerMainSourcesDoNotDependOnRootRuntimePackages`）：这是有价值的架构约束检查，但应（A）限制扫描文件数量（仅扫描几个 core 文件而非所有 Java 文件），（B）用 `@Tag("archtest")` 标记以便 CI 中有选择地执行<br>4. **Minecraft import 范围检查保留**（`directMinecraftAndForgeImportsStayInRootPackage`）：同上 |

### New（新建）

| 测试 | 理由 |
|------|------|
| **BedrockAddonSchemaValidationTest** | schema 验证测试：非法 JSON、缺失必填字段、未知类型的 manifest 模块等边界场景。当前 `BedrockAddonLoaderTest` 只覆盖了部分警告场景。 |
| **BedrockModelLoaderSchemaEdgeCasesTest** | 模型加载器边界：空 geometry 文件、不合法的 bone 结构、uv 越界等。 |

### Priority 排序

1. **P0 - 立即做**: `BedrockAddonLoaderTest` 方法拆分——单文件拆成 12 个独立方法（每类资源一个），不改文件数量（Eager Test 最严重，813L 单个文件最影响可维护性）
2. **P0 - 立即做**: `BedrockAddonRealFixtureIntegrationTest` 拆分 `assertOfficialFixtureLoaded`（35 断言/方法）
3. **P1 - 本周做**: `ImporterModuleIdentityTest` 重构（移除文档断言，保持架构约束检查）
4. **P2 - 本月做**: 新增 `BedrockAddonSchemaValidationTest` + `BedrockModelLoaderSchemaEdgeCasesTest`

---

## 3. eyelib-material（共 15 个文件）

### Keep（保留）

| 文件 | 理由 |
|------|------|
| **EnumCodecTest** (146L) | 数据驱动：`@ParameterizedTest @MethodSource` 覆盖 GLStates/DepthFunc/BlendFactor/VertexFormatElementEnum 所有枚举值。结构清晰，`assertRoundtrip` 通用断言复用。 |
| **BrSamplerStateCodecTest** | sampler state 编解码验证，聚焦且独立 |
| **BrMaterialCodecTest** (94L) | 编解码 roundtrip + entryKey 存在性验证，方法粒度适中 |
| **CircularInheritanceTest** (85L) | 专注循环继承检测：A→B→A, self-reference, 正常链。每个 case 一个断言。极简且有效。 |
| **BrMaterialEntryVariantTest** | variant entry 行为验证，保留确认 |
| **BrMaterialCodecRegressionTest** (92L) | 与 `BrMaterialCodecTest` 高度重叠（几乎相同的 roundtrip 逻辑，输入相同 JSON 但 data provider 不同）。确认后考虑合并。 |
| **BrMaterialCodecIntegrationTest** (115L) | 与 `BrMaterialCodecTest` 重叠度较高（都是 roundtrip + 存在性验证，输入 JSON 不同但逻辑相同）。确认后考虑合并或以不同 JSON 输入区分。 |
| **BrMaterialNewFieldsTest** | 新增字段编解码验证 |
| **DispatchedMapCodecTest** | 分发 map codec 测试 |
| **VertexFormatElementEnumTest** | 顶点格式枚举测试 |

### Delete（删除或合并）

| 文件 | 理由 |
|------|------|
| **BrMaterialCodecIntegrationTest** (115L) | 与 `BrMaterialCodecTest` (94L) + `BrMaterialCodecRegressionTest` (92L) **高度重复**：三者都是 BrMaterial CODEC 的 JSON → BrMaterial → JSON → BrMaterial roundtrip + entryKey 存在性断言。输入 JSON 略有不同（IntegrationTest 用全量 JSON，CodecTest 用 3-entry JSON，RegressionTest 用 3-entry JSON 但结构一致）。建议合并为一个 `BrMaterialCodecRoundtripParametrizedTest`，用不同 JSON fixture 作为参数源。 |

### Rewrite（重写）

| 文件 | 问题诊断 | 改造建议 |
|------|----------|----------|
|| **GLStateApplierTest** (259L) | **9 个 @Disabled 测试，零断言**：所有 9 个测试方法都标注了 `@Disabled("需要 OpenGL 上下文")`，且方法体中没有断言语句，只有 `GLStateApplier.apply(...)` 调用后跟注释 `// Verify: ...`。这些测试在任何环境中都不提供验证价值。 | **方案 C（唯一方案）**：<br>由于 `GLStateApplier` 仅仅是 Minecraft 环境的 GL 包装器，其纯逻辑部分无法在不修改生产代码的前提下提取（不准改生产代码），且 eyelib-material 作为库项目不应负担 GL 集成测试，**直接删除整个测试类**。在 Minecraft 端（root module）中补充 GL 集成测试（如有时需要）。理由：<br>- 9 个 `@Disabled` 零断言测试在任何环境中都不执行有效验证<br>- 提取纯逻辑需要修改 `GLStateApplier` 生产代码——不准改<br>- 无 GL 环境的方法是库级测试的死胡同<br>- 删除后不影响任何可运行的测试覆盖率 |
| **ShaderManagerIntegrationTest** (218L) | **Fragile Test + 反射访问私有方法**：<br>1. 反射调用 `ShaderManager.injectDefines()` 和 `ShaderManager.buildCacheKey()` 私有方法（line 50-55），当实现重构时测试立刻失效<br>2. 6 个非 `@Disabled` 方法已覆盖 injectDefines 和 buildCacheKey 的行为<br>3. 3 个 `@Disabled("Requires GL context")` 方法在无 GL 环境无价值 | 1. **合并到 ShaderManagerTest**：将反射测试的 6 个方法（`injectDefines_noDefines`, `injectDefines_nullDefines`, `injectDefines_withDefines`, `injectDefines_withVersion`, `buildCacheKey_differentDefines`, `buildCacheKey_sameInputs`）移到 `ShaderManagerTest`<br>2. **重构私有方法为 public 或 package-private**：如果 `injectDefines` 和 `buildCacheKey` 有必要单独测试，应改为可见性包级别或 public 而非反射调用<br>3. **删除或标记**：3 个 `@Disabled(GL)` 方法移到 `ShaderManagerGLTest`（用 `@Tag("gl-integration")` 标记）<br>4. `loadFromResource` 测试已有 `ShaderManagerTest` 覆盖，保留不重复 |
| **ShaderManagerTest** (113L) | 4 个 `@Disabled("Requires GL context")` 方法 + 3 个 GL 集成测试方法有冗余。`loadFromResource_validPath_returnsContent` 用 `mods.toml` 作为资源路径（耦合到 Minecraft 构建产物结构）。 | 1. 将 `loadFromResource` 测试改为用真实 shader 文件（`pass_through.vert` 已存在于测试资源中）<br>2. 删除 `@Disabled(GL)` 方法（与 `ShaderManagerIntegrationTest` 重复）<br>3. 纯资源加载测试 + `injectDefines`/`buildCacheKey` 逻辑测试合并到此文件 |
| **MaterialEndToEndTest** (276L) | 整体质量中等。`testManagerPutGetRoundtrip` 用 `HashMap` 模拟 MaterialManager，但注释说明「圆依赖无法访问 MaterialManager」，意味着这个测试验证的是 Map 的 put/get 行为而非真实 manager。`testInheritance` 跨越多个材质变体验证，每个变体断言 2-4 个字段。 | 1. **移除 `testManagerPutGetRoundtrip`**：它在测试 HashMap 而非业务逻辑。企业数据的编解码已有 `BrMaterialCodecTest` + `BrMaterialCodecIntegrationTest` 覆盖<br>2. **`testInheritance` 拆分**：拆成 4 个独立测试方法（`alphatestInheritsFromEntity`, `nocullInheritsFromEntity`, `alphablendInheritsFromEntity`, `particlesBlendStandalone`），每个方法聚焦一个子材质变体<br>3. `testVariantLookup` 和 `testCODECParsing_all9Entries` 保留不变 |
| **BrMaterialEntryRenderTypeTest** (123L) | （未完整阅读但需评估）可能依赖于 Minecraft `RenderType` 类，属于 Minecraft 集成测试。如果无 GL 环境被 `@Disabled` 或抛异常，则需与 GLStateApplierTest 一样处理。 | 评估后决定是否拆分成 logic-only + Minecraft-integration 两部分。 |

### New（新建）

| 测试 | 理由 |
|------|------|
| **BrMaterialStateCombinationTest** | GL 状态组合测试：确保 `Blending + Wireframe + DisableDepthTest` 等多状态组合按 Bedrock 管线顺序正确执行。纯逻辑测试，无需 GL 上下文。 |
| **BrMaterialInheritanceResolveTest** | 集中测试材质继承链解析：多层继承、覆盖语义（`+` 前缀 vs `=` 覆盖）、missing base 异常等。当前分散在 `MaterialEndToEndTest.testInheritance` 和 `CircularInheritanceTest` 中，可抽取为专用的 inheritance resolve 测试套件。 |

### Priority 排序

1. **P0 - 立即做**: `GLStateApplierTest` 删除——9 个 @Disabled 零断言测试，提取纯逻辑需改生产代码（不准改），直接删除整个测试类
2. **P0 - 立即做**: 合并 `BrMaterialCodecIntegrationTest` + `BrMaterialCodecTest` + `BrMaterialCodecRegressionTest` 去重
3. **P1 - 本周做**: `ShaderManagerIntegrationTest` 反射测试移至 `ShaderManagerTest`，将私有方法改为包可见
4. **P1 - 本周做**: `MaterialEndToEndTest` 重构（移除 Map 测试，拆分 inheritance 方法）
5. **P2 - 本月做**: 新增 `BrMaterialStateCombinationTest` + `BrMaterialInheritanceResolveTest`

---

## 跨模块共享建议

| 事项 | 描述 | 优先级 |
|------|------|--------|
| **@TempDir + 真实 I/O 标准化** | MolangDiskCacheTest, BedrockAddonLoaderTest, BedrockAddonRealFixtureIntegrationTest 都使用 `@TempDir` 做文件 I/O。建议各模块统一使用 `InMemoryStorage` 抽象层 + 1-2 个集成测试验证真实 I/O。 | P2 |
| **反射访问私有方法禁止** | ShaderManagerIntegrationTest 反射调用私有方法。建议约束：不允许在新测试中反射调用被测类的私有方法，现有反射测试应优先改为提高方法可见性。 | P1 |
| **文档/元数据文件断言约束** | ImporterModuleIdentityTest 断言 MODULES.md 等文档内容。建议约束：不允许在测试中断言文档文件内容（`@link` 和 `@see` 替代），允许但谨慎地断言构建配置关键属性。 | P2 |
