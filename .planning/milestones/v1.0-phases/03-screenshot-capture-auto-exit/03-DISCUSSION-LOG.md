# Phase 3: Screenshot Capture + Auto-Exit - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-07
**Phase:** 3-Screenshot Capture + Auto-Exit
**Areas discussed:** State machine wiring, Screenshot capture API, HUD hiding timing, File naming and output

---

## State Machine Wiring

| Option | Description | Selected |
|--------|-------------|----------|
| 直接扩展现有 ClientSmokeState enum | 在 enum 里追加 HUD_HIDE / SCREENSHOT / EXIT，同一个 switch 分发 | ✓ |
| 新建 Phase3SmokeState 枚举 | Phase 3 单独定义枚举和额外事件处理器 | |
| 保持现有 enum 仅加 switch case | 不扩展枚举，用额外变量分阶段路由 | |

| Option | Description | Selected |
|--------|-------------|----------|
| 两个独立状态 HUD_HIDE + SCREENSHOT | 保证 F1 hideGui 在截图前一帧生效 | ✓ |
| 合并为 SCREENSHOT_WITH_HIDE | 同一 tick 内设 hideGui 然后截图 | |
| 用 RenderLevelStageEvent 阶段控制 | HUD 隐藏不通过状态机 | |

| Option | Description | Selected |
|--------|-------------|----------|
| 一个世界逐个测试 | STABILIZE → TEST_EXEC → HUD_HIDE → SCREENSHOT → NEXT_TEST → EXIT | ✓ |
| 每个测试单独进世界 | 每个测试重新创建世界 | |
| 先执行再批量截图 | Phase 4 先跑完所有测试再截图 | |

| Option | Description | Selected |
|--------|-------------|----------|
| EXIT 内部倒计数 | tick 0 mc.stop() → tick 60 Runtime.halt(0) | ✓ |
| EXIT_STOP + EXIT_HALT 两个状态 | 多一个状态但更显式 | |
| 调度线程 | 不依赖 tick 事件，另起线程 | |

---

## Screenshot Capture API

| Option | Description | Selected |
|--------|-------------|----------|
| 状态机类内加 @SubscribeEvent | ClientSmokeStateMachine 类内新增 RenderLevelStageEvent handler | ✓ |
| 新建 ScreenshotCaptureHandler | 单独的事件订阅类 | |
| 不在 Forge 事件中截图 | TickEvent 中直接调 Screenshot.grab() | |

| Option | Description | Selected |
|--------|-------------|----------|
| 自定义 framebuffer 读 + NativeImage.write() | 自己读 getMainRenderTarget()，完全控制输出 | ✓ |
| 原版 Screenshot.grab() | 自动 PNG + 聊天通知，文件名自动递增 | |
| Forge ScreenShotHelper | Forge 包装工具类 | |

| Option | Description | Selected |
|--------|-------------|----------|
| AFTER_LEVEL | 所有 level 渲染完成后截取 | ✓ |
| AFTER_ENTITIES | 实体渲染后，缺后处理 | |
| AFTER_PARTICLES | 粒子之后，可能缺半透明层 | |

| Option | Description | Selected |
|--------|-------------|----------|
| 直接渲染线程写入 | 在 render event handler 中直接 NativeImage.write() | ✓ |
| 异步线程池写入 | 提交到线程池，渲染线程不阻塞 | |
| buffer 拷贝 + 异步 flush | 拷贝像素数 据到 buffer，异步 IO | |

---

## HUD Hiding Timing

| Option | Description | Selected |
|--------|-------------|----------|
| 仅 hideGui=true | Minecraft.options.hideGui 控制 F1 行为 | ✓ |
| hideGui + 抑制聊天覆盖层 | 额外处理聊天可见性 | |
| 发布 HideGuiEvent | 自定义事件通知其他 mods | |

| Option | Description | Selected |
|--------|-------------|----------|
| SCREENSHOT 状态内一并恢复 | tick handler 截图后立即 hideGui=false | ✓ |
| NEXT_TEST 状态恢复 | 多一帧无 HUD 画面 | |
| 保存原值，截图后恢复 | 更健壮但 v1 不需要 | |

| Option | Description | Selected |
|--------|-------------|----------|
| TickEvent.Phase.START | tick 开始阶段设 hideGui，同帧渲染读取 | ✓ |
| TickEvent.Phase.END | tick 后但渲染可能已开始 | |
| 无所谓 | 假设单线程顺序保证 | |

| Option | Description | Selected |
|--------|-------------|----------|
| transition 到 SCREENSHOT，事件判断 state | HUD_HIDE → SCREENSHOT，render event 检查 state | ✓ |
| HUD_HIDE 不变状态 | 保持在 HUD_HIDE 直到下一 tick | |
| 不做状态管理 | 混乱 | |

---

## File Naming and Output

| Option | Description | Selected |
|--------|-------------|----------|
| 游戏运行目录相对路径 | ./clientsmoke-reports/screenshots/ | ✓ |
| 硬编码 run/ | 仅开发环境有效 | |
| config 可配 | 太灵活 v1 不需要 | |

| Option | Description | Selected |
|--------|-------------|----------|
| yyyyMMdd-HHmmss | 简洁排序友好无特殊字符 | ✓ |
| 完整 ISO-8601 | 更标准但文件名长 | |
| 毫秒精度 | 过于精确 | |

| Option | Description | Selected |
|--------|-------------|----------|
| 简名 Class.getSimpleName() | 简洁易读 | ✓ |
| 全限定名 | 可能路径过长 | |
| 自定义 ID | 不必要 | |

| Option | Description | Selected |
|--------|-------------|----------|
| 自动创建目录 + 覆盖同名文件 | Files.createDirectories() + 直接覆盖 | ✓ |
| 自动创建 + 递增后缀 | 防御性强但不必要 | |
| 不自动创建会报错 | 与自动化原则矛盾 | |

---

## the agent's Discretion

- NativeImage framebuffer 数据读取的具体方式（RenderTarget API 细节）
- 状态枚举中 TEST_EXEC / NEXT_TEST 的实际命名
- EXIT tick 计数器是否需要 try-catch 包装

## Deferred Ideas

None — discussion stayed within phase scope.
