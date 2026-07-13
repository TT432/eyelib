# Known-Good Capture Workflow

已验证通过的完整端到端流程：从零到 .rdc 截帧文件。

## 前置条件

- `build/moddev/runClient.cmd` 已通过 `createLaunchScripts` 生成(经 `eyelib_debug_build`)
- 无僵尸进程占用 port 25999

```powershell
netstat -ano | findstr 25999  # 查僵尸
```

## 完整命令序列

> 以下 curl 步骤既可在 PowerShell(`curl.exe ...`) 也可在 WSL bash(`curl ...`)执行。MCP 工具(`eyelib_debug_*`) 是首选,本文是 fallback。

### 1. 启动 RenderDoc capture 模式

```powershell
Set-Location E:\_ideaProjects\qylEyelib
& "E:\RenderDoc\renderdoccmd.exe" capture `
    -c eyelib_capture --opt-hook-children `
    "E:\_ideaProjects\qylEyelib\build\moddev\runClient.cmd"
```

使用 `background=true, notify_on_complete=false`（永不退出的长进程，静默运行正确）。

### 2. 等待 AI Debug Server

```bash
until curl -sf --proxy http://127.0.0.1:10808 http://localhost:25999/ping > /dev/null 2>&1; do sleep 5; done
```

### 3. 验证实例真实性

```bash
echo 'net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
return "screen=" + (mc.screen == null ? "null" : mc.screen.getClass().getSimpleName())
  + " gpu=" + com.mojang.blaze3d.platform.GlUtil.getRenderer();' \
| curl -s --proxy http://127.0.0.1:10808 -X POST http://localhost:25999/eval \
  -H "Content-Type: text/plain" -d @-
```

正常：GPU=NVIDIA RTX 4070，屏幕=TitleScreen
僵尸：GPU=llvmpipe，任何 ≠ TitleScreen 的屏幕

### 4. 进世界

```bash
curl -s --proxy http://127.0.0.1:10808 -X POST http://localhost:25999/enterworld \
  -H "Content-Type: text/plain" -d "Debug World"
```

等待 world load（约 10-30 秒）：

```bash
until echo '... return "screen=" + (mc.screen == null ? "null" : mc.screen.getClass().getSimpleName())
  + " level=" + (mc.level == null ? "null" : "loaded");' | curl ... | grep -q '"level=loaded"'; do sleep 5; done
```

### 5. 🚨 取消暂停（关键！）

`/enterworld` 结束后游戏停在 **PauseScreen** —— 必须手动取消暂停，否则场景不渲染：

```bash
echo 'net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
mc.setScreen(null); return "unpaused";' \
| curl -s --proxy http://127.0.0.1:10808 -X POST http://localhost:25999/eval \
  -H "Content-Type: text/plain" -d @-
```

不执行这一步，`startCapture`/`endCapture` 会成功但截到的是暂停菜单 + 冻结场景。

### 6. 召唤史莱姆

```bash
echo '
net.minecraft.world.entity.Mob slime = (net.minecraft.world.entity.Mob) net.minecraft.world.entity.EntityType.SLIME.create(minecraft.getSingleplayerServer().overworld());
slime.setPos(minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ() + 3);
minecraft.getSingleplayerServer().overworld().addFreshEntity(slime);
return "ok entity=" + slime.getId();
' | curl -s --proxy http://127.0.0.1:10808 -X POST http://localhost:25999/eval \
  -H "Content-Type: text/plain" -d @-
```

返回实体 ID，用这个 ID 验证 `shouldRender()`：

```bash
echo '
net.minecraft.world.entity.Entity target = mc.level.getEntity(ID);
double distSq = mc.player.distanceToSqr(target);
boolean shouldRender = target.shouldRender(mc.player.getX(), mc.player.getY(), mc.player.getZ());
return "distSq=" + distSq + " shouldRender=" + shouldRender + " pos=" + target.getX() + "," + target.getY() + "," + target.getZ();
' | curl ...
```

### 7. 截帧

```bash
# start
echo 'io.github.tt432.eyelib.common.debug.RenderDocCapturer
  .setCaptureFilePathTemplate("E:\\\\_ideaProjects\\\\qylEyelib\\\\eyelib_bgfx_v4");
io.github.tt432.eyelib.common.debug.RenderDocCapturer.startCapture();
return "started";' | curl ...

# 等至少 3 秒让场景渲染
sleep 3

# end
echo 'io.github.tt432.eyelib.common.debug.RenderDocCapturer.endCapture();
return "done";' | curl ...
```

### 8. 确认 .rdc 生成

```powershell
Get-ChildItem -Path E:\_ideaProjects\qylEyelib -Filter "*.rdc" -Recurse -Depth 3 |
    Where-Object { $_.LastWriteTime -gt (Get-Date).AddMinutes(-5) } |
    Select-Object FullName, LastWriteTime
```

## 陷阱

| 陷阱 | 表现 | 解决 |
|------|------|------|
| 暂停未取消 | 截到黑屏/冻结帧 | `mc.setScreen(null)` |
| 史莱姆掉进虚空 | pos.y=负数, shouldRender=true 但不可见 | `setPos` 用玩家 y 值 |
| Zombie 端口 | /ping 秒回, GPU=llvmpipe | `taskkill /F /PID` |
| JAVA_HOME 泄漏 (历史, WSL 时代) | Linux GLFW, GPU 不对 | Windows PowerShell 已无此问题 |

## 典型耗时

| 步骤 | 耗时 |
|------|------|
| renderdoccmd 启动 → /ping 就绪 | ~15s |
| /enterworld → level=loaded | ~10-30s |
| 召唤史莱姆 + 验证 | ~1s |
| start + endCapture 循环 | ~5s |
