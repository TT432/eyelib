# 实体渲染调试：数据对照方法论

## 核心原则

**基准不是 vanilla JE，是 .mcpack 中的 Bedrock 数据。**

当用户说"X 实体在 Bedrock 有 Y 效果但 JE 显示为蓝色方块"，正确流程是：

## 阶段 1：提取 Bedrock 源数据

通过 /eval 获取 eyelib 加载后的完整 BrClientEntity 数据：

```java
// 获取 BrClientEntity
Object ce = cap.getClientEntityComponent().getClientEntity();

// 核心字段
ce.identifier()        // "minecraft:vex"
ce.textures()          // shortName → texturePath 映射
ce.geometry()          // shortName → geometryName 映射  
ce.materials()         // shortName → materialName 映射（entity-level）
ce.render_controllers() // RC 名称列表
```

## 阶段 2：提取 RenderController 数据

```java
RenderControllerManager.INSTANCE.get(rcName)
// → rc.materials()  // List<MolangMapEntry>, 每项 {bonePattern: materialRef}
// → rc.textures()   // texture 引用（MolangValue）
// → rc.geometry()   // geometry 引用
```

## 阶段 3：逐条对照

| 对照项目 | Bedrock 数据（RC） | eyelib 运行结果 | 
|---|---|---|
| RC 骨模式 | `ja89loaf8` | 模型骨骼名 `ja89l62j` → 不匹配 ❌ |
| 材质引用 | `Material.vppzlt` | entity.materials 中有 `vppzlt=vppzlt` |
| geometry 引用 | `Geometry.default` | entity.geometry 中 `default=geometry.oreville_ans.atvfvf` |
| texture 引用 | `Array.ncyhyp[q.is_charging]` | Molang 求值 → `textures/oreville/ans/btl.png` |

## 阶段 4：定位差距

常见差距模式：
1. **骨骼名不一致** — RC 的 bonePattern 与模型实际 boneName 不同（obfuscation 导致）
2. **材质 key 不匹配** — RC 引用 `Material.x` 但 entity.materials 中没有 `x` 的映射
3. **纹理路径错误** — texture 经过 Molang 求值后的路径在资源系统中不存在
4. **`*` 默认材质覆盖所有骨骼** — 即使骨模式错配，`*` 仍然生效，所以"完全不渲染"不可能由骨模式错配单独导致
