# Material→RenderType 全链路验证方法论

基于 2026-06-05 史莱姆 RenderController Material 拆分验证过程总结的闭环验证方法。

## 原则

1. **由外到内**：先查 Bedrock 规范（schema + wiki），再对照 eyelib 实现
2. **静态→运行时**：先计算预期值，再用 `/eval` 验证实际值，交叉对比
3. **每层独立验证**：骨骼分配、材质查找、RenderType 解析分别验证，不跳步
4. **不依赖肉眼**：用 `/eval` 做程序化验证，不靠用户说"看到了什么"

## 验证流程

### Layer 0: 获取 Bedrock 规范数据

```bash
# 1. 读 RC materials 映射（从 .mcpack JSON 或运行时 BrClientEntity）
/eval → ce.render_controllers() → RenderControllerManager.get(name) → rc.materials()

# 2. 读模型骨骼名（从 ModelManager）
/eval → model.allBones() → 逐骨检查 name + id

# 3. 理解材质继承链（从 vanilla.material + mcpack .material）
MaterialManager.all() → 检查每个条目的 name/base/defines/states
```

### Layer 1: 计算预期结果

```
RC materials:
  "2dh923d6f5fg72f4q" → kipfdc (exact match) → bone#37
  "armor*"             → armor (prefix)       → bone#257  
  "*"                  → klduzy (wildcard)    → 其余骨骼(bones - bone#37 - bone#257)

预期 ModelComponent 数 = 3（去重后无空匹配组）
预期骨骼分配: kipfdc=1, armor=1, klduzy=6
```

### Layer 2: 运行时验证

```bash
# 骨骼分配
/eval → rd.getModelComponents() → 逐 component 检查 partVisibility 计数

# 材质查找
/eval → matMap.get("key") 确认命中预期 BrMaterialEntry

# RenderType 解析
/eval → mc.getRenderType(dummyTex).toString() 确认 GL 类型字符串
/eval → mc.isSolid() 确认 solid/transparent 分类
```

### Layer 3: 交叉验证

- 预期数 == 实际数? → 骨骼分配正确
- 每个 material key 的 expected RenderType == actual? → 继承链正确
- 骨骼名/ID 与 partVisibility 匹配? → pattern 匹配正确

## 常见 /eval 模式

### 精简骨骼分配验证

```java
// 文件: /tmp/eval.txt
for (Entity e : level.entitiesForRendering()) {
    RenderData rd = RenderData.getComponent(e);
    BrClientEntity ce = rd.getClientEntityComponent().getClientEntity();
    if (ce == null || !ce.identifier().contains("slime")) continue;
    List comps = rd.getModelComponents();
    for (int i = 0; i < comps.size(); i++) {
        ModelComponent mc = (ModelComponent) comps.get(i);
        Int2BooleanOpenHashMap pv = mc.getPartVisibility();
        int visible = 0;
        for (int k : pv.keySet()) { if (pv.get(k)) visible++; }
        result.add("[" + i + "] " + info.renderType() + " solid=" + mc.isSolid() + " bones=" + visible);
    }
    break;
}
```

### RenderType 验证

```java
// 确认最终 GL 类型
ResourceLocation dummyTex = new ResourceLocation("minecraft", "test");
for (...) {
    RenderType rt = mc.getRenderType(dummyTex);
    result.add("[" + i + "] " + key + " -> " + rt.toString());
}
```

## 关键陷阱

- **`ModelComponent.getRenderType(texture)` 需要 texture 参数** — 不能裸调
- **Janino 不支持 `var`**，全显式类型
- **Int2ObjectMap 迭代** 需要 `(Iterable)` cast 或 `.keySet()` 遍历
- **大脚本拆小** — Janino 有编译长度限制，3+ 组件 + 骨骼名遍历可能超限 → 拆成多次 /eval 调用
- **预期数据先算清** — 不要边跑 /eval 边猜预期值，先静态分析确定每个 component 应该有多少骨骼、什么 RenderType
