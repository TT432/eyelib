# Stonecutter 多版本改造详细设计

**Date:** 2026-06-17
**Related ADR:** [ADR-0015](../../decisions/0015-stonecutter-multi-version.md)
**Status:** Design approved, pending implementation plan

本文档是 ADR-0015 的实施层补充，包含具体文件改造、渗透清单、Phase 任务分解。决策动机与方案对比见 ADR-0015。

## 1. 目标版本矩阵

| Node | MC | Loader/Plugin | Java | Parchment | 备注 |
|---|---|---|---|---|---|
| `1.20.1` | 1.20.1 | Forge 47 / `legacyforge` | 17 | 1.20.1-2023.09.03 | 现状基线 |
| `1.21.1` | 1.21.1 | NeoForge 21.1 / `moddev` | 21 | 1.21.1 | 默认 active |
| `26.1.2` | 26.1.2 | NeoForge / `moddev` | 25 | 待确认（游戏类不混淆） | 最高难度 |

## 2. 目录结构改造

### 改造前（现状）

```
eyelib/
├── settings.gradle              # 16 行，单 project
├── build.gradle                 # 378 行，legacyforge + publishing
├── gradle.properties            # 50 行，所有版本相关属性
├── src/main/java/...            # 共享源码
├── src/main/resources/...
├── src/test/java/...
└── clientsmoke/                 # 组合构建
```

### 改造后

```
eyelib/
├── settings.gradle              # 【改】加 Stonecutter 插件 + maven.kikugie.dev
├── stonecutter.gradle           # 【新】tree 控制器
├── build.gradle                 # 【改】共享脚本，sc.current 感知
├── gradle.properties            # 【改】只留共享属性
├── versions/
│   ├── 1.20.1/
│   │   └── gradle.properties    # 版本特定属性
│   ├── 1.21.1/
│   │   └── gradle.properties
│   └── 26.1.2/
│       └── gradle.properties
├── src/main/java/...            # 【不动】共享源码 = active version
├── src/main/resources/...
├── src/test/java/...
└── clientsmoke/                 # Phase 5 升级为独立 Stonecutter tree
```

## 3. 配置文件内容

### 3.1 `settings.gradle`（改造后要点）

```gradle
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven { url = 'https://maven.minecraftforge.net/' }
        maven { url = 'https://maven.parchmentmc.org' }
        maven { url = 'https://maven.kikugie.dev/releases' }   // 新增
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.7.0'
    id 'dev.kikugie.stonecutter' version '0.5.x' apply false    // 新增
}

rootProject.name = "eyelib"

includeBuild("clientsmoke")

// 新增：Stonecutter bootstrap
buildscript {
    dependencies {
        classpath 'dev.kikugie:stonecutter-gradle:0.5.x'
    }
}
apply plugin: 'dev.kikugie.stonecutter'
```

> 实施时按 Stonecutter 当前版本号校准（查 `https://maven.kikugie.dev/releases/dev/kikugie/stonecutter-gradle/`）。插件 apply 方式以 Stonecutter 文档为准（`settings` 里 apply 还是 root build.gradle 里 apply）。

### 3.2 `stonecutter.gradle`（新增）

```gradle
stonecutter.create(rootProject) {
    versions "1.20.1", "1.21.1", "26.1.2"
    current = "1.21.1"   // 默认 active

    // 版本范围常量（具体 DSL 以 Stonecutter 实际为准，实现时校准）
    constants {
        V_FORGE   = { it < "1.20.6" }
        V_NEOFORGE = { it >= "1.20.6" && it < "26.1" }
        V_MODERN  = { it >= "26.1" }
    }
}
```

### 3.3 `build.gradle`（改造要点）

关键：用 `apply plugin:` 运行时条件，不能用 `plugins {}` block。

```gradle
// 不可变插件仍在 plugins {} block
plugins {
    id 'idea'
    id 'java-library'
    id 'maven-publish'
    id 'io.freefair.lombok' version '8.6'
    id 'net.ltgt.errorprone' version '4.4.0'
    id 'signing'
    id 'io.github.gradle-nexus.publish-plugin' version '2.0.0'
}

// 版本相关的 ModDevGradle 用 apply plugin 运行时条件
def isLegacyForge = sc.current.parsed < "1.20.6"
if (isLegacyForge) {
    apply plugin: 'net.neoforged.moddev.legacyforge'
} else {
    apply plugin: 'net.neoforged.moddev'
}

version = "${mod_version}+${minecraft_version}-${isLegacyForge ? 'forge' : 'neoforge'}"
group = mod_group_id

java.toolchain.languageVersion = JavaLanguageVersion.of(property("java_version") as int)

if (isLegacyForge) {
    legacyForge {
        version = project.minecraft_version + '-' + project.forge_version
        parchment { ... }
        runs { ... }
        mods { ... }
    }
} else {
    neoForge {
        version = project.neoforge_version
        parchment { ... }
        runs { ... }
        mods { ... }
    }
}

// 其余 dependencies/publishing/mixin/nullawayMain 等块基本保留，
// 仅版本特定值（forge_version_range、loader_version_range 等）从 versions/<ver>/gradle.properties 读
```

### 3.4 `gradle.properties`（改造后，只留共享属性）

```properties
org.gradle.jvmargs=-Xmx1G
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true

mod_id=eyelib
mod_name=eyelib
mod_license=MIT
mod_version=21.1.14
mod_group_id=io.github.tt432
mod_authors=TT432
mod_description=A render lib for Minecraft
githubUserName=TT432
githubRepoName=eyelib
enableSmokeTest=false
```

### 3.5 `versions/1.20.1/gradle.properties`

```properties
minecraft_version=1.20.1
forge_version=47.1.3
minecraft_version_range=[1.20.1, 1.21)
forge_version_range=[47.1.3,)
loader_version_range=[47,)
java_version=17
parchment_minecraft_version=1.20.1
parchment_mappings_version=2023.09.03
```

### 3.6 `versions/1.21.1/gradle.properties`

```properties
minecraft_version=1.21.1
neoforge_version=21.1.x
minecraft_version_range=[1.21.1, 1.22)
loader_version_range=[4,)
java_version=21
parchment_minecraft_version=1.21.1
parchment_mappings_version=...
```

### 3.7 `versions/26.1.2/gradle.properties`

```properties
minecraft_version=26.1.2
neoforge_version=...
minecraft_version_range=[26.1.2,)
loader_version_range=[...,)
java_version=25
# 26.1 游戏类不混淆，parchment 角色变化，待实施时确认
```

## 4. MC import 渗透清单（Phase 3 输入）

以下是 domain 包内当前违反「domain 不得 import MC」规则的接触点，按 domain 模块分组。ArchUnit freeze 模式会把这些写入 baseline。

### `molang/`
- `molang/mapping/MolangQuery.java` — import `net.minecraft.world.entity.{Entity, LivingEntity, WitherBoss, Creeper}`
- `molang/platform/compiler/MolangCompileLifecycleHooks.java` — `net.minecraftforge.*`
- `molang/platform/mapping/ForgeMolangMappingDiscovery.java` — `net.minecraftforge.*`
- `molang/platform/mapping/MolangMappingTreeLifecycleHooks.java` — `net.minecraftforge.*`

### `material/`（主包，非 `bridge/material/`）
- `material/material/BrMaterial.java` — `RenderStateShard`（1.21.5+ 破坏）
- `material/material/BrShaderMapping.java` — `Minecraft` + `ShaderInstance`
- `material/gl/GLStateApplier.java` — `RenderSystem`
- `material/shared/VertexFormatElementEnum.java` — `blaze3d.vertex.*`
- `material/shader/ShaderManager.java` — `RenderSystem`

### `util/`
- `util/model/InventoryModelResourceLocations.java` — `ResourceLocation`
- `util/resource/ResourceLocations.java` — `ResourceLocation`
- `util/streamcodec/{StreamCodec,StreamDecoder,StreamEncoder,EyelibStreamCodecs}.java` — `FriendlyByteBuf`
- `util/codec/EyelibCodec.java` — `ExtraCodecs` + `AABB`（DFU 部分 `ExtraCodecs` 走白名单，`AABB` 需抽走）
- `util/math/Shapes.java` — `RandomSource`

### `importer/`（基本干净）
- 仅 `com.mojang.serialization.*` + `com.mojang.datafixers.*`（DFU 白名单），无 `net.minecraft.*` 渗透

### `model/`、`animation/`、`behavior/`、`particle/`
- 实施时由 ArchUnit baseline 扫描补充

## 5. Phase 0 清理 checklist

### 5.1 反射残留清理

| 文件 | 行 | 问题 | 处理 |
|---|---|---|---|
| `animation/bedrock/BrAnimationEntryDefinition.java` | 117-186 | `resolveLocatorPosition` 用 25+ 行反射链（`Class.forName("...RenderData")` → `getMethod("getComponent")` → 链式反射访问 `ModelComponents → Model → Bone → locator.offsets()`）绕 subproject 依赖隔离 | 直接 import + 调用，压缩到 ~5 行 |

**合法保留**（不清理）：
- `client/loader/ClientLoaderLifecycleHooks.java:39` — `@ResourceLoader` 注解发现
- `molang/platform/mapping/ForgeMolangMappingDiscovery.java:46` — 注解发现
- `Eyelib.java:22` — 反射 `AIDebugServer` 隔离 server-only

### 5.2 旧 modid 字符串清理

| 文件 | 问题 | 处理 |
|---|---|---|
| `track/EyelibTrack.java:12` | `MOD_ID="eyelibtrack"` | 改为 `"eyelib"`（或并入主 mod） |
| `attachment/dataattach/mc/DataAttachmentContainerCapability.java:21` | `new ResourceLocation("eyelibattachment", ...)` | 改为 `"eyelib"` namespace（注意：会破坏已存档世界，需评估迁移策略） |
| 测试文件 | 大量 `"eyelibmaterial:"` / `"assets/eyelibmaterial/"` namespace | 全部改为 `eyelib` |

### 5.3 ArchUnit 骨架

新增 `src/test/java/io/github/tt432/eyelib/archunit/ArchitectureTest.java`，规则按 ADR-0015 §4 定义，初始以 freeze 模式运行，生成 `archunit-baseline.json` baseline 文件提交进 repo。

## 6. 差异分级应用示例

### L1（`//?` 注释）

26.1.2 的 `render` → `extract` 方法重命名：

```java
//? if >=26.1 {
graphicsRenderer.extract(...);
//?} else {
graphicsRenderer.render(...);
//}
```

### L2（Port + per-version 实现）

`ResourceLocation` 在 1.21.11 → `Identifier`：

```java
// bridge/render/ResourceLocationPort.java（版本无关，无 MC import）
public interface ResourceLocationPort {
    String namespace();
    String path();
    ResourceLocationPort parse(String input);
}

// bridge/render/v1_20/ResourceLocationImpl.java
//? if <1.20.6 {
package ...;
import net.minecraft.resources.ResourceLocation;
public class ResourceLocationImpl implements ResourceLocationPort { ... }
//}

// bridge/render/v26/IdentifierImpl.java
//? if >=26.1 {
package ...;
import net.minecraft....Identifier;  // 26.1 重命名后
public class IdentifierImpl implements ResourceLocationPort { ... }
//}
```

### L3（per-version 子系统并行实现）

26.1.4+ 的 render states + RenderPipeline 重写：整个 `client/render/pipeline/` 子系统在 1.20.1/1.21.1 走 legacy 实现，在 26.1.x 走 modern 实现，两套并行维护。判定依据：单方法 L2 抽取后 Port 接口本身在版本间无法统一。

## 7. Phase 详细任务分解

### Phase 0：清理与准备（不引入 Stonecutter）

**Deliverable**：现有 1.20.1 单版本项目更干净，ArchUnit baseline 落地。

- [ ] 清理 `BrAnimationEntryDefinition.java:117-186` 反射链 → 直接 import + 调用
- [ ] 清理 `track/EyelibTrack.java:12` MOD_ID
- [ ] 清理 `DataAttachmentContainerCapability.java:21` ResourceLocation namespace（先评估存档世界影响）
- [ ] 清理测试文件 namespace
- [ ] 新增 `ArchitectureTest.java` + baseline 文件
- [ ] `jetbrain_build_project` 退出码 0
- [ ] `runClient` 冒烟通过

### Phase 1：Stonecutter 脚手架 + 1.20.1 node 回归

**Deliverable**：1.20.1 node 产可用 jar，与现状等价。

- [ ] `settings.gradle` 加 Stonecutter 插件 + maven.kikugie.dev
- [ ] 新增 `stonecutter.gradle`
- [ ] 拆 `gradle.properties` → 共享 + `versions/1.20.1/`、`versions/1.21.1/`、`versions/26.1.2/`
- [ ] 改造 `build.gradle` 为 sc.current 感知（`apply plugin:` 运行时条件 + `legacyForge`/`neoForge` 块切换）
- [ ] 1.20.1 node 完整通过编译 + 测试 + runClient 冒烟
- [ ] 1.21.1/26.1.2 node 声明存在（`stonecutter.gradle` 已列），但允许编译失败
- [ ] 安装 Stonecutter IDEA 插件，验证 active version 切换 UI

### Phase 2：1.21.1 node 编译通过

**Deliverable**：1.21.1 node 产可用 jar。

- [ ] 1.20.1 → 1.21.1 的 L1 差异用 `//?` 处理（扫描 `net.minecraft.*` API 变化）
- [ ] Forge → NeoForge 的 plugin/依赖差异（`legacyForge` → `neoForge` 块）
- [ ] Java 17 → 21 toolchain 切换
- [ ] 必要 L2 抽取（超过 ~20 行 `//?` 块的接触点抽 Port）
- [ ] mixin/MixinExtras 在 1.21.1 验证
- [ ] ArchUnit 在 1.21.1 node 也能跑
- [ ] 1.21.1 node `jetbrain_build_project` 退出码 0 + runClient 冒烟

### Phase 3：ArchUnit 收紧（与 Phase 2/4 并行）

**Deliverable**：domain 模块逐个清零违规。

按 domain 模块还债顺序（从最干净开始）：
1. `importer`（仅 DFU 白名单，预期快速清零）
2. `molang`（抽 `MolangQuery` + platform/ 到 bridge）
3. `util`（抽 `ResourceLocation`/`FriendlyByteBuf`/`AABB`/`RandomSource` 到 bridge）
4. `material`（抽 `RenderStateShard`/`Minecraft`/`ShaderInstance`/`RenderSystem` 到 bridge）
5. `model`/`animation`/`behavior`/`particle`（按 ArchUnit baseline 补充）

每清零一个模块：ArchUnit 规则对那个模块从 freeze 升级为 fail。

### Phase 4：26.1.2 node 编译通过（最高难度）

**Deliverable**：26.1.2 node 产可用 jar。

子阶段建议：
- 4a 编译通过（先让 javac 过）
- 4b 运行通过（runClient 启动不崩）
- 4c smoke 通过（@ClientSmoke 测试能跑）

任务清单：
- [ ] Java 21 → 25 toolchain
- [ ] mixin 移除 refmap（`eyelib.mixins.json` 的 `"refmap"` 字段 + `mixin { add ... }` 配置按版本切）
- [ ] L1：`GuiGraphics` → `GuiGraphicsExtractor`（`//?`）
- [ ] L1：`render` → `extract` 方法重命名（`//?`）
- [ ] L2：`ItemStack` + 注册表访问 + `ItemStackTemplate`（Port）
- [ ] L2：`ResourceLocation` → `Identifier`（Port，影响面大）
- [ ] L3：渲染管线 render states / `RenderPipeline`（并行实现）
- [ ] 校准 NullAway 与 26.1 Jspecify 原生可空的冲突（配置或 `@SuppressWarnings`）

### Phase 5：clientsmoke 多版本化

**Deliverable**：三 node 都支持 clientsmoke 测试。

- [ ] clientsmoke 升级为独立 Stonecutter tree（自己的 `versions` 声明）
- [ ] 主项目移除 `if (sc.current.version == "1.20.1")` 门控
- [ ] 1.21.1/26.1.2 node 的 `@ClientSmoke` 测试能跑

### Phase 6+：持续改进（无终点）

- ArchUnit 白名单持续收紧，最终 domain 完全无 MC import
- bridge 抽取持续进行，新破坏点按 L1/L2/L3 判定处理
- 新 MC 版本增量加入（声明新 node + 处理差异）
- 渲染路径 L3 并行实现持续优化

## 8. 风险与缓解

| 风险 | 概率 | 影响 | 缓解 |
|---|---|---|---|
| Stonecutter 版本号/DSL 与本文档假设不一致 | 中 | 中 | Phase 1 第一步查 maven.kikugie.dev 最新版 + 文档校准 |
| ModDevGradle `legacyforge` 与 `moddev` 的 DSL 差异超过 `apply plugin:` 切换能承载 | 中 | 中 | 退化到 `mapBuilds` 拆 buildscript（ADR-0015 §6 已预留） |
| 26.1.x Jspecify 与 NullAway 冲突无法调和 | 中 | 高 | Phase 4 先评估，必要时对 26.1 node 单独降级 NullAway 为 warning 或加 selective `@SuppressWarnings` |
| 26.1.x 渲染管线 L3 改造工作量爆炸 | 高 | 高 | 子阶段 4a/4b/4c 渐进；先编译通过，legacy 渲染路径在 26.1 node 临时跑占位实现，后续 phase 补全 |
| ArchUnit baseline 失控（违规太多无法清零） | 中 | 中 | 按模块逐个清零，不追求一次性全绿 |
| clientsmoke 独立 tree 化后与主项目版本联动失配 | 低 | 中 | Phase 5 才动，且 Phase 1-4 用门控保证 1.20.1 node 不受影响 |

## 9. 后续 ADR 同步

实施时需同步更新：
- **ADR-0010**：标注 ArchUnit 强制恢复（Superseded by ADR-0015 §4）
- **ADR-0014**：标注从「单版本单 project」演化为「单 project + Stonecutter 多 node」（Amended by ADR-0015）
- **MODULES.md**：新增「多版本节点结构」章节，记录 versions/<ver>/ + bridge/<feature>/v<ver>/ 包约定
- **AGENTS.md**：Repository Shape 段补充多版本说明
