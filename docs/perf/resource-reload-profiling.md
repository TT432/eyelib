# MC 资源重载分类计时

MC 1.20.1 内置 `ProfiledReloadInstance`，按 `PreparableReloadListener` 分类型输出 reload 耗时。

## 机制

`ReloadableResourceManager.createReload()` line 45：
```java
return SimpleReloadInstance.create(..., LOGGER.isDebugEnabled());
```

`SimpleReloadInstance.create()` line 87-89：
```java
return profiled ? new ProfiledReloadInstance(...) : of(...);
```

当 `ReloadableResourceManager` logger 为 DEBUG 时不使用普通重载，而是用 `ProfiledReloadInstance` 对每个 listener 单独计时。

## 启用方式

**运行时动态设置**（推荐，无需改文件）：

```java
// /eval 执行
org.apache.logging.log4j.core.config.Configurator.setLevel(
    "net.minecraft.server.packs.resources.ReloadableResourceManager",
    org.apache.logging.log4j.Level.DEBUG
);
```

然后 F3+T 或 `Minecraft.reloadResourcePacks()`。

**log4j2.xml 方式**（⚠️ Forge dev 环境不生效——Forge/ModLauncher 覆盖了配置）：

```xml
<Logger name="net.minecraft.server.packs.resources.ReloadableResourceManager" level="debug"/>
```

## 输出解读

```
Resource reload finished after 34821 ms
BedrockAddonAutoLoader took approximately 34557 ms (31637 ms preparing, 2920 ms applying)
ModelManager took approximately 2748 ms (2727 ms preparing, 21 ms applying)
...
Total blocking time: 3099 ms
```

- **总数**：总 reload 时间
- **per-listener**：各 listener 的 preparation（后台线程）+ applying（主线程）耗时
- **Total blocking time**：主线程被阻塞的总时间（所有 listener 的 applying 之和）

## 典型分析

最慢的 listener 通常是 BedrockAddonAutoLoader（.mcpack 解压/解析）。用 profiling 定位后可以针对性优化。
