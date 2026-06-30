# T6 · 重复内容排查

## 改动清单（供 T7 验证）
- `docs/molang/design/README.md` Repository Context：3 行逐字重复详情 → 1 行定位 + 指向 `molang-ast-and-semantics-draft.md` 的 "Repository-Specific Boundary Constraints" 章节。

## 排查范围与结论

### 已处理的重复
design/README.md `Repository Context`（原 line 37-40）与 `molang-ast-and-semantics-draft.md` `Repository-Specific Boundary Constraints`（line 18-23）几乎逐字重复（路径、flat-merge 说明、generated/ 区）。draft 版本更完整（多 ADR-0015 引用 + 未来变更约束）。README 作为索引保留定位价值，详情指向 draft。

### 扫描后判定为"非有害重复"
- **references/（48 文件）**：大小分布合理（最大 codec-design 16KB，多数 4-6KB）。深度排查记录之间的内容交叉是各 reference 针对不同问题的自然特征，每个 reference 自包含，不做过度去重。
- **eyelib-debug vs progressive-exploration**：两者都涉及 /eval 调试，但职责清晰分离——前者是 MCP 工具使用手册（工具表 + /eval 语法 + 渲染诊断），后者是系统性探索方法论（probe→act→assert + UI 导航 + 状态检查）。startup guard 的端口检查/minecraft.stop() 信息重叠是安全约束冗余。
- **testing vs unit-test/smoke-test**：testing/SKILL.md 描述明说 "Does NOT describe how to write them"，有意只做决策引导。

## 判断标准
- **有害重复** = 相同文本在多处出现，需同步维护，一处改则处处改。→ 处理。
- **安全冗余** = 同一操作从不同视角描述，或安全约束需多处强调。→ 保留。
