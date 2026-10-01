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

## 回归验证

Forge 1.20.1 / 47.1.3 + Shoulder Surfing 4.21.0 的实际开发客户端中，`CameraMolangSmoke` 在 `AFTER_ENTITIES` 阶段验证：

- 第一人称、第三人称后视、第三人称前视、实际启用的越肩视角。
- 三组相机相对位置 × 四组身体旋转（包含正反方向跨 ±180°）× 五个插值时刻，共每模式 60 组、四模式 240 组。
- 通过真实 Molang 注册、JSON 动画解析与 Bedrock 动画采样得到骨骼旋转；组合实体身体旋转、模型根 Y 轴 180° 和骨骼旋转后，平面法线与实际相机方向点积须大于 0.9999。
- 编译、NullAway 与 JAR 构建通过；1710 项单测中 1705 通过、1 跳过、4 项因上游已有模型 fixture 缺失失败，没有新增单测失败。

本功能客户端检查通过，随后完整 clientsmoke 在既有 Spider 渲染检查失败，不能宣称整套 smoke 通过。YesSteveCamera 与其他 Minecraft 版本尚未实测。当前修复保存在本地独立分支，等待使用方的实际特效包测试后再决定上游 PR。
