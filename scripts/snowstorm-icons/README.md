# lucide 图标光栅化管线（snowstorm 图标资产）

产出：`src/main/resources/assets/eyelib/snowstorm/icons/*.png`（59 个，48×48，#bcc3ca 描边，透明底）。

来源：lucide-static@0.298.0（Snowstorm package.json 的 lucide-vue 版本）CDN SVG →
浏览器 canvas 光栅化（`currentColor` → `#bcc3ca` 替换后 Image+canvas.drawImage）。

执行方式（2026-09-30 实证）：omp eval 的 browser 工具（js）打开空白 tab，分批 `tab.evaluate`
内 fetch+渲染（单批 ≤8 图标，evaluate 30s 上限），`window.__icons` 跨批暂存，
`Bun/node:fs` 落盘 PNG。透明底经角像素 alpha=0 抽查。

重跑场景：lucide 升级或新增图标时，按上述流程重跑；图标名清单来自
`build/_snowstorm_src` 各 .vue 的 lucide-vue import（PascalCase → kebab-case 转换）。
