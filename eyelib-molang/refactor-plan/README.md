# Eyelib Molang 语法解析器重构计划

## 目的

本目录替代旧的 refactor-plan，基于 2026-05-02 形式化审计的**可证明事实**制定重构计划。旧计划的根本问题：基于乐观的 design draft 而非可验证的代码证据，导致文档与实现持续漂移。

## 与旧计划的关键区别

| 维度 | 旧计划 | 新计划 |
|---|---|---|
| 依据 | design drafts（讨论稿） | 审计报告中的可证明缺口 |
| 组织方式 | 按"阶段"编排（Phase 0-6） | 按"问题类型"编排（每类有已验证的解决模式） |
| 验证方式 | 口述 gate criteria | **每个 claim 附带可定位到文件行号的证据链** |
| 进度追踪 | ROADMAP.md 中的 KR 表格 | 每个计划文件末尾的 check-list，完成即勾选 |

## 问题清单（按严重性排序）

| # | 问题 | 类型 | 严重性 | 计划文件 |
|---|---|---|---|---|
| P1 | 手写解析器缺失 `<` `<=` `>=` 运算符 | 解析器完整性 | **严重** | `01-operator-completeness.md` |
| P2 | `active()` 前端与生产编译器脱钩 | 架构耦合 | 高 | `02-frontend-consolidation.md` |
| P3 | 全流水线测试仅3个（仅覆盖"1+2"） | 测试覆盖 | 高 | `03-test-expansion.md` |
| P4 | ROADMAP中声称的3个测试文件不存在 | 文档漂移 | 高 | `04-documentation-verification.md` |
| P5 | MolangOwnerSet已删除但ROADMAP称"未开始" | 文档漂移 | 高 | `04-documentation-verification.md` |
| P6 | 生成解析器与手写解析器零交叉验证 | 测试覆盖 | 高 | `03-test-expansion.md` |
| P7 | 5种AST类型零直接测试 | 测试覆盖 | 中 | `03-test-expansion.md` |
| P8 | 6种延迟构造静默返回null | 语义完整性 | 中 | `05-deferred-semantics.md` |
| P9 | 箭头访问是字节码存根 | 语义完整性 | 中 | `05-deferred-semantics.md` |
| P10 | HostContext使用Class<?>而非设计中的HostRole<T> | 设计-实现鸿沟 | 中 | `06-host-context-alignment.md` |
| P11 | `compiler/diagnostic/` 死文档指针 | 文档漂移 | 低 | `04-documentation-verification.md` |
| P12 | ROADMAP中3处文件路径不准确 | 文档漂移 | 低 | `04-documentation-verification.md` |

## 全局规则

1. **可证明性**：每个修复必须有对应的测试，测试失败则修复不完整
2. **JetBrains MCP 强制**：所有 Gradle 命令必须通过 `jetbrain_run_gradle_tasks` 执行
3. **generated/ 只读**：不得编辑 `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/`
4. **增量修改**：每步修改后运行 `jetbrain_run_gradle_tasks :eyelib-molang:test` 验证
5. **测试先行**：每个问题先写失败的测试，再修复代码
6. **ROADMAP 同步**：每次修改涉及 phase status/milestones/evidence 时更新 ROADMAP.md

## 执行顺序

```
P1（运算符补全）──┐
                  ├── 可并行 ──► P4（文档修复）──► P5（HostContext）
P2（前端统一）──┘                P6（交叉验证）
                                 P3（测试扩展）
```

## 验证命令

- 单模块：`jetbrain_run_gradle_tasks :eyelib-molang:test`
- 全量：`jetbrain_run_gradle_tasks :eyelib-molang:test :eyelib-importer:test :eyelib-preprocessing:test :test`
