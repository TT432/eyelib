## 渲染能力与 Molang 语义补齐

**栓绳渲染**：eyelib 接管实体渲染后，1.21.1+ 的原版栓绳（在 `LivingEntityRenderer.render` 末尾绘制）会随 `RenderLivingEvent.Pre` 的取消一起被跳过。新增 `LeashRenderer` 按实体每帧补画一条绳，锚点优先取模型 `lead` locator（Bedrock 约定），无 locator 时回落原版 `getLeashOffset`；1.20.1 抑制被接管实体的原版绳，避免双绳。

**26.1 受伤/白闪 overlay**：26.1 适配器此前把 overlay 坐标写死为 `NO_OVERLAY`，受伤红闪与苦力怕白闪不显示。改为 `LivingEntityRenderer.getOverlayCoords(state, getWhiteOverlayProgress(state))`。

**Bedrock 引擎变量**：新增 `EngineEntityVariable` + `RenderSystemPort#engineVariable` + `EngineEntityVariables` 统一绑定（惰性读取，资源包已写同名变量时不覆盖）。首个内置为羊驼 `variable.DecorTextureIndex`；同时补齐官方 Molang 语义：变量名与 struct 成员大小写不敏感，`==`/`!=` 在数值类型之间按 `asFloat` 比较。

**材质**：内置 `llama` 材质补 `MASKED_MULTITEXTURE`，官方三层羊驼纹理（base / decor / decor_none）可按标准渲染。

- 1.20.1 / 1.21.1 / 26.1.2 三节点实机验证：栓绳单绳、26.1 受伤红闪、行商羊驼蓝金装饰层、守卫者眨眼闭合。
- 新增单测 `MolangEqualityTest` 与 `MolangScopeTest` 大小写用例；`molangCheck`、NullAway、`:test` 失败集合与既有基线一致。
