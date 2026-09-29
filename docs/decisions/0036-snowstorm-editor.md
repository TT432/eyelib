# ADR-0036: Snowstorm 粒子编辑器游戏内完整复刻

> 状态：Accepted (2026-09-29)
> 设计规格全文：[../concepts/snowstorm-editor-port.md](../concepts/snowstorm-editor-port.md)
> 前置：ADR-0034（wintersky as-is 移植）、ADR-0035（wintersky 融入）、ADR-0028/0031（LDLib2 前置语义）

## 背景

wintersky 移植与游戏内渲染闭环（wu7 预览界面）完成后，用户要求下一步完整复刻 Snowstorm
编辑器。Snowstorm v3.2.2 源码已全量盘点（~200KB，数据层 vue 无关可 as-is 移植，UI 层仅作行为参照）。

## 用户决策（2026-09-29 ask 当场确认，规格 §1）

| # | 决策 |
|---|---|
| U1 | 完整复刻（表单/曲线/事件/渐变/贴图绘制/QuickSetup/CodeViewer/子效果全量） |
| U2 | 游戏内全屏 Screen，预览为编辑器内嵌 3D 舞台（自由轨道相机，脱离世界） |
| U3 | 贴图编辑器 V1 即复刻（brush/eraser/fill/undo 全套） |

## 决策

| # | 决策 | 要点 |
|---|------|------|
| D1 | **双层模块** | `io.github.tt432.eyelib.snowstorm`（顶层模块，零 MC，数据层 as-is）；`client/gui/snowstorm`（LDLib2 UI 适配） |
| D2 | **UI 栈 = LDLib2** | 表单密集编辑器用 retained-mode 工具包；保持可选前置语义，缺失时入口关闭（同 nodegraph 编辑器） |
| D3 | **数据流 as-is** | Input.value→Config.set 回写、registerEdit 600ms 合并、generateFile/setFromJSON 对称、select meta_value 反查等 Snowstorm 行为全保留 |
| D4 | **内嵌 3D 舞台** | FOV45 透视 + 轨道相机（OrbitControls 数学 as-is）+ 网格/轴/参考方块；渲染复用 WinterskyParticleRenderer；帧驱动 tick 节流规则 as-is |
| D5 | **贴图编辑器纯栅格 domain** | `int[]` ABGR + 工具逻辑 as-is；MC 侧 NativeImage/DynamicTexture 桥接；canvas 抗锯齿偏离记录（R4） |
| D6 | **导入导出本地化** | 导出写 `run/snowstorm_exports/`+剪贴板；导入 FileDialogService（+onFilesDrop 待核对 C3）；VSCode 通道剔除 |
| D7 | **三层验证** | Node golden oracle（数据层）+ ldlib2-uitest in-client 场景 + 实机截图与网页版并排比对 |

## 关键不变量

- I1 `eyelib.snowstorm..` 零 MC import（纳入 ArchUnit 白名单守卫，同 wintersky）。
- I2 snowstorm 只依赖 `eyelib.wintersky` 公共 API，不修改其文件（I3 of ADR-0035 延伸）。
- I3 `import com.lowdragmc.*` 仅限 `client/gui/snowstorm` 包。
- I4 编辑器 Screen 关闭时发射器/纹理/监听全部释放；子效果注册表会话级。
- I5 导入异常不抛穿 Screen（WarningDialog + unsupported_fields，as-is）。

## 理由

- 数据层 as-is 移植继承 wintersky 移植已验证的方法学（Node oracle 逐值比对），把「编辑器导出的 JSON」
  与 Snowstorm 网页版字节级对齐，消除工具链失真。
- UI 层不逐字移植 Vue 代码（无意义），但布局/交互/默认值/节流规则 as-is，保证用户从网页版迁移零学习成本。
- LDLib2 复用避免在 eyelib.ui 上手写 ~80 个表单控件；可选前置语义与 nodegraph 编辑器一致，不新增部署负担。

## 后果

- 新增一个顶层模块（MODULES.md 重生成）与 ~30 个 client UI 类；分期 P1–P6（规格 §5）。
- 代码库持有 GPL-3.0 移植资产（Snowstorm assets/examples），保留许可声明。
- 26.1 无 ldlib2-uitest 框架，in-client UI 测试仅覆盖 1.20.1/1.21.1（R1）。

## 验证

- G1：ArchUnit 谓词扩展覆盖 snowstorm 包零 MC。
- G2：`scripts/snowstorm-oracle/` golden 测试全绿（导出/导入/曲线/渐变/贴图）。
- G3：`-PldTest` 场景 + 实机截图与 Snowstorm 网页版并排比对。
