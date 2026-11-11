## CarryOn 兼容：同帧重复提交去重 + render state 按身份解析

**两个 26.1 侧缺陷**（三节点共用同一渲染入口，修复对 1.20.1 / 1.21.1 同样生效）：

- **同帧重复提交**：`LevelRenderState.entityRenderStates` 只在 `renderLevel` 末尾 reset，一帧内 `extractLevel` 被调用多次就会累积同一实体的多份 render state；CarryOn 第一人称每次 main/offhand 各提交一次携带实体。同一实体在同一姿态下被提交 2–3 次，附属挂载物（盔甲/手持物）跟着多画——一旦某一份本体几何因延迟提交窗口/烘焙跳过没有画出来，屏幕上就只剩「多出来的一份甲」。现在按「实体身份 + 实体根位姿指纹」在帧内去重，命中即按已渲染返回（同时取消 vanilla 兜底）。
- **render state 坐标反查串实体**：`RenderLivingEvent` 不携带实体，原先按「类型 + 插值坐标（±0.5 格）」反查。两只同类型生物站得太近（同一格、0.5 格）时，多份 state 会全部解析到同一个实体，该实体的模型与盔甲被画在每个 state 的位置上。现在 `EntityRenderDispatcher.extractEntity` 对**所有**活体 render state 登记身份映射，坐标反查只作兜底。

**前置：配合 dokimobs 0.3.15**（CarryOn 抱起带甲生物 / 同格同类型生物的场景由 dokimobs 的萌化模型复现）。

**验证**：三节点 `compileJava` / `:test` / `:nullawayMain` 全绿；1.20.1 / 1.21.1 / 26.1.2 实机（真 CarryOn 2.1.2.7 / 2.2.6.13 / 2.10.0）同帧重复提交计数 26.1.2 修前 54+ → 修后 0，三节点甲离体计数 0，抱起带甲生物第一/第三人称各一份，两只相距 0.5 格的溺尸（铁甲/金甲）各自显示自己的甲。
