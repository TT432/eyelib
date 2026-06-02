# Root 边界测试 + eyelib-util + eyelib-attachment 测试重写计划

> 生成日期: 2026-06-02
> 审计范围: root `src/test/` 边界测试（~10 文件）+ eyelib-util（4 文件）+ eyelib-attachment（3 文件）
> 原则: **不改任何 Java 代码**，仅输出 Keep/Delete/Rewrite/New 判定

---

## 模块一：Root 边界测试（src/test/）

### 1.1 ParticleApiDelegationBoundaryTest
| 属性 | 值 |
|---|---|
| 文件 | `src/test/java/.../particle/ParticleApiDelegationBoundaryTest.java` |
| 方法数 | 2 |
| 行数 | 120 |
| 模式 | 读 5+ 源文件 + 2 文档，20+ 子串断言 |
| 判定 | **Rewrite** |
| 优先级 | **P0** |

- **C2 (Fragile Test — Data Sensitivity + Interface):** 断言 `ParticleSpawnService.java` 中的精确 import 语句和方法签名，源文件每重构一次测试必红
- **C5 (Obvious Test):** "transitional" 注释、"this facade after..." 注释断言，测试验证文档注释
- **建议方向:** 删除 import 级别断言，改为 `Class.forName()` 验证模块不依赖类；deleted-facade 断言改为 compile-time check；文档锚点断言移到集中式的 `DocumentationAnchorTest`

### 1.2 ParticleFinalSplitBoundaryTest
| 属性 | 值 |
|---|---|
| 文件 | `src/test/java/.../particle/ParticleFinalSplitBoundaryTest.java` |
| 方法数 | 5 |
| 行数 | 153 |
| 模式 | 读源文件 + 正则 + 文件存在 + 全树遍历 |
| 判定 | **Delete** (部分语义 → Rewrite 到其他文件) |
| 优先级 | **P0** |

- **C2 (Fragile Test — Data+Interface):** 方法体内 `Files.walk` 整个 `src/main/java` 然后过滤每个 .java 文件，匹配方法调用字符串 → 源代码级行为耦合
- **C5 (Obvious Test):** `rootLegacyComponentsAreDeleted` 的 regex 反模式（拼字符串避免 CI 误触）
- **C7 (Unclear / Test Smoke):** `normalFinalGateTestsDoNotReadPlanningArtifacts` 断言测试文件自身不含 "planning/" → meta 断言
- **建议:** 删除；`packetContractsBelongToParticleModule` 的包声明 + record shape + delta/codec 语义可重写为独立 DTO/Codec 单元测试；legacy deletion 断言用编译期 `@Deprecated` + `--release` 替代

### 1.3 ParticleRuntimeDelegationBoundaryTest
| 属性 | 值 |
|---|---|
| 文件 | `src/test/java/.../particle/ParticleRuntimeDelegationBoundaryTest.java` |
| 方法数 | 4 |
| 行数 | 142 |
| 模式 | 读源文件 + JSON 发布 + 运行时断言 |
| 判定 | **Keep** (method 1) + **Delete** (method 2–4) |
| 优先级 | **P0** |

- Method 1 (`spawnServiceBuildsModuleRuntimeAndDelegatesToModuleRenderManager`): 混合源文件断言（C2）和 JSON 发布 + `ParticleDefinitionRegistry` 运行时断言（C3 可接受）。**Rewrite 后半段为纯单元测试，删除前半段源文件断言**
- Method 2 (`animationParticleEffectsResolvePublishedModuleDefinitions`): 读三个源文件做字符串断言。**Delete**
- Method 3 (`legacyRootEmitterRuntimeAndSchemaTreeAreDeleted`): 文件存在断言。**Delete**
- Method 4 (`spawnAndRemovePacketShapesRemainStringKeyed`): 正则 + 字符串断言。**Delete**（由 `ParticleFinalSplitBoundaryTest` 变体 + codec 测试覆盖）

### 1.4 ParticleSpawnServiceBoundaryTest
| 属性 | 值 |
|---|---|
| 文件 | `src/test/java/.../particle/ParticleSpawnServiceBoundaryTest.java` |
| 方法数 | 3 |
| 行数 | 80 |
| 判定 | **Delete** |
| 优先级 | **P0** |

- **C2 (Fragile Test):** 三个方法全部读源文件做 import 字符串断言（`.contains("import io.github.tt432.eyelibparticle.api.ParticleSpawnApi;")`）
- **建议:** 与 `ParticleApiDelegationBoundaryTest` 合并，替换为编译期验证（`--add-exports` + `--add-reads` 模块隔离检查）

### 1.5 RenderMixinAccessorOwnershipTest
| 属性 | 值 |
|---|---|
| 文件 | `src/test/java/.../render/RenderMixinAccessorOwnershipTest.java` |
| 方法数 | 1 |
| 行数 | 57 |
| 判定 | **Delete** |
| 优先级 | **P1** |

- **C2 (Fragile Test):** 读 accessor 源文件 + mixin JSON + 4 个文档，断言具体字符串
- **C3 (Weak Test):** 核心语义应是 mixin 配置中声明的 accessor 类都能被加载或 accessor 方法签名匹配
- **建议:** 删除。Reflection/ASM 验证仍是 Fragile Test——只是换了脆弱类型，没有可靠替代方案

### 1.6 EyelibParticleCommandBoundaryTest
| 属性 | 值 |
|---|---|
| 文件 | `src/test/java/.../command/EyelibParticleCommandBoundaryTest.java` |
| 方法数 | 2 |
| 行数 | 50 |
| 判定 | **Delete** |
| 优先级 | **P1** |

- **C2 (Fragile Test):** 16 个子串断言全部依赖 `EyelibParticleCommand.java` 源文件
- **建议:** 删除。命令的结构性验证可改为注册表集成测试（注册命令 → 解析参数 → 验证 packet 构建）；import 层次用编译隔离保证

### 1.7 ParticleCommandNetworkDocumentationTest
| 属性 | 值 |
|---|---|
| 文件 | `src/test/java/.../docs/ParticleCommandNetworkDocumentationTest.java` |
| 方法数 | 2 |
| 行数 | 64 |
| 判定 | **Delete** |
| 优先级 | **P1** |

- **C2 (Fragile Test):** 读 6 个文档文件断言关键词（"Phase 13", "command/network integration" 等）
- **建议:** 删除。文档关键词验证没有自动测试价值，应通过 doc review checklist 靠人工保障

### 1.8 ParticleFinalDocumentationGateTest
| 属性 | 值 |
|---|---|
| 文件 | `src/test/java/.../docs/ParticleFinalDocumentationGateTest.java` |
| 方法数 | 2 |
| 行数 | 103 |
| 模式 | `Map.ofEntries` 存储 5 个文档 × ~10 锚点 = 60+ 断言 |
| 判定 | **Delete** |
| 优先级 | **P0** |

- **C2 (Fragile Test):** 60+ 硬编码锚点字符串，文档改一句话测试就断
- **C7 (Unclear / Test Smoke):** `finalDocumentationGateSourceReadsStableDocsOnly` meta 自检
- **C4 (Implicit Assert):** 批量 `assertAll` + stream map，断言失败只能知道"哪个锚点缺失"，文档维护者无法快速定位
- **建议:** 删除。文档完整性通过 CI 中的 Markdown lint + link checker 保障

### 1.9 NetworkOwnershipBoundaryTest
| 属性 | 值 |
|---|---|
| 文件 | `src/test/java/.../network/NetworkOwnershipBoundaryTest.java` |
| 方法数 | 5 |
| 行数 | 116 |
| 判定 | **Delete** (methods 1, 3–5) + **Keep** (method 2) |
| 优先级 | **P0** |

- Method 1 (`rootNetworkOwnsOnlySharedEntrypointsAndDelegation`): 读 2 个源文件断言 import/方法。→ **Delete**（reflection 验证仍是 Fragile Test，没有可靠替代方案）
- Method 2 (`featureOwnedPacketContractsStayOutOfRootNetworkPackage`): `Files.walk` 查文件名 → 合理的结构性检查，但可用编译期 `package-list` 或 module-info 替代。→ **Keep** （改为路径命名规则的更稳定版本）
- Method 3 (`transportOwnsChannelContextInEyelibNetworkModule`): 读 `EyelibNetworkTransport.java` 断言 6 个字符串。→ **Delete**（reflection 验证仍是 Fragile Test，没有可靠替代方案）
- Method 4 (`rootCoupledPacketsDocumentedInRegistryLookup`): 读 4 个 attachment packet 源文件断言调用。→ **Delete**（集成测试应验证实际编解码）
- Method 5 (`docsLockFm014NetworkResponsibility`): 读 5 个文档断言 FM-014 等。→ **Delete**

### 1.10 BedrockAddonRuntimeBridgeTest
| 属性 | 值 |
|---|---|
| 文件 | `src/test/java/.../loader/BedrockAddonRuntimeBridgeTest.java` |
| 方法数 | 1 |
| 行数 | 149 |
| 模式 | 跨 6 个 manager 的集成断言 |
| 判定 | **Rewrite** |
| 优先级 | **P1** |

- **C6 (Eager Test):** 一个方法测 `replaceFromAddon` → 同时验证 AnimationManager, ClientEntityManager, AttachableManager, ModelManager, MaterialManager, RenderControllerManager
- **建议:** 拆分为多个专注测试，每方法仅验证 1–2 个 manager。核心集成保留 1 个"smoke"，其他拆单

### 1.11 新增测试建议（New）

| 建议 | 优先级 | 说明 |
|---|---|---|
| `ModuleCompileIsolationTest` | **P0** | 具体场景：① 验证各模块（eyelib-core, eyelib-particle, eyelib-network 等）之间的编译隔离——eyelib-particle 不能直接引用 eyelib-network 的包；② 验证模块间不存在循环依赖；③ 验证 public API 包与 internal 包的隔离。实现方式：Gradle `--api`/`--implementation` 模块隔离约束 |
| `ParticleSpawnServiceNetworkIntegrationTest` | **P1** | 具体场景：① 构造 `ParticleSpawnPacket` → 序列化为 JSON → 反序列化 → 调用 `ParticleSpawnService.spawnFromPacket`，验证 `ParticleDefinition` 被正确注册；② 验证网络包中所有字段（particleId, position, velocity, count 等）的完整编解码往返 |
| `EyelibParticleCommandRegisteredTest` | **P1** | 具体场景：① 使用 Minecraft Test Framework 或 Mock 验证 `/eyelib particle` 命令及其子命令在命令注册表中正确注册；② 验证命令参数解析器（如 ParticleArgument）的参数约束和错误处理；③ 验证权限和命令路径的唯一性 |

---

## 模块二：eyelib-util（4 文件）

### 2.1 UtilModuleIdentityTest
| 属性 | 值 |
|---|---|
| 文件 | `eyelib-util/src/test/.../UtilModuleIdentityTest.java` |
| 方法数 | 3 |
| 行数 | 73 |
| 判定 | **Delete** |
| 优先级 | **P0** |

- **C2 (Fragile Test):** 读 `build.gradle` 断言插件名称、读 `mods.toml` 断言 modId、读 `package-info.java` 断言描述关键词
- **建议:** 删除。build 脚本结构由 Gradle 自身验证，modid 读取由 `@Mod` 注解保证一致性，package-info 文本无自动验证必要

### 2.2 ListAccessorsTest
| 属性 | 值 |
|---|---|
| 文件 | `eyelib-util/src/test/.../collection/ListAccessorsTest.java` |
| 方法数 | 3 |
| 行数 | 37 |
| 判定 | **Rewrite** |
| 优先级 | **P2** |

- **C6 (Eager Test):** `firstAndLastReturnListEnds` 在一个 `assertEquals` 里同时测 `first()` 和 `last()`，如果 `last()` 正确但 `first()` 错误，测试名会误导
- **C3 (Weak Test):** `entryStreamsCollectsWithJdkCollectorsSemantics` 正确且足够
- **建议:** 将 `firstAndLastReturnListEnds` 拆为两个独立的测试方法

### 2.3 ColorEncodingsTest
| 属性 | 值 |
|---|---|
| 文件 | `eyelib-util/src/test/.../color/ColorEncodingsTest.java` |
| 方法数 | 1 |
| 行数 | 13 |
| 判定 | **Keep** |
| 优先级 | — |

- 纯单元测试：输入 → 期望输出，边界覆盖（全 FF alpha + 交错通道）。无问题

### 2.4 FixedStepTimerStateTest
| 属性 | 值 |
|---|---|
| 文件 | `eyelib-util/src/test/.../time/FixedStepTimerStateTest.java` |
| 方法数 | 4 |
| 行数 | 54 |
| 判定 | **Keep** |
| 优先级 | — |

- 状态机测试：每个方法覆盖一个独立场景（first step、stepped seconds、catch-up、real seconds）。无问题

### 2.5 新增测试建议（New）

| 建议 | 优先级 | 说明 |
|---|---|---|
| — | — | 当前 eyelib-util 测试覆盖已较充分，无需新增 |

---

## 模块三：eyelib-attachment（3 文件）

### 3.1 AttachmentModuleIdentityTest
| 属性 | 值 |
|---|---|
| 文件 | `eyelib-attachment/src/test/.../AttachmentModuleIdentityTest.java` |
| 方法数 | 3 |
| 行数 | 76 |
| 判定 | **Delete** (methods 1–2) + **Keep** (method 3) |
| 优先级 | **P0** |

- Method 1 (`moduleDocsDeclareMinecraftFunctionalIdentity...`): **C2 (Fragile Test)** — 读 README.md 断言关键词（"Minecraft/Forge functional module", "FriendlyByteBuf", ":eyelib-util"）。→ **Delete**
- Method 2 (`attachmentModuleDoesNotDependOnRootRuntimePackages`): `Files.walk` 全部源文件检查 "import io.github.tt432.eyelib."。→ **C2 + 合理但在模块较小时等价于编译期检查**。→ **Delete**（由 Gradle 模块隔离 + compile classpath 验证替代）
- Method 3 (`directMinecraftAndForgeImportsStayInAttachmentFacingPackages`): 检查 Minecraft/Forge import 是否限于 `network/`, `capability/`, `runtime/` 等允许包。→ **Keep**（有价值的结构性纪律）但可重写为一次性脚本简化

### 3.2 CommonRuntimeUpdaterTest
| 属性 | 值 |
|---|---|
| 文件 | `eyelib-attachment/src/test/.../runtime/CommonRuntimeUpdaterTest.java` |
| 方法数 | 3 |
| 行数 | 47 |
| 判定 | **Keep** |
| 优先级 | — |

- 纯单元测试：`ExtraEntityDataUpdater.update` 的 same-instance 和 flags replacement 路径 + `EntityStatisticsUpdater` 增量计算。边界覆盖到位，无问题

### 3.3 DataAttachmentStorageTest
| 属性 | 值 |
|---|---|
| 文件 | `eyelib-attachment/src/test/.../dataattach/DataAttachmentStorageTest.java` |
| 方法数 | 3 |
| 行数 | 97 |
| 判定 | **Keep** |
| 优先级 | — |

- 接口行为测试：set/get/has/remove + getOrCreate 创建一次 + null-read replacement。测试命名清晰，覆盖了存储层面所有关键路径。`NullWhenReadStorage` 辅助类合理。无问题

### 3.4 新增测试建议（New）

| 建议 | 优先级 | 说明 |
|---|---|---|
| `DataAttachmentTypeRegistryCodecTest` | **P2** | 验证 `DataAttachmentTypeRegistry` 的 `getById` 查询路径，替代 `NetworkOwnershipBoundaryTest` 中跨模块的源文件断言 |

---

## 汇总

### 判定统计

| 判定 | Root 边界 | eyelib-util | eyelib-attachment | 合计 |
|---|---|---|---|---|
| **Keep** | 0 file* | 2 files | 2 files | 4 |
| **Delete** | 4 files + 部分方法 | 1 file | 1 file (部分) | 6+ |
| **Rewrite** | 2 files (部分/整体) | 1 file | 0 files | 3 |
| **New** | 3 条建议 | 0 | 1 条建议 | 4 |

> *`ParticleRuntimeDelegationBoundaryTest` method 1 保留后半段运行时断言；`NetworkOwnershipBoundaryTest` method 2（文件名结构检查）保留

### 优先级排序执行建议

| 优先级 | 任务 |
|---|---|
| **P0** | 删除 `ParticleSpawnServiceBoundaryTest`、`ParticleFinalDocumentationGateTest`、`UtilModuleIdentityTest`、`AttachmentModuleIdentityTest` methods 1–2 |
| **P0** | Rewrite `ParticleApiDelegationBoundaryTest` (→ 编译隔离 + 集成测试替代) |
| **P0** | Rewrite `ParticleFinalSplitBoundaryTest` (→ 删除，packet DTO 转 codec 测试) |
| **P0** | Delete + Keep `NetworkOwnershipBoundaryTest`（删除 methods 1,3–5；保留 method 2 结构性检查） |
| **P0** | New `ModuleCompileIsolationTest` 统一替代所有 import 字符串断言 |
| **P1** | Delete `RenderMixinAccessorOwnershipTest` |
| **P1** | Rewrite `BedrockAddonRuntimeBridgeTest` (→ 拆分 Eager Test) |
| **P1** | Delete `EyelibParticleCommandBoundaryTest`、`ParticleCommandNetworkDocumentationTest` |
| **P2** | Rewrite `ListAccessorsTest` (→ 拆分 Eager Test method) |
