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
2. `cmd.exe /c gradlew.bat -p clientsmoke :1.20.1:publishToMavenLocal :1.21.1:publishToMavenLocal :26.1.2:publishToMavenLocal --no-daemon --console=plain`
3. 再发主项目三节点：
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

## 步骤 5：验收（必须）
- Central：`https://repo1.maven.org/maven2/io/github/tt432/eyelib/<version>/` 目录 200，且 `eyelib-<version>.jar` 200（只有 pom 200 说明该版本不完整）。
- CurseForge：`https://r.jina.ai/https://www.curseforge.com/minecraft/mc-mods/eyelib/files/all` 能看到新文件名与 `Latest release`；文件详情页显示正确的 game version / loader / size。

## Common Pitfalls
- [when 想用 OSSRH staging API 发布] `https://s01.oss.sonatype.org/...` 对现有账号返回 402（已退役）；一律走 Portal bundle API。
- [when Portal 上传后 FAILED 且 errors 全是 "Missing md5/sha1 checksum"] mavenLocal 产物没有校验和；按步骤 2 补齐后重传（同一 GAV 的失败部署不会占用坐标，可重试）。
- [when 用 GET 或 HTTP Basic 查 `/publisher/status`] 返回 500/401；必须 `POST` + `Bearer base64(user:token)`。
- [when bundle 上传 22 MB 报 HTTP 524] Cloudflare 100s 超时；成功时 22 MB 只需 3–12s（≥1.8 MB/s），524 时实测上传被限速到 ~150 KB/s。**直接重试**，不要在慢速时反复重传（每次 125s）。可用 11 MB 小 bundle 测速，但见下一条。
- [when 想用「小 bundle 探测端点是否可用」] NEVER 用真实 GAV 的不完整 bundle 探测：POM-only 部署会通过校验并 `PUBLISHED`，Maven Central 不可覆盖，该坐标永久只有一个 POM（2026-10-20 事故：`21.1.22+1.20.1-forge` 被占用，只能 bump 到 21.1.23 重发）。要探测就用不存在的 group/version。
- [when 用 `x-api-key` + `api.curseforge.com` 调 CurseForge] 作者 token 只对 legacy Upload API 有效，返回 403；上传/版本查询用 `www.curseforge.com` 或 `minecraft.curseforge.com` + `X-Api-Token`。
- [when 在 `www.curseforge.com` 取 `/api/game/versions`] 返回的是别的游戏版本（29 条）；必须用 `minecraft.curseforge.com/api/game/versions`（7622 条）。
- [when 直接 curl / 无头浏览器访问 CurseForge 页面] Cloudflare 拦成 403 或 "Just a moment"；用 `https://r.jina.ai/<url>` 读取页面校验（URL 加唯一 query 参数绕缓存）。刚上传的文件详情页可能 404 数十秒到数十分钟（实测 10–40 分钟），以 `files/all` 出现且标 `Latest release` 为验收依据；上传接口返回 id 只证明接受，不证明已公开。
- [when 需要通过 API 改已上传文件的 changelog/metadata] `POST /api/projects/<id>/update-file`，multipart 只带 `metadata`（JSON，含 `fileID`）；偶发 500 直接重试（2026-10-20 实测 3 次内成功）。
- [when 用脚本重试上传 Portal] 成功判定要解析响应里的 HTTP 码字段（`-w "|%{http_code}|"` 的**倒数第二段**），别用 `${out##*|}` 抓末段（那是 `time_total`），否则会把成功当失败、反复重传并产生大量重复部署（重复部署会因「already exists」FAILED，但不会污染已发布内容）。
- [when 发布前未确认版本] 版本一旦 `PUBLISHED` 不可修改；发版前先确认 `mod_version` 未被占用：`curl -o NUL -w "%{http_code}" https://repo1.maven.org/maven2/io/github/tt432/eyelib/<version>/` 应为 404（Git Bash 下 Windows curl 写 `/dev/null` 会 exit 23，用 `NUL`）。
