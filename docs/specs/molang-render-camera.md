# 模型动画的摄像机朝向查询

`query.rotation_to_camera(0/1)` 以实际世界渲染摄像机的位置为目标，返回 X/Y 轴的对准角度。不能以相机宿主实体的眼睛代替镜头位置：第三人称、越肩视角和相机碰撞都会令两者不同。

官方语义：[query.rotation_to_camera](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/molangreference/examples/molangconcepts/queryfunctions/query_rotation_to_camera)、[query.body_y_rotation](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/molangreference/examples/molangconcepts/queryfunctions/query_body_y_rotation)。

常见骨骼动画表达式：

```json
"rotation": [
  "query.rotation_to_camera(0)",
  "query.rotation_to_camera(1) - query.body_y_rotation",
  0
]
```

`query.body_y_rotation` 使用与实体渲染一致的最短路径角度插值，跨越 ±180° 时仍正确抵消实体身体旋转。保留现有 Java 方法名 `bodyXRotation`，避免修改已有调用者的二进制链接；公开 Molang 名称不变。

读取摄像机的版本差异位于 `CameraQueryPort` / `MinecraftMolangQueryRuntime`，不依赖 Shoulder Surfing 或其他相机模组的专有 API。相机模组只要更新游戏世界渲染使用的主摄像机，该查询即可读取其偏移。动画仍受父骨骼旋转、绑定姿态和其他动画混合影响；这不是自动抵消任意骨骼层级的 billboard 约束。本次沿用实体插值眼睛作为方向起点，没有引入逐骨骼位置查询。

## 始终与屏幕平行的平面

按位置朝向镜头不等于屏幕对齐。VFX 微小载体的眼高为 0.009 格，而特效平面可能位于载体上方数格；镜头被地面挤到载体附近时，位置查询仍会朝向脚底附近的镜头，无法体现平面的实际旋转中心。

只要求始终正对屏幕的平面使用标准 `query.camera_rotation`：

```json
"rotation": [
  "-query.camera_rotation(0)",
  "query.camera_rotation(1) + 180 - query.body_y_rotation",
  0
]
```

在 1.20.1 实现中，该查询读取本帧主摄像机的 pitch/yaw，包含越肩视角的独立旋转和第三人称前视翻转，不再以玩家视角代替。1.21.1 共享此适配代码但尚未运行验证；26.1.2 保留原有宿主视角近似。本写法不补偿任意父骨骼旋转，也不涵盖相机 roll。

实际 `xin_pack:sa1` 的开发副本在越肩 -90°、镜头贴地时复现了位置查询约 23.5° 的倾角；屏幕对齐表达式产生 90° 倾角，实际采样骨骼法线与镜头反向视线点积为 1。动画文件中的固定关键帧、子骨骼附加旋转仍遵循原动画配置，只有使用该表达式的部分保持屏幕对齐。eyelib 只提供查询，不会改写动画资产。

## 回归验证

Forge 1.20.1 / 47.1.3 + Shoulder Surfing 4.21.0 的实际开发客户端中，`CameraMolangSmoke` 在 `AFTER_ENTITIES` 阶段验证：

- 第一人称、第三人称后视、第三人称前视、实际启用的越肩视角；每种视角测试 0°、±89.9°、±90° 俯仰，共 20 个实际摄像机场景。
- 位置朝向：三组相机相对位置 × 四组身体旋转（包含正反方向跨 ±180°）× 五个插值时刻，共每场景 60 组、1200 组。
- 屏幕对齐：0.009 格眼高的载体，覆盖贴近镜头、镜头正上方与正下方位置，各三组身体朝向，共每场景 9 组、180 组。验证实际镜头俯仰确实到达极限，再比较最终动画平面法线与摄像机反向视线。
- 通过真实 Molang 注册、JSON 动画解析与 Bedrock 动画采样得到骨骼旋转；组合实体身体旋转、模型根 Y 轴 180° 和骨骼旋转后，平面法线与实际相机方向点积须大于 0.9999。
- 编译、NullAway 与 JAR 构建通过；1710 项单测中 1705 通过、1 跳过、4 项因上游已有模型 fixture 缺失失败，没有新增单测失败。

本功能客户端检查通过，随后完整 clientsmoke 在既有 Spider 渲染检查失败，不能宣称整套 smoke 通过。本修复已随 PR #26 并入上游（合并时修复了 smoke 匿名类在本工具链下的编译失败）；YesSteveCamera 与其他 Minecraft 版本尚未实测。
