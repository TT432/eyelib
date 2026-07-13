# Clientsmoke 测试框架

> 2026-06-09 实战验证：Bridge 适配器测试可通过 clientsmoke 在 MC 进程内运行，解决了 plain JUnit 中 MC 类初始化失败的问题。

## 框架原理

clientsmoke 是 eyelib 的 Git 子模块 (`clientsmoke/`)，在 MC 客户端生命周期内通过状态机自动执行 `@ClientSmoke` 注解的测试。

- **注解扫描**：Mod 构造时通过 Forge `ModFileScanData` (ASM bytecode) 发现 `@ClientSmoke`，不触发类加载
- **状态机**：Tick 驱动，每次 client tick 处理一个状态转换
- **执行时机**：Phase 4 (TEST_EXEC)，在 world 加载 + render 稳定后
- **输出**：JSON 报告 + JUnit XML + 每测试一截图

## 状态机流转

```
INIT → CONFIG_LOAD → SCAN → WORLD_CREATE → WORLD_WAIT → STABILIZE
  → TEST_EXEC → (SCREENSHOT) → REPORT → (EXIT|IDLE)
```

| 状态 | 触发条件 | 动作 |
|------|---------|------|
| INIT | first client tick | 检查 `clientsmoke.enabled` |
| CONFIG_LOAD | enabled=true | 读 `reloadStabilizeTicks` 等配置 |
| SCAN | config loaded | 调用 `ClientSmokeScanner.scan()` 发现测试 |
| WORLD_CREATE | tests found | 创建 `ClientSmokeTest` creative flat world |
| WORLD_WAIT | world initiated | 轮询 `mc.player != null` |
| STABILIZE | player spawned | 等待 `reloadStabilizeTicks` (默认 40) ticks |
| TEST_EXEC | stabilized | `Class.forName()` + `newInstance()` 执行测试 |
| REPORT | all done | 写 JSON + XML 报告 |
| EXIT/IDLE | report written | `autoExit=true` → 退出，否则 IDLE |

## build.gradle 配置

```groovy
runs {
    client {
        client()
        systemProperty 'clientsmoke.enabled', 'true'
        systemProperty 'clientsmoke.autoExit', 'false'  // 测试后保持运行
    }
}
```

系统属性优先级：JVM `-D` arg > `ForgeConfigSpec` (默认 false)。

## 测试编写模板

```java
package io.github.tt432.eyelib.smoke;

import io.github.tt432.clientsmokeannotation.ClientSmoke;

@ClientSmoke(description = "验证 Bridge RenderPassAdapter 全链路", priority = 10)
public class RenderPassAdapterSmoke {

    private static void require(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }

    public RenderPassAdapterSmoke() {
        // 全部验证逻辑在构造器中
        // 可用：Minecraft.getInstance(), MaterialManager.INSTANCE, 所有 Bridge 类
        // 不可用：JUnit 注解 (@Test, @BeforeEach 等)
    }
}
```

## 与三层测试的配合

| 要测什么 | 用什么 | 为什么 |
|---------|--------|-------|
| `BrMaterialResolver.resolve()` 继承链 | 纯 JUnit | 零 MC 依赖 |
| `BrRenderStateFactory.from()` 语义 | 纯 JUnit | 零 MC 依赖 |
| `RenderTypeResolver.resolve()` 材质路由 | clientsmoke | 需 `MaterialManager` (MC 资源) |
| `RenderPassAdapter.toRenderType()` | clientsmoke | 需 `RenderType` MC 静态工厂 |
| `EntityRenderSystem.setupClientEntity()` | clientsmoke + Fake | 需 Entity + 所有 Manager |
| 渲染输出 (GPU) | RenderDoc | 需 GL 状态 |

## 一键运行：eyelib_debug_clientsmoke MCP 工具

```python
eyelib_debug_clientsmoke(timeout=180)
```

封装完整流程：
1. 重建 (`:compileJava createLaunchScripts`)
2. 杀僵尸进程
3. 注入 `-Dclientsmoke.enabled=true -Dclientsmoke.autoExit=true` 到 `clientRunVmArgs.txt`
4. 启动 `runClient.cmd`（无 RenderDoc）
5. 轮询 `/ping` 直到就绪
6. 等 clientsmoke 完成（检测 `clientsmoke-reports/report-*.json` 文件稳定 2s+）
7. 解析报告并输出 passed/failed 详情

输出示例：
```
📋 Clientsmoke Report (20260609-120924)
   Total: 2  |  ✅ Passed: 1  |  ❌ Failed: 1
   ✅ RenderTypeBridgeSmoke (3ms) — Bridge RenderTypeResolver + RenderPassAdapter 全链路验证
   ❌ AttachableSmoke (33ms) — attachable 资源自动加载 + 端到端渲染链路
      ↳ java.lang.AssertionError: Attachable not auto-loaded for minecraft:stick
```

实现位于 `scripts/eyelib_debug_mcp.py`。客户端 `runClient` 的 build.gradle 配置中**不应**永久加 `clientsmoke.enabled`（会干扰普通调试启动），由 MCP 工具在启动前注入。

## 已验证的陷阱

### MaterialManager key 格式

`.mcpack` 真实数据中材质 key 用 `name:base` 格式：
```
materialManager key        正确的代码
entity                      materials.get("entity")
entity_nocull:entity        materials.get("entity_nocull:entity")    ← 不是 "entity_nocull"
entity_alphablend:entity    materials.get("entity_alphablend:entity")
entity_alphatest:entity     materials.get("entity_alphatest:entity")
```

### entity_nocull 的真实语义

Bedrock `.mcpack` 中 `entity_nocull:entity` **只有** `+states[DisableCulling]`，**没有** `ALPHA_TEST` define。所以在 domain 管道测试中要区分"真实 entity_nocull"和"假想材质（ALPHA_TEST + DisableCulling）"。

### launch 后不要 enter_world

clientsmoke 状态机在 `WORLD_CREATE` 阶段自己创建 `ClientSmokeTest` 世界。`eyelib_debug_enter_world` 会创建另一个世界导致冲突。

### MC 1.20.1 ResourceLocation

用 `new ResourceLocation("minecraft", "textures/entity/test")`，不是 `ResourceLocation.fromNamespaceAndPath()` (1.21+ 才有)。

## 报告解读

```json
{
  "totalTests": 2, "passed": 1, "failed": 1,
  "entries": [
    {
      "className": "io.github.tt432.eyelib.smoke.RenderTypeBridgeSmoke",
      "status": "passed", "durationMs": 3
    }
  ]
}
```

`status` 值：`"passed"` / `"failed"`；`error` 含 `message` + `stackTrace` (前 5 行)。
