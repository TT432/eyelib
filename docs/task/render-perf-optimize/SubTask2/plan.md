# SubTask2: ModelComponent 材质缓存（Opt-1）

## 目标
消除 `ModelComponent` 三个渲染查询方法（`getRenderType`/`isSolid`/`usesColorMask`）每帧每实体重复执行 `BrMaterialResolver.find`(O(n)×2 线性扫描) + `resolve`(继承链归并)。

基线（spark JxI1zQR418）：find+resolve 链 ~1484ms self-time(2.5% Render thread)，5 处 find 热点(480+476+256+140+132)。

## 根因
- `ModelComponent.java:84-126` 三方法各自 `MaterialManager.INSTANCE.all()` → `find` → `resolve`，无缓存。
- `find`(BrMaterialResolver.java:97-120) 含双重 O(n) entrySet 线性扫描（冒号前缀 + name equals）。
- `resolve`(:25-73) 递归 collectChain 走继承链，每层 find 一次（`findBase`→`find`）。
- 即同一 entry 每帧被 find 3 次 + resolve 3 次。

## 缓存可行性（已验证）
- `Registry<BrMaterialEntry>` 用 `AtomicReference<RegistrySnapshot>` copy-on-write。
- 稳态渲染期间 `MaterialManager.INSTANCE.all()` 返回**同一 map 实例**（snapshot 不变）。
- 资源重载（`putAll`/`replaceAll`/`clear`）时 snapshot 原子替换 → map identity 变化 → 缓存自然失效。
- `ResolvedBrMaterial` 是 record（不可变），`resolve(entry, materials)` 是纯函数 → 缓存安全。

## 执行计划

### Step 1: RenderTypeResolver 新增接受 ResolvedBrMaterial 的重载
文件：`bridge/material/RenderTypeResolver.java`
- 新增 `resolve(PortResourceLocation texture, ResolvedBrMaterial material)` → `BrRenderTypeFactory.create(texture, BrRenderStateFactory.from(material))`
- 新增 `isSolid(ResolvedBrMaterial material)` → `BrRenderStateFactory.from(material).isSolid()`
- 目的：让 ModelComponent 用缓存的 ResolvedBrMaterial 直接算 RenderType/isSolid，避免 RenderTypeResolver 旧重载内部重复 resolve。
- 旧重载（接受 entry+matMap）保留，供 fallback（resolve 异常）路径使用。

### Step 2: ModelComponent 缓存字段 + 失效逻辑
文件：`capability/component/ModelComponent.java`
新增字段：
```java
@Nullable private Map<String, BrMaterialEntry> matMapRef;   // identity 失效信号
@Nullable private BrMaterialEntry cachedEntry;              // find 结果
private boolean entryResolved;                              // cachedEntry 是否已算（含 null 结果）
@Nullable private ResolvedBrMaterial cachedMaterial;        // resolve 成功结果
private boolean materialResolved;                           // resolve 是否已成功（cachedMaterial 有效）
@Nullable private RenderTypeResolver.EntityRenderTypeData cachedFallback;  // entry==null 时的 fallback data
```

新增 helper：
- `ensureMaterialCache()`：取 `MaterialManager.INSTANCE.all()`，若 `!= matMapRef` 则全部清零 + 更新 matMapRef。
- `resolveCachedMaterial()`：若 `!materialResolved`，try resolve(cachedEntry, matMapRef)，成功存 cachedMaterial+materialResolved=true，失败 materialResolved=true + cachedMaterial=null。返回 cachedMaterial（null 表示失败）。

改三个方法：
- 开头调 `ensureMaterialCache()`。
- `if (!entryResolved) { entryResolved = true; cachedEntry = BrMaterialResolver.find(matMapRef, serializableInfo.renderType().path()).orElse(null); }`
- entry!=null：`ResolvedBrMaterial mat = resolveCachedMaterial()`；mat!=null 走新 RenderTypeResolver 重载；mat==null（resolve 异常）走旧 RenderTypeResolver.resolve(texture,entry,matMap)（其内部 catch fallback）。
- entry==null：缓存 `RenderTypeResolver.resolve(serializableInfo.renderType())` 到 cachedFallback，复用。

`setInfo` 中：若 serializableInfo 实际变化（ Objects.equals 已判），置 `matMapRef = null`（强制下次失效重算）。

### Step 3: 编译验证
`eyelib_debug_build(version="1.20.1")`，要求 exit 0。

### Step 4: spark 复测对比
重新采集 60s Render thread（与基线同条件：Debug World，83 实体，eyelib 100% 接管），导出 CSV，跑 `python scripts/analyze_render_profile.py`。

### 验收（有效/无效判定）
- **有效**：eyelib 占比下降 AND BrMaterialResolver.find/resolve self-time 显著下降（目标 find 五处热点合并或消失，总 self-time < 基线一半）。
- **无效**：无显著变化 → 回滚。
- 语义保持：渲染结果视觉无变化（材质/RenderType 不变）。

## 规格说明

### 前置条件
- serializableInfo != null 时才有意义（==null 三方法返回默认值，不进缓存路径）。

### 后置条件
- 同一 (serializableInfo, matMap) 下，三个方法结果与优化前逐位等价。
- matMap identity 不变时，find/resolve 每帧最多执行一次（首方法算，其余两方法复用 cachedMaterial）。

### 不变量
- 缓存仅在 `matMapRef == MaterialManager.INSTANCE.all()`（identity）时有效。
- 资源重载后第一次调用必定失效（matMap 实例已变）。
- setInfo 改变 serializableInfo 后必定失效。

### 异常行为
- resolve 抛 IllegalStateException（循环继承）：cachedMaterial=null，各方法走各自 fallback（与优化前行为一致）。
- find 返回 empty：cachedEntry=null，走 entry==null fallback 路径（缓存 cachedFallback）。

### 副作用
- ModelComponent 实例多占 ~6 个字段的内存（每实体，可忽略）。
- 无外部可见副作用。

## 不改动的部分
- `BrMaterialResolver.find/resolve` 实现本身（O(n) 扫描）—— Opt-1B 留待后续，先消除重复调用。本次只缓存调用结果。
- Molang 链（Opt-2）—— 本子任务不碰。
