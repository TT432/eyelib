# Phase 22 Context

## Goal

完成 v1.4 的结构清理起手式：删除 `client/animation` 中确认零引用的无效接口，并把 instrumentation H2 数据库文件从项目根目录迁移到 `.cache/` 下，建立后续 Phase 23-26 的干净基线。

## Inputs

- `.planning/ROADMAP.md` — Phase 22 目标与成功标准
- `.planning/REQUIREMENTS.md` — `CODEQ-01`, `CODEQ-02`
- `docs/architecture/01-module-boundaries.md`
- `docs/architecture/02-side-boundaries.md`
- `src/main/java/io/github/tt432/eyelib/client/animation/README.md`

## Findings

- `KeyFrame` 仅在 `src/main/java/io/github/tt432/eyelib/client/animation/KeyFrame.java` 自身出现，源码范围内无其它 `.java` / `.json` / `.toml` 引用，可视为零引用无效接口。
- instrumentation 当前通过 `InstrumentDatabase` 使用 `jdbc:h2:file:./eyelib_instrument...`，会在项目根目录生成数据库文件。
- instrumentation 相关测试存在根目录路径假设，需要与 `.cache/` 迁移同步更新。

## Affected Modules

- `client.animation.runtime`
- `client.instrument` / `client.instrument.db`

## Non-Goals

- 不删除整个 instrument 子系统
- 不做 module rename、data relocation、capability migration、docs rewrite
