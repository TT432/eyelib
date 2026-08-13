---
name: eyelib-debug
description: Eyelib 客户端调试——mcmcp 拓展启动客户端、/eval 执行代码、渲染诊断 Phase、实体操作。Use when debugging rendering, executing /eval code, or diagnosing entity issues.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
  tags: eyelib, debug, mcp, eval, rendering
  related-skills: eyelib, eyelib-build, eyelib-renderdoc, eyelib-clientsmoke, mcmcp
---

# eyelib-debug

Eyelib 客户端调试：用 mcmcp 拓展启动 MC 客户端、/eval 执行 Java 方法体、按诊断 Phase 排查渲染/实体问题、实体操作与 GUI 导航。

## When to use
- 调试渲染问题（实体不渲染、部位缺失、材质/RenderType 错误）
- 在运行中的 MC 客户端 JVM 内执行 /eval Java 代码
- 诊断实体问题（召唤、拉近、距离剔除）

## Rules
- When 跨模块调试：委派模式:
  - NEVER 问题涉及多个模块时不要自己读源码，走委派模式。
  - 子代理 context 只写「目标」+「文件列表（不允许查看其他文件）」+「约束」，不预设注入点；让子代理自己发现，你只提供原材料和目标。
- When 已知限制:
  - Bridge 模块 Forge dev 声明要求——根模块必须：1) mods.toml 含完整 [[mods]]；2) 至少一个 @Mod 注解类；3) build.gradle 加 api/modImplementation/jarJar 三行。
  - NEVER Bridge → Domain 单向依赖，禁止反向。
  - Port 共享规则——多 domain 模块共享的 Port 移至 util 包。
  - NEVER PortFriendlyByteBuf 不可用于 StreamCodec——Mojang 的 StreamCodec<T> 强制要求 FriendlyByteBuf。
- When /eval 语法:
  - /eval 只传方法体，不包 Object run(...) { }；服务端自动包装并注入 Minecraft minecraft、LocalPlayer player、ClientLevel level 三参数，直接使用、不要重新声明。
  - PREFER AIDebugServer 使用 JDK 自带编译器（非 Janino），支持完整 Java 语法：var、lambda、Map.of()、while、多行 if return 均可。
  - eval 代码在游戏工作目录（versions/<node>/run/）执行；访问项目文件必须用绝对路径。
- When 实体操作:
  - 拉近实体用 setPos，不用 teleportTo——后者只在 ServerLevel 生效。
- When 原则:
  - PREFER 先数据，后管线——基准是 .mcpack Bedrock 数据 + Mojang 官方文档，不是 vanilla JE。
  - PREFER 先空间，后代码——实体距离剔除是最常见的「不渲染」原因。
  - NEVER 禁止问用户「你看到了什么」——非视觉优先，用程序化手段（/eval、RenderDoc）验证。
  - PREFER 管线卫生 ≠ 渲染正确——数据经过管线后变成了什么，才是问题所在。
  - 禁止猜测——先用 /eval 查实际状态，再定位根因。
  - PREFER 系统性思维——多实体共享 bug 优先找系统级根因。
- When 诊断 Phase:
  - 实体渲染距离阈值 = boundingBox.getSize() × 64 × viewScale。
  - useBuiltInRenderSystem=true → eyelib 接管渲染；false → fallback 到 vanilla 渲染器。
  - 若所有组件的 visCount == totalCount 但视觉上有「部位缺失」→ 判定为深度互斥。
- When 资源重载分类计时:
  - 资源重载分类计时：先启用 ProfiledReloadInstance（log4j 把 ReloadableResourceManager 设为 DEBUG），然后 reloadResourcePacks() 或 F3+T。
- When 1.20.1 截图在虚拟显示器上捕获全暗:
  - [when OrayIddDriver 虚拟显示器环境] NEVER 不要在 OrayIddDriver 虚拟显示器上用 ScreenshotRecorder.grab / Screenshot.grab 验证 1.20.1 渲染——捕获 PNG 整体偏暗（白天天空 avg≈47，应为亮蓝 ~150），无法用于视觉验证；原因是 1.20.1 与 26.1.2 截图 GPU 回读管线不同，OrayIddDriver 对 1.20.1 路径回读异常。
  - [when OrayIddDriver 虚拟显示器环境] 验证 1.20.1 渲染正确性用 clientsmoke EntitySceneRenderer 的 FBO 回读路径（实体渲染到独立 RenderTarget，不经主显示器），或换非虚拟显示器；26.1.2 截图正常，可作跨版本对照基准。
  - PREFER grab 后客户端可能因 GPU 回读崩溃（OrayIddDriver），但 PNG 会先保存——可用 python(PIL) 分析磁盘文件。
- When 26.1.2 渲染偶发 IllegalStateException: Not building!:
  - NEVER 不要按已废弃的「每 submit 新建 RenderType 实例」结论诊断——RenderType 驻留已实证（2026-08-06）：自定义 eyelib_material_* 由 BrRenderTypeFactory.CACHE（Key=texture+state 记录结构相等）驻留；vanilla 路径 RenderTypes.entitySolid 等在 26.1.2 走 Util.memoize（javap 字节码实证）；两条路径都不存在批次合并收益丧失问题。
- When 驱动 LDLib2 节点画布内控件:
  - LDLib2 nodegraphtookit 画布（GraphView）内控件坐标在缩放/平移变换下——合成点击前先把控件中心经 view transform 换算到屏幕坐标；点击落空会使控件未获焦点、后续键盘事件静默无效。
- When 驱动 LDLib2 UI 的鼠标事件必须先用 glfwSetCursorPos 喂悬停:
  - LDLib2 的悬停/命中（ModularUIWidget.lastMouseX → getLastHoveredElement）只在渲染时从真实光标位置更新——合成调用 Screen.mouseMoved/mouseClicked(x,y) 不会改变悬停目标，MOUSE_DOWN 会派给真实光标下的元素；java.awt.Robot 在客户端 JVM 不可用（headless）。驱动鼠标事件必须先用 glfwSetCursorPos 喂悬停。
- When 子代理委派：不预设结论:
  - NEVER 子代理 context 禁止：身份提示词（「你是 eyelib 项目的调试助手」）、文件摘要、引导性结论（「注意 XXX 已被覆盖」）、实现路径预设（「应该在 X 文件中改 Y」）。
- When Attachable 渲染注入点错误:
  - Bedrock attachable 只在物品装备（手持/穿戴）时激活，不覆盖掉落物或 GUI；MC Java 中不同路径走不同类。
  - NEVER 不要混入 net.minecraft.client.renderer.entity.ItemRenderer——该类渲染物品实体和 GUI，不处理手持物品；手持 attachable 注入点是 ItemInHandRenderer.renderItem()（@Inject HEAD cancellable，检查后 ci.cancel()）。
- When launch 超时：僵尸进程占用端口:
  - mcmcp_launch 超时最常见原因是上次未正常关闭的客户端仍占用调试端口（默认 25999），超时后子进程可能残留；修复：launch 前先 mcmcp_close（等待端口释放并 force kill）。
- When fallback shell（mcmcp 拓展不可用时）:
  - AIDebugServer 仅在显式配置 ai_debug_port（JVM 系统属性，fallback 环境变量 AI_DEBUG_PORT，默认 25999）时开启；手动启动客户端必须显式传 -Dai_debug_port=<port> 或设环境变量，否则服务器不开启；mcmcp_launch 会自动经环境变量传入端口。
- When 设计原则:
  - 无状态：所有状态从 AIDebugServer 端点实时查询。
  - mcmcp_launch 不自动重建、不跑构建：启动前要求 run 产物已生成，缺产物时报错并提示先跑 mcmcp_build；编译量大有超时风险时先 mcmcp_build 再 launch。

## Workflow
1. 查询会话状态 mcmcp_status(info="all")
2. 启动客户端 mcmcp_launch(timeout=120)；超时时先 mcmcp_close 释放端口再重试（见 R-ZOMBIE）
3. 进入单人世界 mcmcp_enter_world(world_name="Debug World")
4. 按需执行 mcmcp_execute(code='...') 诊断/操作（只传方法体） [loop]
5. 关闭客户端 mcmcp_close() [stop]
6. 确定涉及模块——根据问题描述判断哪些子项目参与
7. 对每个模块跑 repomix --style markdown --include "src/main/**" 生成单文件上下文
8. 委托子代理——将所有 repomix 文件和相关的 .mcpack 提取文件传给子代理，context 按模板：当前问题<一句话> + 全部所需文件列表 + 关键约束
9. 不预设注入点——context 只写「目标」和「可用文件」，不写「应该在哪改」或「应该怎么改」（见 R-CTX-ALLOW/R-CTX-DENY） [stop]
10. Phase 0 空间位置（最重要）：/eval 查 distSq 与 target.shouldRender(playerX, playerY, playerZ)；距离阈值见 R-DIST
11. Phase 2 BrClientEntity 注册：/eval 查 ClientEntityManager.INSTANCE.get(id) 是否为 null（BR_NULL/BR_OK）
12. Phase 4 ModelComponent 完整性：/eval 反射调 RenderData.getComponent(target).getModelComponents() 查 comps 数量
13. Phase 5 Eyelib 接管状态：/eval 反射读 useBuiltInRenderSystem 字段并查当前 renderer 类名；useBuiltIn=true → eyelib 接管，false → fallback 到 vanilla（见 R-USEBUILTIN） [decision]
14. Phase G 多组件渲染顺序/深度排查：/eval 遍历组件 getSerializableInfo() 查 model/visCount；visCount == totalCount 但部位缺失 → 深度互斥（见 R-DEPTHMUTEX） [decision]
15. Phase 11 GL 状态查询：/eval 用 LWJGL GL20.glGetInteger(GL_CURRENT_PROGRAM) 等查询当前 GL 状态
16. 召唤实体：/eval EntityType.X.create(server.overworld()) → setPos → addFreshEntity
17. 拉近实体：/eval target.setPos(player 附近坐标)——不用 teleportTo（见 R-SETPOS）
18. GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), lx*guiScale, ly*guiScale) 把真实光标移到目标控件上（窗口内物理 px）（可行路径已验证 2026-08-04）
19. 让一帧渲染过去——分两次 mcmcp_execute，不要在同一 eval 里连着点
20. 再合成 screen.mouseClicked/mouseDragged/mouseReleased/mouseScrolled（坐标用 logical px = physical/guiScale）
21. 验证悬停命中用 rootElement.hitTest(x, y)（ModularUI.ui.rootElement，double 签名，返回 oshi Pair）
22. 环境前提失效（OrayIddDriver 远程会话覆写光标）时：判定法 = glfwSetCursorPos(w,100,100) 后同 eval 立即 glfwGetCursorPos 读回，不等于 (100,100) 即不可用；此时悬停卡在角落元素上，改用反射直驱（写 widget 的 lastMouseX/lastMouseY 字段或直接调 widget 的 mouseMoved/mouseClicked） [fallback]
23. 26.1.2 偶发 IllegalStateException: Not building! 已有护栏（DeferredRenderSink.submit 丢弃该段几何 + ERROR 日志，不崩客户端）；根因未定位，下次复现时：1) 取护栏日志中的 consumer 身份；2) 在 BufferSource.endBatch 处下条件断点对照是谁结束的该 builder [fallback]

<!-- locked residual (verbatim, do not edit) -->

```powershell
# 查端口占用
netstat -ano | findstr 25999

# 手动启动 RenderDoc capture (PowerShell, 不走 WSL)。
# 先向 VM 参数文件追加调试端口（runClient.cmd 经 @clientRunVmArgs.txt 读 JVM 参数）：
Add-Content versions\1.20.1\build\moddev\clientRunVmArgs.txt "`n-Dai_debug_port=25999"
Set-Location E:\_ideaProjects\qylEyelib
& "E:\RenderDoc\renderdoccmd.exe" capture `
    -c eyelib_capture --opt-hook-children `
    "E:\_ideaProjects\qylEyelib\versions\1.20.1\build\moddev\runClient.cmd"

# curl 交互 (Windows 自带 curl.exe; PowerShell 别名需显式调用)
'return "hello";' | curl.exe -s --proxy http://127.0.0.1:10808 `
    -X POST http://localhost:25999/eval -H "Content-Type: text/plain" -d "@-"
```

```
当前问题：<一句话>

全部所需文件（不允许查看其他文件）：
/path/to/module-a.md
/path/to/module-b.md
/path/to/entity-data.md

<关键约束或特性>
```
```
mcmcp_execute(code='net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance(); net.minecraft.world.entity.Entity target = mc.level.getEntity(250); double distSq = mc.player.distanceToSqr(target); return "distSq=" + distSq + " shouldRender=" + target.shouldRender(mc.player.getX(), mc.player.getY(), mc.player.getZ());')
```

```
mcmcp_execute(code='return io.github.tt432.eyelib.client.manager.ClientEntityManager.INSTANCE.get("minecraft:slime") == null ? "BR_NULL" : "BR_OK";')
```

```
mcmcp_execute(code='Object cap = io.github.tt432.eyelib.capability.RenderData.getComponent(target); java.util.List comps = (java.util.List) cap.getClass().getMethod("getModelComponents").invoke(cap); return "comps=" + comps.size();')
```

```
mcmcp_execute(code='Object cap = io.github.tt432.eyelib.capability.RenderData.getComponent(target); java.lang.reflect.Field f = cap.getClass().getDeclaredField("useBuiltInRenderSystem"); f.setAccessible(true); boolean ub = f.getBoolean(cap); return "useBuiltIn=" + ub + " renderer=" + minecraft.getEntityRenderDispatcher().getRenderer(target).getClass().getSimpleName();')
```

```
mcmcp_execute(code='return "GL_PROGRAM=" + org.lwjgl.opengl.GL20.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM);')
```

```
mcmcp_execute(code='Object cap = io.github.tt432.eyelib.capability.RenderData.getComponent(target); java.util.List comps = (java.util.List) cap.getClass().getMethod("getModelComponents").invoke(cap); for (int i=0; i<comps.size(); i++) { Object comp = comps.get(i); Object info = comp.getClass().getMethod("getSerializableInfo").invoke(comp); String model = (String) info.getClass().getMethod("model").invoke(info); ... }')
```

```
mcmcp_execute(code='net.minecraft.world.entity.Mob slime = (net.minecraft.world.entity.Mob) net.minecraft.world.entity.EntityType.SLIME.create(minecraft.getSingleplayerServer().overworld()); slime.setPos(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ() + 3); minecraft.getSingleplayerServer().overworld().addFreshEntity(slime); return "ok";')
```

```
mcmcp_execute(code='target.setPos(minecraft.player.getX() + 3, minecraft.player.getY(), minecraft.player.getZ() + 3); return "ok";')
```

```
mcmcp_execute(code='net.minecraft.client.gui.screens.TitleScreen ts = (net.minecraft.client.gui.screens.TitleScreen) mc.screen; ((net.minecraft.client.gui.components.Button) ts.children().get(0)).onPress(); return "ok";')
```
```java
org.apache.logging.log4j.core.config.Configurator.setLevel(
    "net.minecraft.server.packs.resources.ReloadableResourceManager",
    org.apache.logging.log4j.Level.DEBUG
);
```

```java
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void onRenderItem(LivingEntity entity, ItemStack stack,
            ItemDisplayContext context, boolean left, PoseStack pose,
            MultiBufferSource buffer, int light, CallbackInfo ci) {
        // 检查 attachable，渲染自定义模型，ci.cancel()
    }
}
```

| # | 特征 | 验证 | 预期 |
|---|---|---|---|
| A | 实体在渲染距离内 | Phase 0 shouldRender() | true |
| B | BrClientEntity 已注册 | Phase 2 ClientEntityManager.get() | 非 null |
| C | ModelComponent 已创建 | Phase 4 getModelComponents() | comps >= 1 |
| D | 材质在 MaterialManager | 后缀匹配 findMaterial | 非 null |
| E | RenderType 正确 | getRenderType() | 按 define flags |
| F | 纹理路径单次 .png | 查 info.texture() | 不以 .png.png 结尾 |
| G | Alpha cutout 阈值 | Phase A 查 UV 区域 alpha | opaque >= 30% |
| H | 多组件渲染顺序 | Phase G 查 components 顺序 | 内层先于外层 |
| Bedrock 路径 | MC Java 类 | 注入？ |
|---|---|---|
| `controller.render.item_default`（手持） | `ItemInHandRenderer.renderItem()` | ✅ |
| `controller.render.armor`（穿戴） | `HumanoidModel.renderArmor()` | ❌ |
| `minecraft:icon`（GUI/掉落） | `ItemRenderer`（实体渲染器，非物品渲染器） | ❌ |
LDLib2 的悬停/命中（`ModularUIWidget.lastMouseX` → `getLastHoveredElement`）只在渲染时从**真实光标**位置更新——合成调用 `Screen.mouseMoved/mouseClicked(x,y)` 不会改变悬停目标，MOUSE_DOWN 会派给真实光标下的元素（通常不是目标）。`java.awt.Robot` 在客户端 JVM 不可用（headless）。
**环境前提（2026-08-06 实证）**：该路径依赖 glfwSetCursorPos 能生效。本工作站 OrayIddDriver 远程会话活跃时，远端的物理光标注入会持续覆写光标位置——set 后立即读回仍是远端坐标（如 33828,32987，虚拟桌面坐标系、远超窗口尺寸，且持续漂移），渲染线程上 set 同样无效。此与 MC/LDLib2 无关，纯环境问题。判定方法：`glfwSetCursorPos(w,100,100)` 后同 eval 立即 `glfwGetCursorPos` 读回，不等于 (100,100) 即不可用于该路径。此时悬停会卡在坐标映射的角落元素上（如 WorkbenchToolbar），应改用反射直驱（写 widget 的 lastMouseX/lastMouseY 字段或直接调 widget 的 mouseMoved/mouseClicked）。2026-08-04 验证有效是因为当时远程会话未注入光标。
现象：26.1.2 进超平坦世界后实体渲染偶发 `IllegalStateException: Not building!`（栈：CustomFeatureRenderer.renderSolid → EntityRenderOrchestrator → RenderHelper → DFSModel → VertexConsumerPort.vertex）。已有护栏（DeferredRenderSink.submit，2026-08-02 提交 edc37263）：writer 回调抛 IllegalStateException 时丢弃该段几何 + ERROR 日志（renderType + consumer 身份 + 完整栈），不再崩客户端。
**根因未定位。** 机制分析：26.1 的 BufferSource.getBuffer 对 canConsolidateConsecutiveGeometry=false 的类型在重复获取时 endBatch 旧 builder；崩溃 = writer 写入一个被 endBatch 的 builder。但 vanilla 流程与 eyelib 回调链都不在循环中调 getBuffer。下次复现时：1) 取护栏日志中的 consumer 身份；2) 在 BufferSource.endBatch 处下条件断点对照是谁结束的该 builder。
**环境噪声**：26.1.2 客户端曾反复 JVM 级死亡（C2 symbol.cpp、0xC0000005 无 hs_err），疑与渲染路径原生不稳定同源（JDK 25 + OrayIddDriver）。
| 工具 | 用途 |
|---|---|
| `mcmcp_launch(version, timeout=120, port=25999)` | 按 `.mcmcp` 中该版本的 launch_cmd 启动客户端 → 轮询 /ping /loaded 就绪 |
| `mcmcp_enter_world(world_name="Debug World", timeout=60)` | 进入单人世界 |
| `mcmcp_execute(code)` | 在 JVM 内执行 Java 方法体（同 /eval） |
| `mcmcp_send_command(side, command_text)` | 发送 slash 命令；side="client" 走玩家网络包，side="server" 在 integrated server 直接执行（仅单人） |
| `mcmcp_status(info="summary")` | 查询会话状态 |
| `mcmcp_close()` | 关闭客户端 |
| `mcmcp_build(version)` | 编译 + 刷新 ModDevGradle 启动产物 |
| `mcmcp_test(version, test_filter="")` | 跑 `:{version}:test` |
| `mcmcp_nullaway(module="", version)` | NullAway/Error Prone nullness 检查 |
| `mcmcp_clientsmoke(timeout=120, version)` | 运行 clientsmoke 测试 |
**生成 repomix** — 对每个模块跑 `repomix --style markdown --include "src/main/**"`，生成单文件上下文
