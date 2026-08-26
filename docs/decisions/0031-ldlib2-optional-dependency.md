# ADR-0031：LDLib2 保持可选前置——撤销 jarJar 内嵌

## 状态

已接受（2026-08-26，**用户决策**）。废止 [ADR-0030](0030-ldlib2-jarjar-embed.md)
（jarJar 内嵌发布，仅存续一天）；恢复 ADR-0021 D7 的部署语义。

## 背景

ADR-0030 为解决「1.20.1 的 LDLib2 fork 仅存在于 mavenLocal、终端用户无法取得」
而将 LDLib2 三版本 jarJar 内嵌进产物 jar。用户裁决：**LDLib2 依旧应该是可选前置**——
不随 eyelib 产物内嵌，编辑器可用性经由用户另行安装 LDLib2 达成。

## 决策

1. **撤销三版本 jarJar 内嵌**（build.gradle 移除全部 `jarJar` ldlib2/KFF 行），
   恢复 `compileOnly` + dev runtime（`modLdlibCompile`/`modLocalRuntime`/`localRuntime`）。
2. **恢复 mods.toml / neoforge.mods.toml 的可选依赖声明**：
   1.20.1 `ldlib2 [2.2.27,)` mandatory=false ordering=AFTER side=CLIENT；
   1.21.1/26.1.2 `ldlib2 [2.2.32,)` type="optional" ordering=AFTER side=CLIENT。
3. **门控语义恢复**：`LdlibCompat.isLdlibLoaded()` 检测 `ldlib2` 是否安装，
   未安装时编辑器入口关闭（Alt+C / 管理屏按钮空转并 warn），图文档仍可构建渲染
   （运行时零 LDLib 依赖，ADR-0021 D1/D7 不变）。
4. **1.20.1 生产安装件形态**（ADR-0030 实证结论保留）：fork 的 `:all` classifier
   （reobfJarJar 产物：SRG 名 + 自带 jarjar 内嵌 taffy/mixinextras）是唯一可用于
   生产的形态；plain jar 是 mojmap 开发形态，生产环境 mixin @Overwrite 找不到
   SRG 目标会启动崩溃。fork 发布管线（fork.6，增补 `all` classifier）因此保留。
5. dev 侧配置不变：fork 坐标 `fork.6`（mavenLocal）+ `compileOnly`/
   `additionalRuntimeClasspath` taffy 1.1.4（fork.5 起 taffy 不再类级 vendor，
   且不可走 localRuntime——会上模块路径导致 NoClassDefFoundError）+
   `modLocalRuntime` KFF 4.12.0（fork mods.toml 强制前置）。

## 后果

- 产物 jar 不再携带 ldlib2/KFF（jarjar 内容回到 h2 / jdk-classfile-backport /
  mixinextras 三项）；未装 LDLib2 的用户失去编辑器入口（门控关闭，其余功能不受影响）。
- 1.20.1 终端用户的 LDLib2 来源仍是 fork 分发问题（上游合入 1.20.1 支持前），
  分发渠道由部署侧解决（如随整合包附带 fork `all` 产物）；本仓只保证
  「装了 LDLib2 的生产环境编辑器可用」。
- 1.21.1 / 26.1.2 用户可安装官方 LDLib2 发布件。
- ADR-0030 的其余实证结论（KFF 外层 jar 携带 kotlin-stdlib、平铺内嵌 stdlib 会
  JPMS split-package、FML 嵌套 jarjar 发现、独立安装与内嵌的版本协商）仍然有效，
  供未来再次评估内嵌时参考。

## 验证

- 2026-08-26，生产实例 1.20.1-Forge_47.4.3（TESTv2）：eyelib 非内嵌新构建 +
  独立安装 `ldlib2-forge-1.20.1-2.2.34+forge.1.20.1-fork.5-all.jar` + KFF 4.12.0
  → 启动日志 `LowDragLib2 is initializing` / `Kotlin For Forge Enabled` →
  世界内 Alt+C 打开节点图工作台（截图：资产检查器列出 nnnpc:npc_skeleton /
  nnnpc:test，属性检查器/变量/调试/预览面板齐全）。
- 产物 jar 结构核验：`META-INF/jarjar/metadata.json` 仅 h2 2.4.240 /
  jdk-classfile-backport 24.0 / mixinextras-forge 0.5.0，无 ldlib2/KFF。
- 三节点 `:1.20.1:reobfJar` / `:1.21.1:compileJava` / `:26.1.2:compileJava` 全绿。
- 1.21.1 / 26.1.2 未做真机验证（无可用生产实例）；其可选前置路径与 1.20.1 同构
  （官方发布件 + mods.toml optional 声明 + LdlibCompat 门控）。
