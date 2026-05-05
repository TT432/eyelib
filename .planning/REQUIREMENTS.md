# Requirements: client-smoke-test

**Defined:** 2026-05-06
**Core Value:** @ClientSmoke 注解驱动的客户端冒烟测试，通过编译时/类加载时的注解扫描分离测试基础设施与模组运行时，杜绝意外的类加载问题。

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Module Structure

- [ ] **MOD-01**: 创建 `eyelib-clientsmoke-annotation` Gradle 子项目（纯 JVM，零 Minecraft 依赖），包含 `@ClientSmoke` 注解定义
- [ ] **MOD-02**: 创建 `eyelib-clientsmoke` Gradle 子项目（Forge 1.20.1 + legacyForge），包含运行时引擎
- [ ] **MOD-03**: Root 模块通过 `compileOnly + localRuntime` 依赖 `eyelib-clientsmoke`，开发模式下可选择性加载

### Annotation

- [ ] **ANN-01**: 定义 `@ClientSmoke` 注解，RetentionPolicy.CLASS，Target TYPE，包含可选的 `description`、`priority`、`modId` 属性
- [ ] **ANN-02**: 通过 `ModFileScanData` 实现字节码级注解扫描，不触发类加载
- [ ] **ANN-03**: 注解扫描支持发现无来源的第三方 JAR 中标记的类

### Configuration

- [ ] **CFG-01**: 通过 NeoForge `ModConfigSpec` 提供配置系统
- [ ] **CFG-02**: 配置项包含：`enabled`（全局开关）、`screenshotDelay`（进入世界后延迟秒数）、`reloadStabilizeTicks`（渲染稳定 tick 数）、`exitAfterSmoke`（测试完成后自动退出）
- [ ] **CFG-03**: 配置文件位于 `run/client/config/clientsmoke-common.toml`，`enabled=false` 时框架完全静默

### Engine

- [ ] **ENG-01**: 通过 `@EventBusSubscriber` 订阅 Forge 1.20.1 的 `TickEvent.ClientTickEvent` (Phase.START) 驱动状态机
- [ ] **ENG-02**: 状态机包含完整流程：INIT → CONFIG_LOAD → SCAN → WORLD_CREATE → WORLD_WAIT → STABILIZE → EXECUTE → SCREENSHOT → HUD_HIDE → NEXT_TEST → EXIT
- [ ] **ENG-03**: 自动创建创造模式超平坦测试世界（无需用户交互）
- [ ] **ENG-04**: 多阶段就绪检查：世界已加载且 player != null → 截图延迟等待 → 渲染稳定 ticks → 测试执行准备完毕

### Screenshot Capture

- [ ] **CAP-01**: 在 `RenderLevelStageEvent.AFTER_LEVEL` 阶段调用 `Screenshot.grab()` 截取帧缓冲
- [ ] **CAP-02**: 截图自动隐藏 HUD（F1），截后恢复
- [ ] **CAP-03**: 截图输出到 `clientsmoke-reports/screenshots/`，文件命名包含测试类名和时间戳

### Test Execution

- [ ] **EXEC-01**: 安全加载已扫描到的测试类（`Class.forName()`），实例化并调用执行入口
- [ ] **EXEC-02**: 测试执行异常被捕获并记录到报告中，不中断后续测试
- [ ] **EXEC-03**: 按 `priority` 排序执行测试（默认值相等的测试按发现顺序）

### Auto-Exit

- [ ] **EXIT-01**: 当 `exitAfterSmoke=true` 时，所有测试完成后自动退出
- [ ] **EXIT-02**: 采用两阶段退出：`mc.stop()` 优雅关闭 + 3 秒等待 + `Runtime.getRuntime().halt(0)` 强制退出

### Reporting

- [ ] **RPT-01**: 生成 JSON 格式测试报告，包含：总测试数、通过/失败数、每个测试的类名、状态、耗时
- [ ] **RPT-02**: 报告输出到 `clientsmoke-reports/report-{timestamp}.json`

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Screenshot Enhancement

- **CAP-10**: 支持多截图预设（不同视角、不同时间），自动批量截图
- **CAP-11**: GIF 录制模式（指定帧数和帧间隔）

### Test Configuration

- **EXEC-10**: 每个测试独立的世界配置（种子、世界类型、游戏模式）
- **EXEC-11**: 无世界测试模式（标题画面截图等）
- **EXEC-12**: 测试间的世界复用选项

### Multi-Mod

- **ANN-10**: 多模组配置命名空间，每个模组的测试集合独立开关
- **RPT-10**: HTML 格式报告，带截图缩略图预览

## Out of Scope

| Feature | Reason |
|---------|--------|
| 游戏内 GUI 测试管理 | 增加渲染干扰，违背解耦原则 |
| 自动化像素级回归比对 | GPU/驱动差异导致不稳定，需人工判断 |
| 服务端冒烟测试 | NeoForge GameTest 已覆盖；本项目专注客户端 |
| CI 集成脚本 | v1 仅本地运行，CI 需要额外基础设施 |
| 性能基准测试 | 不同硬件不可比，需要专用工具 |
| 测试方法级粒度（method-level @ClientSmoke） | v1 用类级别，简洁可控 |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| MOD-01 | Phase 1 | Pending |
| MOD-02 | Phase 1 | Pending |
| MOD-03 | Phase 1 | Pending |
| ANN-01 | Phase 1 | Pending |
| ANN-02 | Phase 1 | Pending |
| ANN-03 | Phase 1 | Pending |
| CFG-01 | Phase 1 | Pending |
| CFG-02 | Phase 1 | Pending |
| CFG-03 | Phase 1 | Pending |
| ENG-01 | Phase 2 | Pending |
| ENG-02 | Phase 2 | Pending |
| ENG-03 | Phase 2 | Pending |
| ENG-04 | Phase 2 | Pending |
| CAP-01 | Phase 3 | Pending |
| CAP-02 | Phase 3 | Pending |
| CAP-03 | Phase 3 | Pending |
| EXIT-01 | Phase 3 | Pending |
| EXIT-02 | Phase 3 | Pending |
| EXEC-01 | Phase 4 | Pending |
| EXEC-02 | Phase 4 | Pending |
| EXEC-03 | Phase 4 | Pending |
| RPT-01 | Phase 4 | Pending |
| RPT-02 | Phase 4 | Pending |

**Coverage:**
- v1 requirements: 22 total
- Mapped to phases: 22
- Unmapped: 0 ✓

---
*Requirements defined: 2026-05-06*
*Last updated: 2026-05-06 after initial definition*
