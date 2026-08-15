# AC 图化与粒子/音效引用（format v10）

> 状态：设计已确认（用户 2026-08-09 拍板：「AC 按 RC 处理 = State 之类单独拿出来，
> 目标形态 = RC Root 节点」），实现中。
> 修订关系：取代 nodegraph-animation-variable-refs 中「AC 图化已完成（ref.ac 纯引用）」
> 的表述——v10 把 AC 库根/状态/转换的字符串引用全部图边化，并新增粒子/音效引用体系。

## 1. 需求

1. AC 库按 RC Root 形态图化：states/transitions 是独立节点，所有跨节点引用都是
   **图边**而非字符串选项。
2. AC state 的粒子/音效（BE schema 的 `particle_effects`/`sound_effects` 字段）获得
   与几何体同等的引用节点待遇：ref.particle / ref.sound + 显式连线。
3. 实体级 `particle_effects`/`sound_effects` 声明表（短名→标识符）不再丢弃，
   与 animations 表同机制进出。
4. ref.particle / ref.sound 节点带**播放键**可预览（另见预览规格段，属后续工作单元）。
5. 缺失资源引用在验证器报 error、编辑器节点红色高亮（后续工作单元）。

## 2. 设计

### 2.1 AC 图边化

| 现状（v9） | v10 |
|---|---|
| `ac.root.initial_state` 字符串选项 | `ac.root.initial` SLOT 输入（单连接）← 初始 `ac.state.state` |
| `ac.transition.target` 字符串选项 | `ac.transition.target` SLOT 输出 → 目标 `ac.state.incoming`（SLOT 输入，multi） |
| `ac.root.identifier` 裸字符串选项 | `NodeOptionDef.asset`（ac 注册表下拉） |
| `ac.state.name` 可见字符串选项 | 保留为数据（导出 states 键），编辑器 UI 行隐藏（仅内部使用，同 short_name 处理） |

- `ac.state` 现有端口不变：on_entry/on_exit（EXEC；v13 起翻转为 OUT 时机源端口，
  规格 nodegraph-event-nodes / ADR-0029）、animations/transitions（SLOT）、
  blend_transition/blend_via_shortest_path（数值/布尔选项）。
- 导出：initial_state 由 initial 连线的 state 名推导，未连线则不输出该字段
  （BE 缺省 "default"）；transition 目标名由 target 边推导，未连线 → UNKNOWN_STATE error。
- SLOT 白名单新增：`ac.root.initial←ac.state`、`ac.state.incoming←ac.transition`、
  `ac.state.particles←particle.entry`、`ac.state.sounds←ref.sound`。

### 2.2 ref.particle / ref.sound 与实体声明表

- 新节点类型：
  - `ref.particle`（CAT_REF）：short_name + identifier（asset 下拉，particle 注册表）
    + out `ref`（新 PortType `PARTICLE_REF`）。
  - `ref.sound`：同形，PortType `SOUND_REF`。
- 实体级：`entity.root` 新增声明端口 `particles` / `sounds`（SLOT，multi），
  ref.particle/ref.sound 接线即入表（同 ENTITY_DECLARATION_PORTS 机制）；
  导出 `particle_effects`/`sound_effects` = 声明端口可达 ref 的 短名→identifier。
- 导入：实体 description 的 particle_effects/sound_effects 表 → 建 ref 节点接线；
  ENTITY_DESC_KEYS 增补两键（不再 UNKNOWN_FIELD）。

### 2.3 AC state 粒子/音效

- 新节点 `particle.entry`（CAT_AC）：选项 `locator`（string）、`bind_to_actor`（bool）；
  端口：ref 槽（SLOT ← ref.particle）、script（ANY，pre_effect_script molang 值槽）、
  out `entry`（SLOT → ac.state.particles）。列表顺序 = uid 序（导入序，无链）。
- `sound_effects`（BE 为短名字符串数组）→ 直接 ref.sound → ac.state.sounds 连线。
- 导入：state.particle_effects 数组 → particle.entry + 按需 ref.particle（跨状态去重）；
  state.sound_effects → 按需 ref.sound。AC_STATE_KEYS 增补两键。
- 导出：state.particles 槽 → `{effect, locator?, bind_to_actor?, pre_effect_script?}` 数组
  （script 连线才发射）；state.sounds 槽 → 短名数组。

### 2.4 迁移 v9 → v10

- ac.root.initial_state 选项 → 按名解析 state 建 initial 边（解析失败丢选项）；
- ac.transition.target 选项 → 按名建 target 边（解析失败丢选项）；
- 实体/AC 库的粒子音效表是 v10 新增支持，旧图无此数据，无迁移动作；
- CURRENT_FORMAT_VERSION = 10。

## 3. 前置 / 后置 / 不变量 / 异常

- 前置：v9 图（含变量流向翻转）；被引动画/粒子/音效内容在注册表中可获得。
- 后置：AC 库全部跨节点引用为图边；实体粒子/音效表往返保真。
- 不变量：导出 JSON 与源 schema 语义相等（键序/数组序不承载语义除外）；
  纯函数验证；编辑器不加载时域行为不变。
- 异常：initial/target 边未连 → UNKNOWN_STATE（组装 error）；
  particle.entry ref 槽接非 ref.particle → INVALID_ENTRY_REF；
  状态重名 → DUPLICATE_STATE（保留先者）。

## 4. 非目标

- ref.particle/ref.sound 的播放键预览、缺失引用红高亮（后续工作单元 D）。
- AC state 变量引用的额外端口（由 state exec 链内 variable 节点承担，v8 结论不变）。
- blend_transition 改 molang 值槽（BE schema 是 float，非 molang）。

## 5. 验证

- 单测：导入（粒子/音效/initial/transition 边）、导出（合写 + 数组序）、迁移 ×2、
  白名单/UNKNOWN_STATE；
- 实机：悦灵闭包重导，AC 库 initial/transition 全为边、粒子音效表与 pack 相等；
- 渲染层：编辑器 AC 图端口可见、identifier 为下拉、state 无名行。
