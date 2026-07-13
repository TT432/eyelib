# 材质继承链运行时诊断

通过 `/eval` 对运行中客户端做程序化材质继承链诊断，不依赖肉眼观察。

## /eval 代码编写技巧

- **必须用文件传参**：`echo '...' | curl -d @-` 会损坏内容（shell 转义冲突）。用 `curl -d @/tmp/eval.txt` 文件方式。
- **禁用 `Map.of()`**：Janino 不支持 "static interface methods only available for target version 8+"。用 `new java.util.HashMap()`。
- **`while` 循环报 "Method must return a value"**：Janino 不能静态证明 while 循环一定执行 return。改用 `for` 循环或数组迭代。

## 快速诊断

### 1. Simulate buildMaterialLookupMap and test suffix resolution

```java
// file: /tmp/eval_matmap.txt
io.github.tt432.eyelib.client.manager.MaterialManager mm = io.github.tt432.eyelib.client.manager.MaterialManager.INSTANCE;
java.util.Map allData = mm.all();
java.util.Map matMap = new java.util.HashMap(allData);
Object[] entries = allData.entrySet().toArray();
for (int i = 0; i < entries.length; i++) {
    java.util.Map.Entry e = (java.util.Map.Entry) entries[i];
    io.github.tt432.eyelib.material.material.BrMaterialEntry val = (io.github.tt432.eyelib.material.material.BrMaterialEntry) e.getValue();
    matMap.putIfAbsent(val.name(), val);
    String key = (String) e.getKey();
    int colon = key.lastIndexOf(':');
    if (colon >= 0) {
        matMap.putIfAbsent(key.substring(colon + 1), val);
    }
}
io.github.tt432.eyelib.material.material.BrMaterialEntry entry = (io.github.tt432.eyelib.material.material.BrMaterialEntry) matMap.get("kipfdc");
boolean hb = entry.hasBlending(matMap);
return "name=" + entry.name() + " base=" + entry.base() + " hb=" + hb;
```

### 2. Check entity_alphablend directly

```java
io.github.tt432.eyelib.client.manager.MaterialManager mm = io.github.tt432.eyelib.client.manager.MaterialManager.INSTANCE;
java.util.Map mats = mm.all();
io.github.tt432.eyelib.material.material.BrMaterialEntry eab = (io.github.tt432.eyelib.material.material.BrMaterialEntry) mats.get("entity_alphablend:entity");
boolean hb = eab.hasBlending(mats);
return "entity_alphablend hasBlending=" + hb;
```

### 3. Check all ModelComponents render types

```java
Object cap = io.github.tt432.eyelib.capability.RenderData.getComponent(level.getEntity(23));
java.lang.reflect.Method m = cap.getClass().getMethod("getModelComponents");
java.util.List comps = (java.util.List) m.invoke(cap);
StringBuilder sb = new StringBuilder();
sb.append("total=").append(comps.size());
for (int i = 0; i < comps.size(); i++) {
    Object comp = comps.get(i);
    boolean solid = (Boolean) comp.getClass().getMethod("isSolid").invoke(comp);
    Object info = comp.getClass().getMethod("getSerializableInfo").invoke(comp);
    if (info != null) {
        String matInfo = info.getClass().getMethod("renderType").invoke(info).toString();
        String tex = info.getClass().getMethod("texture").invoke(info).toString();
        sb.append(" | [").append(i).append("] mat=").append(matInfo).append(" tex=").append(tex).append(" solid=").append(solid);
    }
}
return sb.toString();
```

### 故障 D：所有材质槽纹理相同（`texture.material` per-pass 未实现）

**症状**：多个 ModelComponent 各有不同 RenderType，但 `info.texture()` 全部相同

**原因**：`setupModel()` 在材料循环**前**一次性求值 `texture`（第107-109行）。`initArrays` 不设裸 `material` scope 变量，`texture.material` 求值为 null → 走 `get` 方法 fallback → `map.get("material")` → `"minecraft:null"` → 所有槽得到 default 纹理

**验证**：
```java
// 检查各 ModelComponent 的纹理
// ... (同故障 C 的 comps 遍历，看 texture() 字段)
```

**修复方向**：纹理求值移入材料循环 + scope 注入 `material` 变量。详见 `references/texture-material-per-pass-evaluation.md`。

## 常见故障模式

### 故障 A：所有材质都是 solid=true（无 blending、无 alphatest）

**症状**：`ModelComponent.getRenderType()` 返回的 RenderType 都是 no_transparency + solid=true

**原因**：`isAlphatest()` / `hasBlending()` 全部返回 false。通常是 `get()/add()/sub()` 缺少 name-index fallback，在 `getAllData()` 裸 key 查询时找不到 base。

**修复**：确保 `BrMaterialEntry.ModifyAble.get/add/sub` 有 name-index fallback。

### 故障 B：源码有修复但客户端返回旧结果

**症状**：源码中 `hasBlending` 逻辑正确，但 `/eval` 调用仍返回 false

**原因**：源码修复后未重新编译部署。Gradle 的 `UP-TO-DATE` / `FROM-CACHE` 标记可能不检测 `patch` 工具的修改。

**验证**：
1. `git status --short` 确认文件有修改
2. 清理受影响模块的 `build/` 目录
3. 全量编译 + 重启客户端
4. 用 `/eval` 测试 `hasBlending(allData)` 验证修复是否生效

### 故障 C：buildMaterialLookupMap suffix 索引返回错误条目

**症状**：
- `entity_alphablend hasBlending(allData)=true`（基材质正确）
- 但 `ModelComponent.isSolid()=true`（`matMap.get("kipfdc")` 返回了错误条目）
- MaterialManager 中有多个相同 suffix 的条目（如 `tbjptt:kipfdc`、`kipfdc:entity_alphablend` 都产生 suffix `"kipfdc"`）

**根因**：`buildMaterialLookupMap()` 用 `putIfAbsent` 建 suffix 索引，HashMap 迭代顺序不确定。非 canonical 条目先被索引，canonical 条目（`suffix == entry.name()`）被跳过。

**修复**：canonical 条目用 `put`（覆盖）而非 `putIfAbsent`。详见主 SKILL.md 中"材质继承陷阱"章节。
