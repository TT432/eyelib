# 材质渲染链路：BrClientEntity → RenderType

从 Bedrock 实体定义到 MC RenderType 的完整路由。

## 1. Subpack 选择

### Bedrock 规则
- `manifest.json` 的 `subpacks[]` 数组定义子包
- `memory_performance_tier`：1-5，对应平台（1=Nintendo Switch, 5=PS5 Pro/PC）
- 选择：最高 tier 且不超过设备能力，tie 取最后一个
- **常见问题**：所有 subpack 设为 `memory_performance_tier: 0`（Actions & Stuff 就这样）→ 全部 tie → 取最后一个

### Eyelib 实现
`BedrockAddonLoader.selectSubpack()` — 遍历求最高 tier，`>= bestTier` 时持续覆盖（tie 取最后）。

```java
for (Subpack sp : subpacks) {
    int tier = sp.memoryPerformanceTier();
    if (tier >= bestTier) {
        bestTier = tier;
        lastBest = sp.folderName();
    }
}
```

### 文件过滤
`BedrockAddonLoader.collectFiles()` — 只加载根目录和选中 subpack 的文件：
```java
if (selectedSubpack != null && subpackFolder != null && !subpackFolder.equals(selectedSubpack))
    continue;  // 跳过其他 subpack
String effectivePath = stripSubpackPrefix(relativePath);  // subpacks/SP2/foo → foo
```

## 2. MaterialManager 条目命名

### CODEC 规则
`BrMaterialEntry.CODEC` 将 key 按 `:` 分割：
- `"pblupe:klduzy"` → `name=pblupe`, `base=klduzy`
- `"klduzy:entity_alphatest"` → `name=klduzy`, `base=entity_alphatest`
- `"entity_alphatest:entity"` → `name=entity_alphatest`, `base=entity`
- 无冒号 key → `name=全串`, `base=""`

### 继承链示例（Actions & Stuff 史莱姆）
```text
vanilla.material:
  entity_alphatest:entity → name=entity_alphatest, base=entity, +defines=[ALPHA_TEST]

entity.material (mcpack):
  klduzy:entity_alphatest → name=klduzy, base=entity_alphatest, -defines=[FANCY]
  pblupe:klduzy          → name=pblupe, base=klduzy, +defines=[USE_EMISSIVE]
  ckkelc:klduzy          → name=ckkelc, base=klduzy, +defines=[USE_SKINNING]
  idymni:klduzy          → name=idymni, base=klduzy, +defines=[USE_UV_ANIM]
```

## 3. RenderController 材质选择

### 实际示例：Actions & Stuff 史莱姆

RenderController `controller.render.oreville_ans.slime` 的 wildcard：
```
armor*=Material.armor
2dh923d6f5fg72f4q*=Material.kipfdc
*=v.iswiaq?Material.idymni:Material.klduzy
2dh923d6f5fg72f4q=v.iswiaq?Material.ojnhbt:Material.kipfdc
```

`*` 是 wildcard 材质槽。Molang 表达式 `v.iswiaq?Material.idymni:Material.klduzy` 根据变量选择分支，不论走 `idymni` 还是 `klduzy`，最终都通过继承链命中 `entity_alphatest:entity → +defines=[ALPHA_TEST]`。

### 排查流程

当渲染效果不对时，按以下顺序排查材质路由：
1. `MaterialManager.getAllData().size()` — 确认 vanilla + mcpack 全部加载
2. `ClientEntityManager.get("entity_id").materials()` — 确认 `default` 映射
3. `ClientEntityManager.get("entity_id").render_controllers()` — 确认有无 RC
4. 有 RC → 查 RC 的 `materials` 是否含 `*`；无 `*` → renderType="" → Fallback
5. `buildMaterialLookupMap().get(matName)` — 确认索引命中预期条目
6. `entry.isAlphatest(matMap)` — 确认最终 RenderType 路由
RenderController 的 `materials` 字段中 `*` 是 wildcard：
```json
"*": "v.iswiaq ? Material.idymni : Material.klduzy"
```

`RenderControllerEntry.createModelComponent()` 处理逻辑：
```java
new ResourceLocation(
    materials.containsKey("*") 
        ? get(scope, materials.get("*"), "material", entity.materials()) 
        : ""
)
```

流程：
1. Molang 表达式求值 → `Material.klduzy` 或 `Material.idymni`
2. `get()` 方法：取 `Material.` 后面的部分 → `klduzy`/`idymni`
3. 在 `entity.materials()`（BrClientEntity 的 materials map）中查找 → 得到裸名
4. 设为 `ModelComponent.renderType`（ResourceLocation）

无 `*` → renderType = `""`（空字符串）→ 走 `RenderTypeResolver` fallback。

## 4. buildMaterialLookupMap — 三层索引

### 索引策略
```java
Map<String, BrMaterialEntry> result = new HashMap<>(getAllData());  // (1) 全键

for (entry : getAllData()) {                                        // (2) 后缀 (putIfAbsent)
    String suffix = key.substring(lastIndexOf(':') + 1);
    result.putIfAbsent(suffix, entry);
}

for (entry : getAllData()) {                                        // (3) name 字段 (put, 覆盖)
    result.put(entry.getValue().name(), entry.getValue());
}
```

### 为什么 `getAllData()` 不够
`ModifyAble.get()` 内部用 `materials.get(base)` 查找基材质。`getAllData()` 的 key 是 `"entity_alphatest:entity"`，但 base 是裸名 `"entity_alphatest"` → 查不到 → 继承链断裂。

`matMap` 通过 name 索引（第三层）解决了这个问题：`matMap.get("entity_alphatest")` 命中 `entity_alphatest:entity`。

### 始终用 matMap
`isAlphatest()`、`hasBlending()`、`getRenderType()` 必须接收 `buildMaterialLookupMap()` 返回的 `matMap`，而不是 `getAllData()`。

验证方式：
```bash
# 用 /eval 确认 matMap 命中
echo '... buildMaterialLookupMap().get("klduzy") ...' | curl ...
```

## 5. 渲染类型映射

### BrShaderMapping 代理
```
ALPHA_TEST  → entityCutout(NoCull)
OPAQUE      → entitySolid
TRANSPARENT → entityTranslucent
```

### 判断顺序
`getRenderType()`:
1. `isAlphatest(matMap)` → ALPHA_TEST shader
2. `hasBlending(matMap)` → TRANSPARENT shader
3. 否则 → OPAQUE shader

### 渲染状态
RenderType 创建时根据 material 条目属性设置：
- Transparency：Blending 状态 → translucent/no_transparency
- DepthTest：DisableDepthTest → GL_ALWAYS / GL_LEQUAL
- Cull：DisableCulling → 关闭 / 开启
- WriteMask：DisableColorWrite/DisableDepthWrite → 控制写入
