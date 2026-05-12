# Requirements: Eyelib v1.5 深度结构清理

**Defined:** 2026-05-12
**Core Value:** Eyelib 的功能模块必须能被独立理解、构建、验证和消费；共享能力必须形成清晰 Gradle 模块边界，避免 root runtime 成为跨功能代码集散地。

## v1.5 Requirements

Requirements for structure cleanup milestone. Each maps to roadmap phases.

### Documentation (DOCS)

- [ ] **DOCS-01**: 审计全部 48 个 README.md，识别过时引用（残留 `eyelib-processor`）、空目录无效文档、缺失模块说明
- [ ] **DOCS-02**: 修正所有残留的 `eyelib-processor` 引用为 `eyelib-preprocessing`
- [ ] **DOCS-03**: 删除空目录下的无效 README（如 `mixin/README.md` — 目录无 Java 源码，`grammer/` — 遗留标记）
- [ ] **DOCS-04**: 为缺失结构文档的模块补充或更新 README
- [ ] **DOCS-05**: 更新顶层 MODULES.md 和 docs/ 架构文档以反映 v1.5 最终状态

### Animation Cleanup (ANIM)

- [ ] **ANIM-01**: 删除 4 个 Port 接口 — `AnimationIdentityPort`、`AnimationStatePort`、`AnimationExecutionPort`、`AnimationRuntimePortSet`（迁移容器残留，无独立业务价值）
- [ ] **ANIM-02**: 删除 `LegacyAnimationRuntimeAdapter`（56 行纯转发，Port 层删除后无存在意义）
- [ ] **ANIM-03**: 简化 `Animation.java` 默认方法 — 去掉 `ports()` 环形委托链，直接调用自身抽象方法
- [ ] **ANIM-04**: 删除 `AnimationRuntimes` 静态工具类（或被调用方直接替代，不再需要中间层）
- [ ] **ANIM-05**: 更新调用方（`BrAnimator` 等）— `animation.identityPort().name()` → `animation.name()`

### Preprocessing Scan (PREP)

- [ ] **PREP-01**: 扫描 root 全量 Java 源码，识别应移入 `:eyelib-preprocessing` 的类（解析、烘焙、重载规划模式）
- [ ] **PREP-02**: 对发现的候选类出具迁移/保留建议报告，按模块边界规则逐项说明理由

### Duplicate Detection (DUP)

- [ ] **DUP-01**: 交叉对比所有子模块的类签名、字段、方法，识别意外复制
- [ ] **DUP-02**: 审计 capability 注册路径是否有重复职责（root `EyelibAttachableData` 注册 attachment 类型 vs attachment 自主注册）
- [ ] **DUP-03**: 审计 `fromSchema()` 模式一致性 — 是否存在 3 种以上不同实现模式
- [ ] **DUP-04**: 出具完整的重复/模式偏离报告，含推荐措施

### Capability (CAP)

- [ ] **CAP-01**: 审计 root `capability/` 下所有类型，逐类分类为"安全可迁"/"必须保留"/"需推迟"
- [ ] **CAP-02**: 将安全可迁类迁移至 `eyelib-attachment`（命名空间 `io.github.tt432.eyelibattachment.capability`）
- [ ] **CAP-03**: 对高风险类（`EntityBehaviorData` 等）出具推迟迁移分析报告，注明耦合链和阻塞原因

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Capability Deep Extraction

- **CAP-F01**: `EntityBehaviorData` codec 提取 — 当前与 `MolangQuery` 行为语义耦合，需要单独 spike 分析后决定
- **CAP-F02**: 能力运行时 owner 完全分离 — root 不留任何 capability 相关代码，需要更大范围的设计变更

### Extended Cleanup

- **CLEAN-F01**: root `util/` 残留工具代码的最终清理确认
- **CLEAN-F02**: 废弃的 `grammer/` 和 `generated/` 标记包的生命周期决策

## Out of Scope

| Feature | Reason |
|---------|--------|
| 新增 Gradle 子项目 | 当前 7 模块拓扑是 v1.4 目标状态，v1.5 不增加模块 |
| 修改模块间依赖方向 | 所有依赖方向（root→子模块、单向）已正确，不变更 |
| 重写动画系统 | ANIM-01 只删除过度设计的 Port 层，不改变动画运行时行为 |
| EntityBehaviorData 强制迁移 | MolangQuery 耦合风险高，出分析报告后推迟到 v1.5+ |
| `eyelib-material` 手动验证自动化 | 非本次范围，保留手动流程 |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| DOCS-01 | Phase 1 | Pending |
| DOCS-02 | Phase 1 | Pending |
| DOCS-03 | Phase 1 | Pending |
| DOCS-04 | Phase 1 | Pending |
| DOCS-05 | Phase 1 | Pending |
| ANIM-01 | Phase 2 | Pending |
| ANIM-02 | Phase 2 | Pending |
| ANIM-03 | Phase 2 | Pending |
| ANIM-04 | Phase 2 | Pending |
| ANIM-05 | Phase 2 | Pending |
| PREP-01 | Phase 3 | Pending |
| PREP-02 | Phase 3 | Pending |
| DUP-01 | Phase 3 | Pending |
| DUP-02 | Phase 3 | Pending |
| DUP-03 | Phase 3 | Pending |
| DUP-04 | Phase 3 | Pending |
| CAP-01 | Phase 4 | Pending |
| CAP-02 | Phase 4 | Pending |
| CAP-03 | Phase 4 | Pending |

**Coverage:**
- v1.5 requirements: 19 total
- Mapped to phases: 19
- Unmapped: 0 ✓

---
*Requirements defined: 2026-05-12*
*Last updated: 2026-05-12 after v1.5 milestone requirement scoping*
