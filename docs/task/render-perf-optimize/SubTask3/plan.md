# SubTask3: BrMaterialResolver.resolve 全局缓存

## 背景

SubTask2（Opt-1）在 ModelComponent 层缓存了 find+resolve 结果，消除了 ModelComponent 路径的每帧重算（find 1484→12ms）。

但 spark 复测显示 `BrMaterialResolver.find` 仍有 **1064ms self-time**，构成：
- find←find 752ms（resolve→collectChain→findBase→find 的继承链递归）
- find←findBase 372ms

这些来自 **RenderControllerEntry 路径**（isAlphaTest 664ms + usesColorMask 328ms total）每帧重复调 `resolve(entry, materials)`，resolve 内部走继承链每层 find（O(n)×2 扫描）。

`resolve` 是纯函数（BrMaterialEntry 是 record 不可变，materials map 稳态不变 → 输入确定→输出确定）。缓存结果后内部 find 递归只算一次。

## 改动

**文件**: `src/main/java/io/github/tt432/eyelib/material/material/BrMaterialResolver.java`

在 BrMaterialResolver 加 resolve 结果缓存：
- 新增两个 static volatile 字段：`cachedMatMap`（Map，identity 失效信号）、`resolveCache`（IdentityHashMap<BrMaterialEntry, ResolvedBrMaterial>）
- `resolve` 方法开头：调 `resolveCacheFor(materials)` 获取 cache → `cache.get(entry)` 命中则返回 → miss 则 `computeResolve` + `cache.put`
- 原 resolve 方法体重命名为 `computeResolve`（private）
- `resolveCacheFor(materials)`：若 `materials != cachedMatMap`（identity），重建 cache（new IdentityHashMap）+ 更新 cachedMatMap；否则返回现有 cache

**为什么用 IdentityHashMap**：entry 是 materials map 内的对象，identity 稳定（map 不变时 entry 对象不变）。用 == 而非 equals，避免 record 全字段 equals/hashCode 开销。

**为什么静态字段安全**：MaterialManager.INSTANCE 是单例，materials map 全局唯一。resolve 只在 Render thread 调用（渲染路径），资源重载不调 resolve。按 matMap identity 失效（Registry copy-on-write，重载时 snapshot 替换 → map 实例替换 → cache 重建），无需监听。

## 规格说明

### 前置条件
- Opt-1（ModelComponent 缓存）已提交并加载
- 编译 exit 0

### 不变量
- resolve 是纯函数：给定 (entry, materials) → 结果唯一确定
- 缓存命中与 cache miss 返回的结果语义等价
- BrMaterialEntry 是 record（不可变），缓存期间 entry 内容不变

### 后置条件
- 正常路径（resolve 成功）：结果缓存，后续同 (entry, matMap) 命中缓存返回
- 异常路径（IllegalStateException 循环继承）：不缓存，异常直接传播（与原行为一致）
- matMap identity 变化时（资源重载）：cache 重建

### 异常行为
- `computeResolve` 抛 IllegalStateException → 不执行 `cache.put` → 异常传播 → 下次同输入仍会重新计算并抛
- 这保持了原有语义：循环继承每次调用都报错（而非缓存错误状态）

### 副作用
- 内存：IdentityHashMap 缓存 entry→ResolvedBrMaterial，大小 = materials map 的 entry 数（通常几十到几百）。随 map identity 失效被 GC。

## 验收标准

spark 复测（60s Render thread）：
1. `BrMaterialResolver.find` self-time 从 1064ms 显著下降（目标 < 300ms）
2. resolve 的分配（ArrayList/LinkedHashSet/ResolvedBrMaterial record 构造）self-time 下降
3. RenderControllerEntry.isAlphaTest/usesColorMask 的 total-time 下降（resolve 子树命中缓存）
4. 语义保持：渲染视觉无变化（缓存是透明的）

无效则回滚。

## 执行步骤

1. 改 BrMaterialResolver.java（加缓存）
2. 编译（exit 0）
3. close → launch → enter_world → pauseOnLostFocus=false
4. spark profiler 60s
5. 下载 profile.bin + metadata
6. 导出 CSV + analyze_render_profile.py + trace_find_callers.py
7. 对比 find self-time + resolve 路径
