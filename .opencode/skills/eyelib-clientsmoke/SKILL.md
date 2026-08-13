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

# eyelib-clientsmoke

Eyelib Clientsmoke 客户端烟雾测试——@ClientSmoke 注解、三层测试架构（ADR-0012）、运行与报告。编写或运行 clientsmoke 测试时使用。

## When to use
- 编写或运行 @ClientSmoke 烟雾测试
- 判断检查属于三层测试架构（domain spec / 管道 / Bridge / 接线）的哪一层
- 在 build.gradle client run 配置中启用 clientsmoke
- 定位或解读 clientsmoke JSON/XML/截图报告
- Do NOT use when: 不需要运行 MC 客户端的纯 JUnit 单元测试（用 unit-test 技能）
- Do NOT use when: 通用 Gradle 构建/测试排障（用 eyelib-build 技能）
- Do NOT use when: 对运行中客户端的交互式状态探查（用 progressive-exploration 技能）

## Rules
- NEVER clientsmoke 运行期间调用 mcmcp_enter_world——clientsmoke 自己创建 ClientSmokeTest 世界，手动 enter_world 会冲突
- When 常见陷阱:
  - MaterialManager 的 key 使用 `name:base` 格式——.mcpack 中的 key 是 `entity_nocull:entity` 而不是 `entity_nocull`
  - ResourceLocation 用 `new ResourceLocation(ns, path)` 构造——MC 1.20.1 没有 fromNamespaceAndPath
- When 启用 clientsmoke:
  - 在 build.gradle 的 client run 配置中设置 systemProperty 'clientsmoke.enabled'='true' 才会执行测试；该属性默认 false，不加则状态机进入 IDLE 不执行任何测试
  - PREFER 同时设置 systemProperty 'clientsmoke.autoExit'='false' 可让测试结束后客户端保持开启
- When 写 @ClientSmoke 测试:
  - 测试类放在 src/main/java/io/github/tt432/eyelib/smoke/，用 @ClientSmoke(description, priority) 注解；全部测试逻辑在无参构造器中执行，throw 异常 = fail，正常返回 = pass；构造器内可调用 Minecraft.getInstance()、所有 Manager、所有 Bridge 类

## Workflow
1. 调用 mcmcp_clientsmoke(timeout=180) 一键完成重建 + 注入 JVM 参数 + 启动客户端 + 解析报告
2. 等待约 30s，状态机按 INIT → CONFIG_LOAD → SCAN → WORLD_CREATE → STABILIZE → TEST_EXEC → REPORT 推进到 REPORT 后，从 clientsmoke-reports/ 读取 report-*.json、junit-*.xml 与 screenshots/ [stop]

## Output
- Type: clientsmoke 报告文件
- report-YYYYMMDD-HHmmss.json (required) — 位于 versions/<version>/run/clientsmoke/clientsmoke-reports/
- junit-YYYYMMDD-HHmmss.xml (required) — 位于 versions/<version>/run/clientsmoke/clientsmoke-reports/
- screenshots/（每测试一张截图） (required) — 位于 versions/<version>/run/clientsmoke/clientsmoke-reports/screenshots/
- Stop when: 状态机推进到 REPORT 阶段后报告已写出

<!-- locked residual (verbatim, do not edit) -->
三层测试架构（ADR-0012）
| 层 | 环境 | 测试内容 |
|---|------|---------|
| Layer 1 domain spec | 纯 JUnit (`:test`) | MaterialResolver, BrRenderStateFactory, Molang, CODEC |
| Layer 2a 管道 | 纯 JUnit | `BrMaterialEntry → BrRenderState` 语义映射 |
| Layer 2b Bridge | clientsmoke | `RenderTypeResolver`, `RenderPassAdapter`, `EntityPortAdapter` — 需 MC 类型 |
| Layer 3 接线 | clientsmoke + Fake | `EntityRenderSystem.setupClientEntity` — Component 接线 |
```groovy
systemProperty 'clientsmoke.enabled', 'true'
systemProperty 'clientsmoke.autoExit', 'false'  // 测试结束后保持开启
```
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
```python
mcmcp_clientsmoke(timeout=180)  # 重建 + 注入 JVM 参数 + 启动 + 解析报告（一键）
# ⚠️ 不要调用 mcmcp_enter_world！clientsmoke 自己创建 ClientSmokeTest 世界
# 等待 ~30s → 状态机 INIT → CONFIG_LOAD → SCAN → WORLD_CREATE → STABILIZE → TEST_EXEC → REPORT
```
`versions/<version>/run/clientsmoke/clientsmoke-reports/report-YYYYMMDD-HHmmss.json`
`versions/<version>/run/clientsmoke/clientsmoke-reports/junit-YYYYMMDD-HHmmss.xml`
`versions/<version>/run/clientsmoke/clientsmoke-reports/screenshots/` — 每测试一张截图
