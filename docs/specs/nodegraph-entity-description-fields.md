# 实体 description 字段建模与原文直通

> 状态：已实现并实机验证（2026-08-10）。
> 起因：导入器对图语言表达不了的 description 字段只留便签（StickyNote 不参与语义），
> 构建时不回写——import→build 往返静默丢 spawn_egg/enable_attachables 等行为字段。

## 1. 需求

1. BE 标准 description 字段建模为 entity.root 选项：可编辑、参与构建发射。
2. 未建模字段一律原文直通：导入保留、构建回写，不丢任何数据。

## 2. 设计

### 2.1 建模字段（entity.root 选项）

| description 键 | 选项 | 类型 | 发射规则 |
|---|---|---|---|
| `enable_attachables` | 同名 | BOOL | 仅 true 输出 |
| `held_item_ignores_lighting` | 同名 | BOOL | 仅 true 输出 |
| `should_update_effects_offscreen` | 同名 | BOOL | 仅 true 输出 |
| `spawn_egg.texture` | `spawn_egg_texture` | STRING | 非空则组装 spawn_egg 对象 |
| `spawn_egg.texture_index` | `spawn_egg_texture_index` | INT | 非 0 才进对象 |
| `spawn_egg.base_color` / `overlay_color` | `spawn_egg_base_color` / `spawn_egg_overlay_color` | STRING（#rrggbb） | 非空进对象 |

- 源里显式 `false` 的布尔导出时消失——与缺省同语义（BE 缺省 false），接受该归一化。
- spawn_egg 两种形态（texture 形 / base_color 形）可混合，组装合并为单对象。

### 2.2 原文直通（extra_fields / extra_scripts）

- entity.root 隐藏选项 `extra_fields`（NodeTypes.EXTRA_FIELDS_OPTION，TEXT 类型存
  JSON 对象文本）：导入时把不在已知键白名单（ENTITY_DESC_KEYS）里的 description 键
  原样收入；构建时合回 description——**只补缺键，不覆盖已组装键**（建模字段优先）。
- scripts 对象内的未建模键同理走 `extra_scripts`（EXTRA_SCRIPTS_OPTION），构建合回
  scripts 对象。实证案例（A&S）：`variables` 可见性表、`should_update_effects_offscreen`
  被包放在 scripts 内且值是字符串 "1"——非标准位置，靠直通保真。
- 无 UI 行（EvmNodeBase.onDefineOptions 跳过，同 ref 节点 short_name 先例）；
  选项数据随图持久化、照常参与导出。
- 可见性不变：未知字段仍产 UNKNOWN_FIELD 诊断 + 便签（用户能在画布上看到原文）。

## 3. 前置 / 后置 / 不变量

- 前置：无（导入与构建两条既有路径内部扩展）。
- 后置：建模字段进 entity.root 选项；未知字段进 extra_fields + 便签 + 诊断。
- 不变量：import→build 不再丢失任何 description 字段；未建模字段逐字节保留。
- 异常：spawn_egg 非 object / 布尔字段非布尔 → INVALID_FIELD 诊断 + 便签，选项取缺省；
  extra_fields 文本非法 JSON（仅可能来自手改存档）→ 构建静默跳过。

## 4. 非目标

- RC/AC 文档的未知字段直通（RC_ENTRY_KEYS 同款便签路径）——机制可同构扩展，
  本次未观察到丢失证据，未做。
- description 之外位置（文件顶层）的未知键：便签保留但不直通（无语义挂载点）。

## 5. 验证

- 单测：entityDescriptionFieldsModelAsOptions / entityDescriptionFieldsRoundTrip /
  unknownFieldsBecomeStickyNotes（更新为真未知键 + extra_fields 断言）。
- 实机：悦灵重导后 entity.root 选项落位（enable_attachables 等）、构建 description
  含全部原字段。
