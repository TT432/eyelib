# T1 决策记录 · 过期快照剪枝

## D1 · audit 报告删除

**文件**: `docs/audits/2026-06-19-documentation-taste-review.md`(707 行)

**决策**: 删除。

**依据**:
- 审计是一次性 taste review,价值在发现,发现已全部落地修复。
- 核实的已修项:superpowers/ 目录已删、MODULES.md 已重生成(干净 30 行)、0009 文件名已去 .adr、0010 stub 已删、两 0013 已用 0013a 区分、ADR 索引补到 0018、concepts/architecture.md 已整篇重写。
- PLAN.md 问题清单(A1-A8/B1-B4/D1-D2)是基于当前事实**独立核实**得出,不依赖审计报告,删除审计不丢失任何待办。
- 审计报告自身贡献了全部 10 处 .hermes 残留 + 大半旧包名/子项目 task 残留,是当前文档熵的最大单点来源。
- AGENTS.md「Don't keep history in active docs」:过期快照误导大于告知。

## D2 · handoff 删除

**文件**: `docs/stonecutter-migration-handoff.md`(120 行, 2026-06-18)

**决策**: 迁移增量后删除。

**依据**:
- handoff 是过渡交接快照,信息分七类,逐一核实归属:
  1. 已完成里程碑(Phase 1/2)→ ADR-0015 Status「Phase 1 implemented — 1.20.1 node live; Phase 2+ pending」已概括。
  2. 设计决策(适配器隔离)→ handoff line 33-34 自身引用 stonecutter-multiversion-patterns.md 范式 2,无增量。
  3. ReflectAccess / Mixin 策略 → 实现期历史决策,mixin-writing skill 覆盖通用模式,具体保留/放弃清单是历史快照。
  4. 未完成工作(1.21.1 网络/运行时/测试、Phase 0/3-6)→ 已被后续工作超越(git log: 26.1.2 EventBusSubscriber 三路条件化、1.21.1 网络已实现)。
  5. **`//?` 注释踩坑(line 70-75)** → 唯一增量。4 条实战限制(闭合标记 `//?}`、行条件无 else、块内禁纯注释、sourceSet 手动替换)未被 patterns.md(设计期语法)或 eyelib-build SKILL 覆盖。**已迁移至 eyelib-build SKILL Common Pitfalls**。
  6. 文件改动清单 → git history 已记录。
  7. 接手优先级 → 过期。

**迁移去向**: handoff line 70-75 `//?` 踩坑 → `eyelib-build/SKILL.md` Common Pitfalls「Stonecutter `//?` 注释语法踩坑」。

**同步**: 删除后移除 `docs/README.md:19` 对 handoff 的导航引用。
