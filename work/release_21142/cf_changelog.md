# eyelib 21.1.42

## 修复：多人服务器首次进入时实体贴图变成紫黑「马赛克」

部分实体在使用运行期派生纹理（`clamped/`、`_color_mask/`）时，整只模型会采样到 MC 的缺省贴图
（16×16 紫黑棋盘，视觉上就是"赛博马赛克"）。命中条件与网络环境无关，而是与「首帧绘制/烘焙早于派生纹理生成」的时序有关，
所以典型表现是：多人服务器首次进入必现，单人正常，切光影或先进单人再回服后恢复。

根因与修复：

- `NativeImageIO.download` 改为**只读**查询：运行期派生键未注册时不再走 vanilla `getTexture`
  （它会 `new SimpleTexture` 并 register，资源里没有这些键 → 加载失败 → `byPath` 被永久写成缺省图占位）。
  像素改由「资源文件解码 → 基图就地派生（`clamped/` = alpha 二值化，`_color_mask/<hex>/` = 颜色掩码）」供给。
- `TextureManager.getTexture` 注入对派生键**按需物化**：绘制、烘焙、GUI 三条消费路径看到同一份内容，
  不再依赖渲染控制器预热动作的排队先后。
- `eyelib$hasTexture` 不再把 `minecraft:missingno` 占位当成「已生成」，派生纹理不会再被判为存在而永不创建。

三个节点均生效（1.20.1 / 1.21.1 / 26.1.2）：修复点在共享纹理层，无版本差异分支。

## 验证

- 1.21.1 真客户端 + 本地专用服务器（`--quickPlayMultiplayer`，复刻用户链路）：修复前日志
  `Failed to load texture: minecraft:clamped/…`、`byPath` 派生键为缺省图、蜘蛛近景整只紫黑；
  修复后零失败、派生键全为 `DynamicTexture`，蜘蛛/村民/苦力怕/羊/蜘蛛实机贴图正常。
- 单人 `enter_world` 同样零失败（派生键 4/0）。
- 1.20.1 / 1.21.1 / 26.1.2 三节点 `compileJava` 通过；1.20.1 / 26.1.2 仅做编译验证（该问题复现链路为 1.21.1 专用服务器）。
