# 模块扁平合并 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 12 个 Gradle 子项目合并到 root 单 Gradle 项目，统一命名空间到 `io.github.tt432.eyelib.<module>`，删除 ArchUnit、简化 publishing。

**Architecture:** 纯机械重构——文件搬运 + 包重命名 + import 替换 + build.gradle/settings.gradle 简化 + mixin/mods.toml 合并 + @Mod bootstrap 处理。无业务逻辑变更。关键约束：合并后只有 `Eyelib.java` 一个 @Mod 入口；所有 @Mod.EventBusSubscriber 自动归到 eyelib；attachment 的 4 处显式 modid 改为 `"eyelib"`；ResourceLocation 命名空间 `"eyelibattachment"` 保留（数据包兼容）。

**Tech Stack:** Gradle (legacyForge/ModDevGradle)、Forge 47.1.3、Java 17、JetBrains MCP（refactor/move）、ArchUnit（将删除）。

**Spec:** `docs/decisions/0014-flat-merge.md`

**工作区纪律：**
- 仓库有大量未提交改动（docs/README.md, scripts/, src/main/ 等），**只允许动合并相关文件**。
- 每个任务结束前用 `git status` 确认没有误碰无关文件。
- 提交时禁止 `git add -A`/`git add .`，必须显式列出要 add 的路径。
- 禁止把提交者标记为 AI。

**Gradle 约束：** 所有 gradle 执行通过 JetBrains MCP（`jetbrain_build_project` / `jetbrain_run_gradle_tasks`）。禁止在 shell 跑 `./gradlew`。

**执行顺序设计原则：** 先把所有子项目源码**物理搬入** root 的 `src/main/java` + `src/test/java`，**一次性**做包重命名 + import 替换（脚本驱动），然后处理 @Mod、mixin、mods.toml、build.gradle、settings.gradle，最后删除旧子项目目录和 ArchUnit。这样避免中间态跨模块引用断裂。

---

## Task 0: 预检快照

**Files:**
- 无文件改动，只记录基线

- [ ] **Step 1: 确认工作区状态**

Run（PowerShell）:
```
git status --short
```
Expected: 列出已存在的未提交改动（docs/README.md、scripts/、src/main/ 等），**不能有 staged 但未 commit 的内容**。

- [ ] **Step 2: 确认当前 HEAD 是 ADR-0014 提交**

Run:
```
git log --oneline -3
```
Expected: 最新一条是 `4c9c2d3e` (ADR-0014) 或其后的提交，前缀包含 docs/decisions/0014。

- [ ] **Step 3: 记录基线编译状态**

通过 `jetbrain_build_project` 构建一次 root，记录退出码。
Expected: 退出码 0（或记录当前错误清单作为基线，区分合并引入的错和预存错）。

- [ ] **Step 4: 记录基线 grep 计数**

Run（PowerShell）:
```
$oldPkgs = 'eyelibutil','eyelibnetwork','eyelibtrack','eyelibmodel','eyelibmolang','eyelibmaterial','eyelibattachment','eyelibanimation','eyelibbehavior','eyelibimporter','eyelibparticle','eyelibbridge'
foreach ($p in $oldPkgs) {
  $count = (rg -c "io\.github\.tt432\.$p" --glob '*.java' --glob '*.toml' --glob '*.json' . | Measure-Object -Line).Lines
  "$p -> $count files"
}
```
Expected: 12 个旧包名各自非零计数，记录到任务日志作为合并完成后的对照基线。

---

## Task 1: 备份标签 + 创建分支

**Files:**
- 无文件改动

- [ ] **Step 1: 在当前 HEAD 打备份标签**

Run:
```
git tag pre-flat-merge
```
Expected: 无输出。标签用于回滚。

- [ ] **Step 2: 创建工作分支**

询问用户分支名（默认 `refactor/flat-merge`）。Run:
```
git checkout -b refactor/flat-merge
```
Expected: `Switched to a new branch 'refactor/flat-merge'`。如果用户希望就在当前分支干，跳过此步。

---

## Task 2: 物理搬运子项目源码到 root src 目录

**Files:**
- Create: `src/main/java/io/github/tt432/eyelib/<module>/...` （从每个 `eyelib-<module>/src/main/java/io/github/tt432/eyelib<module>/...` 搬入）
- Create: `src/test/java/io/github/tt432/eyelib/<module>/...` （从每个 `eyelib-<module>/src/test/java/io/github/tt432/eyelib<module>/...` 搬入）
- Create: 子项目的 resources 适当并入 root（见 Step 4-5）

**说明：** 本任务只做**物理文件移动 + 包目录重命名**，**不改正代码内容**（package 声明和 import 留到 Task 3 用脚本批量改）。这样 git 能识别为 rename，保留 history。

- [ ] **Step 1: 搬运每个子项目的 main java 到 root（包路径转换）**

对 12 个子项目逐一执行（PowerShell 脚本）。映射表：

| 源目录 | 目标目录 |
|---|---|
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil` | `src/main/java/io/github/tt432/eyelib/util` |
| `eyelib-network/src/main/java/io/github/tt432/eyelibnetwork` | `src/main/java/io/github/tt432/eyelib/network` |
| `eyelib-track/src/main/java/io/github/tt432/eyelibtrack` | `src/main/java/io/github/tt432/eyelib/track` |
| `eyelib-model/src/main/java/io/github/tt432/eyelibmodel` | `src/main/java/io/github/tt432/eyelib/model` |
| `eyelib-molang/src/main/java/io/github/tt432/eyelibmolang` | `src/main/java/io/github/tt432/eyelib/molang` |
| `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial` | `src/main/java/io/github/tt432/eyelib/material` |
| `eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment` | `src/main/java/io/github/tt432/eyelib/attachment` |
| `eyelib-animation/src/main/java/io/github/tt432/eyelibanimation` | `src/main/java/io/github/tt432/eyelib/animation` |
| `eyelib-behavior/src/main/java/io/github/tt432/eyelibbehavior` | `src/main/java/io/github/tt432/eyelib/behavior` |
| `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter` | `src/main/java/io/github/tt432/eyelib/importer` |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle` | `src/main/java/io/github/tt432/eyelib/particle` |
| `eyelib-bridge/src/main/java/io/github/tt432/eyelibbridge` | `src/main/java/io/github/tt432/eyelib/bridge` |

PowerShell 命令（合并到 root 前需要先确认目标包目录不存在或为空——对 `util`/`network`/`track`/`model`/`molang`/`material` 这些与 root 已有子包同名的需要特别处理：root 已有 `io.github.tt432.eyelib.network`（含 EyelibNetworkManager 等 3 个文件）和 `io.github.tt432.eyelib.molang.mapping`（含 MolangQuery.java）：

```powershell
$moves = @(
  @{src='eyelib-util/src/main/java/io/github/tt432/eyelibutil'; dst='src/main/java/io/github/tt432/eyelib/util'},
  @{src='eyelib-network/src/main/java/io/github/tt432/eyelibnetwork'; dst='src/main/java/io/github/tt432/eyelib/network'},
  @{src='eyelib-track/src/main/java/io/github/tt432/eyelibtrack'; dst='src/main/java/io/github/tt432/eyelib/track'},
  @{src='eyelib-model/src/main/java/io/github/tt432/eyelibmodel'; dst='src/main/java/io/github/tt432/eyelib/model'},
  @{src='eyelib-molang/src/main/java/io/github/tt432/eyelibmolang'; dst='src/main/java/io/github/tt432/eyelib/molang'},
  @{src='eyelib-material/src/main/java/io/github/tt432/eyelibmaterial'; dst='src/main/java/io/github/tt432/eyelib/material'},
  @{src='eyelib-attachment/src/main/java/io/github/tt432/eyelibattachment'; dst='src/main/java/io/github/tt432/eyelib/attachment'},
  @{src='eyelib-animation/src/main/java/io/github/tt432/eyelibanimation'; dst='src/main/java/io/github/tt432/eyelib/animation'},
  @{src='eyelib-behavior/src/main/java/io/github/tt432/eyelibbehavior'; dst='src/main/java/io/github/tt432/eyelib/behavior'},
  @{src='eyelib-importer/src/main/java/io/github/tt432/eyelibimporter'; dst='src/main/java/io/github/tt432/eyelib/importer'},
  @{src='eyelib-particle/src/main/java/io/github/tt432/eyelibparticle'; dst='src/main/java/io/github/tt432/eyelib/particle'},
  @{src='eyelib-bridge/src/main/java/io/github/tt432/eyelibbridge'; dst='src/main/java/io/github/tt432/eyelib/bridge'}
)
foreach ($m in $moves) {
  if (-not (Test-Path -LiteralPath $m.dst)) { New-Item -ItemType Directory -Path $m.dst -Force | Out-Null }
  # 罗列源下的直接子项，逐个 move（避免整个目录覆盖目标里已有的子目录）
  Get-ChildItem -LiteralPath $m.src | ForEach-Object {
    $target = Join-Path $m.dst $_.Name
    if (Test-Path -LiteralPath $target) {
      Write-Error "命名冲突: $target 已存在（源: $($_.FullName)）。停止。"
      exit 1
    }
    Move-Item -LiteralPath $_.FullName -Destination $m.dst
  }
  Write-Host "OK $($m.src) -> $($m.dst)"
}
```

Expected: 12 行 OK。若有 `Write-Error` 触发，记录冲突清单，停下报告给主管（说明重命名后会出现同包同名类，需要人工裁决）。

**已知风险点**（ADR-0014 §2 已分析）：
- `eyelib.network` 包：root 有 `EyelibNetworkManager.java`、`NetClientHandlers.java`、`package-info.java`；子模块 eyelib-network 搬入后同包新增 `EyelibNetworkTransport.java` 等。`EyelibNetworkMod.java` 会在 Task 4 删除。`package-info.java` 会冲突（两边各有一个）——**Step 1 结束后立即处理**：保留 root 的 `package-info.java`，删除子模块搬来的那份。
- `eyelib.molang.mapping` 包：root 有 `MolangQuery.java`；子模块搬入 `MolangMath.java`、`MolangToplevel.java`。类名不冲突。
- 任何子模块若搬入后与 root 现有类同名，立即停止并报告。

- [ ] **Step 2: 处理 package-info 冲突**

Run（PowerShell）检查 `src/main/java/io/github/tt432/eyelib/network/package-info.java` 是否被覆盖：
```
git status -- src/main/java/io/github/tt432/eyelib/network/package-info.java
```
如果 move 步骤因 package-info 冲突报错，手动保留 root 的版本（子模块的 package-info 内容若不同，合并语义到 root 版本后丢弃）。

对所有与 root 同名的模块（network、molang）都需重复此检查。

- [ ] **Step 3: 搬运每个子项目的 test java 到 root test 目录**

PowerShell（结构与 Step 1 同，src 改为 test 路径）：

```powershell
$tmoves = @(
  @{src='eyelib-util/src/test/java/io/github/tt432/eyelibutil'; dst='src/test/java/io/github/tt432/eyelib/util'},
  @{src='eyelib-network/src/test/java/io/github/tt432/eyelibnetwork'; dst='src/test/java/io/github/tt432/eyelib/network'},
  @{src='eyelib-track/src/test/java/io/github/tt432/eyelibtrack'; dst='src/test/java/io/github/tt432/eyelib/track'},
  @{src='eyelib-model/src/test/java/io/github/tt432/eyelibmodel'; dst='src/test/java/io/github/tt432/eyelib/model'},
  @{src='eyelib-molang/src/test/java/io/github/tt432/eyelibmolang'; dst='src/test/java/io/github/tt432/eyelib/molang'},
  @{src='eyelib-material/src/test/java/io/github/tt432/eyelibmaterial'; dst='src/test/java/io/github/tt432/eyelib/material'},
  @{src='eyelib-attachment/src/test/java/io/github/tt432/eyelibattachment'; dst='src/test/java/io/github/tt432/eyelib/attachment'},
  @{src='eyelib-animation/src/test/java/io/github/tt432/eyelibanimation'; dst='src/test/java/io/github/tt432/eyelib/animation'},
  @{src='eyelib-behavior/src/test/java/io/github/tt432/eyelibbehavior'; dst='src/test/java/io/github/tt432/eyelib/behavior'},
  @{src='eyelib-importer/src/test/java/io/github/tt432/eyelibimporter'; dst='src/test/java/io/github/tt432/eyelib/importer'},
  @{src='eyelib-particle/src/test/java/io/github/tt432/eyelibparticle'; dst='src/test/java/io/github/tt432/eyelib/particle'},
  @{src='eyelib-bridge/src/test/java/io/github/tt432/eyelibbridge'; dst='src/test/java/io/github/tt432/eyelib/bridge'}
)
foreach ($m in $tmoves) {
  if (-not (Test-Path -LiteralPath $m.src)) { continue }  # 某些子项目没 test
  if (-not (Test-Path -LiteralPath (Split-Path $m.dst -Parent))) { New-Item -ItemType Directory -Path (Split-Path $m.dst -Parent) -Force | Out-Null }
  if (-not (Test-Path -LiteralPath $m.dst)) { New-Item -ItemType Directory -Path $m.dst -Force | Out-Null }
  Get-ChildItem -LiteralPath $m.src | ForEach-Object {
    $target = Join-Path $m.dst $_.Name
    if (Test-Path -LiteralPath $target) {
      Write-Error "test 命名冲突: $target"
      exit 1
    }
    Move-Item -LiteralPath $_.FullName -Destination $m.dst
  }
  Write-Host "OK $($m.src) -> $($m.dst)"
}
```

Expected: 最多 12 行 OK（无 test 的子项目跳过）。

- [ ] **Step 4: 处理子项目 resources**

对每个子项目 `eyelib-<module>/src/main/resources/` 下：
- `META-INF/mods.toml` —— **全部丢弃**（root 的 templates/mods.toml 才是源）。直接删源，不搬。
- `META-INF/accesstransformer.cfg`（如有）—— 搬到 root `src/main/resources/META-INF/`，若 root 已有同名则合并内容。
- 其他 resources（assets/、data/、反射字符串 json 等）—— 整体搬到 root `src/main/resources/`（保持相对路径）。

先用 glob 列出每个子项目 resources 实际内容：
```
eyelib-*/src/main/resources/**/*
```
然后对每个子项目：
1. 删除 `META-INF/mods.toml`
2. 剩余文件 move 到 root `src/main/resources/` 对应路径

PowerShell 草稿：
```powershell
Get-ChildItem -Directory -Filter 'eyelib-*' | ForEach-Object {
  $sub = $_.FullName
  $resDir = Join-Path $sub 'src/main/resources'
  if (-not (Test-Path -LiteralPath $resDir)) { return }
  # 删除 mods.toml
  $toml = Join-Path $resDir 'META-INF/mods.toml'
  if (Test-Path -LiteralPath $toml) { Remove-Item -LiteralPath $toml }
  # 搬其余内容到 root src/main/resources
  Get-ChildItem -LiteralPath $resDir -Recurse -File | ForEach-Object {
    $rel = $_.FullName.Substring($resDir.Length + 1)
    $dst = Join-Path 'src/main/resources' $rel
    $dstDir = Split-Path $dst -Parent
    if (-not (Test-Path -LiteralPath $dstDir)) { New-Item -ItemType Directory -Path $dstDir -Force | Out-Null }
    if (Test-Path -LiteralPath $dst) {
      Write-Error "resources 冲突: $dst"
      exit 1
    }
    Move-Item -LiteralPath $_.FullName -Destination $dst
  }
  Write-Host "OK resources from $($_.Name)"
}
```

**预期**：根据 mixin json 调查，`eyelib-track/src/main/resources/eyelibtrack.mixins.json` 和 `eyelib-attachment/src/main/resources/eyelibattachment.mixins.json` 会被搬到 root。这两个文件将在 Task 5 合并到 `eyelib.mixins.json` 后删除。其他子项目若无 resources，跳过。

- [ ] **Step 5: 提交搬运结果**

Run:
```
git add src/main/java src/test/java src/main/resources eyelib-*
git status --short
```
确认暂存的只是搬动相关文件（新增于 src/ 下、删除于 eyelib-*/ 下）。再：
```
git commit -m "refactor: 物理搬运子项目源码到 root src 目录

将 12 个子项目的 main/test java 和 resources 整体移入 root，
包路径从 io.github.tt432.eyelib<module> 映射到 io.github.tt432.eyelib.<module>。
代码内容（package 声明、import）暂未改动，留待下一步批量替换。

ADR-0014"
```

Expected: 提交成功。这一步之后 **Gradle 还无法编译**（因为代码里的 package 声明与文件路径不符），属正常。

---

## Task 3: 批量替换 package 声明与 import

**Files:**
- Modify: `src/main/java/**/*.java`（所有搬运过来的 + 被它们 import 的）
- Modify: `src/test/java/**/*.java`

**说明：** 现在所有源码已在 root src 下。代码里的 `package io.github.tt432.eyelibutil;` 和 `import io.github.tt432.eyelibutil.X;` 都需要替换为 `io.github.tt432.eyelib.util`。这一步纯文本替换，但要**按"最长前缀优先"顺序**执行，避免 `eyelibutil` 替换污染 `eyelibutility`（本项目无此情况，但仍按规则）。

- [ ] **Step 1: 执行批量文本替换（PowerShell）**

对所有 `.java` 文件，依次替换 12 个旧包名（注意：顺序无关紧要，因为 12 个旧包名互不为前缀）：

```powershell
$repl = @(
  @('io.github.tt432.eyelibutil',      'io.github.tt432.eyelib.util'),
  @('io.github.tt432.eyelibnetwork',   'io.github.tt432.eyelib.network'),
  @('io.github.tt432.eyelibtrack',     'io.github.tt432.eyelib.track'),
  @('io.github.tt432.eyelibmodel',     'io.github.tt432.eyelib.model'),
  @('io.github.tt432.eyelibmolang',    'io.github.tt432.eyelib.molang'),
  @('io.github.tt432.eyelibmaterial',  'io.github.tt432.eyelib.material'),
  @('io.github.tt432.eyelibattachment','io.github.tt432.eyelib.attachment'),
  @('io.github.tt432.eyelibanimation', 'io.github.tt432.eyelib.animation'),
  @('io.github.tt432.eyelibbehavior',  'io.github.tt432.eyelib.behavior'),
  @('io.github.tt432.eyelibimporter',  'io.github.tt432.eyelib.importer'),
  @('io.github.tt432.eyelibparticle',  'io.github.tt432.eyelib.particle'),
  @('io.github.tt432.eyelibbridge',    'io.github.tt432.eyelib.bridge')
)
$files = Get-ChildItem -Path 'src/main/java','src/test/java' -Recurse -Filter '*.java'
foreach ($f in $files) {
  $t = [System.IO.File]::ReadAllText($f.FullName)
  $orig = $t
  foreach ($r in $repl) {
    $t = $t.Replace($r[0], $r[1])
  }
  if ($t -ne $orig) {
    [System.IO.File]::WriteAllText($f.FullName, $t)
    Write-Host "patched $($f.FullName)"
  }
}
```

Expected: 所有从子项目搬来的文件、以及引用了它们的 root 文件都被 patch。

**重要边界情况**：
- `io.github.tt432.eyelibutil` 作为字符串前缀出现在 `io.github.tt432.eyelibutility` 时会被误替换——经 grep 确认项目中无 `eyelibutil*`（带后缀）的包名，安全。
- 字符串字面量（如 `"eyelibattachment"` 在 ResourceLocation 构造里）**不会被这次替换影响**，因为替换的是 `io.github.tt432.eyelibutil`，不是 `eyelibattachment`。

- [ ] **Step 2: 验证旧包名清零（不含 mods.toml / mixin json / modid 字符串）**

Run:
```
rg "io\.github\.tt432\.eyelib(util|network|track|model|molang|material|attachment|animation|behavior|importer|particle|bridge)\b" src/main/java src/test/java
```
Expected: 无输出（0 匹配）。若有残留，手动修复。

注意：`\b` 边界保证 `eyelibattachment` 不匹配到 `eyelibattachmentx`；但 `eyelibutil` 也不会匹配到 `eyelibutility`（因为 util 后面是 `ity` 不是单词边界后的内容——实际上 `io.github.tt432.eyelibutility` 的 `y` 也是单词字符，所以 `\b` 不会卡在 util 后）。结论：上面 `\b` 应改为 `(?=\.|$|;)` 更精确。改用：
```
rg "io\.github\.tt432\.eyelib(util|network|track|model|molang|material|attachment|animation|behavior|importer|particle|bridge)(?=\.|$|;)" src/main/java src/test/java
```
Expected: 无输出。

- [ ] **Step 3: 通过 JetBrains MCP 让 IDE 重新索引**

调用 `indexide_ide_sync_files`，paths 为 `['src/main/java', 'src/test/java']`。

- [ ] **Step 4: 编译尝试（预期会暴露非包名问题，比如 @Mod 重复、mods.toml 残留）**

通过 `jetbrain_build_project` 构建 root。
Expected: 大概率失败。**记录全部错误**。典型错误：
- 11 个 @Mod bootstrap 类现在与 root 同 sourceSet，但本身仍编译通过（@Mod 只是注解）——编译错误主要来自 `EyelibTrack.java` 之类引用是否完整、import 残留。
- 真正的编译失败主要会在 Task 4-7 解决。

此时不要慌，继续 Task 4。

- [ ] **Step 5: 提交替换结果**

Run:
```
git add src/main/java src/test/java
git commit -m "refactor: 批量替换 package 声明与 import 到新命名空间

所有 io.github.tt432.eyelib<module> 替换为 io.github.tt432.eyelib.<module>。
@Mod 入口、mods.toml、mixin json 尚未处理，编译仍可能失败。

ADR-0014"
```

---

## Task 4: 删除子项目 @Mod bootstrap 类，迁移注册逻辑

**Files:**
- Delete: 11 个 `EyelibXxxMod.java`（util/network/track/model/molang/material/animation/behavior/importer/particle）+ `EyelibBridge.java`
- Modify: `src/main/java/io/github/tt432/eyelib/Eyelib.java`（吸收 attachment 的 DeferredRegister 注册）
- Delete: `src/main/java/io/github/tt432/eyelib/track/EyelibTrack.java`（保留 MOD_ID 常量与 id() 方法则需迁移，见 Step 3）

**说明：** 合并后只有 `Eyelib.java` 持有 `@Mod("eyelib")`。12 个子项目 bootstrap 类全部删除。唯一有实际逻辑的 `EyelibAttachmentMod`（注册 `DataAttachmentTypeRegistry.DATA_ATTACHMENTS`）需要迁移到 `Eyelib.java` 构造函数。

- [ ] **Step 1: 调查每个 bootstrap 的实际依赖**

用 `indexide_ide_find_references` 检查每个 `EyelibXxxMod.MOD_ID` 常量与类本身被谁引用。

预期（基于 spec 阶段调查）：
- 几乎所有 bootstrap 类**无人引用**（只被 Forge 通过 `@Mod` 字符串反射加载）。
- `EyelibTrack.MOD_ID = "eyelibtrack"` 可能被 track 模块内部代码引用作 ResourceLocation 命名空间。
- `EyelibAttachmentMod.MOD_ID = "eyelibattachment"` 被 `DataAttachmentContainerCapability.java:21` 用作 `new ResourceLocation("eyelibattachment", "data_attachments")` —— 但这里是**字符串字面量**，不引用常量。

逐个查并记录结果。

- [ ] **Step 2: 处理 EyelibTrack 的 MOD_ID 引用**

如果 `EyelibTrack.MOD_ID` 被引用，**不要直接删 EyelibTrack.java**。两个选项：
- (A) 把 `EyelibTrack` 改名为 `TrackIds` 或保留类名但删除 `@Mod` 相关内容（EyelibTrack 本身没 @Mod，是个常量类）。
- (B) 如果它只被 EyelibTrackMod 引用，且 EyelibTrackMod 删除后无人用，则 EyelibTrack 也可删。

根据 Step 1 调查结果决定。`EyelibTrack.TRACK_ID_KEY = "eyelib_track_id"` 如果被引用，保留这个常量类。

**保守做法：保留 `EyelibTrack.java`**（仅常量类，无 @Mod），只删 `EyelibTrackMod.java`。

- [ ] **Step 3: 迁移 EyelibAttachmentMod 的注册逻辑到 Eyelib.java**

读 `src/main/java/io/github/tt432/eyelib/attachment/EyelibAttachmentMod.java`（搬迁后的路径）。当前内容：
```java
package io.github.tt432.eyelib.attachment;

import io.github.tt432.eyelib.attachment.dataattach.mc.DataAttachmentTypeRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(EyelibAttachmentMod.MOD_ID)
public class EyelibAttachmentMod {
    public static final String MOD_ID = "eyelibattachment";

    public EyelibAttachmentMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(bus);
    }
}
```

修改 `src/main/java/io/github/tt432/eyelib/Eyelib.java`，在构造函数中加入 DeferredRegister 注册：

```java
package io.github.tt432.eyelib;

import io.github.tt432.eyelib.attachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelib.network.EyelibNetworkManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@Mod(Eyelib.MOD_ID)
@NullMarked
public class Eyelib {
    public static final String MOD_ID = "eyelib";

    public Eyelib() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        EyelibNetworkManager.register();
        DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(bus);
        if (!FMLLoader.isProduction()) {
            try {
                Class<?> serverClass = Class.forName("io.github.tt432.eyelib.common.debug.AIDebugServer");
                Object server = serverClass.getDeclaredConstructor().newInstance();
                serverClass.getMethod("start").invoke(server);
            } catch (Exception ignored) {
            }
        }
    }
}
```

- [ ] **Step 4: 修改 attachment 模块 4 处显式 modid**

以下 4 处的 `modid = "eyelibattachment"` 改为 `modid = "eyelib"`（注意 Eyelib.MOD_ID 的值是 `"eyelib"`）：

| 文件（搬运后路径） | 行 | 改动 |
|---|---|---|
| `src/main/java/io/github/tt432/eyelib/attachment/runtime/ExtraEntityUpdateDataRuntimeHooks.java` | 原 ~24 | `modid = "eyelibattachment"` → `modid = "eyelib"` |
| `src/main/java/io/github/tt432/eyelib/attachment/capability/DataAttachmentContainerCapability.java` | 原 ~26 | 同上 |
| `src/main/java/io/github/tt432/eyelib/attachment/capability/DataAttachmentContainerCapability.java` | 原 ~34 | 同上 |
| `src/main/java/io/github/tt432/eyelib/attachment/dataattach/DataAttachmentEventHandlers.java` | 原 ~13 | 同上 |

**保留不动**：同文件里的 `new ResourceLocation("eyelibattachment", "data_attachments")` —— 这是数据包命名空间，改名会破坏存档兼容。

用 grep 定位精确路径：
```
rg 'modid = "eyelibattachment"' src/main/java
```

- [ ] **Step 5: 删除 12 个子项目 bootstrap 类**

路径（全部在搬运后的 root src 下）：
```
src/main/java/io/github/tt432/eyelib/util/EyelibUtilMod.java
src/main/java/io/github/tt432/eyelib/network/EyelibNetworkMod.java
src/main/java/io/github/tt432/eyelib/track/EyelibTrackMod.java
src/main/java/io/github/tt432/eyelib/model/EyelibModelMod.java
src/main/java/io/github/tt432/eyelib/molang/EyelibMolangMod.java
src/main/java/io/github/tt432/eyelib/material/EyelibMaterialMod.java
src/main/java/io/github/tt432/eyelib/attachment/EyelibAttachmentMod.java
src/main/java/io/github/tt432/eyelib/animation/EyelibAnimationMod.java
src/main/java/io/github/tt432/eyelib/behavior/EyelibBehaviorMod.java
src/main/java/io/github/tt432/eyelib/importer/EyelibResourcesImporterMod.java
src/main/java/io/github/tt432/eyelib/particle/EyelibParticleMod.java
src/main/java/io/github/tt432/eyelib/bridge/EyelibBridge.java
```

PowerShell 删除：
```powershell
$bootstraps = @(
  'src/main/java/io/github/tt432/eyelib/util/EyelibUtilMod.java',
  'src/main/java/io/github/tt432/eyelib/network/EyelibNetworkMod.java',
  'src/main/java/io/github/tt432/eyelib/track/EyelibTrackMod.java',
  'src/main/java/io/github/tt432/eyelib/model/EyelibModelMod.java',
  'src/main/java/io/github/tt432/eyelib/molang/EyelibMolangMod.java',
  'src/main/java/io/github/tt432/eyelib/material/EyelibMaterialMod.java',
  'src/main/java/io/github/tt432/eyelib/attachment/EyelibAttachmentMod.java',
  'src/main/java/io/github/tt432/eyelib/animation/EyelibAnimationMod.java',
  'src/main/java/io/github/tt432/eyelib/behavior/EyelibBehaviorMod.java',
  'src/main/java/io/github/tt432/eyelib/importer/EyelibResourcesImporterMod.java',
  'src/main/java/io/github/tt432/eyelib/particle/EyelibParticleMod.java',
  'src/main/java/io/github/tt432/eyelib/bridge/EyelibBridge.java'
)
foreach ($f in $bootstraps) {
  if (Test-Path -LiteralPath $f) {
    Remove-Item -LiteralPath $f
    Write-Host "deleted $f"
  } else {
    Write-Host "missing $f"
  }
}
```

Expected: 12 行 deleted。注意保留 `EyelibTrack.java`（常量类，非 bootstrap）。

- [ ] **Step 6: 验证没有外部引用指向已删除的 bootstrap 类**

Run:
```
rg 'Eyelib(UtilMod|NetworkMod|TrackMod|ModelMod|MolangMod|MaterialMod|AttachmentMod|AnimationMod|BehaviorMod|ResourcesImporterMod|ParticleMod|Bridge)\b' src/main/java src/test/java
```
Expected: 仅 ArchUnit 测试可能残留（这些在 Task 8 删除）。除此之外应无匹配。

如果有匹配，用 `indexide_ide_find_references` 确认是否需要清理。

- [ ] **Step 7: 提交**

```
git add src/main/java src/test/java
git commit -m "refactor: 删除 12 个子项目 @Mod bootstrap，注册逻辑归并到 Eyelib

- 11 个 EyelibXxxMod.java + EyelibBridge.java 全部删除
- EyelibAttachmentMod 的 DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register 迁移到 Eyelib 构造函数
- attachment 模块 4 处显式 modid='eyelibattachment' 改为 modid='eyelib'
- ResourceLocation('eyelibattachment', ...) 数据包命名空间保留不变

ADR-0014 §4"
```

---

## Task 5: 合并 Mixin 配置

**Files:**
- Modify: `src/main/resources/eyelib.mixins.json`
- Delete: `src/main/resources/eyelibtrack.mixins.json`（从子项目搬来的）
- Delete: `src/main/resources/eyelibattachment.mixins.json`（从子项目搬来的）
- Move: track 的 4 个 mixin 类到 `io.github.tt432.eyelib.mixin.track.*`
- Move: attachment 的 1 个 mixin 类到 `io.github.tt432.eyelib.mixin.attachment.*`
- Modify: `build.gradle`（jar manifest 的 MixinConfigs 从 3 条减为 1 条）

**说明：** ADR-0014 §3。3 个 mixin json 合并为单一 `eyelib.mixins.json`，package 统一为 `io.github.tt432.eyelib.mixin`。track 和 attachment 的 mixin 类物理移到 `mixin.track.*` 和 `mixin.attachment.*` 子包。

- [ ] **Step 1: 列出 track 和 attachment 的 mixin 类文件**

track mixin 类（位于 `src/main/java/io/github/tt432/eyelib/track/mixin/`，注意它们原本在 `io.github.tt432.eyelibtrack.mixin.common`，Task 3 后变成 `io.github.tt432.eyelib.track.mixin.common`）：
- `common/ItemStackMixin.java`
- `common/AbstractContainerMenuMixin.java`
- `common/InventoryMixin.java`
- `common/LivingEntityMixin.java`

attachment mixin 类（位于 `src/main/java/io/github/tt432/eyelib/attachment/mixin/`）：
- `MultiPlayerGameModeMixin.java`（注意原 json 没说 client/ 前缀，直接在根）

用 glob 确认：
```
src/main/java/io/github/tt432/eyelib/track/mixin/**/*.java
src/main/java/io/github/tt432/eyelib/attachment/mixin/**/*.java
```

- [ ] **Step 2: 把 track mixin 类移到 root 的 mixin.track.common 包**

目标路径：`src/main/java/io/github/tt432/eyelib/mixin/track/common/`

PowerShell：
```powershell
$dst = 'src/main/java/io/github/tt432/eyelib/mixin/track/common'
New-Item -ItemType Directory -Path $dst -Force | Out-Null
Get-ChildItem 'src/main/java/io/github/tt432/eyelib/track/mixin/common' -Filter '*.java' | ForEach-Object {
  Move-Item -LiteralPath $_.FullName -Destination $dst
}
```

然后批量改这些文件的 package 声明（从 `io.github.tt432.eyelib.track.mixin.common` → `io.github.tt432.eyelib.mixin.track.common`）。PowerShell：
```powershell
$files = Get-ChildItem 'src/main/java/io/github/tt432/eyelib/mixin/track/common' -Filter '*.java'
foreach ($f in $files) {
  $t = [System.IO.File]::ReadAllText($f.FullName)
  $t = $t.Replace('io.github.tt432.eyelib.track.mixin.common', 'io.github.tt432.eyelib.mixin.track.common')
  [System.IO.File]::WriteAllText($f.FullName, $t)
}
```

清理空目录 `src/main/java/io/github/tt432/eyelib/track/mixin/`。

- [ ] **Step 3: 把 attachment mixin 类移到 root 的 mixin.attachment 包**

目标路径：`src/main/java/io/github/tt432/eyelib/mixin/attachment/`

```powershell
$dst = 'src/main/java/io/github/tt432/eyelib/mixin/attachment'
New-Item -ItemType Directory -Path $dst -Force | Out-Null
Get-ChildItem 'src/main/java/io/github/tt432/eyelib/attachment/mixin' -Filter '*.java' | ForEach-Object {
  Move-Item -LiteralPath $_.FullName -Destination $dst
}
$files = Get-ChildItem $dst -Filter '*.java'
foreach ($f in $files) {
  $t = [System.IO.File]::ReadAllText($f.FullName)
  $t = $t.Replace('io.github.tt432.eyelib.attachment.mixin', 'io.github.tt432.eyelib.mixin.attachment')
  [System.IO.File]::WriteAllText($f.FullName, $t)
}
```

清理空目录 `src/main/java/io/github/tt432/eyelib/attachment/mixin/`。

- [ ] **Step 4: 更新 eyelib.mixins.json**

读 `src/main/resources/eyelib.mixins.json`，改为：
```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "io.github.tt432.eyelib.mixin",
  "compatibilityLevel": "JAVA_17",
  "refmap": "eyelib.refmap.json",
  "mixins": [
    "track.common.ItemStackMixin",
    "track.common.AbstractContainerMenuMixin",
    "track.common.InventoryMixin",
    "track.common.LivingEntityMixin"
  ],
  "client": [
    "HumanoidModelMixin",
    "LivingEntityRendererAccessor",
    "client.ItemRendererMixin",
    "client.ItemInHandRendererMixin",
    "attachment.MultiPlayerGameModeMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

**注意 compatibilityLevel 从 JAVA_8 提升到 JAVA_17**（track/attachment 原本就是 JAVA_17，root 的 JAVA_8 是历史遗留，统一为 17 与 toolchain 一致）。

- [ ] **Step 5: 删除被合并的 mixin json**

```
Remove-Item -LiteralPath 'src/main/resources/eyelibtrack.mixins.json'
Remove-Item -LiteralPath 'src/main/resources/eyelibattachment.mixins.json'
```

- [ ] **Step 6: 更新 build.gradle 的 jar manifest**

`build.gradle` 第 250-253 行：
```gradle
jar {
    manifest.attributes([
            "MixinConfigs": "${mod_id}.mixins.json,eyelibtrack.mixins.json,eyelibattachment.mixins.json"
    ])
}
```
改为：
```gradle
jar {
    manifest.attributes([
            "MixinConfigs": "${mod_id}.mixins.json"
    ])
}
```

同样，`build.gradle` 第 343 行 `libraryJar` task 里的 `'MixinConfigs': "${mod_id}.mixins.json"` 不用改（它本来就只有一条），但整个 `libraryJar` task 会在 Task 7 删除。

- [ ] **Step 7: 提交**

```
git add src/main/java src/main/resources build.gradle
git commit -m "refactor: 合并 3 个 mixin json 为单一 eyelib.mixins.json

- track/attachment 的 mixin 类物理移到 io.github.tt432.eyelib.mixin.{track,attachment}.*
- compatibilityLevel 统一为 JAVA_17
- jar manifest MixinConfigs 从 3 条减为 1 条
- 删除 eyelibtrack.mixins.json 和 eyelibattachment.mixins.json

ADR-0014 §3"
```

---

## Task 6: 更新 mods.toml 和 settings.gradle

**Files:**
- Modify: `src/main/templates/META-INF/mods.toml`（删除 eyelibimporter 依赖条目）
- Modify: `settings.gradle`（删除 12 个 include）
- Delete: 所有 `eyelib-*/src/main/resources/META-INF/mods.toml`（其实 Task 2 Step 4 已删，确认）

- [ ] **Step 1: 更新 root mods.toml，删除 eyelibimporter 依赖**

`src/main/templates/META-INF/mods.toml` 第 84-89 行：
```toml
[[dependencies.${mod_id}]]
modId="eyelibimporter"
mandatory=true
versionRange="[${mod_version},)"
ordering="AFTER"
side="BOTH"
```
整段删除（包括前面的空行）。

- [ ] **Step 2: 确认所有子项目 mods.toml 已删**

Run:
```
Get-ChildItem -Path 'eyelib-*' -Recurse -Filter 'mods.toml' | Select-Object FullName
```
Expected: 无输出（Task 2 Step 4 已删）。如果有残留，删除。

- [ ] **Step 3: 更新 settings.gradle**

`settings.gradle` 第 16-27 行整段删除（12 个 include）。结果：
```gradle
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven { url = 'https://maven.minecraftforge.net/' }
        maven { url = 'https://maven.parchmentmc.org' }
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '0.7.0'
}

rootProject.name = "eyelib"

includeBuild("clientsmoke")
```

- [ ] **Step 4: 提交**

```
git add src/main/templates settings.gradle eyelib-*
git commit -m "refactor: 更新 mods.toml 和 settings.gradle

- root mods.toml 删除 eyelibimporter 自依赖条目
- settings.gradle 删除 12 个子项目 include
- 确认所有子项目 mods.toml 已清理

ADR-0014 §5 §9"
```

---

## Task 7: 简化 build.gradle（删除 project 依赖、publishing、subprojects 块）

**Files:**
- Modify: `build.gradle`（大段删除）

- [ ] **Step 1: 删除 root dependencies 块里的 project() 三件套**

在 `build.gradle` 的 `dependencies { ... }` 块（root 的主 dependencies 块，以 `api project(':eyelib-network')` 开始）中，删除所有形如以下的三件套（共 12 组 36 行）：

```gradle
api project(':eyelib-<module>')
modImplementation project(':eyelib-<module>')
jarJar project(':eyelib-<module>')
```

（涉及 eyelib-network、eyelib-animation、eyelib-behavior、eyelib-attachment、eyelib-material、eyelib-bridge、eyelib-particle、eyelib-importer、eyelib-molang、eyelib-model、eyelib-track、eyelib-util）

**注意**：此时 build.gradle 已经在 Task 5/6 改过 jar manifest 和 mods.toml 相关行，原行号会漂移。按上述锚点（`api project(':eyelib-`）用 grep 定位：

```
rg -n "project\(':eyelib-" build.gradle
```

全部删除。

保留同 dependencies 块里的外部依赖（acceleratedrendering、clientsmoke、janino、jdk-classfile-backport、h2、jspecify、errorprone、nullawayProcessor、mixinextras、junit 等）。

- [ ] **Step 2: 删除 mavenCentralLibrary 相关代码**

用 grep 定位锚点：
```
rg -n "centralCompileDeps|centralRuntimeDeps|libraryJar|librarySourcesJar|libraryJavadocJar|mavenCentralLibrary|centralDir" build.gradle
```

删除以下段落（按代码内容锚点定位，行号已漂移）：
- `def centralCompileDeps = [] as List<Map>` 及 `def centralRuntimeDeps = [] as List<Map>` 声明
- `gradle.projectsEvaluated { ... }` 整块（收集 subproject 依赖数据）
- `def centralDir = layout.buildDirectory.dir('libs-maven-central')` 及 `tasks.register('libraryJar', ...)`、`tasks.register('librarySourcesJar', ...)`、`tasks.register('libraryJavadocJar', ...)` 三个 task
- `publishing { publications { ... } }` 块里的 `mavenCentralLibrary(MavenPublication) { ... }` 整个子块（保留 `mavenJava`）

- [ ] **Step 3: 删除 subprojects 块**

用 grep 定位：
```
rg -n "^subprojects \{" build.gradle
```

从该行 `subprojects {` 到对应的闭合 `}` 整块删除（含 javadoc/publishing/signing 配置）。该块以 `// Configure publishing for all subprojects` 注释开头，可作为锚点。

- [ ] **Step 4: 验证 publishing 块只剩 mavenJava**

检查 `build.gradle` 的 `publishing { publications { ... } repositories { mavenLocal() } }` 现在只含 `mavenJava` publication。

- [ ] **Step 5: 通过 jetbrain_sync_gradle_projects 让 IDE 同步**

调用 MCP 同步 Gradle，预期项目结构从多 project 变为单 project。

- [ ] **Step 6: 编译尝试**

通过 `jetbrain_build_project` 构建。此时应该能通过编译（业务代码已就绪，build.gradle 已简化）。
Expected: 退出码 0 或剩余错误清单（这些应该是 ArchUnit 测试相关，留 Task 8 处理）。

- [ ] **Step 7: 提交**

```
git add build.gradle
git commit -m "refactor: 简化 build.gradle

- 删除 12 组 project() 三件套依赖
- 删除 mavenCentralLibrary publication 及 libraryJar 等 task
- 删除 subprojects {} 块（子项目 publishing/signing 配置）
- 只保留 mavenJava publication + signing + nexusPublishing

ADR-0014 §7 §8"
```

---

## Task 8: 删除 ArchUnit 测试

**Files:**
- Delete: 6 个 `ArchitectureRules.java`（位于 test 目录）
- Modify: 若 build.gradle 有 archunit 依赖，删除

- [ ] **Step 1: 列出所有 ArchitectureRules.java**

Run:
```
Get-ChildItem -Path 'src/test/java' -Recurse -Filter 'ArchitectureRules.java' | Select-Object FullName
```
Expected（基于 spec 调查，路径已搬迁）:
```
src/test/java/io/github/tt432/eyelib/animation/archunit/ArchitectureRules.java
src/test/java/io/github/tt432/eyelib/behavior/archunit/ArchitectureRules.java
src/test/java/io/github/tt432/eyelib/material/archunit/ArchitectureRules.java
src/test/java/io/github/tt432/eyelib/model/archunit/ArchitectureRules.java
src/test/java/io/github/tt432/eyelib/molang/archunit/ArchitectureRules.java
src/test/java/io/github/tt432/eyelib/particle/archunit/ArchitectureRules.java
```

- [ ] **Step 2: 删除 ArchUnit 测试文件**

```powershell
Get-ChildItem -Path 'src/test/java' -Recurse -Filter 'ArchitectureRules.java' | Remove-Item
# 清理空的 archunit 目录
Get-ChildItem -Path 'src/test/java' -Recurse -Directory -Filter 'archunit' | Where-Object { @(Get-ChildItem $_.FullName).Count -eq 0 } | Remove-Item
```

- [ ] **Step 3: 删除 build.gradle 的 ArchUnit 依赖（如果有）**

Run:
```
rg 'archunit' build.gradle gradle.properties
```
如果有 `com.tngtech.archunit:archunit` 依赖，删除该行。

- [ ] **Step 4: 提交**

```
git add src/test/java build.gradle
git commit -m "refactor: 删除 6 个 domain 模块的 ArchUnit 测试

animation/behavior/material/model/molang/particle 的 ArchitectureRules.java 全部删除。
六边形架构 domain 隔离改为文档约定（ADR-0010）。

ADR-0014 §6"
```

---

## Task 9: 删除空的子项目目录

**Files:**
- Delete: 12 个 `eyelib-<module>/` 目录（此时应已空，只剩 build/ 和空的 src 目录结构）

- [ ] **Step 1: 确认子项目目录已空（除 build 缓存）**

Run:
```
Get-ChildItem -Directory -Filter 'eyelib-*' | ForEach-Object {
  $remain = Get-ChildItem $_.FullName -Recurse -File | Where-Object { $_.FullName -notlike '*\build\*' -and $_.FullName -notlike '*\.gradle\*' }
  [PSCustomObject]@{ Dir = $_.Name; RemainingFiles = $remain.Count }
}
```
Expected: 每个 RemainingFiles 都是 0。如果有残留，手动删除或搬入 root。

- [ ] **Step 2: 删除 12 个子项目目录**

```powershell
Get-ChildItem -Directory -Filter 'eyelib-*' | Remove-Item -Recurse -Force
```

**注意**：这一步可能也会删掉子项目的 `build/` 输出和 `.gradle/` 缓存——这是预期的（合并后子项目不再存在，缓存无用）。

- [ ] **Step 3: 提交**

```
git add -u eyelib-*
git commit -m "refactor: 删除空的子项目目录

12 个 eyelib-<module>/ 目录及其 build/.gradle 缓存全部删除。

ADR-0014"
```

注意用 `-u`（更新已跟踪），不要用 `-A`（会带入未跟踪的 3rdparty/rd_src 等）。

---

## Task 10: 验证编译与测试

**Files:**
- 无文件改动，只验证

- [ ] **Step 1: 完整构建**

通过 `jetbrain_build_project`（全量 rebuild）。
Expected: 退出码 0。

如果失败，记录错误，定位到具体文件，回到对应 Task 修复。

- [ ] **Step 2: 运行单元测试**

通过 `jetbrain_run_gradle_tasks` 跑 `["test"]`。
Expected: BUILD SUCCESSFUL。ArchUnit 已删除，其他测试应全绿。如果有失败，区分是预存失败还是合并引入——对照 Task 0 记录的基线。

- [ ] **Step 3: 运行 NullAway 检查**

通过 `jetbrain_run_gradle_tasks` 跑 `[":nullawayMain"]`。
Expected: 无 ERROR。

- [ ] **Step 4: grep 检查旧包名残留**

Run:
```
rg "io\.github\.tt432\.eyelib(util|network|track|model|molang|material|attachment|animation|behavior|importer|particle|bridge)(?=\.|$|;)" --glob '*.java' --glob '*.json' --glob '*.toml' --glob '*.gradle' --glob '*.properties' .
```
Expected: 无输出。如果有残留，逐个修复。

**例外**：`ResourceLocation("eyelibattachment", ...)` 这种字符串字面量**不应**被上面的正则匹配（因为不是 `io.github.tt432.eyelibattachment.` 形式），可保留。

---

## Task 11: Runtime 烟雾测试

**Files:**
- 无文件改动，只验证

- [ ] **Step 1: 启动客户端**

通过 `eyelib-debug_eyelib_debug_launch`（或 `jetbrain_run_gradle_tasks` 跑 `["runClient"]`）启动 MC 客户端。
Expected: 客户端正常启动，日志中无 `@Mod` 加载失败、mixin 应用失败、ResourceLocation 解析失败等错误。

- [ ] **Step 2: 进入世界**

通过 `eyelib-debug_eyelib_debug_enter_world` 进入 Debug World。
Expected: 世界正常加载，无崩溃。

- [ ] **Step 3: 检查 attachment 注册**

通过 `eyelib-debug_eyelib_debug_execute` 执行：
```java
System.out.println("DATA_ATTACHMENTS contents: " + io.github.tt432.eyelib.attachment.dataattach.mc.DataAttachmentTypeRegistry.DATA_ATTACHMENTS);
```
Expected: 输出非 null，注册表已通过 Eyelib 构造函数正常注册。

- [ ] **Step 4: 运行 clientsmoke 套件**

通过 `eyelib-debug_eyelib_debug_clientsmoke`。
Expected: 全部测试 PASS。

- [ ] **Step 5: 关闭客户端**

通过 `eyelib-debug_eyelib_debug_close`。

---

## Task 12: 同步文档

**Files:**
- Modify: `MODULES.md`
- Modify: `docs/decisions/0002-module-boundaries.md`（标注 amendment）
- Modify: `docs/decisions/0006-key-architecture-decisions.md`（标记 superseded）
- Modify: `docs/decisions/0010-hexagonal-architecture.md`（修订 ArchUnit 强制条款）
- Modify: `AGENTS.md`（Repository Shape 段重写）
- Modify: `docs/README.md`（若有子项目引用）
- Modify: `docs/decisions/0014-flat-merge.md`（Status: Proposed → Accepted）

- [ ] **Step 1: MODULES.md 重写**

读 `MODULES.md`，把"12 子项目"结构改为"单 project + 按包划分的模块清单"。每个模块一节，列出包路径和职责。

- [ ] **Step 2: ADR-0002 标注 amendment**

在 `docs/decisions/0002-module-boundaries.md` 顶部加：
```markdown
> **Amended by [ADR-0014](0014-flat-merge.md):** Gradle project 边界已取消，模块边界改为包边界。
```

- [ ] **Step 3: ADR-0006 标注 superseded**

在 `docs/decisions/0006-key-architecture-decisions.md` 的「Independent Gradle subproject for each seam」决策行后加：
```markdown
> **Superseded by [ADR-0014](0014-flat-merge.md).**
```

- [ ] **Step 4: ADR-0010 修订 ArchUnit 条款**

在 `docs/decisions/0010-hexagonal-architecture.md` 中，把"由 ArchUnit 强制"的相关条款改为"由文档约定 + PR review 把关，ArchUnit 已于 ADR-0014 移除"。

- [ ] **Step 5: AGENTS.md Repository Shape 重写**

`AGENTS.md` 的 `## Repository Shape` 段，把"Multi-project Gradle + 13 Gradle subprojects"改为"单 Gradle project，按包划分模块"。更新子项目列表为包列表。

- [ ] **Step 6: ADR-0014 状态改为 Accepted**

`docs/decisions/0014-flat-merge.md` 第 3 行：
```markdown
**Status:** Accepted
```

- [ ] **Step 7: 检查 docs/README.md 是否引用子项目**

读 `docs/README.md`，如有 `eyelib-xxx/` 路径引用，更新为 `src/main/java/io/github/tt432/eyelib/<module>/`。

- [ ] **Step 8: 提交**

```
git add MODULES.md docs AGENTS.md
git commit -m "docs: 同步扁平合并后的文档

- MODULES.md 改为单 project + 包划分清单
- ADR-0002 标注 amendment
- ADR-0006 标注 superseded
- ADR-0010 修订 ArchUnit 条款为文档约定
- ADR-0014 状态 Proposed -> Accepted
- AGENTS.md Repository Shape 重写

ADR-0014 §11"
```

---

## Task 13: 最终验证

**Files:**
- 无文件改动

- [ ] **Step 1: 完整 build + test + nullaway 一次通过**

通过 `jetbrain_build_project` (rebuild=true) → `jetbrain_run_gradle_tasks` `["test", "nullawayMain"]`。
Expected: 全绿。

- [ ] **Step 2: 旧包名 grep 清零（全仓库）**

Run:
```
rg "io\.github\.tt432\.eyelib(util|network|track|model|molang|material|attachment|animation|behavior|importer|particle|bridge)(?=\.|$|;)" --glob '!3rdparty/**' --glob '!**/build/**' --glob '!.gradle/**' .
```
Expected: 无输出。

- [ ] **Step 3: 旧 mod id grep 清零（除保留的 ResourceLocation 命名空间）**

Run:
```
rg '"eyelib(util|network|track|model|molang|material|animation|behavior|importer|particle|bridge)"' --glob '*.java' --glob '*.toml' --glob '*.json' .
```
Expected: 仅 `ResourceLocation("eyelibattachment", "data_attachments")` 这一类保留，其他清零。如果还有 bootstrap mod id 残留（如 mods.toml、mixin json），修复。

- [ ] **Step 4: 确认子项目目录全部删除**

Run:
```
Test-Path 'eyelib-util'
```
Expected: False。对 12 个子项目都验证。

- [ ] **Step 5: 合并完成报告**

向主管报告：
- 提交数
- 删除文件数
- 新增/搬迁文件数
- 编译/测试/clientsmoke 状态
- 任何偏离 spec 的地方

---

## 风险与回滚

- **回滚点**：Task 1 Step 1 打的 `pre-flat-merge` 标签。任何时候 `git reset --hard pre-flat-merge` 可回到合并前状态（注意这会丢失标签后的所有提交）。
- **高风险 Task**：
  - Task 2（物理搬运）：可能遇到未预见的命名冲突，停下报告。
  - Task 4（@Mod 迁移）：attachment 的 DeferredRegister 迁移若位置/时机不对，运行时会丢注册。Task 11 Step 3 专门验证这点。
  - Task 5（mixin 合并）：mixin 类的 package 声明改了但 json 没同步，会导致 mixin 不加载。Task 11 clientsmoke 应能捕获。
- **编译缓存**：合并后首次 build 会全量重编，较慢，正常现象。

## 已知边界情况备忘

- `io.github.tt432.eyelib.network` 包：root 原有 3 文件 + 子模块搬入若干文件，需确保 `EyelibNetworkManager.register()` 仍被 Eyelib 构造函数调用。
- `io.github.tt432.eyelib.molang.mapping` 包：root 的 `MolangQuery.java`（root-coupled）+ 子模块 `MolangMath.java`、`MolangToplevel.java` 同包共存。
- `EyelibTrack.java`：常量类（非 @Mod），保留。
- `DataAttachmentContainerCapability.java:21` 的 `new ResourceLocation("eyelibattachment", "data_attachments")`：数据包命名空间，保留字符串不变。
- clientsmoke composite build：`includeBuild("clientsmoke")` 保留不动，经 spec 阶段确认无旧包引用。
