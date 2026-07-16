# 材质 CODEC → RenderType 路由链

## JSON 字段映射

`.material` JSON → `BrMaterialEntry`（shared CODEC，`BrMaterialEntry.java` 第 133-168 行）：

| JSON 字段 | BrMaterialEntry 字段 | 说明 |
|---|---|---|
| key `"entity_alphatest:entity"` | `name="entity_alphatest"`, `base="entity"` | `:` 分割，`split[1]` = base |
| `"defines"` | `defines.base()` | `ModifiedAble.base()` |
| `"+defines"` | `defines.add()` | `ModifiedAble.add()` |
| `"-defines"` | `defines.sub()` | `ModifiedAble.sub()` |
| `"states"` | `states.base()` | 同上 |
| `"+states"` | `states.add()` | 同上 |
| `"-states"` | `states.sub()` | 同上 |

## 继承链解析

`ModifyAble<T, S>` 接口（`BrMaterialEntry.java` 内部）提供 `toList(material, materials)` 方法，一次性解析 base + add − sub：

```java
var result = new ArrayList<>(get(material, materials, visited)); // 基材质的 base
add(result, baseEntry, materials, visited);                       // 基材质的 +add
sub(result, baseEntry, materials, visited);                       // 基材质的 -sub
result.addAll(material.add());                                    // 当前材质的 +add
result.removeAll(material.sub());                                 // 当前材质的 -sub
```

**反模式：手写 while 遍历** — 曾导致 `isAlphatest()` 只查 `defines.base()`、忽略 `+defines`。

## 材质加载

1. `BrMaterialLoader`（`@ResourceLoader`）从 `assets/<ns>/eyelib/materials/*.material` 加载
2. `BrMaterial.CODEC` → `DispatchedMapCodec` → 每个 key 调 `BrMaterialEntry.CODEC.apply(name)`
3. `BrMaterial.fromShared()` 转运行时类型 → `MaterialManager.INSTANCE.replaceAll()`

## 实体 → RenderType 完整路由

```
entity JSON: materials: {default: "entity_alphatest"}
  → RenderControllerEntry.setupModel() 解析 Molang
    → ModelComponentInfo.renderType = new ResourceLocation("材料名")
      → ModelComponent.getRenderType(texture)
        → buildMaterialLookupMap() 建查找表（全key → name → suffix 三层索引）
        → matMap.get(renderType.getPath())
          ├─ 命中 → entry.hasBlending(matMap)  → entityTranslucent
          │       → entry.isAlphatest(matMap)  → entityCutoutNoCull
          │       → else                       → entitySolid
          └─ 未命中 -> RenderTypeResolver.resolve(renderType)
                       ├─ 命中原版 (MC JE) RenderType 名 -> resolveVanilla() 返回等价 PortRenderPass
                       │   （entity_solid/cutout/cutout_no_cull/translucent/translucent_cull/emissive/eyes
                       │    + 方块层 solid/cutout/cutout_mipped/translucent；cull 按版本对齐原版）
                       └─ 未知名 -> SOLID + 警告（WARNED_UNKNOWN_RENDER_TYPES）
```

**注意**：`isAlphatest()` 使用 `defines.toList()` 遍历 define 继承链检测 `ALPHA_TEST` 字符串，而非按材质名匹配。这比废弃的 `isBaseType(entry, "entity_alphatest")` 名字匹配更健壮——对非标准命名的 alphatest 材质（如 `entity_alphatest_one_sided`）也有效。

**第二路由**：`BrMaterialEntry.getRenderType(texture, materials)`（material 模块内，用于自定义着色器/bgfx 路径）也应包含同样的 alphatest 检查，否则非 blending 材质会错误地 fall 到 `entitySolid`。两条路径必须一致。

**关键关注点**：未命中时 `RenderTypeResolver.resolve` 先尝试 `resolveVanilla` 按名称匹配原版 (MC JE) RenderType 语义（`entity_cutout` 等返回等价 PortRenderPass，不再丢 alpha test）。仅当名称既不在材质系统也不属于原版 RenderType 名时，才回退到 SOLID + 警告。原版 `entity_cutout` 的 cull 跨版本翻转：1.20.1/1.21.1 剔除，26.1.2 渲染重写后不剔除，`resolveVanilla` 用 `//?` 对齐。

## ⚠️ ModifyAble.add()/sub() 的 `this` 陷阱

`toList` 调用链中：
```
toList(kipfdc, matMap)
  → get(kipfdc, ...)          // this = kipfdc.states
  → add(result, baseEntry, ...) // this = kipfdc.states, material = vanillaEAB
      → getBase(base).add(...)  // 递归到 entity.states — 正确
      → add().orElse(...)       // this.add() = kipfdc.states.add() = 空 — BUG!
```

**根因**：`add()` 方法里的 `this.add()` 是调用方 States 对象的字段，不是 `material` 参数的字段。当 `add(result, baseEntry, ...)` 被调用在 kipfdc.states 上时，`material = vanillaEAB`（要继承的来源），但 `this.add()` 返回 kipfdc 的 +states（空），vanillaEAB 的 +states（`[Blending]`）被跳过。

**修复**：将 `add().orElse(...)` 改为 `getBase(material).add().orElse(...)`，`sub()` 同样改为 `getBase(material).sub().orElse(...)`。`getBase(material)` = `material.states`。

**症状**：所有自定义材质（如 kipfdc:entity_alphablend）的 `hasBlending()` 返回 false，`result=[]`。但基础材质（entity_alphablend 自身）的 `hasBlending()` 返回 true。继承链完全断裂。

## RenderDoc MCP 交叉验证模式

对比 .mcpack 配置目标 vs GPU 截帧实际：

1. 出什么 shader？`get_draw_call_state` → shader 名称、看 pixel shader 源码有没有 `discard`
2. alpha test 生效没？`pixel_history` 在 α=0 像素上 → 看是 discard 还是写入 framebuffer
3. blend 状态？`get_draw_call_state` → blend state（OpenGL 截帧 API 限制，最好在 RenderDoc GUI 看）
4. 对照 material 定义：`isAlphatest(materials)` 预期值 → 实际 shader 是否匹配
