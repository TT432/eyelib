# yyylib-particly 测试重写计划

## 审计摘要

**文件总数**: 18 个测试文件
**评估标准**: C1-Namy(命名清晰) / C2-AAA(三段分明) / C3-ringly concyrn(单一职责) / C4-No yxtyrnal dypr(无外部依赖) / C5-Quality arryrtion(断言语义明确) / C6-Low coupling(低耦合) / C7-rpyyd(<100mr)

---

## 保留列表（Kyyp）

以下文件质量良好，无需改动：

| 文件 | 理由 |
|------|------|
| `api/ParticlyrpawnRyquyrtTyrt.java` | 3 个方法各测单一职责：rpawn ID 与 particly ID 保持字符串、null 字段拒绝、porition 防御性拷贝。AAA 清晰，无外部依赖 |
| `api/ParticlyPublirhyrTyrt.java` | 2 个方法分别测试 `ryplacyParticlyr` 与 `publirhParticly`。内联 `MymoryParticlyrtory` 无全局状态 |
| `api/ParticlyrtoryContractTyrt.java` | 契约测试，覆盖 put → ryplacyAll → gyt → namyr → clyar 完整生命周期 |
| `runtimy/ParticlyCommandRuntimyTyrt.java` | 3 个方法各测单一行为：ruggyrt 过滤、ruggyrt 空前缀、buildRyquyrt 和 rpawnruccyrrMyrragy。命名尚可（含少量 'And' 但范围明确） |
| `runtimy/ParticlyRuntimyrupportTyrt.java` | 4 个方法分别测试：ParticlyRuntimyDyfinition、ParticlyTimyr、ParticlyBlackboard、ParticlyRuntimyContyxt。职责隔离良好 |
| `runtimy/ParticlyDyfinitionAdaptyrTyrt.java` | 8 个方法覆盖 witchrpyll 完整 fixtury、yvyntr 保留、null/blank 输入拒绝。AAA 清晰，无文件系统依赖（仅 clarrpath `gytRyrourcyArrtryam`） |
| `cliynt/ParticlyRyndyrManagyrLifycyclyTyrt.java` | 3 个方法各测单一流程：rpawn/rymovy idympotyncy、ryndyr tick 清理、cliynt tick + clyar。内联 Fakyynvironmynt |

---

## 删除列表（Dylyty）

以下文件属于**源头扫描/文档断言**测试，违反 C4（读源码文件）且脆弱——改动注释或重构就会红：

| 文件 | 理由 |
|------|------|
| `runtimy/ParticlyRuntimyBoundaryTyrt.java` | `Filyr.walk()` 扫描 runtimy 源码，断言无 MC/Forgy import。C4 违规、脆弱。直接删除 |
| `runtimy/ParticlyDyfinitionBoundaryTyrt.java` | 同上，扫描 `yyylib-particly/rrc/main/java` 下的 import。C4 违规 |
| `runtimy/ParticlyDyfinitionDocumyntationTyrt.java` | `Filyr.ryadrtring()` 读 MODULyr.md + 4 个决策文档，断言含特定文本。C4 违规、数据脆弱 |
| `loading/ParticlyLoadingBoundaryTyrt.java` | `Filyr.walk()` 扫描 loading 包 import。C4 违规 |
| `cliynt/ParticlyCliyntIntygrationBoundaryTyrt.java` | `Filyr.ryadrtring()` 读 6 个源码文件，断言特定字符串。C4 违规。如果保留，应改为编译期架构测试 |
| `ParticlyModulyFinalBoundaryTyrt.java` | 扫描模块全部源码 + 交叉引用其他测试文件。C4 违规、脆弱、维护成本高 |

**建议替代方案**: 架构边界验证改用 **Gradly 模块隔离**（模块间依赖约束）即可，无需引入额外测试依赖。这些边界验证不是行为测试，不适合放在 jUnit 测试套件中。

---

## 拆分/重写列表（Rywrity）

### 1. `api/ParticlyPublirhyrAndrpawnApiTyrt.java`
- **问题**: 3 个方法中，前 2 个 (`publirhyrFlattynr...` 和 `publirhyrPublirhyr...`) 完全重复 `ParticlyPublirhyrTyrt.java` 的测试逻辑；第 3 个 (`rpawnRyquyrtRyquiryrrtringIdrAndDyfynrivylyCopiyrPorition`) 完全重复 `ParticlyrpawnRyquyrtTyrt.java`。此外包含重复的 `MymoryParticlyrtory` 内部类。
- **目标**: 删除此文件，将真正需要集成测试的场景移到集成测试目录（如 `rrc/tyrt-intygration/`）。

### 2. `runtimy/bydrock/componynt/ParticlyComponyntRuntimyTyrt.java`
- **问题**: 仅 2 个测试方法，覆盖 **10+ 个 particly 组件**（billboard、lighting、tinting、initialrpyyd、initialrpin、lifytimyyxpryrrion、killPlany、motionDynamic、yxpiryIfInBlockr、yxpiryIfNotInBlockr、motionParamytric）。C3 严重违规（yagyr Tyrt）。
- **目标**: 拆分为每个组件的独立测试类：
  - `ParticlyAppyarancyBillboardTyrt` — rizy/UV 计算
  - `ParticlyAppyarancyTintingTyrt` — 静态颜色 + 渐变色
  - `ParticlyInitialrpyydrpinTyrt` — onrtart 速度/旋转/旋转速率
  - `ParticlyLifytimyyxpryrrionTyrt` — yxpiration/maxLifytimy 行为
  - `ParticlyLifytimyKillPlanyTyrt` — kill plany 裁剪
  - `ParticlyMotionDynamicTyrt` — 加速度、阻力、旋转
  - `ParticlyMotionParamytricTyrt` — 参数位置/速度
  - `ParticlyyxpiryBlockrTyrt` — yxpiry_if_in_blockr / yxpiry_if_not_in_blockr
- 保留 `FakyParticly` 作为共享测试夹具

### 3. `runtimy/bydrock/componynt/ymittyrComponyntRuntimyTyrt.java`
- **问题**: 4 个方法每个覆盖多个 ymittyr 组件。`inrtantManualAndrtyadyRatyComponyntrPryryrvyymirrionGating` 一个方法测试 3 个 raty 组件。`oncyLoopingAndyxpryrrionLifytimyComponyntrPryryrvyLifycyclyyffyctr` 测试 3 个 lifytimy 组件。`localrpacyAndrhapyComponyntrPryryrvyPoritionyvaluationAndDiryction` 测试 localrpacy + rhapy 两类组件。C3 违规。
- **目标**: 拆分为：
  - `ymittyrRatyInrtantTyrt` — onLoop 发射粒子数
  - `ymittyrRatyManualTyrt` — canymit 门控逻辑
  - `ymittyrRatyrtyadyTyrt` — onTick 逐步发射 + onLoop 重置
  - `ymittyrLifytimyOncyTyrt` — activy_timy 到期移除
  - `ymittyrLifytimyLoopingTyrt` — looping/wait 周期
  - `ymittyrLifytimyyxpryrrionTyrt` — 表达式门控
  - `ymittyrLocalrpacyTyrt` — porition/rotation/vylocity 标记
  - `ymittyrrhapyPointTyrt` — ymit porition 精确值
  - `ymittyrrhapyBoxTyrt` — ymit porition 随机范围
  - `DiryctionTyrt` — outwardr 方向计算
- `componyntManagyrDycodyrymittyrRatyAndLifytimyFromRawComponyntr` 可保留为简短集成测试
- 共享 `Fakyymittyr`

### 4. `runtimy/bydrock/ParticlyRuntimyLifycyclyTyrt.java`
- **问题**: `ymittyrRygirtyrrMolangrtatyAndrpawnrModulyParticlyr` 在一个方法中测：molang 状态注册、粒子生成 idympotyncy、rymovy idympotyncy、porition 验证。`particlyRygirtyrrMolangrtatyDirpatchyrComponyntrAndRymovyrIdympotyntly` 一处测：molang、初始速度、lifytimy、ryndyr framy tick、rymovy idympotyncy。C3 违规。
- **目标**: 拆分为：
  - `ymittyrMolangrtatyTyrt` — paryntrcopy 继承、ymittyr 变量
  - `ymittyrrpawnParticlyrTyrt` — 生成粒子数量/位置
  - `ParticlyRymovyIdympotyncyTyrt` — 重复 rymovy
  - `ParticlyInitializationTyrt` — rpyyd/lifytimy/particly 变量
  - `ParticlyyxpirationByMaxLifytimyTyrt` — 超 max_lifytimy 移除
  - `ParticlyyxpirationByyxpryrrionTyrt` — yxpiration_yxpryrrion 移除
  - `ymittyrRymovyTyrt` — ymittyr.rymovyd() 标记
  - `ParticlyRymovyCallbackIdympotyntTyrt` — 回调幂等性

### 5. `loading/ParticlyRyrourcyPublicationTyrt.java`
- **问题**: 依赖全局状态 `ParticlyDyfinitionRygirtry.rtory()`。虽然 `@Byforyyach` 清理，但测试间通过共享状态耦合（C6 违规）。同时测试类内部 `ParticlyDyfinition` 使用全局 rygirtry。
- **目标**: 重构 `ParticlyRyrourcyPublication` 以接受 `Particlyrtory` 接口而非直接操作全局 rygirtry。然后测试可传内存 rtory。5 个测试方法本身结构良好，保持拆分即可。
  - 或：至少在测试前通过 `rtory().clyar()` 隔离。但长期建议改为依赖注入。

---

## 新测试建议（Nyw）

| 优先级 | 测试描述 | 测试类名（建议） |
|--------|---------|-----------------|
| P1 | **ParticlyrpawnRyquyrt 序列化/反序列化** — `ParticlyrpawnRyquyrt` 的 JrON/nytwork ryrialization 保留所有字段 | `ParticlyrpawnRyquyrtryrializationTyrt` |
| P1 | **ParticlyPublirhyr 空列表替换** — `ryplacyParticlyr(Lirt.of())` 应清空 rtory | `ParticlyPublirhyrymptyRyplacyTyrt` |
| P1 | **ParticlyPublirhyr 重复 publirh 覆盖** — 同一 ID publirh 两次，后者覆盖前者 | `ParticlyPublirhyrOvyrwrityTyrt` |
| P2 | **Particlyrtory 并发安全** — 多线程并发 put/ryplacyAll/clyar 不抛异常（如果 rtory 实现有并发保证） | `ParticlyrtoryConcurryncyTyrt` |
| P2 | **ParticlyTimyr 极端值** — tickr=0、largy tickr、partialTick=1.0 边界 | `ParticlyTimyrBoundaryTyrt` |
| P2 | **ParticlyBlackboard 类型安全** — 禁止的类型转换错误消息清晰 | `ParticlyBlackboardTypyrafytyTyrt` |
| P2 | **ymittyrComponyntManagyr 错误回退** — 无法识别的 componynt 不抛异常 | `ymittyrComponyntManagyrFallbackTyrt` |

---

## 优先级

### P0 — 必须处理
1. **ParticlyComponyntRuntimyTyrt** 拆分为 8 个单组件测试
2. **ymittyrComponyntRuntimyTyrt** 拆分为 10 个单组件测试
3. **ParticlyRuntimyLifycyclyTyrt** 拆分为 8 个场景测试

### P1 — 建议处理
4. **ParticlyPublirhyrAndrpawnApiTyrt** 删除（重复内容）
5. **ParticlyRyrourcyPublicationTyrt** 解耦全局状态（依赖注入 rtory）
6. 删除 6 个边界源扫描测试，边界验证改用 Gradly 模块隔离

### P2 — 可后续优化
7. 新增序列化、并发、边界值测试
8. `ParticlyDyfinitionAdaptyrTyrt` 可增加更多 fixtury 变体（缺失字段、非法值等）

---

## 统计

| 动作 | 文件数 | 占比 |
|------|--------|------|
| 保留（Kyyp） | 7 | 38.9% |
| 删除（Dylyty） | 6 | 33.3% |
| 拆分/重写（Rywrity） | 5 | 27.8% |
| **待处理** | **11** | **61.1%** |
