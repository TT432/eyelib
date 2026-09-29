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
