# Stonecutter 多版本复杂度控制 — 设计范式

> 调研日期: 2026-06-07
> 来源: Stonecutter 官方文档、stonecutter-mod-template 源码分析、Architectury @ExpectPlatform 模式、Blahaj crash course

## Stonecutter 概览

- **作者**: KikuGie，Gradle 插件 `dev.kikugie.stonecutter`，最新 0.9.4 (2026-05)
- **仓库**: `github.com/stonecutter-versioning/stonecutter` (Codeberg 镜像)
- **语言**: Kotlin + ANTLR (Stitcher 解析器)
- **许可**: LGPL-2.1

### 三文件架构

| 文件 | 职责 |
|---|---|
| `settings.gradle.kts` | 根，注册支持的版本+loader |
| `stonecutter.gradle.kts` | 控制器，为每个版本创建独立 Gradle subproject |
| `build.gradle.kts` | 实际构建逻辑，被各版本共用 |

`.sc_active_version` 控制当前活跃版本。

### Stitcher 注释语法

```java
//? if >1.2.0           // 版本比较
//? if >1.2.0 { ... //?} // 多行块
//? if >1.2.0 || <0.3.0 // 布尔运算
//? if id: >1.2.0        // 命名变量
//?} else /*...*/        // else 分支
/*? if dep >=0.2 >>*/    // 内联注入
```

---

## 六大设计范式 (按 eyelib 适用性排序)

### 1. 平台抽象层 (Platform/Strategy 模式)

定义一个与 loader 无关的 `Platform` 接口，各 loader 各自实现。共享代码只依赖接口。

```
platform/
├── Platform.java          ← 接口
├── fabric/FabricPlatform.java
├── forge/ForgePlatform.java
└── neoforge/NeoforgePlatform.java
```

选型通过 Stonecutter 注释在编译时决定：
```java
//? fabric { return new FabricPlatform(); //?}
//? forge   { /*return new ForgePlatform();*/ //?}
```

**适用 eyelib**: 次要。eyelib 主要问题是 MC 版本 API 差异，不是 loader 差异。

---

### 2. 版本桥接层 (Version Barrier / Adapter 模式) ★ 核心

**原则**: 识别 MC API 断点 → 为每个断点创建窄接口适配器 → **Stonecutter 注释只出现在适配器内部** → 业务代码完全干净。

**接触面积定律**:
- 直接 `//? if` 散布 → 复杂度 O(n × 代码行数)
- 适配器收敛到 K 个点 → 复杂度 O(K)，业务代码 O(1)

**eyelib 的 MC API 断点**:

| 断点 | 1.19.2 | 1.20.1 | 1.21+ |
|---|---|---|---|
| ResourceLocation 构造 | `new RL(ns, path)` | 同 | `RL.fromNamespaceAndPath(ns, path)` |
| 注册表 | `Registry.REGISTRY` | `ForgeRegistries.Keys` | `BuiltInRegistries` |
| 网络包 | `SimpleChannel` | 同 | `IPayloadRegistrar` |
| Mixin target | 类名/方法签名可能变 | — | — |

```java
// 适配器接口 — 零 Stonecutter 注释
public interface ResourceLocationFactory {
    ResourceLocation create(String namespace, String path);
}

// 适配器实现 — Stonecutter 注释的唯一栖息地
class ResourceLocationFactoryImpl implements ResourceLocationFactory {
    public ResourceLocation create(String ns, String path) {
        //? > 1.19.2 {
        return ResourceLocation.fromNamespaceAndPath(ns, path);
        //?} <= 1.19.2 {
        /*return new ResourceLocation(ns, path);*/
        //?}
    }
}
```

**对 eyelib 的建议**: 3-5 个适配器接口 (`ResourceLocationFactory`, `RegistryAccess`, `NetworkBridge`) 即可隔离渲染管线、材质系统、动画等核心代码。

---

### 3. 能力检测优于版本检测

**反模式**: `//? if > 1.21 { doNewWay(); } else { doOldWay(); }` 散布业务代码。

**推荐**: 在 Capabilities 类中收敛所有检测，暴露语义化的 boolean：
```java
public static boolean hasDataComponentApi() {
    //? > 1.20.5 { return true;
    //?} else { /*return false;*/ //?}
}

// 业务代码 — 零 Stonecutter 注释
if (Capabilities.hasDataComponentApi()) { doNewWay(); }
```

**好处**: 版本语义只存在于一个类，加版本时只改一处。

---

### 4. 依赖属性注入 (Gradle 层参数化)

用 `/*$ property_name*/` 语法注入 Gradle 变量，避免 Java 硬编码：
```java
public static final String MC_VERSION = /*$ mc_version*/ "1.20.1";
```

配合 `stonecutter.properties.toml` 做 per-version 配置。

---

### 5. 模块分层隔离 (大型项目)

```
├── eyelib-api/          ← 纯接口，零 MC 依赖 (永不碰 //? if)
├── eyelib-core/         ← 共享实现 + 适配器 (Stonecutter 注释的唯一栖息地)
└── eyelib-forge/        ← Forge 胶水
    eyelib-neoforge/     ← NeoForge 胶水
```

**Architectury vs Stonecutter 对比**:

| | Architectury | Stonecutter 单源 |
|---|---|---|
| 模块数 | 3+ (common/forge/fabric) | 1 |
| 共享代码位置 | common subproject | `src/main/java/` |
| 平台差异 | `@ExpectPlatform` | `//? if` 注释 |
| IDE 体验 | 各自子项目独立索引 | 单项目，切换 `.sc_active_version` |
| 适用规模 | 大量平台特定代码 | 平台差异少 |

**eyelib 建议**: 已是 10 子模块多模块项目，合理做法是核心库 (eyelib-util, eyelib-molang 等) 作为 common，loader 胶水在 `eyelib-forge`/`eyelib-neoforge` 中。Stonecutter 注释只出现在 bridge 层。

---

### 6. Mixin 版本隔离

每个版本独立 Mixin 文件 + Stonecutter 条件编译：
```
mixin/
├── MixinClientPacketListener.java          ← 共享 (签名不变时)
├── MixinClientPacketListener_1_20_1.java   ← 版本特定
└── MixinClientPacketListener_1_21.java
```

---

## 复杂度估算

假设支持 N 个 MC 版本 + M 个 loader:

| 策略 | 业务代码复杂度 | 维护点 |
|---|---|---|
| 裸 `//? if` 散布 | O(N×M×代码行数) | 每个差异点 × 文件数 |
| Version Barrier 适配器 | O(N×M×K) | K 个适配器文件 |
| Capabilities 检测 | O(N×M×能力数) | 1 个 Capabilities 类 |
| 模块分层 | O(N×M) | 每 loader 的 bridge 模块 |

---

## eyelib 引入路线 (建议)

1. **不碰逻辑，先建桥**: 创建 3-5 个 Version Barrier 适配器，迁移现有直接 MC API 调用
2. **收敛注释**: 加 `Capabilities` 类，所有 `//? if` 聚合到一个文件
3. **多 loader 扩展**: 如需 NeoForge，加 `eyelib-bridge-forge` / `eyelib-bridge-neoforge` 模块

**禁止**: 在渲染管线、Molang 引擎、动画系统里撒 `//? if` 注释。
