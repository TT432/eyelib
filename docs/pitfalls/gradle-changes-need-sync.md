# 修改 build.gradle 后必须 Sync Gradle Project

## 现象
修改 `build.gradle`（添加依赖、改版本等）后，直接 build 可能找不到新依赖，IDE 报红或编译失败。

## 原因
IntelliJ IDEA 的 Gradle 集成不会自动感知 `build.gradle` 的变更。需要通过 Sync 操作让 IDE 重新解析依赖图、刷新类路径。

## 正确做法
每次修改 `build.gradle` 后，通过 JetBrains MCP 执行 `jetbrain_sync_gradle_projects`，或手动在 IDE 中点击 Gradle 工具窗口的刷新按钮。
