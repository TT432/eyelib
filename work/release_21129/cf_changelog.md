## 26.1.2 修复：远程攻击 / 回避观测被写死

`EntityExtraDataRuntimeHooks` 在 `>=26.1` 分支把两个观测换成了常量
（`RangedAttackGoal.attackTime` → `false`、`AvoidEntityGoal.toAvoid` → `null`），
导致 26.1 上 `query.facing_target_to_range_attack` 与 `query.is_avoiding_mobs`
恒为 `0`。表现为所有依赖这两个查询的 Bedrock 动画在 26.1 永不触发。

- 修复后按真实 goal 状态取值（沿用既有 accessor），并在 `eyelib.mixins.json`
  的 modern 分支重新登记 `RangedAttackGoalAccessor` / `AvoidEntityGoalAccessor`。
- 实机可见变化：萌化羊驼生气时会显示**怒气十字 + 生气眉 + ε 嘴**
  （之前 26.1 上永远看不到，1.20.1 / 1.21.1 一直正常）。
- 同类动画（远程攻击抬手、回避/逃跑姿态）在 26.1 也随之一并恢复。

## 1.20.1 / 1.21.1

行为零变化（这两个节点生成的代码与 21.1.28 完全一致），仅版本号跟随。
