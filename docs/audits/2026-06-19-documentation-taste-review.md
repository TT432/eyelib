# 文档品味审查报告

**Date:** 2026-06-19
**Author:** opencode (深度审查 pass)
**Scope:** 仓库内所有面向 AI / 人的文档与配置,以及它们与实际代码的对照
**Output type:** Audit(审计),非 ADR(决策)、非 spec(规范)。放 `docs/audits/` 因为这是审查产物,不属于已有任何目录的职责。

---

## Executive Summary

eyelib 的**架构决策本身品味在线**——ADR-0010(六边形 + Gall's Law)、ADR-0014(承认 ArchUnit 收益不抵成本并主动退化)、ADR-0015(Stonecutter + 重新恢复 ArchUnit)是一组**有自我修正能力的决策链**,明显优于一般 AI 生成的"理论正确但脱离现实"的架构文档。

但**执行层严重跟不上决策**:多个 AI 接手留下的最大问题不是"决策错了",而是**前一个 AI 立的 ADR,后一个 AI 没回头同步前置文档**。`concepts/architecture.md` 是最刺眼的例子——它在 ADR-0014 之后就该重写,经过 ADR-0015(Stonecutter 落地)仍没人动。同时文档里把实际是"六边形架构(Ports & Adapters)+ 包边界模块化 + Stonecutter 多版本"的混合体笼统称为"DDD",会误导任何按 DDD 思维接手的人。

**一句话结论:架构决策品味在线,文档系统的"决策落地同步"几乎不存在,且对外架构标签错误。**

---

## 一、调研方法与范围

### 1.1 调研对象(全部读完或抽样核对)

| 类别 | 实际核查内容 |
|---|---|
| 顶层文档 | `AGENTS.md`、`MODULES.md`、`docs/README.md` |
| 所有 ADR | `docs/decisions/0001` ~ `0015`(共 16 个文件,含两个 0013) |
| 架构概念 | `docs/concepts/architecture.md`、`docs/architecture/domain-module-map.md`、`docs/architecture/0010-hexagonal-architecture.md` |
| Molang 子系统 | `docs/molang/ROADMAP.md`(210 行)、`docs/molang/design/README.md`、`docs/molang/refactor-plan/README.md` |
| 实施计划 | `docs/superpowers/plans/2026-06-16-flat-merge.md`(1109 行)、`docs/superpowers/specs/2026-06-17-stonecutter-migration-design.md`(412 行)、`docs/stonecutter-migration-handoff.md` |
| 行为规范 | `docs/specs/behavior-component-spec.md` |
| 全部 Skills | `.opencode/skills/eyelib/SKILL.md`、`.opencode/skills/eyelib-build/SKILL.md`(其余 11 个抽样) |
| 实际代码 | root / `bridge/` / `util/` / `molang/` 的 package-info 与目录结构,核对实际类与文档描述 |
| 实际配置 | `build.gradle`(473 行)、`settings.gradle`、`stonecutter.gradle`、`gradle.properties`、`versions/*/gradle.properties` |
| 业界参考 | [AGENTS.md 开放标准](https://agents.md/)、[Diátaxis 框架](https://diataxis.fr/) |

### 1.2 工作目录与术语约定

- 工作目录 `E:\_ideaProjects\qylEyelib`,平台 `win32`(纯 Windows PowerShell,**不是 WSL**)。
- 引用格式 `file:line`,例如 `MODULES.md:41` 指 MODULES.md 第 41 行。
- 本报告中 "ADR-NNNN" 指 `docs/decisions/NNNN-*.md`。
- "实际" / "代码侧" 指当前 git 工作树状态;"文档侧" 指 markdown 描述的状态。

---

## 二、用户的三个问题:直接回答

### Q1. 文档是不是过多导致 AI 总是不能将实际工作和文档联系在一起?

**是,但"多"不是核心问题,"碎片化 + 多源真相 + 缺乏单一入口"才是。**

#### 数量上确实偏多

| 维度 | 数量 |
|---|---|
| 顶层 markdown | 4 个(`AGENTS.md`、`MODULES.md`、`docs/README.md`、`docs/stonecutter-migration-handoff.md`) |
| ADR | 16 个文件(0001~0015,其中 0013 有两份) |
| `docs/` 子目录 | 8 个(`concepts/` `decisions/` `architecture/` `specs/` `gap-analysis/` `tests/` `molang/` `superpowers/`) |
| `docs/molang/` 子文件 | 24 个(ROADMAP + design 21 + refactor-plan 6 + design README + refactor-plan README) |
| `.opencode/skills/` | 13 个 skill |
| `package-info.java` | 全项目几十个 |

**核心问题不是数量,而是:**

1. **同一事实被多个文档反复描述,且不一致**(详见 §四 P3)。例如"domain 提取进度"在 `ADR-0010`、`domain-module-map.md`、`concepts/architecture.md`、`ADR-0015` 四处有四种说法;"Molang 当前状态"在 `ROADMAP.md`(Phase 0-7 表 + Active Milestones M1-M5 + OKR KR 表)、`refactor-plan/README.md`(P1-P12 问题清单)、`design/README.md`(草稿索引)三处描述。

2. **同一系统在不同文档用不同分层术语**(详见 §四 P3-13):
   - `concepts/architecture.md`:Root / Bridge / Domain / Adapter(4 层)
   - `ADR-0010`:Bridge / Domain / util / Adapter / Root(5 层)
   - `MODULES.md`:10 个分组
   新 AI 读完后**不知道信哪个**。

3. **导航入口 `docs/README.md` 不完整**(详见 §四 P1-7):没列 `docs/molang/`、`docs/superpowers/`、`docs/stonecutter-migration-handoff.md`,ADR 索引只到 0014(漏 0015)。

4. **大量"历史计划"留在活跃文档里**(详见 §四 P2-9):
   - `MODULES.md` 充满 `Phase 13`、`Phase 14`、`FM-005`、`Phase 17-20`、`(removed)` 字面占位符
   - `docs/superpowers/plans/2026-06-16-flat-merge.md`(1109 行)是已完成的 flat-merge 计划,所有 task 仍是 `- [ ]` 未勾选
   - `ADR-0006` 用 `~~Superseded~~` 删除线保留过期行

5. **AI 接手时的"阅读路径"不明确**:`AGENTS.md` 说"Reading Order: AGENTS → docs/README → MODULES → package-info → code",但实际 `docs/README.md` 把操作流程指向 `.hermes/`(不存在),`MODULES.md` 自身有过期内容,`concepts/architecture.md` 是 ADR-0014 之前的世界。

#### 一个 AI 实际能"看到"多少?

按 `AGENTS.md` 推荐的阅读路径,一个新 AI 第一次进入项目,会读:
- `AGENTS.md`(235 行)
- `docs/README.md`(66 行)
- `MODULES.md`(165 行)
- `docs/concepts/architecture.md`(115 行)

总计约 **581 行 = ~6K tokens**,已经能塞满短上下文窗口的关键部分。但**这 581 行里有过半内容是过期的或自相矛盾的**——AI 拿到的不是"项目当前真相",而是"项目某个时间点的快照 + 一些互相打脸的描述"。这才是 AI 接手后频繁偏离实际工作的根因。

### Q2. 需要整理项目内的 AGENTS.md 让约束更精简并且涵盖文档维护规则吗?

**需要,而且这是最高杠杆的修复点。**

#### 当前 AGENTS.md 的问题

按 [AGENTS.md 开放标准](https://agents.md/)(2025 年 12 月已由 OpenAI / Anthropic 捐给 Linux Foundation 旗下的 AAIF,被 60k+ 项目采用):

> "Think of AGENTS.md as a **README for agents**: a dedicated, predictable place to provide the context and instructions to help AI coding agents work on your project."
>
> "We intentionally kept it separate to: Give agents a clear, predictable place for instructions. Keep READMEs concise..."
>
> Popular choices: Project overview / Build and test commands / Code style guidelines / Testing instructions / Security considerations.

当前 eyelib `AGENTS.md` 是 235+ 行的混合体,违反了 AGENTS.md 标准的"单一职责 / 可预测"精神:

| 段落 | 应在 AGENTS.md? | 当前状态 |
|---|---|---|
| Start Here / Reading Order | ✅ 应在 | OK |
| Repository Shape | ✅ 应在(简版) | OK,但有旧包名 `eyelibmolang/generated/` |
| Editing Rules | ✅ 应在 | OK |
| Comment Rules(66 行) | ⚠️ 偏长,但全是规则 | 可保留 |
| Documentation Rules | ✅ 应在 | **缺失文档维护机制**(见下) |
| Generated Code (Historical) | ❌ 这是历史/架构知识 | 违反 ADR-0011 自家规则"AGENTS.md 只放规则" |
| Tooling Restrictions | ✅ 应在 | OK,但未提 Stonecutter |
| Build & Test Verification | ✅ 应在 | OK |
| Skill Usage | ✅ 应在 | OK |
| Molang Roadmap | ❌ 这是子系统知识 | 违反 ADR-0011——应移到 `docs/molang/` |
| Pitfall Records | ✅ 应在(指针) | OK,但缺乏"何时该更新哪个 skill"的规则 |
| Reading Order | ✅ 应在 | OK |

#### AGENTS.md 缺失的关键规则:文档维护机制

当前 AGENTS.md 的 "Documentation Rules" 段只有 5 条通用原则(Paths must resolve / Don't keep history / ADR in `decisions/` / Code is authoritative / Docs-only changes verify paths),**没有任何机制约束"代码改动后必须同步哪些文档"**。

这是文档与代码持续脱节的根本原因。需要补的规则:

```
## Documentation Sync Rules(建议新增)

代码改动类型 → 必须同步的文档:

| 代码改动 | 必须同步检查 |
|---|---|
| 新增/删除/重命名包 | `MODULES.md`、`docs/decisions/0002-module-boundaries.md`、对应 `package-info.java` |
| 新增/删除/重命名顶层模块 | `MODULES.md`、`docs/README.md`、`AGENTS.md` Repository Shape |
| 新增 ADR | `docs/README.md` ADR 索引、相关前置 ADR 的 amended/superseded 标注 |
| 改 build.gradle 依赖图 / settings.gradle / Stonecutter node | `AGENTS.md` Tooling Restrictions、`docs/README.md`(若结构变) |
| Molang 阶段状态/里程碑/闸门变化 | `docs/molang/ROADMAP.md`(已有规则) |
| 删除/重命名文件被 docs 引用 | grep 全 `docs/` + `AGENTS.md` + `MODULES.md` + 所有 SKILL.md |
| 新增/删除 Skill | `docs/README.md` Skill 索引、`AGENTS.md` |

提交前自检:
1. 改动是否触发了上表任何一行? → 是 → 必须同步
2. 同步后,grep 全仓库验证没有旧路径残留
3. 文档-only PR:必须 grep 验证每个引用路径存在
```

#### AGENTS.md 应该瘦身的段落

- **Generated Code (Historical — ANTLR Removed)**:整段移到 `docs/decisions/0004-generated-code-policy.md`(已经在那了),AGENTS.md 只留一句"见 ADR-0004"。同时此段当前引用 `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/`,这条路径用的是 ADR-0014 之前的旧包名,**AGENTS.md 自身的规则过期了**。

- **Molang Roadmap**:整段移到 `docs/molang/ROADMAP.md` 的"何时必须更新本文件"段(已存在)。AGENTS.md 只留指针。

- **Comment Rules**:66 行的注释规则可以拆成"基本规则(15 行) + 详细示例(移到 `docs/conventions/comment-rules.md`)。AGENTS.md 是 AI 的快速参考,不是教学材料。

### Q3. 文档结构是否需要优化?

**需要,但不是大重构,而是修漏 + 收口。**

#### 当前结构问题

`docs/README.md` 声称"按 Diátaxis 组织",但 [Diátaxis 原始定义](https://diataxis.fr/)的四个象限是 **Tutorials / How-to guides / Reference / Explanation**(学习 / 操作 / 信息 / 理解)。当前 eyelib 实际目录是:

```
docs/
├── README.md
├── stonecutter-migration-handoff.md     ← 没被任何索引引用
├── concepts/                            ← Diátaxis "Explanation"
├── decisions/                           ← ADR,跟 Diátaxis 无关
├── architecture/                        ← 操作手册,Diátaxis "How-to"
├── specs/                               ← 自创类目
├── gap-analysis/                        ← 自创类目
├── tests/                               ← 自创类目
├── molang/                              ← 子系统专区(24 个文件)
└── superpowers/                         ← 自创类目(plan/spec 形式的实施文档)
```

ADR-0011 自己列的"Diátaxis 四象限"是"概念/指南/参考/决策",跟原始 Diátaxis 不一致;`docs/README.md` 又改成"概念/决策/规格/差距分析"——**两个文档对"四象限"的定义不一样,且都不符合 Diátaxis**。

这是典型的"AI 套用流行词但不查原始定义"——把 Diátaxis 当装饰用,实际目录结构按需要随手加。

#### 推荐结构(改动最小化)

不必推倒重来。当前结构在功能上其实够用,问题在于:

1. **`docs/README.md` 的导航表不完整**(漏掉 `molang/`、`superpowers/`、`stonecutter-migration-handoff.md`、ADR-0015)
2. **"Diátaxis" 标签名不副实**——要么真的按 Diátaxis 重排(成本高),要么放弃 Diátaxis 标签,直接说"docs 按类型分目录"(成本低,推荐)
3. **缺一个 audit 目录**(本报告所在)

推荐的新结构(改动最小化):

```
docs/
├── README.md                  ← 导航入口(必须列全所有子目录和 ADR)
├── concepts/                  ← 解释/概念(architecture、domain-map)
├── decisions/                 ← ADR(0001-0015+)
├── operations/                ← 改名自 architecture/,操作手册
├── specs/                     ← 行为规格
├── gap-analysis/              ← Bedrock 差距分析
├── tests/                     ← 测试计划
├── audits/                    ← 审计报告(新)
├── molang/                    ← Molang 子系统专区
├── superpowers/               ← 实施计划(spec/plan 形式)
└── handoffs/                  ← 交接报告(改名自散落的 *-handoff.md)
```

主要变化:
- 新建 `audits/`(本报告)
- 新建 `handoffs/`,把 `docs/stonecutter-migration-handoff.md` 移进去
- `docs/README.md` 改写导航表,**列出所有子目录**,**删除 Diátaxis 标签**(或真正按 Diátaxis 重排,二选一)

---

## 三、术语校正:这不是 DDD

**用户描述说"项目采用 DDD 作为准则",但代码里没有任何 DDD 战术模式,实际是另一组架构风格的混合。**

### 3.1 DDD 战术模式 vs 代码现实

DDD(Domain-Driven Design,Eric Evans 2003)的核心战术构建块是:

| DDD 战术模式 | 在 eyelib 代码里? | 实际是什么 |
|---|---|---|
| Aggregate(聚合根) | ❌ 无 | 模块按"功能"划分(particle/material/animation 等) |
| Entity(领域实体) | ❌ 无 `*Entity` 领域类(只有 MC `Entity`) | record + Component 模式 |
| Value Object(值对象) | ❌ 无明确 VO 概念 | record 被用作 DTO / schema |
| Repository(仓储) | ❌ 无 `*Repository` | `*Manager` / `*Registry` 模式 |
| Domain Event(领域事件) | ❌ 无 `*Event` 领域事件 | `ManagerEventPublisher` 是观察者模式,不是 DDD event |
| Application Service | ❌ 无 | `EntityRenderSystem` 是 system,不是 service |
| Bounded Context(限界上下文) | ⚠️ 部分对应 | 模块边界 ≈ BC,但没有 Context Mapping |
| Ubiquitous Language(通用语言) | ⚠️ 部分 | Bedrock 术语被保留 |

### 3.2 实际架构风格

代码里**真正**使用的架构风格:

1. **六边形架构(Ports & Adapters)** — Alistair Cockburn 2005
   - `bridge/molang/PortEntity.java`、`bridge/material/PortRenderPass.java` 等 Port 接口
   - `bridge/` 包是 adapter,各 domain 包(部分)是 core
   - 但目前只覆盖 molang / material / animation 三个 domain 的部分接触点

2. **Modular Monolith by Package Convention** — Kamil Grzybek 等的系列
   - 单 Gradle project,按包名划分模块(ADR-0014)
   - 模块边界靠 review + 包名约定,不靠物理隔离

3. **ECS(Entity-Component-System)** — Bedrock 原生架构
   - eyelib 复刻:`ComponentStore`、`ModelComponent`、`AnimationComponent` 等

4. **Stonecutter 多版本构建** — 工程实践,不是架构风格
   - 单 src + `//?` 注释 + per-version bridge 实现 Ports

### 3.3 影响

把这套混合体笼统称为"DDD"会导致:
- **新 AI 按 DDD 思维找代码**(找 `*Repository`、`*Aggregate`)→ 找不到 → 困惑
- **架构讨论时空对空**(讨论"bounded context" 时,实际想说的可能是"模块边界")
- **ADR-0009 自称"Phase 3 DDD 重构目标"**,但实际描述的是**纯模块化解耦**(把直接 import 改成接口边界)——这是接口隔离原则(ISP),跟 DDD 无关

### 3.4 推荐

对外统一改称:**"六边形架构(Ports & Adapters)+ 包边界模块化 + ECS + Stonecutter 多版本"**。

如果一定要用 DDD 词汇,只能用**战略 DDD**(Bounded Context ≈ 模块),不能暗示用了战术 DDD。

---

## 四、详细发现(按严重程度)

### P0 — 概念性错位(影响每个新 AI 的认知)

#### P0-1. `concepts/architecture.md` 完全是 ADR-0014 之前的世界

文件:`docs/concepts/architecture.md`

| 行 | 内容 | 实际 |
|---|---|---|
| 全文 C4 容器图 | 画 `eyelib-bridge`、`eyelib-network`、`eyelib-track` 等独立容器 | ADR-0014 已合并为单 project,所有"eyelib-xxx"容器不存在 |
| Line 81 | "Domain 模块不 import `net.minecraft.*` — **由 ArchUnit 规则强制**" | ArchUnit 已被 ADR-0014 删除,又被 ADR-0015 恢复为 freeze 模式 |
| Line 86-95 "提取进度" 表 | 全 ✅,无"剩余工作" | ADR-0010 line 114-115 明确写 `R2 未完成`、`R4 Entity→Port 待完成`;ADR-0015 §4 列出 6+ 处 MC import 渗透 |
| Line 99-115 ADR 索引 | 只列 0001-0011 | 实际已到 0015 |

**这是项目的"门面文档"——按 AGENTS.md 的 Reading Order 是新 AI 第 3 站。当前它给 AI 一套完全过期的脑图。**

#### P0-2. "DDD" 标签错误

见 §三。ADR-0009 line 5 自称"Phase 3 DDD 重构目标",但描述的是模块解耦(ISP),不是 DDD。

### P1 — 文档与代码大量断裂

#### P1-3. `.hermes` 路径不存在,Skill 索引完全错位

| 文件 | 行 | 引用 | 实际 |
|---|---|---|---|
| `docs/README.md` | 11 | `../../.hermes/profiles/qyleyelib/skills/eyelib/` | `.hermes` 目录不存在 |
| `.opencode/skills/eyelib-build/SKILL.md` | 20, 65 | `~/.hermes/profiles/qyleyelib/scripts/env.sh` | 用户家目录的 `.hermes` 也不存在 |
| `.opencode/skills/eyelib/references/renderdoc-mcp.md` | 14 | `~/.hermes/profiles/qyleyelib/config.yaml` | 同上 |

实际 Skill 在 `.opencode/skills/`(13 个,前缀 `eyelib-*` 和工具类)。这是一套"两套 skill 系统的幽灵"(Hermes vs opencode),AI 接手会完全迷失。

#### P1-4. Stonecutter 落地后文档零同步

| 配置文件实际状态 | 文档状态 |
|---|---|
| `settings.gradle` 已是 Stonecutter 0.7.11 + versions `"1.20.1"`、`"1.21.1"` | `docs/README.md`、`concepts/architecture.md`、`MODULES.md`、`AGENTS.md` **完全没提 Stonecutter** |
| `build.gradle:12-441` 全文 Stonecutter-aware(`stonecutter.current.version`、`isLegacyForge`、freeze store) | `AGENTS.md "Tooling Restrictions"` 没提 `//?` 注释、active version、node 切换这些新约束 |
| `docs/stonecutter-migration-handoff.md` 显示 Phase 1+2 已完成 | 该 handoff 文件**未被 docs/README.md 索引** |
| ADR-0015 Status: Accepted | `docs/README.md` ADR 索引**只到 0014,漏 0015** |

#### P1-5. ADR-0014 之后旧包名到处残留

grep `eyelibutil|eyelibnetwork|eyelibmolang|...` 在 `docs/` 下 **69 处匹配**,主要分布:

| 文件 | 匹配数 | 严重程度 |
|---|---|---|
| `docs/molang/ROADMAP.md` | 13+ | 高(210 行的"现行路线图"通篇用旧包名) |
| `docs/molang/refactor-plan/*.md`(6 个文件) | 20+ | 高(执行计划引用旧路径) |
| `docs/molang/design/*.md` | 10+ | 中 |
| `docs/decisions/0004-generated-code-policy.md` | 3 | 中(引用已删除的 `generated/`) |
| `docs/decisions/0009-domain-events-particle-interaction.adr.md` | 多 | 中 |
| `docs/superpowers/plans/2026-06-16-flat-merge.md` | 多 | 低(计划文档本身就是改包名的脚本) |
| **`AGENTS.md "Generated Code"` 段** | 1 | **高**(AGENTS.md 自身的规则过期了) |

ADR-0014 Verification 第 4 条要求"在 `.java`、`mods.toml`、`mixin json`、反射字符串中应清零"——但**仅限于代码**,docs 没 hold 自己到同样标准。

#### P1-6. ADR-0011(文档基线)自我违反

ADR-0011 是 2026-06-09 立的"文档设计基线",其中规定:

| ADR-0011 规定 | 实际状态 |
|---|---|
| 建 `docs/concepts/module-map.md` | 实际叫 `docs/architecture/domain-module-map.md` |
| 建 `docs/reference/` | 不存在 |
| 建 `docs/pitfalls/` | 不存在 |
| 建 `docs/guides/` | 不存在 |
| "ADR-0010 在 docs/architecture/ 而非 docs/decisions/" | 直到现在 `docs/architecture/0010-hexagonal-architecture.md` stub redirect 还在 |
| "AGENTS.md 只放规则,不含架构知识" | 当前 AGENTS.md 仍含 `Repository Shape`、`Generated Code (Historical)`、`Molang Roadmap` 段 |

ADR-0011 自己 Context 段列出要解决的问题,3 个月后多数仍在。

#### P1-7. `docs/decisions/0001` 与现状直接矛盾且未标注

`ADR-0001` 仍写:
- Line 4: "bounded **multi-project** Forge project with one runtime root module plus focused functional subprojects"
- Line 13: "Keep Eyelib as a bounded **multi-project** Forge project"
- Line 18: "No further Gradle module split beyond current functional needs"

ADR-0014 已经把所有子项目合并了。ADR-0001 **没有 `amended by 0014` 标注**。新 AI 读到 0001 会以为还在多 project 时代。

`ADR-0003` Line 15、35-36 同样的问题——仍引用 `eyelib-network/` 独立子项目和 "Subproject `build.gradle` `project(:)` edges define the real architecture"。

#### P1-8. `docs/specs/behavior-component-spec.md` 完全过期

| 行 | 内容 | 实际 |
|---|---|---|
| 21, 32, 46, 100 | `io.github.tt432.eyelibbehavior.component.Component` | ADR-0014 之后应是 `io.github.tt432.eyelib.behavior.component.Component` |
| 114-119 | `cmd.exe /c "cd /d E:\... && gradlew.bat :eyelib-behavior:compileJava"` | 直接违反 AGENTS.md "禁止在 shell 跑 gradlew",且 `:eyelib-behavior:` 子项目已不存在 |

#### P1-9. `.opencode/skills/eyelib/SKILL.md` WSL 路径

文件:`.opencode/skills/eyelib/SKILL.md`

| 行 | 内容 | 问题 |
|---|---|---|
| 15 | "项目路径 `/mnt/e/_ideaProjects/qylEyelib`,MC 1.20.1 / Forge 47.1.3 / Java 17" | WSL 路径风格,当前是 Windows PowerShell 项目;只标 1.20.1 漏了 Stonecutter 多版本 |
| 32-34 | `/mnt/e/_____基岩版文档/minecraft-creator/...` | WSL 路径,实际访问应通过 Windows |
| 56 | `/mnt/e/_ideaProjects/qylEyelib/eyelib-xxx` | ADR-0014 之后 `eyelib-xxx` 子目录不存在 |

#### P1-10. `.opencode/skills/eyelib-build/SKILL.md` 完全是 WSL 工作流

文件:`.opencode/skills/eyelib-build/SKILL.md`

| 行 | 内容 | 问题 |
|---|---|---|
| 15 | "MC 1.20.1 / Forge 47.1.3 / Java 17" | Stonecutter 多版本未提 |
| 20 | `source ~/.hermes/profiles/qyleyelib/scripts/env.sh` | `.hermes` 不存在 |
| 32-43 | `cmd.exe /c "cd /d E:\... && gradlew.bat :compileJava"` | 直接违反 AGENTS.md "禁止在 shell 跑 gradlew",应该是 `jetbrain_run_gradle_tasks` |
| 76-84 | "Common Pitfalls: WSL Gradle 必须通过 Windows cmd.exe" | 与 AGENTS.md "Tooling Restrictions" 互相打脸 |

这个 skill 整体假设 WSL + cmd.exe 交叉编译工作流,但项目已经迁移到 Windows PowerShell + JetBrains MCP。

#### P1-11. MolangQuery 实际位置与所有文档描述不符

| 文档 | 描述 | 实际 |
|---|---|---|
| `AGENTS.md "Generated Code"` 段 | `src/main/java/io/github/tt432/eyelib/molang/mapping/MolangQuery.java` 持有 root-coupled 查询函数 | 实际 `MolangQuery.java` 在 `src/main/java/io/github/tt432/eyelib/bridge/molang/MolangQuery.java`(已在 bridge 模块) |
| `MODULES.md:30` | "root keeps only root-coupled `MolangQuery` in `molang/mapping/`" | 同上,已搬到 bridge |
| `docs/molang/ROADMAP.md:37` | "Root `src/main/java/io/github/tt432/eyelib/molang/mapping/MolangQuery.java` is the only file remaining in the root `molang/` path" | 同上 |

代码已经按六边形架构搬到了 bridge,但**所有文档还描述它在 root `molang/`**。

### P2 — ADR 编号系统损坏

#### P2-12. 两个 ADR-0013

磁盘上有:
- `0013-bedrock-animation-controller-and-calculation.md`(531 行,主文档)
- `0013-bedrock-animation-query-functions.md`(431 行,标"附录 A")

后者是前者的附录,本该用同一编号 + 子编号(`0013a-...` 或 `0013.1-...`),或者直接合并。

#### P2-13. ADR-0015 未被 docs/README.md 索引

`docs/README.md` ADR 表只到 0014。

#### P2-14. 0009 文件名后缀异常

`0009-domain-events-particle-interaction.adr.md`——独此一份用 `.adr.md` 后缀,其他都是 `.md`。

#### P2-15. 0010 在两个目录都有副本

- `docs/decisions/0010-hexagonal-architecture.md`(正文,115 行)
- `docs/architecture/0010-hexagonal-architecture.md`(stub redirect,9 行)

ADR-0011 自己提到"ADR-0010 在 docs/architecture/ 而非 docs/decisions/"是要解决的问题,但 stub redirect 至今还在。

#### P2-16. `concepts/architecture.md` 的 ADR 索引过期

只列到 0011(实际已到 0015),且 0010 状态未更新为 Implemented。

### P2 — `MODULES.md` 自身污染

#### P2-17. MODULES.md 字段问题

| 行 | 问题 |
|---|---|
| 41 | `<module>` 字面占位符未填值:`"<module>` is a Forge canonical model data leaf module" |
| 47 | `ItemStack tracking module` 后四列**全空**(被截断) |
| 40, 全文 | `` :util`` ` 冒号写法到处用,但实际已没有 `:util` Gradle project |
| 全文(多处) | `(removed)` 字面占位符当路径(line 49、112、116、117、124、142 等) |
| 全文 | 充满 `Phase 13 rewires`、`Phase 14 verification`、`FM-005`、`Phase 17-20` 历史阶段标识 |
| Bridge 段 | `:`bridge`` is the hexagonal architecture adapter layer... All MC import surface for domain modules concentrates here | 实际 bridge 只有 13 个类(3 个 domain 子包),`util/` 还有 49 个 java 文件其中很多 import 了 MC 类型 |

MODULES.md 违反 AGENTS.md 三条规则:
- "Code is the authoritative reference"(`(removed)` 不是代码里存在的路径)
- "Don't keep history in active docs"(Phase 13/14/15/17-20 是历史)
- "Paths must resolve"(`(removed)` 不解析)

#### P2-18. `ADR-0006` 用删除线保留过期行

`docs/decisions/0006-key-architecture-decisions.md:18, 26`:

```
| Independent Gradle subproject for each seam | Build isolation | v1.0 | ~~Superseded by [ADR-0014](0014-flat-merge.md)~~ |
| `io.github.tt432.eyelibutil` namespace | ... | v1.3 | ~~Superseded by [ADR-0014](0014-flat-merge.md)~~ |
```

AGENTS.md 规则是"已完成 / 解决的问题属于 git history"——这两行**应该直接删行**,不是保留删除线。

#### P2-19. 实施计划文档未勾选已完成 task

- `docs/superpowers/plans/2026-06-16-flat-merge.md`(1109 行,13 个 Task):所有 task 仍是 `- [ ]`,但 ADR-0014 已 Status: Accepted,代码已合并完成
- `docs/superpowers/specs/2026-06-17-stonecutter-migration-design.md`(412 行,Phase 0-6 任务):全部 `- [ ]`,但 `docs/stonecutter-migration-handoff.md` 显示 Phase 1+2 已完成
- 后者还引用 Stonecutter `0.5.x`,实际 settings.gradle 是 `0.7.11`(line 70, 80)

这些 plan 文档是"工作进行中"的产物,完成后没归档也没删除。

### P3 — 同一事实的多源不一致

#### P3-20. "Domain 提取进度"四处四种说法

| 文档 | 状态描述 | 是否提"剩余工作"? |
|---|---|---|
| `ADR-0010:66-75` 表格 | ✅ 全 ArchUnit 通过 | 是(`R2/R4 未完成`) |
| `domain-module-map.md:30-37` 表格 | ✅ ArchUnit + Spec 测试数 | 是("剩余工作"列) |
| `concepts/architecture.md:88-95` 表格 | ✅ 全就绪 | **否**(完全没提剩余工作) |
| `ADR-0015:20, 71-76` | MC import 渗透(`material/material/BrMaterial` 用 `RenderStateShard`、`molang/mapping/MolangQuery` 直接 import `net.minecraft.world.entity.*`)+ ArchUnit 已恢复 freeze 模式 | 是 |

四处中 `concepts/architecture.md` 最误导(完全不提问题),`ADR-0015` 最准。

#### P3-21. "Molang 当前状态"三处三种描述

| 文档 | 描述方式 |
|---|---|
| `docs/molang/ROADMAP.md` | Phase 0-7 表 + Active Milestones M1-M5 + OKR KR 表(Phase 5/6 已 Superseded 但仍保留) |
| `docs/molang/refactor-plan/README.md` | P1-P12 问题清单(另一套编号) |
| `docs/molang/design/README.md` | 21 个设计草稿索引 + "engine code lives in `eyelib-molang/...`"(过期) |

ROADMAP.md 自身 line 196 写"Docs-only roadmap changes: verify every referenced path still exists",但它通篇引用 `io.github.tt432.eyelibmolang.*` 旧包名,**自己违反自己的规则**。

#### P3-22. 分层术语三处三种层级划分

见 Q1 §2。

#### P3-23. "bridge 模块"实际 vs 文档

| 来源 | bridge 描述 |
|---|---|
| `MODULES.md` "Bridge Subproject" 段 | "All MC import surface for domain modules concentrates here" |
| `concepts/architecture.md:36-40` | "所有 MC import 集中于此" |
| `ADR-0010:34-32` | "Bridge 模块是所有 Stonecutter `//? if` 的唯一栖息地" |
| `ADR-0015:62-65` | "现有 bridge(仅 molang/material)需扩展:新增 `render/`、`itemstack/`、`controller/`" |
| **实际代码** | bridge 只有 `animation/`(1 类) + `material/`(5 类) + `molang/`(7 类),共 13 类 + 3 个 package-info;`util/` 仍有 49 个 java 文件含 MC import 渗透 |

文档集体夸大 bridge 完备度。最准的是 ADR-0015,但 ADR-0015 也未被 `docs/README.md` 索引。

---

## 五、文档规模分析

### 5.1 按"AI 上下文成本"排序的 TOP 10 大文件

| 文件 | 行数 | 估算 tokens | 状态 |
|---|---|---|---|
| `docs/superpowers/plans/2026-06-16-flat-merge.md` | 1109 | ~11K | 已完成未归档 |
| `AGENTS.md` | ~235 | ~2.5K | 部分过期 |
| `docs/molang/ROADMAP.md` | 210 | ~3K | 通篇旧包名 |
| `MODULES.md` | 165 | ~3K | 充满历史 |
| `docs/superpowers/specs/2026-06-17-stonecutter-migration-design.md` | 412 | ~5K | 部分完成 |
| `docs/decisions/0013-bedrock-animation-controller-and-calculation.md` | 531 | ~7K | 参考类,无问题 |
| `docs/decisions/0013-bedrock-animation-query-functions.md` | 431 | ~5K | 附录,无问题 |
| `docs/decisions/0012-system-testing-strategy.md` | 260 | ~3K | 引用 `:eyelib-material:test` 等过期 Gradle project |
| `docs/decisions/0008-item-track-design.md` | 375 | ~4K | 引用 `eyelib-track` 子项目 |
| `docs/decisions/0009-domain-events-particle-interaction.adr.md` | 196 | ~2K | 通篇 `eyelib-animation/`、`eyelib-particle/` 旧路径 |

**问题特征**:大量"实施计划"和"架构 ADR"留在了 ADR-0014 之前的世界,从未同步。

### 5.2 文档"信息密度"问题

低密度文档(应缩短或删除):
- `MODULES.md`:165 行里大量 `(removed)` / `<module>` / `Phase 13/14/15/17-20` 噪音。真正的"当前模块清单"信息只占 ~40%。
- `concepts/architecture.md`:115 行,大部分是过期 C4 图和被 ADR-0015 否定的"ArchUnit 强制"声称。真正的"当前架构"信息只占 ~20%。

高密度文档(应作为 source of truth):
- `ADR-0014`(147 行):决策清晰、consequences 完整、verification 可执行
- `ADR-0015`(147 行):同上
- `docs/stonecutter-migration-handoff.md`(120 行):简洁的"已完成什么、未完成什么、下次接手优先级"

**模式**:越新的文档质量越高,越旧的越腐化。说明问题不是"AI 写文档能力差",而是"AI 不回头更新旧文档"。

---

## 六、推荐方案

### 6.1 三阶段修复路线

#### 阶段 A:止血(2-3 次会话,~3-4 小时)

目标:**让 AGENTS.md / docs/README.md / MODULES.md / concepts/architecture.md 这四个"必读入口"完全准确反映当前代码状态。**

1. **AGENTS.md**:
   - 删除 `Generated Code (Historical)` 段(移到 ADR-0004,它已经在那了)
   - 删除 `Molang Roadmap` 段(移到 ROADMAP.md 自己的"update rule"段)
   - 新增 `Documentation Sync Rules` 段(见 Q2 推荐)
   - 新增 `Tooling Restrictions` 补充 Stonecutter `//?` 注释、active version、node 切换约束
   - Repository Shape 段更新为单 project + Stonecutter 多版本描述

2. **docs/README.md**:
   - ADR 索引补到 0015
   - Skill 索引路径全部从 `.hermes/...` 改为 `.opencode/skills/`
   - 快速导航表补 `docs/molang/`、`docs/superpowers/`、`docs/audits/`、`docs/handoffs/`(新建)
   - 删除"Diátaxis"标签或真正按 Diátaxis 重排(推荐:删除标签,直接说"按类型分目录")

3. **concepts/architecture.md**:**整篇重写**
   - C4 容器图改为单 project + Stonecutter node 视图
   - 删除"ArchUnit 强制"声称,改为"ArchUnit freeze 模式 + ADR-0015 还债路线"
   - ADR 索引列全 0001-0015
   - 提取进度表合并到 `domain-module-map.md`,此处只放指针

4. **MODULES.md**:
   - 删除所有 `(removed)` / `<module>` 占位符行
   - 删除所有 `Phase 13/14/15/17-20` / `FM-005` 历史标识
   - 第 41 行 `<module>` 填值或删行
   - 第 47 行 `ItemStack tracking module` 完整化或删行
   - `` :util`` ` 改为 `util`
   - 添加 `smoke`、`debug`、`event` 等遗漏的顶层包

#### 阶段 B:清债(3-5 次会话,~5-8 小时)

目标:**清理过期引用,让全仓库 grep 旧包名清零。**

1. **`docs/molang/ROADMAP.md`** 全文包名替换 + 删除 Phase 5/6 Superseded 行
2. **`docs/molang/refactor-plan/*.md`**(6 个文件)包名替换
3. **`docs/molang/design/*.md`** 包名替换 + 删除 `generated/` 引用
4. **`.opencode/skills/eyelib/SKILL.md`** WSL 路径改 Windows + Stonecutter 多版本
5. **`.opencode/skills/eyelib-build/SKILL.md`** 整篇重写为 Windows PowerShell + JetBrains MCP 工作流
6. **`docs/specs/behavior-component-spec.md`** 包名替换 + Gradle 命令改 MCP
7. **`docs/decisions/0001, 0003, 0004, 0006, 0008, 0009`** 各加 `amended/superseded by 0014/0015` 标注
8. **`docs/decisions/0006`** 删除 `~~Superseded~~` 两行,直接删行
9. **`docs/decisions/0009`** 文件名去掉 `.adr`(改为 `0009-domain-events-particle-interaction.md`)
10. **`docs/architecture/0010-hexagonal-architecture.md`** stub 删除
11. **合并 / 重命名两个 0013**

#### 阶段 C:建规则(1-2 次会话,~2 小时)

目标:**防止再次腐化。**

1. 在 AGENTS.md 加 `Documentation Sync Rules` 段(Q2 推荐的表)
2. 在 AGENTS.md 加 `Adversarial Doc Check` 段:每次代码 PR 必跑的 doc-sync 自检脚本
3. 给 `MODULES.md` 加"自动生成"标注:理想状态是 MODULES.md 由 `scripts/generate-modules-md.ps1` 从实际包结构生成,不是手写

### 6.2 长期治理

1. **每个 ADR 必须有 `amended/superseded by` 互链**:新 ADR 修订旧 ADR 时,旧 ADR 头部必须加标注(目前 ADR-0002、0010 有,0001、0003、0006 部分有,0004、0008、0009 没有)。

2. **`docs/audits/` 作为常规审查产物目录**:本报告是第一份。建议每季度跑一次 doc-sync 审查。

3. **`concepts/architecture.md` 锁定为"反映当前"**:任何架构变更 PR 必须同时更新此文件。这是新人/AI 第一站,不允许它过期。

4. **plan 文档完成后归档**:`docs/superpowers/plans/` 完成的 plan 应移到 `docs/superpowers/plans/archived/` 或直接删除(git history 保留)。

---

## 七、行动清单(按优先级)

### P0(本周内)

- [ ] 重写 `docs/concepts/architecture.md`(整篇)
- [ ] 修 `docs/README.md`:补 ADR-0015、Skill 路径全改 `.opencode/`、导航表补全
- [ ] 修 `AGENTS.md`:删 `Generated Code (Historical)`、删 `Molang Roadmap`、新增 `Documentation Sync Rules`、新增 Stonecutter 约束
- [ ] 在 `AGENTS.md` 顶部"项目实际架构"段加正确的术语(六边形 + 包边界模块化 + Stonecutter,**不是 DDD**)

### P1(本月内)

- [ ] 清理 `MODULES.md`(删占位符 / 历史标识 / 填空)
- [ ] 全仓库 grep `eyelibmolang|eyelibutil|...` 清零(`docs/` 和 `.opencode/skills/`)
- [ ] 修 `.opencode/skills/eyelib-build/SKILL.md`(整篇重写)
- [ ] 修 `.opencode/skills/eyelib/SKILL.md`(WSL 路径改 Windows)
- [ ] 修 `docs/specs/behavior-component-spec.md`
- [ ] `docs/decisions/0009` 改名去 `.adr`
- [ ] 删 `docs/architecture/0010-hexagonal-architecture.md` stub
- [ ] 合并 / 重命名两个 0013

### P2(下季度)

- [ ] 给所有过期 ADR(0001、0003、0004、0006、0008)加 `amended/superseded by` 标注
- [ ] `docs/molang/ROADMAP.md` 整理:删除 Superseded Phase 5/6、合并三处"状态"描述
- [ ] `docs/superpowers/plans/2026-06-16-flat-merge.md` 完成归档或删除
- [ ] 建立 doc-sync 自检脚本(可选:`scripts/check-doc-sync.ps1`)

---

## 附录 A:本次审查发现的过期引用清单

### A.1 旧包名(grep `eyelibutil|eyelibnetwork|eyelibtrack|eyelibmodel|eyelibmolang|eyelibmaterial|eyelibattachment|eyelibanimation|eyelibbehavior|eyelibimporter|eyelibparticle|eyelibbridge`)

| 文件 | 出现次数(估) |
|---|---|
| `docs/molang/ROADMAP.md` | 13+ |
| `docs/molang/refactor-plan/01-operator-completeness.md` | 3 |
| `docs/molang/refactor-plan/02-frontend-consolidation.md` | 5 |
| `docs/molang/refactor-plan/03-test-expansion.md` | 2 |
| `docs/molang/refactor-plan/04-documentation-verification.md` | 10+ |
| `docs/molang/refactor-plan/05-deferred-semantics.md` | 2 |
| `docs/molang/refactor-plan/06-host-context-alignment.md` | 5 |
| `docs/molang/refactor-plan/README.md` | 1 |
| `docs/molang/design/README.md` | 2 |
| `docs/molang/design/molang-ast-and-semantics-draft.md` | 3 |
| `docs/decisions/0004-generated-code-policy.md` | 3 |
| `docs/decisions/0006-key-architecture-decisions.md` | 2(已标 Superseded) |
| `docs/decisions/0009-domain-events-particle-interaction.adr.md` | 6+ |
| `docs/decisions/0014-flat-merge.md` | 多(自身是改名计划,合理) |
| `docs/superpowers/plans/2026-06-16-flat-merge.md` | 多(自身是改名计划,合理) |
| **`AGENTS.md` "Generated Code" 段** | **1** |
| **`MODULES.md`** | 0(已用新名,但用 `:`module`` 冒号语法过期) |

### A.2 不存在的路径

| 引用 | 出处 | 实际 |
|---|---|---|
| `../../.hermes/profiles/qyleyelib/skills/eyelib/` | `docs/README.md:11` | `.hermes` 目录不存在 |
| `~/.hermes/profiles/qyleyelib/scripts/env.sh` | `.opencode/skills/eyelib-build/SKILL.md:20, 65` | 不存在 |
| `~/.hermes/profiles/qyleyelib/config.yaml` | `.opencode/skills/eyelib/references/renderdoc-mcp.md:14` | 不存在 |
| `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/generated/` | `AGENTS.md "Generated Code" 段`、`docs/molang/design/README.md:48`、`docs/molang/design/molang-ast-and-semantics-draft.md:21` | 已删除 |
| `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang/` | `docs/molang/ROADMAP.md:35, 38`、`docs/molang/design/README.md:46` | ADR-0014 后改为 `src/main/java/io/github/tt432/eyelib/molang/` |
| `docs/architecture/acceptance-gates.md` | `docs/decisions/0010:97`、`docs/decisions/0012-system-testing-strategy.md:259` | **不存在** |
| `docs/architecture/domain-extraction-guide.md` | `docs/architecture/0010-hexagonal-architecture.md:6` | **不存在** |
| `docs/architecture/port-design-template.md` | `docs/architecture/0010-hexagonal-architecture.md:8` | **不存在** |
| `docs/concepts/module-map.md` | `ADR-0011:53` | 实际叫 `docs/architecture/domain-module-map.md` |
| `docs/reference/`、`docs/pitfalls/`、`docs/guides/` | `ADR-0011:38-42` | **都不存在** |
| `src/main/java/io/github/tt432/eyelib/molang/mapping/MolangQuery.java` | `AGENTS.md`、`MODULES.md:30`、`docs/molang/ROADMAP.md:37` | 实际在 `bridge/molang/MolangQuery.java` |
| `/mnt/e/_ideaProjects/qylEyelib/eyelib-xxx` | `.opencode/skills/eyelib/SKILL.md:56` | ADR-0014 后 `eyelib-xxx` 不存在 |

### A.3 不存在的 Gradle project / task

| 引用 | 出处 | 实际 |
|---|---|---|
| `:eyelib-molang:test` | `docs/molang/ROADMAP.md:66, 79, 198` 等 | 已无 `:eyelib-molang` 子 project(ADR-0014 合并),应改为 `:1.20.1:test` 等 Stonecutter node 路径 |
| `:eyelib-material:test`、`:eyelib-importer:test`、`:eyelib-preprocessing:test` | `docs/molang/ROADMAP.md:199-200`、`docs/decisions/0012:110` 等 | 同上 |
| `:eyelib-bridge:test` | `docs/decisions/0012:254` | 同上 |
| `:eyelib-behavior:compileJava`、`:eyelib-behavior:test` | `docs/specs/behavior-component-spec.md:116, 119` | 同上 |

---

## 附录 B:用户三个问题的速查答案

| 问题 | 速查答案 |
|---|---|
| 文档过多导致 AI 跟不上? | 数量是问题,但**碎片化 + 多源真相 + 缺乏入口完整性**是更核心问题。前 4 个必读文件(AGENTS/README/MODULES/architecture)里有 ~581 行 = ~6K tokens,但过半内容过期或自相矛盾。 |
| 需要精简 AGENTS.md + 加文档维护规则? | **需要,且最高杠杆**。当前 AGENTS.md 是 235 行混合体(规则 + 架构知识 + 子系统 roadmap),违反 ADR-0011 自家规则和 AGENTS.md 开放标准的"单一职责"精神。需新增 `Documentation Sync Rules` 段(代码改动 → 必须同步哪些文档)。 |
| 文档结构是否需要优化? | 需要,但**修漏 + 收口**而不是大重构。当前结构功能够用,问题在于:导航不完整、Diátaxis 标签名不副实(原始 Diátaxis 是 Tutorials/How-to/Reference/Explanation)、缺 audits/handoffs 目录。 |

---

## 附录 C:本报告自身遵守的约定

- 放在 `docs/audits/`(新建),不进 `decisions/`(不是 ADR)、不进 `specs/`(不是行为规范)
- 所有路径引用都验证过实际存在或明确标"不存在"
- 不留历史阶段标识(Phase XX、FM-XX 等)
- 中文撰写,符合 AGENTS.md 注释规则
- 不用 HTML 标签、不用段分隔装饰符
- 引用格式 `file:line` 让后续修复者可定位

---

**报告结束。** 建议按 §七行动清单 P0 → P1 → P2 推进。如要立即开始,推荐从 P0-3(重写 `concepts/architecture.md`)切入——这是新 AI 第一站,修好它后续会话质量立刻提升。
