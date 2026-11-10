---
name: eyelib-release
description: Eyelib 发版流程——Maven Central（Portal bundle）与 CurseForge（legacy Upload API）三节点发布与校验。Use when publishing a new eyelib version.
license: MIT
compatibility: opencode
metadata:
  author: https://github.com/TT432
  version: "1.0.0"
  tags: eyelib, release, maven-central, curseforge
  related-skills: eyelib-build, windows-omp-gradle-wrapper
---

# eyelib-release

Eyelib 发版 SOP：把当前 `mod_version` 的三个 Stonecutter 节点产物发布到 Maven Central 与 CurseForge，并验证到公开可下载。

## When to use
- 用户要求「发版 / publish 到 Maven Central / CurseForge」
- 排查发版失败（Portal 校验报错、524、坐标冲突、CurseForge 上传 403）
- Do NOT use when: 仅本地构建/测试（属 eyelib-build）

## 版本与坐标
- 版本号唯一来源：`gradle.properties` 的 `mod_version`；节点版本由 `build.gradle` 拼成 `${mod_version}+${minecraft_version}-${forge|neoforge}`。
- 坐标：`io.github.tt432:eyelib:<version>`，artifactId 固定 `eyelib`（三节点共用 artifactId，靠 version 区分）。
- 三节点：`1.20.1`（legacyforge）、`1.21.1`、`26.1.2`（neoforge）。

## 凭据（都在 `%USERPROFILE%/.gradle/gradle.properties`）
- `ossrhUsername` / `ossrhPassword`：Central Portal **user token**（`https://central.sonatype.com/account` 生成）。文件里有多组重复键时，Gradle/Properties 以**最后一组**为准。
- `signing.keyId` / `signing.password` / `signing.secretKeyRingFile`：GPG 签名（`signing` 插件自动使用）。
- `CURSE_TOKEN`：CurseForge **作者 Upload API token**（`https://authors.curseforge.com/#/settings/api-tokens` 生成，36 字符）。它不是 CurseForge for Studios 的 API key。

## 步骤 1：构建并发布到 mavenLocal（自动签名）
主项目编译依赖 `io.github.tt432:clientsmoke:${version}`（版本号跟随主项目 `mod_version`），所以**先 bump 并发布 clientsmoke**，否则主构建报 `Could not find io.github.tt432:clientsmoke:<version>`：
1. 同步改 `clientsmoke/gradle.properties` 的 `mod_version`（clientsmoke 是 git submodule，提交需在其仓库内单独提交）。
2. **把该提交推送到 `TT432/clientsmoke` 远端 `main`**，再在主仓库提交 gitlink：`git -C clientsmoke push origin <sha>:main`。漏这步会让主仓库 pin 一个远端不存在的 commit，所有 CI compile job 在 checkout 阶段 7–10s 秒败（`upload-pack: not our ref`）。2026-10-10（PR #28）与 2026-11-03（PR #37）两次踩中。
3. `cmd.exe /c gradlew.bat -p clientsmoke :1.20.1:publishToMavenLocal :1.21.1:publishToMavenLocal :26.1.2:publishToMavenLocal --no-daemon --console=plain`
4. 再发主项目三节点：
```text
cmd.exe /c gradlew.bat :1.20.1:publishMavenJavaPublicationToMavenLocal :1.21.1:publishMavenJavaPublicationToMavenLocal :26.1.2:publishMavenJavaPublicationToMavenLocal --no-daemon --console=plain
```
校验 `~/.m2/repository/io/github/tt432/eyelib/<version>/` 每个版本有 10 个文件：jar / sources / javadoc / pom / module + 各自 `.asc`。

## 步骤 2：补校验和 + 组 bundle
Portal 要求每个文件带 `.md5` 与 `.sha1`；mavenLocal 发布**不生成**校验和，必须自己补：
- 对每个非校验和文件计算 md5/sha1，写 `<file>.md5`、`<file>.sha1`（裸十六进制，无换行）。
- bundle 是 Maven 仓库布局的 zip：`io/github/tt432/eyelib/<version>/<files>`，每版本一个 bundle（单版本约 22 MB）。

## 步骤 3：上传 Maven Central（Portal Publisher API）
- 上传：`POST https://central.sonatype.com/api/v1/publisher/upload?name=<name>&publishingType=AUTOMATIC`，HTTP Basic（user token），multipart 字段名 `bundle`，返回 deployment id（201）。
  - 响应体是**裸 UUID 文本**（`c584cda0-9458-...`），不是 JSON；`resp.json()` 会抛 `Extra data: line 1 column 2`。
  - 现成脚本：`work/release_21126/central_publish.py`（组 bundle + 补校验和 + 上传 + 轮询；`--dry-run` 只组包，`--check <deploymentId>` 只查状态）。
- 查询状态：`POST /api/v1/publisher/status?id=<id>`，`Authorization: Bearer <base64(user:token)>`。
  - 状态序列：`PENDING → VALIDATING → VALIDATED → PUBLISHING → PUBLISHED`（或 `FAILED`，`errors` 字段给出逐文件原因）。
  - 发布耗时可达 10–30 分钟，需轮询到 `PUBLISHED`。
- 放弃部署：`DELETE /api/v1/publisher/deployment/<id>`，**仅** `VALIDATED`/`FAILED` 可删；`PUBLISHING` 不可撤销。

## 步骤 4：上传 CurseForge（legacy Upload API）
- 项目 ID：`1388469`（slug `eyelib`）。
- 上传：`POST https://www.curseforge.com/api/projects/1388469/upload-file`，头 `X-Api-Token: <CURSE_TOKEN>`，multipart 字段 `metadata`（JSON）与 `file`（jar）。
- metadata 用 `gameVersionNames`（比 id 稳，1.20.1 等版本有多个重复 id）：
  - 1.20.1：`["Client","Server","1.20.1","Forge"]`
  - 1.21.1：`["Client","Server","1.21.1","NeoForge"]`
  - 26.1.2：`["Client","Server","26.1.2","NeoForge"]`
  - `releaseType: "release"`，`changelogType: "markdown"`，`displayName` 用文件名去扩展名。
- 返回 `{"id": <fileId>}` 即上传成功。
- 2026-11-06 实测：Python/requests 直连上传被 Cloudflare 重置（`ConnectionResetError(10054)`）；改用浏览器辅助上传（本地 CORS jar 服务 + 页面 `fetch` + `X-Api-Token`），做法与时间线见 dokimobs `docs/release-curseforge.md` 的「Cloudflare 拦截 → 浏览器辅助上传」。

## 步骤 5：验收（必须）
- Central：`https://repo1.maven.org/maven2/io/github/tt432/eyelib/<version>/` 目录 200，且 `eyelib-<version>.jar` 200（只有 pom 200 说明该版本不完整）。
- CurseForge：`https://r.jina.ai/https://www.curseforge.com/minecraft/mc-mods/eyelib/files/all` 能看到新文件名与 `Latest release`；文件详情页显示正确的 game version / loader / size。

## 发布记录（21.1.26，2026-11-04）
- Central Portal（`publishingType=AUTOMATIC`），三节点各一个 deployment：

  | 节点 | deploymentId | 结果 |
  | --- | --- | --- |
  | `21.1.26+1.20.1-forge` | `2f7015fc-fa2e-4e8c-b2d7-908b85561d67` | PUBLISHED（上传→PUBLISHED 约 12.6 分钟） |
  | `21.1.26+1.21.1-neoforge` | `25d82623-00c9-4a00-9162-01aa9451f576` | PUBLISHED |
  | `21.1.26+26.1.2-neoforge` | `6dd58e7f-d031-4df9-bcd5-ef92714eb498` | PUBLISHED |

  - repo1 同步实测：`21.1.26+1.20.1-forge` / `+1.21.1-neoforge` 目录在 portal 显示 PUBLISHED 后约 2 分钟出现；`+26.1.2-neoforge` 晚了约 15 分钟（PUBLISHED 后短时间内 404 属正常，不要重发）。目录内 `eyelib-...jar` 与 mavenLocal 字节数一致，`.asc`/`.md5`/`.sha1` 齐全。
  - `21.1.25` 当时只发 CurseForge、未进 Central，所以 21.1.26 是 `21.1.24` 之后 Central 上的第一个版本。
- CurseForge（legacy Upload API），displayName 去扩展名：

  | 节点 | fileId |
  | --- | --- |
  | `21.1.26+1.20.1-forge` | 9432072 |
  | `21.1.26+1.21.1-neoforge` | 9432073 |
  | `21.1.26+26.1.2-neoforge` | 9432074 |

  - 17:32 上传 → 17:52 `files/all` 三文件全部可见（约 20 分钟），各自 `Latest release`、`R Release`，Size 10.6 / 9.9 / 9.8 MB 与本地一致，Loader 与 game version 正确。
  - 详情页 9432074：`File name` 把 `+` 显示成 `-`（`eyelib-21.1.26-26.1.2-neoforge.jar`），`What's new` 的 markdown changelog 已渲染。
- 本次脚本：`work/release_21126/{central_publish.py, cf_upload.py, cf_wait_verify.py, cf_changelog.md, cf_ids.json}`。

## 发布记录（21.1.27，2026-11-04）

内容：26.1 实体材质改走 `LIGHTMAP_ONLY`（ADR-0040）——去掉 26.1 vanilla 实体着色器的逐面方向光，
修复 Bedrock 模型面亮度在 0.4↔1.0 间跳变（表现为暗面闪烁）。1.20.1 / 1.21.1 行为零变化。

- 代码：PR #42（`release/21.1.27` → `master`），CI `compile 1.20.1/1.21.1/26.1.2` 全绿；
  `Codacy Static Code Analysis` 为 `action_required`（非必需门禁，`mergeStateStatus=UNSTABLE` 仍可合并）。
- Central（`publishingType=AUTOMATIC`）：

  | 节点 | 结果 |
  | --- | --- |
  | `21.1.27+1.20.1-forge` | PUBLISHED，repo1 jar 200 |
  | `21.1.27+1.21.1-neoforge` | PUBLISHED，repo1 jar 200 |
  | `21.1.27+26.1.2-neoforge` | 首轮上传因代理失败，改直连 session 后补发（见下条 pitfall） |

- CurseForge（displayName 去扩展名）：`9434633` / `9434634` / `9434635`；23:26 上传 → 23:35 `files/all` 三文件全部可见（约 9 分钟，快于往常的 15–20 分钟）。
- 本次脚本：`work/release_21127/{central_publish.py, cf_upload.py, cf_wait_verify.py, cf_changelog.md, cf_ids.json}`。
  `central_publish.py` 已改为 `requests.Session()` + `trust_env = False` 直连。

## 发布记录（21.1.30，2026-11-06）

内容：实体 pass 状态的 alpha test / 深度写 / cull 统一到单一 policy（ADR-0042）。

- 代码：`cb6c…`（主仓库，含 clientsmoke gitlink 21.1.30）；三节点 `compileJava` 通过；
  `BrRenderTypePassPolicyTest` 三节点全绿；`material.*`/`bridge.material.*` 测试仅剩 HEAD 既有基线
  失败；NullAway 三节点错误集与基线一致（`ClientEntitySetup` 95/96/115 + 26.1 的 `IrisPipelineCompat:38`）。
- **本次只发 CurseForge（用户指定）**，Maven Central 未发布；`mavenLocal` 已发布三节点（供 dokimobs 构建）。
- CurseForge（displayName 去扩展名）：

  | 节点 | fileId |
  | --- | --- |
  | `21.1.30+1.20.1-forge` | 9472600 |
  | `21.1.30+1.21.1-neoforge` | 9472601 |
  | `21.1.30+26.1.2-neoforge` | 9472604 |

  - 19:31 上传（三节点串行 18 秒）→ 19:45 `files/all` 三文件全部可见（约 14 分钟），各自
    `Latest release`、`R Release`，Forge/NeoForge 与 game version 正确。
- 本次脚本：`work/release_21130/{cf_upload.py, cf_wait_verify.py, cf_changelog.md, cf_ids.json}`。
- 静态审计脚本（本次新增，可复用）：`scripts/alpha_pass_audit.py --pack <cospack> --material <*.material>`。

## Common Pitfalls
- [when 用本机 Windows 自带 curl 调这些 API] 任何 HTTPS 请求都返回 exit 43（`A libcurl function was given a bad argument`），`-o NUL` / `--proxy` 都不改变结果——这是 curl 自身问题，不是网络；改用 `python -m pip install requests` 后的 requests（本机 3.13 + requests 2.34.2 直连可用，无需代理）。
- [when 上传返回非 JSON] Portal `upload` 的响应体是**裸 deploymentId UUID**（HTTP 201），`resp.json()` 抛 `Extra data: line 1 column 2`；按纯文本取。
- [when 同一 GAV 再次上传] 仍返回 201 但状态 `FAILED`，`errors.common` 提示 `Component ... is currently being published in another deployment (<id>)`——说明首次上传已受理，用那个 `<id>` 查状态，不要重发。
- [when 想用 OSSRH staging API 发布] `https://s01.oss.sonatype.org/...` 对现有账号返回 402（已退役）；一律走 Portal bundle API。
- [when Portal 上传后 FAILED 且 errors 全是 "Missing md5/sha1 checksum"] mavenLocal 产物没有校验和；按步骤 2 补齐后重传（同一 GAV 的失败部署不会占用坐标，可重试）。
- [when 用 GET 或 HTTP Basic 查 `/publisher/status`] 返回 500/401；必须 `POST` + `Bearer base64(user:token)`。
- [when bundle 上传 22 MB 报 HTTP 524] Cloudflare 100s 超时；成功时 22 MB 只需 3–12s（≥1.8 MB/s），524 时实测上传被限速到 ~150 KB/s。**直接重试**，不要在慢速时反复重传（每次 125s）。可用 11 MB 小 bundle 测速，但见下一条。
- [when 想用「小 bundle 探测端点是否可用」] NEVER 用真实 GAV 的不完整 bundle 探测：POM-only 部署会通过校验并 `PUBLISHED`，Maven Central 不可覆盖，该坐标永久只有一个 POM（2026-10-20 事故：`21.1.22+1.20.1-forge` 被占用，只能 bump 到 21.1.23 重发）。要探测就用不存在的 group/version。
- [when 用 `x-api-key` + `api.curseforge.com` 调 CurseForge] 作者 token 只对 legacy Upload API 有效，返回 403；上传/版本查询用 `www.curseforge.com` 或 `minecraft.curseforge.com` + `X-Api-Token`。
- [when 在 `www.curseforge.com` 取 `/api/game/versions`] 返回的是别的游戏版本（29 条）；必须用 `minecraft.curseforge.com/api/game/versions`（7622 条）。
- [when 直接 curl / 无头浏览器访问 CurseForge 页面] Cloudflare 拦成 403 或 "Just a moment"；用 `https://r.jina.ai/<url>` 读取页面校验（URL 加唯一 query 参数绕缓存）。刚上传的文件详情页可能 404 数十秒到数十分钟（实测 10–40 分钟），以 `files/all` 出现且标 `Latest release` 为验收依据；上传接口返回 id 只证明接受，不证明已公开。
- [when 2026-11-06 起 Python/requests 上传 CurseForge 被 `ConnectionResetError(10054)`] 不要重试真实版本；改用浏览器辅助上传（见步骤 4）。上传返回 ID 后公共 API 可能仍查不到，等约 6–15 分钟再验收，重复上传会产生重复文件。
- [when 需要通过 API 改已上传文件的 changelog/metadata] `POST /api/projects/<id>/update-file`，multipart 只带 `metadata`（JSON，含 `fileID`）；偶发 500 直接重试（2026-10-20 实测 3 次内成功）。
- [when 用脚本重试上传 Portal] 成功判定要解析响应里的 HTTP 码字段（`-w "|%{http_code}|"` 的**倒数第二段**），别用 `${out##*|}` 抓末段（那是 `time_total`），否则会把成功当失败、反复重传并产生大量重复部署（重复部署会因「already exists」FAILED，但不会污染已发布内容）。
- [when 发布前未确认版本] 版本一旦 `PUBLISHED` 不可修改；发版前先确认 `mod_version` 未被占用：`curl -o NUL -w "%{http_code}" https://repo1.maven.org/maven2/io/github/tt432/eyelib/<version>/` 应为 404（Git Bash 下 Windows curl 写 `/dev/null` 会 exit 23，用 `NUL`）。
- [when Portal 上传中途抛 `ProxyError ... ConnectionResetError(10054)`] 本机系统代理会重置到 `central.sonatype.com` 的长连接（2026-11-04 实测：前两个节点成功、第三个在 upload 阶段被重置）。修法：脚本用 `s = requests.Session(); s.trust_env = False` 直连，别只清环境变量（Windows 下 requests 还会读注册表代理设置）。已失败的节点可单独补发：`python central_publish.py --nodes-only <完整节点名>`，注意 `--nodes-only` 匹配的是**完整**节点名（`21.1.27+26.1.2-neoforge`），传简称会静默不做事并打印 `{}`。

## 发布记录（21.1.28，2026-11-05）

内容：26.1 光影下不透明材质实体整体不显示（只剩阴影）——Iris 26.1 的 `ShaderKey.findBestMatch`
在管线无 `ALPHA_CUTOUT` define 时按枚举顺序命中 `ENTITIES_ALPHA`（顶点 alpha 测试 = NEVER），
片元全被丢弃。改为经 `IrisPipelines.assignPipeline/assignPipelineShadow` 按 `ShaderKey` 精确登记
（ADR-0041）。1.20.1 / 1.21.1 行为零变化（该桥接类只在 26.1 分支编译）。

- **本次只发 CurseForge**（用户指定「到 CF」），未发 Maven Central。
- 前置：`clientsmoke` 同步 bump 到 21.1.28 并发布 mavenLocal（提交 `fe15f7a`）；主项目三节点
  `publishToMavenLocal` 均已含最终源码（`~/.m2/.../21.1.28+<node>/` 各 10 个文件齐全，1.20.1 的
  jar 为 SRG 名，可直接分发）。
- CurseForge（displayName 去扩展名）：

  | 节点 | fileId |
  | --- | --- |
  | `21.1.28+1.20.1-forge` | 9449975 |
  | `21.1.28+1.21.1-neoforge` | 9450004 |
  | `21.1.28+26.1.2-neoforge` | 9450034 |

  - 14:41 上传（三节点串行，共 189 s，HTTP 200 三次）→ 14:45/14:49 轮询仍 `0/3` → 14:53
    `files/all` 三文件全部可见（约 12 分钟），各自 `Latest release`、`R`，版本与 loader 正确。
  - 上传脚本沿用 21.1.27 的直连修复：`requests.Session()` + `trust_env = False`。
  - 本次脚本与页面快照：`work/release_21128/{cf_upload.py, cf_wait_verify.py, cf_changelog.md, cf_ids.json, cf-files-page.txt}`。
- 联动：dokimobs 0.3.4 依赖本版本，14:54 上传、15:06 三节点可见。

## 发布记录（21.1.29，2026-11-06）

内容：26.1 分支的 `EntityExtraDataRuntimeHooks` 把 `RangedAttackGoal.attackTime` /
`AvoidEntityGoal.toAvoid` 观测写死成常量（false / null），且 modern 分支的
`eyelib.mixins.json` 剔除了对应 accessor —— `query.facing_target_to_range_attack` 与
`query.is_avoiding_mobs` 在 26.1 恒为 0（doki 羊驼生气表情、回避类动画永不播放）。
修复后两个观测走 accessor，modern 分支重新登记 accessor。1.20.1 / 1.21.1 生成代码不变。

- **本次只发 CurseForge**（用户指定「到 CF」），未发 Maven Central。
- 前置：`clientsmoke` 同步 bump 21.1.29 并推送 `TT432/clientsmoke`（提交 `5810030`），
  主项目三节点 `publishToMavenLocal` 完成（`~/.m2/.../21.1.29+<node>/` 齐全）。
- 代码：PR #44（`release/21.1.29` → `master`），CI `compile 1.20.1/1.21.1/26.1.2` 全绿
  （Codacy 仍 fail，按用户决策忽略），merge commit `b7c1a978`。
- CurseForge（displayName 去扩展名）：

  | 节点 | fileId |
  | --- | --- |
  | `21.1.29+1.20.1-forge` | 9468327 |
  | `21.1.29+1.21.1-neoforge` | 9468328 |
  | `21.1.29+26.1.2-neoforge` | 9468329 |

  - 11:2x 上传（三节点串行 15 s）→ `cf_wait_verify.py` 轮询到三文件全部可见，
    各自 `Latest release`、`R`，game version / loader 正确。
  - 本次脚本与页面快照：`work/release_21129/{cf_upload.py, cf_wait_verify.py, cf_changelog.md, cf_ids.json, cf-files-page.txt}`。
- 联动：dokimobs 0.3.6 依赖本版本。

## 发布记录（21.1.31，2026-11-06）

内容：新增 `CarriedBlockRenderer`，实体搬运方块时按宿主的 `carriedBlockState` 绘制方块本体；位置取
`lefthand`/`righthand` 骨骼当前姿态的中点，26.1 在实体 submit 阶段提交。资源包只负责 `carrying`
姿态，方块由渲染补齐；1.20.1 / 1.21.1 / 26.1.2 实机验证。

- **本次只发 CurseForge**（用户指定），未发 Maven Central；`clientsmoke` 与主项目 21.1.31 已发布
  mavenLocal 供 dokimobs 构建。
- Python/requests 直连上传被 Cloudflare 重置；改用浏览器辅助上传（本地临时 CORS 静态服务 +
  受管浏览器 `page.evaluate`），详见 dokimobs `docs/release-curseforge.md`。
- CurseForge（displayName 去扩展名）：

  | 节点 | fileId |
  | --- | --- |
  | `21.1.31+1.20.1-forge` | 9474334 |
  | `21.1.31+1.21.1-neoforge` | 9474338 |
  | `21.1.31+26.1.2-neoforge` | 9474341 |

  - 14:03Z 上传 → 14:09:35Z `files/all` 三文件全部可见（约 6 分钟），各自 `Latest release`、
    `R Release`；文件页 Size 10.6 / 9.9 / 9.8 MB，Forge/NeoForge、game version 正确，
    `What's new` 已渲染完整 changelog（`+` 在 File name 里显示为 `-`）。
  - 本次脚本：`work/release_21131/{cf_upload.py, cf_wait_verify.py, cf_changelog.md, cf_ids.json}`；
    `cf_upload.py` 直连失败日志保留在 `cf_upload.log`。
- 联动：dokimobs 0.3.8 依赖本版本。

## 发布记录（21.1.32，2026-11-08）

内容：栓绳渲染（`LeashRenderer` + `lead` locator 锚点 + 1.20.1 原版绳抑制）、26.1
受伤/白闪 overlay 改用原版 `getOverlayCoords`、Bedrock 引擎变量能力
（`EngineEntityVariable` / `engineVariable` / `EngineEntityVariables`，首个内置
羊驼 `variable.DecorTextureIndex`）、Molang 变量大小写不敏感与 `==`/`!=` 数值按值比较、
内置 `llama` 材质补 `MASKED_MULTITEXTURE`。同时包含 21.1.31 的搬运方块渲染
（21.1.31 当时未提交，本次一并入库）。

- **本次只发 CurseForge**（用户指定），未发 Maven Central；mavenLocal 三节点用于
  dokimobs 0.3.11 构建。
- 前置：`clientsmoke` bump 21.1.32（提交 `4adf177`，已推送 `TT432/clientsmoke` main）
  并 `publishToMavenLocal` 三节点（`~/.m2/.../clientsmoke/21.1.32+<node>/` 各 5 文件）。
- 构建：`clean assemble --no-daemon --console=plain --no-build-cache`，三节点
  `compileJava` 均真实执行（无 `FROM-CACHE`），`BUILD SUCCESSFUL`；
  `publishMavenJavaPublicationToMavenLocal` 三节点，各 10 个文件（jar/sources/javadoc/
  pom/module + `.asc`）。
- CurseForge（displayName 去扩展名）：

  | 节点 | fileId |
  | --- | --- |
  | `21.1.32+1.20.1-forge` | 9495289 |
  | `21.1.32+1.21.1-neoforge` | 9495291 |
  | `21.1.32+26.1.2-neoforge` | 9495304 |

  - 2026-11-08 00:32 上传（Python/requests 直连成功，三节点串行约 9 s）→
    00:34/00:38/00:42 轮询仍 `0/3` → 00:46:55 `files/all` 三文件全部可见（约 14 分钟），
    快照 `work/release_21132/cf-files-page.txt`。
  - 本次脚本：`work/release_21132/{cf_upload.py, cf_wait_verify.py, cf_changelog.md, cf_ids.json}`。
- 联动：dokimobs 0.3.11 依赖本版本（`eyelib_version=21.1.32`）。

## 发布记录（21.1.33，2026-11-08）

内容：装备挂载抽象（ADR-0043）——手持物按 item 骨骼（`rightitem` / `leftitem`）pivot 锚定，删除
`Rx(-90)·Ry(180)` 之外的固定偏移；新增盔甲渲染（vanilla 人形盔甲部件按 body-part 骨骼名绑定，
四槽 + 染色 tint）；挂载物改到模型几何提交之前每实体渲染一次（修复 26.1 完全不显示与多渲染组件
重复绘制）。三个节点的行为变化均经实机验证（原版可持工具僵尸 + 铁剑 + 全套铁甲）。

- **本次只发 CurseForge**（用户指定），未发 Maven Central；mavenLocal 三节点供 dokimobs 0.3.12 构建。
- 前置：`clientsmoke` bump 21.1.33（提交 `e9f7fb8`，**未推送远端**——本次不建 PR 也不发 CI，
  主仓库 gitlink 指向该本地提交，推主仓库前必须先 `git -C clientsmoke push origin e9f7fb8:main`）。
  `~/.m2/.../clientsmoke/21.1.33+<node>/` 各 4 文件（无 javadoc，与 21.1.32 一致）。
- 构建：`clean assemble --no-daemon --console=plain --no-build-cache`，3m34s / 44 tasks executed，
  三节点 `compileJava` 均真实执行（无 `FROM-CACHE`），`BUILD SUCCESSFUL`（日志
  `work/release_21133_build.log`）。
- `publishMavenJavaPublicationToMavenLocal` 三节点各 10 文件；m2 jar 与 `versions/<node>/build/libs`
  的 sha256 逐一相符（`84f557bc…` / `f5cbaf3b…` / `8fac0304…`；
  11 128 986 / 10 424 519 / 10 337 681 B）。
- CurseForge（displayName 去扩展名）：

  | 节点 | fileId |
  | --- | --- |
  | `21.1.33+1.20.1-forge` | 9506218 |
  | `21.1.33+1.21.1-neoforge` | 9506220 |
  | `21.1.33+26.1.2-neoforge` | 9506221 |

  - 15:19:27 上传完成（Python/requests 直连成功，未触发 Cloudflare，三节点串行 23 s）→
    15:19:42 / 15:23:46 / 15:28:03 轮询仍 `0/3` → 15:32:11 `files/all` 三文件全部可见
    （约 12.7 分钟），各自 `Latest release`、`R`，Forge/NeoForge 与 game version 正确
    （快照 `work/release_21133/cf-files-page.txt`）。
  - 本次脚本：`work/release_21133/{cf_upload.py, cf_wait_verify.py, cf_changelog.md, cf_ids.json}`。
- 联动：dokimobs 0.3.12 依赖本版本（`eyelib_version=21.1.33`；CF 9506313 / 9506315 / 9506318）。

## 发布记录（21.1.34，2026-11-09）

内容：head 槽普通物品渲染（掠夺者不祥旗帜）、body 盔甲锚点改 head pivot（skeleton/villager/husk/drowned/zombie_villager）、26.1 `query.is_powered` 恢复并显示 charged creeper 闪电层、26.1 off-level 实体 render state→实体映射（CarryOn 抱起的 doki 实体不再退回 vanilla）。1.20.1 / 1.21.1 的 `is_powered` 旧分支不变。

- **本次只发 CurseForge**（用户指定），未发 Maven Central。
- 代码：PR #48（`release/21.1.34` → `master`），CI `compile 1.20.1/1.21.1/26.1.2` 全绿；Codacy 仍 fail（非必需门禁，按用户决策忽略），merge commit `08e195d0`。
- 前置：`clientsmoke` bump 21.1.34（提交 `f67b099`，已推送 `TT432/clientsmoke` main）；主项目三节点 `publishMavenJavaPublicationToMavenLocal` 完成，`~/.m2/.../21.1.34+<node>/` 各 10 文件齐全。
- CurseForge（displayName 去扩展名）：

  | 节点 | fileId |
  | --- | --- |
  | `21.1.34+1.20.1-forge` | 9526576 |
  | `21.1.34+1.21.1-neoforge` | 9526579 |
  | `21.1.34+26.1.2-neoforge` | 9526580 |

  - 21:33:20 CST 上传完成（三节点串行）→ 21:46:12 CST `files/all` 三文件全部可见（约 13 分钟），各自 `Latest release`、`R`，Forge/NeoForge 与 game version 正确。
- 本次脚本与页面快照：`work/release_21134/{cf_upload.py, cf_wait_verify.py, cf_changelog.md, cf_ids.json, cf-files-page.txt}`。
- 联动：dokimobs 0.3.13 依赖本版本。

## 发布记录（21.1.35，2026-11-10）

内容：盔甲槽 attachable 参与渲染 + 逐骨骼绑定（`binding` Molang 求值 / 同名骨骼 / 持有者根位姿三级锚点，ADR-0043 §5.5）；盔甲四槽命中 attachable 即接管，头部物品与 vanilla 人形盔甲回退路径跳过避免双画。`Model.Bone.binding()` 从「已解析未使用」变为真实消费点，clientsmoke 增加 binding 求值断言。

- **本次只发 CurseForge**（用户指定），未发 Maven Central。
- 代码：PR #51（`feat/armor-attachable-slots` → `master`）。
- 前置：`clientsmoke` bump 21.1.35（提交 `b5d98d2`，已推送 `TT432/clientsmoke` main；首次 CI 因该提交未推送导致 submodule checkout 失败，推送后重跑）；主项目三节点 `publishToMavenLocal` 完成，`~/.m2/.../21.1.35+<node>/` 齐全。
- CurseForge（displayName 去扩展名）：

  | 节点 | fileId |
  | --- | --- |
  | `21.1.35+1.20.1-forge` | 9537831 |
  | `21.1.35+1.21.1-neoforge` | 9537833 |
  | `21.1.35+26.1.2-neoforge` | 9537835 |

  - 三节点串行 34 s 上传完成（Python/requests 直连 legacy Upload API，未触发 Cloudflare）→
    14:39:46 / 14:43:55 / 14:48:02 轮询仍 `0/3` → 14:52:09 `files/all` 三文件全部可见
    （约 12 分钟），各自 `Latest release`、`R`，Forge/NeoForge 与 game version 正确
    （快照 `work/release_21135/cf-files-page.txt`）。
  - 本次脚本：`work/release_21135/{cf_upload.py, cf_wait_verify.py, cf_changelog.md, cf_ids.json}`。
- 联动：dokimobs 0.3.14 依赖本版本（`eyelib_version=21.1.35`）。
