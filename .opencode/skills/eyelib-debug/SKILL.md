---
name: eyelib-debug
description: Eyelib MCP 调试——启动客户端、/eval 执行代码、渲染诊断 Phase、实体操作。Use when debugging rendering, executing /eval code, or diagnosing entity issues.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
  tags: eyelib, debug, mcp, eval, rendering
  related-skills: eyelib, eyelib-build, eyelib-renderdoc, eyelib-clientsmoke
---

# Eyelib 调试与渲染诊断

## MCP 工具

eyelib-debug MCP server 封装了整个调试流程。工具以 `eyelib_debug_*` 前缀注册。

| 工具 | 用途 |
|---|---|
| `eyelib_debug_launch(timeout=120)` | RenderDoc capture 模式启动客户端。内部：重建 → 杀僵尸 → renderdoccmd capture → 轮询就绪 |
| `eyelib_debug_enter_world(world_name="Debug World", timeout=60)` | 进入单人世界 |
| `eyelib_debug_execute(code)` | 在 JVM 内执行 Java 方法体（同 /eval） |
| `eyelib_debug_send_command(side, command_text)` | 发送 slash 命令；side="client" 走玩家网络包，side="server" 在 integrated server 直接执行（仅单人） |
| `eyelib_debug_capture_frame()` | 程序化截帧 |
| `eyelib_debug_status(info="summary")` | 查询会话状态 |
| `eyelib_debug_close()` | 关闭客户端 |
| `eyelib_debug_build(module="")` | 构建单个或全部模块（:module:jar + createLaunchScripts）。不传 module 则构建全部 |
| `eyelib_debug_nullaway(module="")` | NullAway/Error Prone nullness 检查 |
| `eyelib_debug_clientsmoke(timeout=120)` | 运行 clientsmoke 测试 |

### 设计原则
- **无状态**：所有状态从 AIDebugServer 端点实时查询
- **launch 自动重建**：内部跑 `:compileJava` + `createLaunchScripts` 后再启动

### 典型流程

```
eyelib_debug_status(info="all")
eyelib_debug_launch(timeout=120)
eyelib_debug_enter_world(world_name="Debug World")
eyelib_debug_execute(code='return "hello world";')
eyelib_debug_capture_frame()
eyelib_debug_close()
```

## /eval 语法

- **只传方法体**，不包 `Object run(...) { }`。服务端自动包装，签名注入 `Minecraft minecraft, LocalPlayer player, ClientLevel level` 三参数，直接用不要重新声明
- AIDebugServer 使用 **JDK 自带编译器**（非 Janino），支持完整 Java 语法：`var`、lambda、`Map.of()`、`while`、多行 `if return` 均可
- 代码在游戏工作目录（`versions/<node>/run/`）执行，访问项目文件用绝对路径

### fallback shell（MCP 不可用时）

```powershell
# 查端口占用
netstat -ano | findstr 25999

# 手动启动 RenderDoc capture (PowerShell, 不走 WSL)
Set-Location E:\_ideaProjects\qylEyelib
& "E:\RenderDoc\renderdoccmd.exe" capture `
    -c eyelib_capture --opt-hook-children `
    "E:\_ideaProjects\qylEyelib\build\moddev\runClient.cmd"

# curl 交互 (Windows 自带 curl.exe; PowerShell 别名需显式调用)
'return "hello";' | curl.exe -s --proxy http://127.0.0.1:10808 `
    -X POST http://localhost:25999/eval -H "Content-Type: text/plain" -d "@-"
```

## 跨模块调试：委派模式

当问题涉及多个模块时，**不要自己读源码**。流程：

1. **确定涉及模块** — 根据问题描述判断哪些子项目参与
2. **生成 repomix** — 对每个模块跑 `repomix --style markdown --include "src/main/**"`，生成单文件上下文
3. **委托子代理** — 将所有 repomix 文件和相关的 .mcpack 提取文件传给子代理
4. **不预设注入点** — context 中只写「目标」和「可用文件」，不写「应该在哪改」或「应该怎么改」

子代理 context 格式：
```
当前问题：<一句话>

全部所需文件（不允许查看其他文件）：
/path/to/module-a.md
/path/to/module-b.md
/path/to/entity-data.md

<关键约束或特性>
```

❌ 不要写「你是 eyelib 项目的调试助手」— 无身份提示词
❌ 不要写文件摘要（「X 模块 — BedrockAddonLoader...」）
❌ 不要写引导性结论（「注意 XXX 已被覆盖」）
❌ 不要写「应该在 X 文件中改 Y」— 不限定实现路径

## 渲染诊断

### 原则

1. **先数据，后管线** — 基准是 .mcpack Bedrock 数据 + Mojang 官方文档，不是 vanilla JE
2. **先空间，后代码** — 实体距离剔除是最常见的"不渲染"原因
3. **非视觉优先** — 用程序化手段验证（/eval、RenderDoc），禁止问"你看到了什么"
4. **管线卫生 ≠ 渲染正确** — 数据经过管线后变成了什么，才是问题所在
5. **禁止猜测** — 先用 /eval 查实际状态，再定位根因
6. **系统性思维** — 多实体共享 bug 优先找系统级根因

### 诊断 Phase

**Phase 0: 空间位置（最重要）**
```
eyelib_debug_execute(code='net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance(); net.minecraft.world.entity.Entity target = mc.level.getEntity(250); double distSq = mc.player.distanceToSqr(target); return "distSq=" + distSq + " shouldRender=" + target.shouldRender(mc.player.getX(), mc.player.getY(), mc.player.getZ());')
```
距离阈值 = `boundingBox.getSize() × 64 × viewScale`

**Phase 2: BrClientEntity 注册**
```
eyelib_debug_execute(code='return io.github.tt432.eyelib.client.manager.ClientEntityManager.INSTANCE.get("minecraft:slime") == null ? "BR_NULL" : "BR_OK";')
```

**Phase 4: ModelComponent 完整性**
```
eyelib_debug_execute(code='Object cap = io.github.tt432.eyelib.capability.RenderData.getComponent(target); java.util.List comps = (java.util.List) cap.getClass().getMethod("getModelComponents").invoke(cap); return "comps=" + comps.size();')
```

**Phase 5: Eyelib 接管状态**
```
eyelib_debug_execute(code='Object cap = io.github.tt432.eyelib.capability.RenderData.getComponent(target); java.lang.reflect.Field f = cap.getClass().getDeclaredField("useBuiltInRenderSystem"); f.setAccessible(true); boolean ub = f.getBoolean(cap); return "useBuiltIn=" + ub + " renderer=" + minecraft.getEntityRenderDispatcher().getRenderer(target).getClass().getSimpleName();')
```
`useBuiltIn=true` → eyelib 接管渲染。`false` → fallback 到 vanilla。

**Phase 11: GL 状态查询**
```
eyelib_debug_execute(code='return "GL_PROGRAM=" + org.lwjgl.opengl.GL20.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM);')
```

**Phase G: 多组件渲染顺序/深度排查**
```
eyelib_debug_execute(code='Object cap = io.github.tt432.eyelib.capability.RenderData.getComponent(target); java.util.List comps = (java.util.List) cap.getClass().getMethod("getModelComponents").invoke(cap); for (int i=0; i<comps.size(); i++) { Object comp = comps.get(i); Object info = comp.getClass().getMethod("getSerializableInfo").invoke(comp); String model = (String) info.getClass().getMethod("model").invoke(info); ... }')
```
若所有组件的 visCount == totalCount 但视觉上有"部位缺失"→ 深度互斥。

### 实体操作

召唤实体：
```
eyelib_debug_execute(code='net.minecraft.world.entity.Mob slime = (net.minecraft.world.entity.Mob) net.minecraft.world.entity.EntityType.SLIME.create(minecraft.getSingleplayerServer().overworld()); slime.setPos(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ() + 3); minecraft.getSingleplayerServer().overworld().addFreshEntity(slime); return "ok";')
```

拉近（用 `setPos` 不用 `teleportTo`——后者只在 ServerLevel 生效）：
```
eyelib_debug_execute(code='target.setPos(minecraft.player.getX() + 3, minecraft.player.getY(), minecraft.player.getZ() + 3); return "ok";')
```

### GUI 导航

```
eyelib_debug_execute(code='net.minecraft.client.gui.screens.TitleScreen ts = (net.minecraft.client.gui.screens.TitleScreen) mc.screen; ((net.minecraft.client.gui.components.Button) ts.children().get(0)).onPress(); return "ok";')
```

### 特征矩阵

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

### 资源重载分类计时

启用 ProfiledReloadInstance：
```java
org.apache.logging.log4j.core.config.Configurator.setLevel(
    "net.minecraft.server.packs.resources.ReloadableResourceManager",
    org.apache.logging.log4j.Level.DEBUG
);
```
然后 `reloadResourcePacks()` 或 F3+T。

## 已知限制

- **Bridge 模块 Forge dev 声明要求** — 根模块必须：1) `mods.toml` 含完整 `[[mods]]`；2) 至少一个 `@Mod` 注解类；3) `build.gradle` 加 `api`/`modImplementation`/`jarJar` 三行
- **Bridge → Domain 单向依赖，禁止反向**
- **Port 共享规则** — 多 domain 模块共享的 Port 移至 `util` 包
- **PortFriendlyByteBuf 不可用于 StreamCodec** — Mojang 的 `StreamCodec<T>` 强制要求 `FriendlyByteBuf`

## Common Pitfalls

### launch 超时：僵尸进程占用端口

`eyelib_debug_launch` 超时最常见原因是上一次未正常关闭的客户端仍占领 25999。MCP 超时后子进程可能残留。

**修复**：launch 前先 `eyelib_debug_close`。close 会等待端口释放并 force kill。

**launch + rebuild 超时**：`eyelib_debug_launch` 内部会 rebuild。如果编译量大有超时风险，先 `eyelib_debug_build(module='all')` 再 launch。

### 子代理委派：不预设结论

子代理 context 中：
- ✅ 写「目标」+「文件列表」+「约束」
- ❌ 身份提示词（「你是 eyelib 项目的调试助手」）
- ❌ 文件摘要（「X 模块 — 包含 BedrockAddonLoader」）
- ❌ 引导性结论（「注意 SP2 覆盖了 root 的 RC」）
- ❌ 实现路径（「应该在 X 文件中改 Y」）

让子代理自己发现这些。你只提供原材料和目标。

### 1.20.1 截图在虚拟显示器上捕获全暗

`ScreenshotRecorder.grab` / `Screenshot.grab` 在 **OrayIddDriver 虚拟显示器**上，1.20.1 捕获的 PNG 整体偏暗（白天天空 avg≈47，应为亮蓝 ~150），无法用于视觉验证渲染。26.1.2 同 API 捕获正常（avg≈144）。

**原因**：1.20.1 与 26.1.2 的截图 GPU 回读管线不同，OrayIddDriver 对 1.20.1 路径回读异常。

**规避**：
- 验证 1.20.1 渲染正确性用 **clientsmoke `EntitySceneRenderer` 的 FBO 回读路径**（实体渲染到独立 RenderTarget，不经主显示器），或换非虚拟显示器。
- 26.1.2 截图正常，可作跨版本对照基准。
- `grab` 后客户端可能因 GPU 回读崩溃（OrayIddDriver），但 PNG 会先保存，可用 python(PIL) 分析磁盘文件。

### Attachable 渲染注入点错误

Bedrock attachable 只在物品装备（手持/穿戴）时激活，不覆盖掉落物或 GUI。MC Java 中不同路径走不同类：

| Bedrock 路径 | MC Java 类 | 注入？ |
|---|---|---|
| `controller.render.item_default`（手持） | `ItemInHandRenderer.renderItem()` | ✅ |
| `controller.render.armor`（穿戴） | `HumanoidModel.renderArmor()` | ❌ |
| `minecraft:icon`（GUI/掉落） | `ItemRenderer`（实体渲染器，非物品渲染器） | ❌ |

常见错误：混入 `net.minecraft.client.renderer.entity.ItemRenderer`——该类渲染物品实体和 GUI，不处理手持物品。手持走 `ItemInHandRenderer`。

正确注入点：
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
