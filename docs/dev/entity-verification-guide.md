# 实体渲染验证指南

## 目标

对 eyelib 注册的每个 BrClientEntity，验证其渲染结果与 `.mcpack` 定义和 Bedrock 文档一致。

## 环境

- 游戏已运行，149 个实体在 `MobArenaGenerator` 生成的场地中
- mcpack: `run/resourcepacks/Actions-and-Stuff-1.10-v2.mcpack`
- Bedrock wiki: `/mnt/e/_____基岩版文档/bedrock-wiki/docs/`
- Mojang Creator 文档: `/mnt/e/_____基岩版文档/minecraft-creator/creator/Documents/`
- 项目代码: `/mnt/e/_ideaProjects/qylEyelib/`

## 验证工具

### 1. /eval 查询（运行时状态）

```
eyelib_debug_execute(code='...Java方法体...')
```

关键查询：

**查实体是否有 ModelComponent**：
```
Object cap = io.github.tt432.eyelib.capability.RenderData.getComponent(target);
java.util.List comps = (java.util.List) cap.getClass().getMethod("getModelComponents").invoke(cap);
return "comps=" + comps.size();
```

**查实体使用的 material name**：
```
// 从 ModelComponent 获取 material
Object comp = comps.get(0);
// comp 有 getMaterialName() 或其他方法
```

**查实体是否在渲染距离内**：
```
double dist = player.distanceToSqr(target);
boolean sr = target.shouldRender(player.getX(), player.getY(), player.getZ());
```

**查 BrClientEntity 是否存在**：
```
return ClientEntityManager.INSTANCE.get("minecraft:实体名") == null ? "NULL" : "OK";
```

**查纹理路径**（防止 .png.png 等）：
```
// 通过 ModelComponent 的 texture 信息
```

### 2. RenderDoc 截帧
```python
eyelib_debug_capture_frame()  # 截取当前帧 → .rdc 文件
```
然后 `renderdoc-mcp` 工具分析（open_capture, list_textures, get_draw_call_state 等）。

### 3. mcpack 内容查阅
```bash
unzip -l run/resourcepacks/Actions-and-Stuff-1.10-v2.mcpack | grep 实体名
```

纹理在 `subpacks/SP2/textures/entity/`，模型/动画在 `__brarchive/*.brarchive`（二进制格式，需 eyelib 代码读取）。

## 验证 checklist（每个实体）

| # | 检查项 | 方法 |
|---|---|---|
| 1 | BrClientEntity 已注册 | `/eval` ClientEntityManager.get() |
| 2 | 实体在渲染距离内 | `/eval` shouldRender() |
| 3 | ModelComponent 数量 > 0 | `/eval` RenderData.getModelComponents() |
| 4 | 材质名称正确（与 mcpack entity.material 对照） | 读 mcpack materials/entity.material |
| 5 | RenderType 正确（solid/cutout/translucent） | `/eval` ModelComponent.getRenderType() |
| 6 | 纹理路径无重复 .png | `/eval` 查 texture 字段 |
| 7 | part_visibility 有可见骨骼 | `/eval` 查 part_visibility |
| 8 | 实体不透明部分无异常透明 | 肉眼 / RenderDoc |
| 9 | 动画播放正常 | 肉眼观察实体是否动 |
| 10 | 特殊实体：染色（sheep）、骑乘（horse）等 | 肉眼对比 Bedrock 截图 |

## 验证优先级

### P0（必须测）：有完整 Bedrock 模型的实体
sheep, cow, pig, chicken, horse, zombie, skeleton, creeper, spider, enderman,
slime, wolf, bat, parrot, cat, fox, panda, turtle, bee, iron_golem, snow_golem,
witch, blaze, ghast, vex, allay, frog, camel, sniffer, armadillo, warden, breeze

### P1：有部分定义的实体
drowned, husk, stray, wither_skeleton, zombie_villager, piglin, piglin_brute,
hoglin, zoglin, strider, phantom, guardian, elder_guardian, dolphin, axolotl,
glow_squid, squid, polar_bear, goat, llama, trader_llama, ravager, pillager,
vindicator, evoker, shulker, endermite, silverfish, cave_spider

### P2：非生物实体（projectile/vehicle/effect）
arrow, fireball, minecart 系列, ender_pearl, potion, etc.
（这些通常没有完整 Bedrock 模型，主要验证不崩溃）

## 如何分工

每批 8-12 个实体，每个子代理负责一批。

子代理的工作流程：
1. 读 mcpack 确定该实体预期的 geometry/texture/animation
2. 用 `/eval` 查询运行时状态
3. 列出发现的问题（缺失、错配、渲染错误）
4. 输出结果：实体名 → 状态（PASS/FAIL）+ 具体问题描述

## 输出格式

```
## 批次结果

| 实体 | 状态 | 问题 |
|------|------|------|
| minecraft:sheep | PASS | — |
| minecraft:cow | FAIL | com.sps=0, no model loaded |
| minecraft:horse | FAIL | texture only shows white, tint not applied |
| ... | ... | ... |

### 发现的具体问题

1. minecraft:cow — ModelComponent 为空，mcpack 未提供 cow 模型
2. minecraft:horse — BrClientEntity OK, comps=1, 但顶点颜色硬编码白色
```

## 注意事项

- 如果 /eval 报 "redefinition of local variable" → 换独特变量名（`_mc1`, `_mc2`）
- 如果 /eval 报 "No key facing_camera_mode" 等 → 是之前已修复的 particle crash
- 如果实体 `shouldRender()=false` → 先用 setPos 拉到玩家面前
- 不要改代码，只做验证和报告
- 不需要截帧 RenderDoc 除非肉眼看到明显的渲染错误需要定位
