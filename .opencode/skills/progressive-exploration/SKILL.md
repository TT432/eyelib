---
name: progressive-exploration
description: Interactive runtime state exploration via the AI debug HTTP server in a running Minecraft client. Use to probe screens, inspect game state, navigate UI, or test hypotheses before writing code.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
---

# progressive-exploration

Interactive runtime state exploration via the AI debug HTTP server (AIDebugServer in clientsmoke mod) in a running Minecraft client: probe screens, inspect game state, navigate UI, test hypotheses before writing code.

## When to use
- 需要知道游戏当前实际处于哪个 screen/state
- 调试运行时问题（实体不渲染、attachment 缺失、粒子不生成）
- 推理 MC/Forge UI 流程
- 检查 manager、entity capability 或粒子状态
- 写生产代码前验证假设
- Do NOT use when: 断点调试：jetbrain_xdebug_* MCP 工具已废弃，断点只能在 IDEA 中手动操作；需要运行时数据时用 /eval 替代

## Rules
- NEVER 退回“猜 → 改代码 → 重建 → 重启”循环而跳过运行时验证；/eval 探测等价于一次可编程断点检视，能证明代码路径到达并给出真实数据
- PREFER AIDebugServer 用 JDK 内置编译器（非 Janino），支持完整 Java 语法：var、lambda、Map.of()、records、switch 表达式、多行 if/return 块
- 代码在 MC 渲染线程执行且有 10 秒超时；世界生成等重操作会使 HTTP 响应超时但可能仍异步完成
- 所有 MC API 调用经 Minecraft.tell() 在渲染线程执行；长时间运行的代码会阻塞渲染循环
- 调试 HTTP 服务器（clientsmoke mod 内 io.github.tt432.clientsmoke.debug.AIDebugServer）仅在配置 ai_debug_port（JVM 系统属性，fallback 环境变量 AI_DEBUG_PORT）时开启；默认端口 25999，mcmcp_launch 自动注入
- When 1. Probe current state:
  - 探测从宽泛查询开始，根据响应逐步收窄；/eval 代码在模板中运行，自动注入 minecraft/player/level（不在世界中时可能为 null）
- When 2. Act on findings:
  - NEVER 预先写好整套脚本；必须由每个响应决定下一个查询
- When 3. Assert and document:
  - 确认行为后写单元测试或更新文档，不让发现停留在临时状态
- When Class name resolution:
  - 写 eval 前用 glob/grep 核实类路径，不要猜包名；模板仅自动导入 Minecraft、LocalPlayer、ClientLevel，其余类必须用全限定名
- When 等待游戏加载完成:
  - 启动后必须等待 /loaded 返回 true 才能开始调试操作（资源加载阶段 minecraft.screen 与 minecraft.player 均为 null）
- When Session verification:
  - 游戏启动后通过游戏内日志时间戳与进程启动时间匹配来确认 debug server 属于本次启动；/eval 结果异常（错误世界/屏幕/坐标）时怀疑 stale session
- When Startup guard:
  - 启动客户端前先 GET /ping 检查调试端口（默认 25999）：返回 ok 说明旧实例仍在运行，须先通过 /eval 执行 minecraft.stop() 关闭并等端口释放；connection refused 才可启动新客户端
  - NEVER 从 shell 杀掉 java 进程

## Workflow
1. 启动前 GET /ping：ok → 先 /eval minecraft.stop() 关旧实例并等端口释放；refused → 启动新客户端 [decision]
2. 轮询 GET /loaded（间隔 3 秒）直到返回 true，再开始调试 [loop]
3. 用 /eval 宽泛探测当前状态（屏幕、UI 元素、管理器、实体状态），根据每次响应收窄并决定下一个查询，循环直至假设被证实或证伪 [loop]
4. 确认行为后写单元测试或更新文档固化发现 [stop]

<!-- locked residual (verbatim, do not edit) -->
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
```java
// Dump registered particle definitions
return io.github.tt432.eyelib.particle.runtime.ParticleDefinitionRegistry
    .publisher().entries().keySet().toString();

// Check an entity's current animation
// (use fully qualified names for project types)
```
```java
// Teleport
player.setPos(100, 70, 100);

// Change time
player.level().setDayTime(0);
```
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
`/eval` 一次就能拿到真实的运行时数据：在可疑代码路径上调用方法、读字段、打印状态，验证假设后再改代码。不要退回"猜 → 改代码 → 重建 → 重启"的循环——`/eval` 等价于一次可编程的断点检视。
