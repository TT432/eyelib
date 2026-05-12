# Phase 26 Context

## Goal

收尾 v1.4：修正文档到当前模块拓扑，并通过最终验证关卡（rebuild + nullawayMain + test）。

## Inputs

- `README.md`
- `MODULES.md`
- `docs/index/repo-map.md`
- `docs/index/network.md`
- `docs/architecture/00-control-spec.md`
- `docs/architecture/01-module-boundaries.md`
- `docs/architecture/02-side-boundaries.md`

## Findings

- root README 需要补充当前 v1.4 模块拓扑。
- `AGENTS.md` 与 `docs/architecture/00-control-spec.md` 还残留旧的 `eyelib-processor` 表述。
- attachment payload 提取后，attachment/network/boundary 文档需要明确 `eyelibattachment.capability` 新职责。

## Affected Modules

- Root documentation modules
- `:eyelib-attachment`
- `:eyelib-preprocessing`
- root network/model/animation docs
