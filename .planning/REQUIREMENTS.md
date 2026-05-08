# Requirements: client-smoke-test

**Defined:** 2026-05-08
**Core Value:** @ClientSmoke 注解驱动的客户端冒烟测试，通过编译时/类加载时的注解扫描分离测试基础设施与模组运行时，杜绝意外的类加载问题。

## v1 Requirements

v1.0 shipped — 23/23 complete. See `.planning/milestones/v1.0-REQUIREMENTS.md`.

## v1.1 Requirements

自动化 Gradle 任务层，实现 `./gradlew runClientSmoke` 一键启动。

### Gradle 构建与任务

- [ ] **GRAD-01**: Root `build.gradle` 中声明 `legacyForge.runs.clientSmoke` run config，MDGL 自动生成 `runClientSmoke` Gradle 任务和 IDE run configuration
- [ ] **GRAD-02**: `clientSmoke` run config 设置独立的 `gameDirectory`（`run/clientsmoke/`），与正常 `runClient` 的 `run/` 目录完全隔离
- [ ] **GRAD-03**: `eyelib-clientsmoke` 从条件 `localRuntime`（`enableSmokeTest==true` 门控）改为始终包含的 `localRuntime`，运行时通过 `ClientSmokeConfig.isEnabled()` 控制
- [ ] **GRAD-04**: `.gitignore` 包含 smoke 产物目录（`run/clientsmoke/`、`clientsmoke-reports/`）

### System Property 配置桥接

- [ ] **OVRD-01**: `ClientSmokeConfig.isEnabled()` 优先检查 `System.getProperty("clientsmoke.enabled")`，未设置时 fallback 到 `ForgeConfigSpec.ENABLED.get()`
- [ ] **OVRD-02**: `ClientSmokeConfig.shouldExitAfterSmoke()` 优先检查 `System.getProperty("clientsmoke.autoExit")`，未设置时 fallback 到 `ForgeConfigSpec.EXIT_AFTER_SMOKE.get()`
- [ ] **OVRD-03**: `clientSmoke` run config 通过 `systemProperty()` 注入 `clientsmoke.enabled=true` 和 `clientsmoke.autoExit=true`
- [ ] **OVRD-04**: 生成 JUnit XML 格式测试报告（`clientsmoke-reports/junit-{timestamp}.xml`），与现有 JSON 报告并存

### 正确性修复与验证

- [ ] **CORR-01**: 修复空测试集 hang：当 `shouldExitAfterSmoke()==true` 且无 `@ClientSmoke` 测试时，状态机从 SCAN 直接进入 REPORT（生成空报告）→ EXIT，而非 IDLE
- [ ] **CORR-02**: 修复 exit code 传播：`handleExit()` 根据 `testResults` 聚合 pass/fail，`Runtime.getRuntime().halt(0)` 通过，`halt(1)` 失败
- [ ] **CORR-03**: 验证 `runClientSmoke` 不破坏正常 `runClient` 行为（smoke mod 默认 idle，不干扰正常开发流程）
- [ ] **CORR-04**: 验证 Windows 环境下 Gradle 能正确捕获 `Runtime.halt()` 的 exit code

## Out of Scope

| Feature | Reason |
|---------|--------|
| 服务端冒烟测试 | 专注于客户端场景 |
| 自动断言/回归比对 | v1 仅截图，人工验证 |
| CI 超时清理/进程管理 | 延后到 v1.2+ |
| 多模组 smoke testing（消费方） | 延后到 v1.2+ |
| `enableSmokeTest` Gradle property 自动设置 | 由 classpath 重构解决，不再需要此 property |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| GRAD-01 | Phase 5 | Pending |
| GRAD-02 | Phase 5 | Pending |
| GRAD-03 | Phase 5 | Pending |
| GRAD-04 | Phase 5 | Pending |
| OVRD-01 | Phase 6 | Pending |
| OVRD-02 | Phase 6 | Pending |
| OVRD-03 | Phase 6 | Pending |
| OVRD-04 | Phase 6 | Pending |
| CORR-01 | Phase 6 | Pending |
| CORR-02 | Phase 6 | Pending |
| CORR-03 | Phase 7 | Pending |
| CORR-04 | Phase 7 | Pending |

**Coverage:**
- v1.1 requirements: 12 total
- Mapped to phases: 12
- Unmapped: 0 ✓

---
*Requirements defined: 2026-05-08*
*Last updated: 2026-05-08 after v1.1 requirements definition*
