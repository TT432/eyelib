# 0034 — 贴图原色材质的世界渲染通道

状态：Forge 1.20.1 实现；受控客户端像素检查和使用方实际特效包测试通过，等待上游审阅。

## 上下文

最大 packed light 不会去掉实体方向光，Oculus 还会替换实体 shader 并应用光影包的曝光、色调映射。普通自定义 shader 在 Oculus 世界阶段会被禁止写入颜色。因此原色不能用 FULL_BRIGHT 或替换实体 shader 单独实现。

## 决策

在 bridge/material 中提供可选的 `eyelib:texture_unlit`、`eyelib:texture_unlit_alpha`、`eyelib:texture_unlit_add`、`eyelib:texture_unlit_opaque` 材质。保持普通材质行为。shader 只计算贴图乘顶点 tint/alpha，不读取法线、光照贴图、overlay 或雾。

在 bridge/client/render 中收集已变换的世界顶点；mixin 只负责开启帧和在 GameRenderer 调用 LevelRenderer 返回后提交。此时 Oculus 已完成主要合成，主 framebuffer 仍保有世界深度，手持物深度清除尚未发生。阴影 pass 跳过。材质使用 CPU 顶点路径，不进入 GPU 蒙皮。粒子与模型共享此通道。

## 后果

此功能不是 PBR 发光，不进入光影的 bloom、反射或照明计算。透明颜色仍按 alpha 或加法与背景混合，顶点 tint 保留。世界不透明物体提供遮挡；水、玻璃等半透明场景对象与此晚绘制通道不能交叉排序，不支持折射中的原色特效。Oculus 末端色彩空间转换及游戏额外后处理仍可能改变最终显示，任意光影包不能宣称逐像素一致。TAA、自定义深度与投影需要实际光影包验证。

多版本入口用 Stonecutter 限制在 Forge 1.20.1，不改变其他节点的材质入口。当前验证范围如下；其他光影包、切换/重载组合和复杂透明场景仍需分别验证。

## 验证

`TextureColorSmoke` 验证 GPU 原色、alpha 和深度，`TextureColorWorldSmoke` 从真实世界事件入队、由 GameRenderer mixin 提交，验证光影合成顺序。2026-10-02，在 Forge 47.1.3 + Oculus 1.8.0 + Embeddium 0.3.31 下，无光影、Nostalgia v4.0a 和 iterationRP Alpha 0.8.22 均通过，RGB 容差 2/255。

通过编译和 NullAway；全量 1710 项单测仍有四项上游缺失模型 fixture 失败（与改动前日志一致），一项跳过。完整 clientsmoke 后续 Spider 实体检查未通过，不能把本功能的受控检查视为整套上游验收通过。

使用方随后在实际 Forge 47.4.16 客户端测试配套 YesSteveVFX，并明确反馈“测试没问题”。提供了 `xin_pack:sa3` 和 `xin_pack:sa1` 两张运行截图作为实测证据。这些截图展示不同特效，不是修改前后对比，也不据此推断截图使用了哪一款光影。

实测代码为 `12643be5`，部署 JAR 的 SHA-256 为 `DBB74F4E86D76AF13871135BFC7E002605A11BC90CABAB0C4A5F349E9572EEEA`。此次整理仅更新文档，保留已经验证的渲染实现。
