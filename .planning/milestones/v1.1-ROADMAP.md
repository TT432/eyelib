# Roadmap: client-smoke-test

## Milestones

- ✅ **v1.0** — Phases 1-4 (shipped 2026-05-07)
- 🚧 **v1.1 ClientSmoke 全自动化** — Phases 5-7 (planning)

## Phases

<details>
<summary>✅ v1.0 — Phases 1-4 (SHIPPED 2026-05-07)</summary>

Full details: `.planning/milestones/v1.0-ROADMAP.md`

- [x] **Phase 1: Module Scaffolding + Config + Annotation Discovery**
- [x] **Phase 2: State Machine + World Lifecycle + Stabilization**
- [x] **Phase 3: Screenshot Capture + Auto-Exit**
- [x] **Phase 4: Test Execution + Report Generation**

</details>

### 🚧 v1.1 ClientSmoke 全自动化 (In Progress)

**Milestone Goal:** `./gradlew runClientSmoke` 一键启动全流程，零手动配置

- [ ] **Phase 5: Gradle Run Configuration & Classpath** — MDGL run config 声明 + 无条件 localRuntime + 产物隔离
- [ ] **Phase 6: Config Override Bridge & State Machine Fixes** — System property 桥接 + JUnit XML + 状态机正确性修复
- [ ] **Phase 7: Verification & Polish** — runClient 无回归验证 + Windows exit code 验证

## Phase Details

### Phase 5: Gradle Run Configuration & Classpath
**Goal**: `./gradlew runClientSmoke` 任务存在且可启动 Minecraft，smoke mod 始终在 classpath 上
**Depends on**: Nothing (first phase of v1.1; v1.0 shipped)
**Requirements**: GRAD-01, GRAD-02, GRAD-03, GRAD-04
**Success Criteria** (what must be TRUE):
  1. Developer can execute `./gradlew runClientSmoke` from root project and Minecraft launches with the smoke mod on classpath
  2. `eyelib-clientsmoke` mod is always on `runClientSmoke` classpath — no Gradle property required
  3. Smoke test `gameDirectory` (`run/clientsmoke/`) is fully isolated from normal `run/` directory
  4. IntelliJ auto-generates a "Run Client Smoke Tests" IDE run configuration usable from the IDE
   5. `run/clientsmoke/` and `clientsmoke-reports/` directories are listed in `.gitignore`
**Plans**: 1 plan

Plans:
- [ ] 05-01-PLAN.md — Gradle run config (clientSmoke) + unconditional localRuntime + .gitignore

### Phase 6: Config Override Bridge & State Machine Fixes
**Goal**: Smoke 测试通过 system property 自动启用并自动退出；状态机正确处理空测试集和 exit code
**Depends on**: Phase 5
**Requirements**: OVRD-01, OVRD-02, OVRD-03, OVRD-04, CORR-01, CORR-02
**Success Criteria** (what must be TRUE):
  1. Running `runClientSmoke` enables smoke testing automatically via system property — no TOML config editing needed
  2. Running `runClientSmoke` causes the client to exit automatically after tests finish — no manual shutdown
  3. Normal `runClient` does NOT enable smoke testing — mod is present but idle by default
  4. When no `@ClientSmoke` tests are annotated, the state machine completes (generates empty report, then exits) instead of hanging
  5. A failing test causes Gradle `BUILD FAILED` (exit code 1); all-pass causes `BUILD SUCCESSFUL` (exit code 0); JUnit XML report is generated alongside JSON
**Plans**: 2 plans

Plans:
- [ ] 06-01-PLAN.md — System property override bridge (isEnabled/shouldExitAfterSmoke) + Gradle systemProperty injection
- [ ] 06-02-PLAN.md — State machine fixes (config bridge wiring, empty test set, JUnit XML, exit code propagation)

### Phase 7: Verification & Polish
**Goal**: 一键启动承诺在真实硬件上验证通过；正常开发流程零回归
**Depends on**: Phase 6
**Requirements**: CORR-03, CORR-04
**Success Criteria** (what must be TRUE):
  1. Running `./gradlew runClient` behaves identically to pre-v1.1 — smoke mod present but idle, zero interference with normal development
  2. On Windows, `echo %ERRORLEVEL%` after `runClientSmoke` returns 0 for pass scenarios and 1 for fail scenarios
  3. All v1.1 success scenarios verified on real hardware (not just unit tests)
**Plans**: 2 plans

Plans:
- [ ] 07-01-PLAN.md — Automated static verification tests (config bridge, build.gradle integrity, state machine idle path)
- [ ] 07-02-PLAN.md — Exit code & JUnit XML verification tests + Windows hardware verification checklist

## Progress

| Phase | Milestone | Plans Complete | Status | Completed |
|-------|-----------|----------------|--------|-----------|
| 1. Module Scaffolding | v1.0 | 5/5 | Complete | 2026-05-07 |
| 2. State Machine + World Lifecycle | v1.0 | 2/2 | Complete | 2026-05-07 |
| 3. Screenshot Capture + Auto-Exit | v1.0 | 2/2 | Complete | 2026-05-07 |
| 4. Test Execution + Report Generation | v1.0 | 1/1 | Complete | 2026-05-07 |
| 5. Gradle Run Configuration & Classpath | v1.1 | 0/- | Not started | - |
| 6. Config Override Bridge & State Machine Fixes | v1.1 | 0/2 | Not started | - |
| 7. Verification & Polish | v1.1 | 0/2 | Not started | - |
