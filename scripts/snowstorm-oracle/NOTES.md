# Snowstorm Node Oracle — import 图与 stub 方案（wave-3 harness 依据）

目的：在 Node 24 中直接运行 Snowstorm 数据层 JS（build/_snowstorm_src/src/），生成 golden 输出
供 Java 移植比对。本文件记录 import 图实证（2026-09-29 findstr 全量扫描）与 stub 设计。

## import 图（实证）

| 模块 | 依赖 |
|---|---|
| util.js | 无外部依赖（IO 对象内含 browser 文件对话框，stub） |
| options.js | （localStorage 全局，stub） |
| edits.js | vscode_extension、./export(generateFile)、./util(compileJSON) |
| variable_placeholders.js | ./edits、./emitter(Emitter)、./input_structure(forEachInput, Data) |
| input.js | ./edits、./components/ExpressionBar(ExpandedInput UI)、./emitter(Config, Emitter)、./texture_edit(Texture)、./util(trimFloatNumber, guid) |
| input_structure.js | ./input、./gradient、./emitter(Config)、vscode_extension |
| curves.js | **vue**（响应式）、./emitter(Emitter, Config)、./input_structure、./input、./util(guid)、./edits |
| gradient.js | ./input、./util(bbuid)、./edits |
| event_sub_effects.js | ./emitter(Scene)、./util(IO)、vscode_extension |
| export.js | ./input_structure、./util(compileJSON, IO)、./emitter(Config)、**three**(MathUtils) |
| import.js | ./util(guid, IO, pathToExtension)、./emitter(Config, **QuickSetup**, Emitter)、vscode、./components/ExpressionBar、./input_structure、./components/Preview(View)、**../examples/*.particle.json ×8**（webpack JSON import）、./curves、./edits、./texture_edit |
| emitter.js | ./texture_edit(Texture)、vscode、**wintersky**(npm)、./components/Preview(View)、./event_sub_effects、./util(bbuid) |
| texture_edit.js | ./util(IO)、vscode、./input_structure |

## Stub 方案

用 Node `--import` 注册 customization hooks（module.register，resolve/load hook），把以下 specifier
映射到 stub 模块，**不改动 Snowstorm 源文件**：

1. `vue` → stub：`nextTick(cb)` 收为 no-op 队列（**实证 2026-09-29**：curves.js 仅用 Vue.nextTick，
   回调里是 Curve.vue 的 updateSVG UI 刷新，模型层无数据副作用；oracle 不跑回调即可，记录偏离）。
   若发现 Vue.set/observable 使用再补 stub。
2. `./vscode_extension` → `export default { postMessage(){}, available:false }`（按 vscode_extension.js 实际导出形态核对）
3. `./components/ExpressionBar` → **实证（ExpressionBar.vue:24-31）**：`export const ExpandedInput =
   { input: 0, axis: null, updateText(){}, setup: false }`——setup 保持 false，input.js:186 的
   `ExpandedInput.setup &&` 守卫自然短路 UI 路径，oracle 零行为偏差
4. `./components/Preview` → `export const View = { ... }`（读 Preview.vue 的 View 对象字段，stub）
5. `three` → 真实 three（build/_oracle/node_modules 已有 three@0.134.0，MathUtils 可用真身）
6. `wintersky` → 真实 wintersky@1.3.3（build/_oracle/node_modules 已有）
7. `../examples/*.particle.json` → load hook 直接 JSON.parse 返回 default export
8. 全局：`window.localStorage`（options.js 实证用 window.localStorage + 裸 localStorage 两处，内存 Map
   stub 两者）；`window.scrollX/scrollY`（util.js getBoundingClientRect 助手，UI-only stub 0）；
   `window.input`/`window.chooseFile`（debug 与 IO，stub no-op）；`requestAnimationFrame`

## 注意

- QuickSetup 定义在 emitter.js 内（import.js 从 emitter import QuickSetup）——不是 QuickSetup.vue。
- edits.js 依赖 export.js（generateFile）→ oracle 加载 edits 即拉起导出链；单测 util 时可绕开。
- canvas 2D（texture_edit.js）在 Node 无实现：texture oracle 用「操作序列→最终像素数组」比对，
  JS 侧需 node-canvas 或将 brush/fill 操作抽象后注入 fake ctx（记录像素写入序列）。优先 fake ctx
  方案：TextureClass 的 ctx 调用点收口后注入记录型 proxy。
- 冻结 golden 输出到 src/test/resources/snowstorm/*.json；脚本归 scripts/snowstorm-oracle/。

## wave-3 落地差异（2026-09-29 harness 实证）

harness 已落地（loader.mjs + hooks.mjs + stubs/ + run_cases.mjs），与原方案的差异：

1. `vscode_extension` **未 stub**：真身在 Node 安全评估（`typeof acquireVsCodeApi == 'function'`
   为 false，`document` 引用在 `if (vscode)` 分支内不触达），`export default false`，零偏差。
2. **webpack 无扩展名相对导入**（`'./input'`）：resolve hook 捕获 ERR_MODULE_NOT_FOUND 后补
   `.js` 重试（原方案未提及，必需）。
3. Snowstorm 源无 package.json `type` 字段：load hook 对 `/_snowstorm_src/**.js` 直接声明
   `format: 'module'`，消除 reparse 警告。
4. bare 包映射到 **ESM 构建**（wintersky `dist/wintersky.esm.js`（有 `export default`）、
   three `build/three.module.js`、molangjs `dist/molang.esm.js`）；tinycolor2 只有 CJS，
   走 createRequire 解析真身（Node CJS-ESM 互操作）。wintersky.esm 内部 bare import 回本 hook。
5. **循环 import 入口顺序**（TDZ 实证）：gradient.js 直接作入口崩溃
   （`Cannot access 'Input' before initialization`，环 gradient→input→edits→export→
   input_structure）。必须先经 input_structure.js（或 emitter.js）热身：input.js body 在环上
   遇 in-progress 跳过而能先完成，与 webpack 实际加载顺序一致。run_cases.mjs gradient 段
   已内置热身 import。
6. 额外全局 stub（原方案未列）：`InputEvent`/`KeyboardEvent`（edits.js registerEdit 的
   `instanceof` 必需，否则 ReferenceError）、`Image`（onerror 立即触发）、
   `document`（fake canvas/ctx 仅保 TextureClass 可构造；像素级 texture oracle 需换记录型
   proxy，同原方案结论）。
7. `Math.random` 替换为 mulberry32（seed 0x5EED1234）：golden 跨运行逐字节可复现
   （已 diff 验证）。Java 对拍不比随机产物，gradient 点 id 已在 golden 中归一化剔除。
8. `lineify` 未含入首个用例集：它是 import.js `updateInputsFromConfig` 的闭包局部函数，
   未导出，不改源无法直接调用；由 wave-2 import 用例经 updateInputsFromConfig 覆盖。

运行：`node --import ./scripts/snowstorm-oracle/loader.mjs scripts/snowstorm-oracle/run_cases.mjs`
（26 用例，golden → src/test/resources/snowstorm/snowstorm_cases.json）
