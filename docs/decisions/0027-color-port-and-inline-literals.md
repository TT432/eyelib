# ADR-0027：COLOR 复合端口类型与 ANY 内联字面值

## 状态

已接受（2026-08-04）。取代 ADR-0021 系规格中 rc.root「16 个 float 颜色通道端口」
与 ANY 端口行内文本原样存储的行为。

## 背景

两个编辑器可用性问题：

1. `exec.set_var` 等节点的 value 端口（ANY）行内文本框把 `5` 存为字符串 `"5"`，
   codegen 发射为 molang 字符串字面量 `'5'`——类型错误，用户被迫拖 const.int 节点。
2. rc.root 的颜色是 4 字段 × 4 通道共 16 个 FLOAT 端口：端口爆炸、纯色要连四根线，
   与「颜色」的用户心智模型不符；用户要求取色器 + 单一颜色端口。

## 决策

1. **PortType.COLOR 严格隔离**：RGBA 四通道复合值，仅 COLOR↔COLOR 可连
   （ANY 通配、VARIABLE 隐式读一律不适用）。复合值不进标量 molang 上下文，
   从类型系统根除「颜色当 float 用」的整类错误。
2. **两个颜色节点**：`const.color`（取色器，`#AARRGGBB` hex 选项）与
   `color.compose`（r/g/b/a 表达式通道，各默认 1）。rc.root 收 4 个 COLOR 端口，
   未连线 = 字段不输出。
3. **颜色发射走专用通道**（`emitColor`）：四通道各自独立会话——JSON 中四通道是
   四个独立 ExprSet 字符串，共享 temp 提取会悬空引用。
4. **保真优先于美观**：`isByteExact` 判据对齐「hex 字节 → float 通道」完整管线，
   非无损常量（0.5、1/3）一律走 compose 内联常量精确保留；const.color 只承接
   管线往返无损的值。
5. **ANY 行内智能解析**（`InlineLiteral` 单一口径）：整数/小数/布尔自动识别，
   其余按字符串字面量。行内字段定位是字面值编辑器；动态表达式应当连线。
   STRING 端口不解析（`"5"` 必须保持字符串）。
6. format_version 4→5 链式迁移：通道常数全无损 → const.color；含表达式 →
   color.compose（线/常数原样搬迁）。

## 理由

- 严格 COLOR 矩阵的代价是 ANY 表达式不能直连颜色端口（必须经 compose），
  换来编译期类型安全；调试徽标等非标量上下文统一跳过 COLOR。
- 通道独立会话放弃了跨通道 temp 复用（重复表达式会重复计算），
  这是 ExprSet 字符串物理隔离的必然结果，正确性优先。
- 智能解析让 set_var/set_temp 的内联编辑第一次可用（此前产出错误类型），
  覆盖用户「赋字面值」的最高频操作。

## 后果

- v4 图加载自动迁移；RC 库与实体库同样迁移（rc.root 两处存在）。
- 编辑器：ldlib1 用 LDLib1 ColorConfigurator，ldlib2 用 LDLib2 ColorConfigurator，
  两版均挂智能解析文本框；COLOR 端口有专属连线颜色（ldlib2 0xFFD81B60）。
- 验证记录见 docs/specs/nodegraph-color-and-inline-literals.md §6。
