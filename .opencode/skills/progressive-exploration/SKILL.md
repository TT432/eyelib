---
name: progressive-exploration
description: Interactive runtime state exploration via the AI debug HTTP server in a running Minecraft client. Use to probe screens, inspect game state, navigate UI, or test hypotheses before writing code.
---

## When to use

- You need to know what screen/state the game is actually in
- You're debugging a runtime issue (entity not rendering, attachment missing, particle not spawning)
- You're reasoning about MC/Forge UI flow
- You want to inspect managers, entity capabilities, or particle states
- You need to verify a hypothesis before writing production code

## Prerequisites

The debug HTTP server starts automatically in development environment (gated by Forge's `FMLLoader.isProduction()` check). It listens on port `25999`.

### Startup guard

Before starting the client, always check port 25999:

```
GET /ping → {"status": "ok"}  → old instance still running → minecraft.stop() first
GET /ping → connection refused → port free, safe to start
```

If an old instance is running, close it via `/eval`:
```java
minecraft.stop();
```
Wait for port to be freed before launching the new client. **Never kill java processes from shell.**

### 等待游戏加载完成

客户端启动后需要经历资源加载阶段，此阶段 `minecraft.screen` 和 `minecraft.player` 均为 null。**启动后必须等待 `/loaded` 返回 `true` 才能开始调试操作。**

```
GET /loaded → {"loaded": true}  → 游戏已就绪（标题画面或世界中）
GET /loaded → {"loaded": false} → 仍在加载资源，继续等待
```

等待示例（轮询间隔 3 秒）：
```
while /loaded == false → sleep 3s → retry
```

### Session verification

After game startup, confirm the debug server belongs to the current launch by checking the in-game log timestamp matches the process start time. If `/eval` gives unexpected results (wrong world, wrong screen, different position), suspect a stale session.

## Core Workflow

### 1. Probe current state

Start broad, narrow down based on the response. The code runs inside a template that auto-injects `minecraft`, `player`, `level`. They may be null if not in a world.

```java
// Where are we?
return minecraft.screen == null ? "in world" : minecraft.screen.getClass().getName();

// What UI elements are visible?
StringBuilder sb = new StringBuilder();
for (Object child : minecraft.screen.children()) {
    sb.append(child.getClass().getName());
    if (child instanceof net.minecraft.client.gui.components.AbstractWidget w) {
        try { sb.append(" msg=").append(w.getMessage().getString()); } catch(Exception e) {}
    }
    sb.append("\n");
}
return sb.toString();
```

### 2. Act on findings

Each response guides the next query. Don't write a script upfront — let the runtime state decide.

### 3. Assert and document

Once you confirm the behavior, write a unit test or update documentation. Don't leave findings ephemeral.

## Practical Patterns

### Navigate MC UI

```java
// Click a button by its text label
for (Object child : minecraft.screen.children()) {
    if (child instanceof net.minecraft.client.gui.components.Button b) {
        if (b.getMessage().getString().equals("Singleplayer")) {
            b.onPress();
        }
    }
}

// Type into a text field
for (Object child : minecraft.screen.children()) {
    if (child instanceof net.minecraft.client.gui.components.EditBox eb) {
        eb.setValue("new value");
    }
}
```

### Inspect eyelib runtime state

```java
// Dump registered particle definitions
return io.github.tt432.eyelib.particle.runtime.ParticleDefinitionRegistry
    .publisher().entries().keySet().toString();

// Check an entity's current animation
// (use fully qualified names for project types)
```

### Modify game state

```java
// Teleport
player.setPos(100, 70, 100);

// Change time
player.level().setDayTime(0);
```

### Error recovery

```java
// If stuck on LoadingErrorScreen, find the skip button
for (Object child : minecraft.screen.children()) {
    if (child instanceof net.minecraft.client.gui.components.Button b) {
        if (b.getMessage().getString().contains("Proceed")) {
            b.onPress();
        }
    }
}
```

### Shutdown

```java
// Close the game properly — never kill java processes from shell
minecraft.stop();
```

## Debugger Workflow

> **`jetbrain_xdebug_*` MCP 工具已废弃**，当前 session 无断点调试 MCP 能力。断点调试只能在 IDEA 中手动操作（agent 无法直接设断点）。需要运行时数据时，用 progressive exploration 的 `/eval`（`eyelib_debug_execute`）作为替代。

`/eval` 一次就能拿到真实的运行时数据：在可疑代码路径上调用方法、读字段、打印状态，验证假设后再改代码。不要退回"猜 → 改代码 → 重建 → 重启"的循环——`/eval` 等价于一次可编程的断点检视。

Never skip runtime verification in favor of "guess → change code → rebuild → restart". An `/eval` probe proves the code path is reached and shows real data.

### Janino class name gotcha

The Janino compiler auto-imports only `Minecraft`, `LocalPlayer`, `ClientLevel`. All other classes need fully qualified names. **Before writing an eval, verify the class path** — don't guess package names. Use `glob`/`grep` 查找类路径。

## Limitations

- **Janino Java dialect**: the embedded compiler does not support `var`, pattern-matching `instanceof`, switch expressions, or records. Use explicit types and traditional syntax.
- **Auto-imports**: only `Minecraft`, `LocalPlayer`, `ClientLevel` are imported. Other classes need fully qualified names.
- **Timeout**: code runs on the MC render thread with a 10-second timeout. World generation and heavy operations will time out the HTTP response but may still complete asynchronously.
- **Thread safety**: all MC API calls execute on the render thread via `Minecraft.tell()`. Long-running code blocks the render loop.
