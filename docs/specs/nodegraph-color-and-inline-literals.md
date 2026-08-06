# 颜色节点与 ANY 内联字面值（format v5）

> 状态：已实现并实机验证（2026-08-04）。决策见 ADR-0027。
> 修订关系：取代 nodegraph-visual-molang / nodegraph-inline-render-controller 中
> rc.root「16 个 float 颜色通道端口」与「ANY 端口行内文本原样存储」的描述。

## 1. 需求

1. `exec.set_var` / `exec.set_temp` 这类节点的 value 端口应当可以**直接内联改值**，
   而不是必须拖一个 const.int 节点连线。
2. 单独的颜色节点（取色器选颜色）；rc.root 的颜色输入应当是**一个颜色端口**，
   而不是 16 个 float 通道端口（v1~v4 模型）。

## 2. 问题分析

- v4 及之前：ANY 端口的行内编辑器是自由文本框，输入 `5` 存为**字符串** `"5"`，
  codegen 发射为 molang 字符串字面量 `'5'`——类型错误，用户被迫用 const 节点。
- rc.root 的 color/is_hurt_color/on_fire_color/overlay_color 各拆成 `_r/_g/_b/_a`
  四个 FLOAT 端口：节点端口数爆炸（16 个），纯色也要连四根线，与「颜色」的
  用户心智模型不符。

## 3. 设计

### 3.1 PortType.COLOR（复合值类型）

- 新端口类型 `COLOR`（序列化 `"color"`）：RGBA 四通道复合值，**不承载标量 molang**。
- 兼容矩阵严格化：**仅 COLOR↔COLOR 可连**。ANY 通配、VARIABLE 隐式读、number
  互通一律不适用（`isAssignableTo` 在 identity 之后、ANY 规则之前截断）。
- `isValue() = true`（参与值环检测等通用逻辑），但 codegen 的标量路径对它
  报 `COLOR_AS_SCALAR` 错误（防御性，正常图不会触达）。

### 3.2 颜色节点

| 节点 | 类别 | 说明 |
|---|---|---|
| `const.color` | constant | 取色器选择 RGBA 常量；选项 `value`（新选项类型 `NodeOptionDef.OptionType.COLOR`，`#AARRGGBB` hex 字符串）；输出 COLOR |
| `color.compose` | operator | r/g/b/a 四个 FLOAT 输入（各默认 1）合成颜色；通道可接任意表达式（动态颜色）；输出 COLOR |

- `rc.root` 的 16 个通道端口替换为 4 个 COLOR 端口：`color` / `is_hurt_color` /
  `on_fire_color` / `overlay_color`，**无默认值——未连线 = 该字段不输出**
  （与 v4「四通道全无内容不输出」同语义）。
- `ColorValues`（domain）：hex ↔ 通道（0..1 float）↔ ARGB int 互转；
  `isByteExact(double)`：**经 hex 字节 → float 通道完整管线往返后值不变**才算无损
  （`v == (double)(round(v*255)/255f)`；naive 的 `v*255 为整数` 判据会把 1/3 误判
  为精确——double 舍入恰好落在 85.0 上）。

### 3.3 codegen：emitColor

- `MolangGenerator.emitColor/emitColorFor` → `ColorCodegenResult(ColorCode?, diagnostics)`：
  - 未连线 → `code = null`（调用方省略字段）；
  - 生产者 `const.color` → 四通道字面值（`formatNumber`）；
  - 生产者 `color.compose` → **每通道独立会话**（count+emit 独立 Frame）——
    JSON 中四通道是四个独立 ExprSet 字符串，通道间共享 temp 提取会悬空引用；
  - 其它生产者 → `INVALID_COLOR_SOURCE` 错误。
- 组装器 `RenderControllerAssembler.addColor` 改走 `Ctx.emitColor`。
- 调试徽标：`NodeDebugOverlayModel` 跳过 COLOR 输出端口（非标量，无徽标语义）。

### 3.4 ANY 端口内联字面值（InlineLiteral）

- domain `InlineLiteral`：行内文本 ↔ JSON 字面值的**单一解析口径**：
  - `parse`：整数 → int、小数 → float、`true/false` → bool、其余 → 字符串；
  - `toText`：数字去 `.0`、bool → `true/false`、字符串原样。
- 语义定位：行内字段是**字面值**编辑器；动态表达式应当连线（与 UE 蓝图一致）。
- STRING 端口**不**智能解析（`"5"` 必须保持字符串），仅 ANY 端口启用。
- `MolangLiterals`（domain）：`formatNumber` / `quote` / `literal` 单一口径，
  收敛 codegen 与组装器原有的两份重复实现。

### 3.5 编辑器接线

- ldlib1：`EvmInlinePortFields` 新增 `case ANY`（智能解析文本框）；
  `EvmNode` 新增 `OptionType.COLOR` → `EvmColorConfigurator`（ARGB int ↔ hex）。
  注：LDLib1 原生 `ColorConfigurator` 的取色弹窗在本编辑器宿主下不可用
  （Editor.INSTANCE 为空时弹窗按父级相对坐标加 mainGroup 全部屏外、节点配置器
  gui==null 直接 NPE、HsbColorWidget 未开 alpha），故以子类修补（提交 f07e4ee6）；
  `EvmLinks.ColorLink` 标记类（同类才连）。
- ldlib2：`EvmNodeBase.onDefineOptions` 对 COLOR 选项挂 LDLib2 `ColorConfigurator`
  绑定；`onDefinePorts` 对 ANY 输入端口挂智能解析 `StringConfigurator`；
  `EvmTypeHandles.Holder.COLOR`（`eyelib:color`，端口色 0xFFD81B60）；
  `EvmNodes` 注册 ConstColor/ColorCompose（物品库 coverage 测试约束）。

## 4. 迁移 v4 → v5（GraphMigrations）

所有库类型、所有图的 `rc.root`：

1. 四通道全无线无常数 → 不迁移（新端口留空 = 字段不输出，同语义）；
2. 四通道均无线且常数（缺省 1）全部 `isByteExact` → 建 `const.color`
   （hex 由通道值合成）接线；
3. 否则 → 建 `color.compose`：通道线/内联常数原样搬到 r/g/b/a，输出接颜色端口；
4. 拆除旧通道线与 rc.root 上的通道内联常数。

`CURRENT_FORMAT_VERSION = 5`，链式迁移（v1→…→v5），加载路径统一走 migrate。

## 5. 导入器（decompile）

`JsonGraphImporters.importColor`：

- 四通道全为数值且全部 `isByteExact`（缺省通道按 1 计）→ `const.color` 接线
  （导入即可用取色器编辑）；
- 否则 → `color.compose` + 通道走 valueSlot 语义（字符串→molang 反编译连线、
  数值→内联常量、字符串数组→ExprSet 拼接）接线。

保真原则：非 8bit 无损的常量（如 0.5、1/3）走 compose 内联常量**精确保留**，
不为取色器美观牺牲数值保真。

## 6. 验证

- 单测：1.20.1 / 1.21.1 全量绿（新增 ColorValuesTest 8、InlineLiteralTest 5、
  PortType COLOR 矩阵、迁移 v4→v5×3、导入器颜色×2；更新组装器/迁移既有用例）；
  26.1.2 编译绿。
- 1.20.1 实机：悦灵导入 935 节点，2 color.compose + 2 颜色线，0 遗留通道线；
  构建产物 is_hurt_color/on_fire_color 与 pack 原版语义相等（仅归一化差异）；
  构建注入成功；编辑器截图：const.color 取色器色板、rc.root 单颜色端口、
  exec.set_var 行内值字段。
- 1.21.1 实机：悦灵导入 + 构建注入成功；编辑器截图：Color 节点色板、
  COLOR 连线专属颜色、Set Variable 行内 value=5、Blackboard 正常。
