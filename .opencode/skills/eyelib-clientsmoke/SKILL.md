---
name: eyelib-clientsmoke
description: Eyelib Clientsmoke 客户端烟雾测试——@ClientSmoke 注解、三层架构、运行、报告。Use when writing or running clientsmoke tests.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
  tags: eyelib, testing, clientsmoke, mc
  related-skills: eyelib, eyelib-build, eyelib-debug
---

# Eyelib Clientsmoke 测试

**clientsmoke** 是 eyelib 的客户端烟雾测试框架——在 MC 客户端加载后、world 就绪时自动执行 `@ClientSmoke` 注解的测试类，输出 JSON/XML 报告。

## 三层测试架构（ADR-0012）

| 层 | 环境 | 测试内容 |
|---|------|---------|
| Layer 1 domain spec | 纯 JUnit (`:test`) | MaterialResolver, BrRenderStateFactory, Molang, CODEC |
| Layer 2a 管道 | 纯 JUnit | `BrMaterialEntry → BrRenderState` 语义映射 |
| Layer 2b Bridge | clientsmoke | `RenderTypeResolver`, `RenderPassAdapter`, `EntityPortAdapter` — 需 MC 类型 |
| Layer 3 接线 | clientsmoke + Fake | `EntityRenderSystem.setupClientEntity` — Component 接线 |

## 启用 clientsmoke

在 `build.gradle` 的 `client` run 配置中添加：

```groovy
systemProperty 'clientsmoke.enabled', 'true'
systemProperty 'clientsmoke.autoExit', 'false'  // 测试结束后保持开启
```

> `clientsmoke.enabled` 默认 `false`，不加此属性状态机进入 IDLE 不执行。

## 写 @ClientSmoke 测试

测试类放在 `src/main/java/io/github/tt432/eyelib/smoke/`。模式：

```java
@ClientSmoke(description = "简短描述", priority = 10)
public class MySmoke {
    public MySmoke() {
        // 全部逻辑在无参构造器中执行
        // throw = fail, 正常返回 = pass
        // 可调用 Minecraft.getInstance()、所有 Manager、所有 Bridge 类
        var materials = MaterialManager.INSTANCE.getAllData();
        require(!materials.isEmpty(), "No materials");
    }

    private static void require(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }
}
```

## 运行

```python
eyelib_debug_launch(timeout=180)     # 启动 + 自动编译
# ⚠️ 不要调用 eyelib_debug_enter_world！clientsmoke 自己创建 ClientSmokeTest 世界
# 等待 ~30s → 状态机 INIT → CONFIG_LOAD → SCAN → WORLD_CREATE → STABILIZE → TEST_EXEC → REPORT
```

输出（gameDir = `versions/<version>/run/clientsmoke`，由 build.gradle clientSmoke run config 决定）：
- `versions/<version>/run/clientsmoke/clientsmoke-reports/report-YYYYMMDD-HHmmss.json`
- `versions/<version>/run/clientsmoke/clientsmoke-reports/junit-YYYYMMDD-HHmmss.xml`
- `versions/<version>/run/clientsmoke/clientsmoke-reports/screenshots/` — 每测试一张截图

## 常见陷阱

- **MaterialManager key 用 `name:base` 格式**：`.mcpack` 中的 key 是 `entity_nocull:entity` 不是 `entity_nocull`
- **不能和 `eyelib_debug_enter_world` 同时使用**：clientsmoke 自己创建世界，手动 enter_world 会冲突
- **`ResourceLocation` 用 `new ResourceLocation(ns, path)`**：MC 1.20.1 没有 `fromNamespaceAndPath`
