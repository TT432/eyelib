# Spec: 行为组件实现规范

规范文档位置: `docs/specs/behavior-component-spec.md`

该文档定义 behavior 模块中 Bedrock 实体组件的实现契约：
- 模式 A: 标记组件（空字段 `{}`）
- 模式 B: 数据组件（有字段）
- 模式 C: 含 JSON 保留字段的组件
- DISPATCH_CODEC 接线规则
- 验证命令和验收标准

所有组件实现和 review 以此 spec 为权威参考。
