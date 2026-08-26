# ADR-0030：LDLib2 随产物 jarJar 内嵌——节点图编辑器非开发环境可用

## 状态

**已废止（2026-08-26，用户决策），由 [ADR-0031](0031-ldlib2-optional-dependency.md) 取代**——
LDLib2 保持可选前置，不随产物内嵌。本文保留作为内嵌路径的实证记录
（fork plain jar 不可作生产件、kotlin-stdlib 不可平铺内嵌、嵌套 jarjar 发现等结论仍有效）。

~~已接受（2026-08-25）~~。曾废止 ADR-0021 D7「LDLib 为可选前置，未安装时编辑器入口关闭」
与 ADR-0022「无 LDLib 环境失去查看/调试入口」的部署语义（运行时「图仍可构建渲染」
的门控语义不变）。

## 背景

节点图编辑器（ClientEntity 可视化作者工具）此前以「可选前置」形态依赖 LDLib2：
compileOnly + dev runtime，生产 jar 不携带；1.20.1 的 fork 仅存在于 mavenLocal，
终端用户无法取得，编辑器在非开发环境事实上不可用。用户需求：编辑器在非开发环境可用。

## 决策

三版本统一改为 **jarJar 内嵌发布**，编辑器开箱可用：

1. **1.21.1 / 26.1.2**：直接内嵌官方发布件（`:all` / 普通 jar），其自身 jarjar 已携带
   taffy/yoga/kotlin-stdlib，依赖 FML/NeoForge 的嵌套 jarjar 发现。
2. **1.20.1**：fork 发布管线修正后内嵌 `:all` classifier——
   - fork（third_party/LDLib2）publishing 增补 `reobfJarJar` 产物为 `all` classifier
     （SRG 名 + 自带 jarjar taffy/mixinextras）。plain jar 是 mojmap 开发形态，
     **不可**内嵌：mixin @Overwrite 找不到 SRG 目标，生产启动即崩（实证）。
   - dev 路径（modLdlibCompile/modLocalRuntime）继续消费 plain jar（mojmap，免 remap），不变。
   - KFF 4.12.0 一并内嵌（fork mods.toml 强制前置）。**严禁再平铺内嵌 kotlin-stdlib**：
     KFF 外层 jar 以自动模块 `thedarkcolour.kotlinforforge` 携带完整 stdlib 类，
     平铺会与 `kotlin.stdlib` 模块重复导出 `kotlin.*` 触发 JPMS split-package 启动崩溃（实证）。
   - fork.5 起 taffy 不再类级 vendor，dev 编译/运行时显式补
     `compileOnly` + `additionalRuntimeClasspath`（**不可** localRuntime：
     会把 taffy 放上模块路径，游戏类加载器读不到，实证）。
3. mods.toml / neoforge.mods.toml 删除 ldlib2 可选依赖声明（内嵌后恒在场）；
   `LdlibCompat.isLdlibLoaded()` 门控保留，作为 jarjar 协商被独立安装覆盖等
   异常路径的防御。
4. 共存语义：mods 文件夹存在独立 ldlib2/KFF 时由 jarjar 版本协商接管
   （实证：独立 fork.5-all 优先于内嵌 fork.6-all 被选中，无重复 mod 错误）。

## 后果

- 生产 jar 体积增加约 8MB（1.20.1：ldlib2-all 7.0MB + KFF 7.4MB 等）。
- 编辑器可用性不再依赖用户手动安装前置；1.20.1 的 fork 分发问题彻底消除。
- nnnpc 内嵌 eyelib 时自动携带编辑器全家桶；nnnpc 自身内嵌的 taffy 与内嵌
  ldlib2 的 taffy 经版本协商去重（fork.5 的 JPMS 修复前提保持不变）。
- 验证记录（2026-08-25，生产实例 1.20.1-Forge_47.4.3）：
  独立 ldlib2/KFF 禁用 → `LowDragLib2 is initializing` / `Kotlin For Forge Enabled`
  日志在 → Alt+C 打开工作台截图确认（资产检查器列出 nnnpc 实体）；共存场景
  JarSelector 选中独立 fork.5-all，无重复 mod 错误。dev 客户端 isEditorAvailable=true、
  编辑器 Screen 正常打开。
- 1.21.1 / 26.1.2 生产 jar 内嵌内容经结构核验（jarjar 条目 + metadata），
  未做真机启动验证（无可用生产实例）。
