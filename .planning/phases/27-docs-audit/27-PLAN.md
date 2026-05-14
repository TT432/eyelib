# Phase 27: DOCS — 文档审计与修正 — PLAN

**Date:** 2026-05-12
**Plans:** 1/1

## Plan Summary

Single-action plan covering all four DOCS requirements in one wave.

## Task: DOCS-ALL — Full Documentation Audit & Fix

### DOCS-01: eyelib-processor 引用清理
- ✅ 验证结果: Java 生产代码零引用
- ✅ `docs/architecture/` 主要文件使用 `eyelib-preprocessing`
- ✅ `docs/architecture/migration/` 为历史记录，保留旧名称
- **状态: PASS**

### DOCS-02: README.md 全量审计
- ✅ 全部 50 个跟踪 README.md 已审计
- ✅ 内容准确，路径引用正确
- ✅ 无空目录残留无用 README
- **状态: PASS**

### DOCS-03: 缺失模块文档补充
- ✅ 创建 `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/README.md`
- ✅ 内容包含 scope, responsibilities, dependency direction, editing rules
- **状态: PASS**

### DOCS-04: MODULES.md + docs/ 同步
- ✅ MODULES.md 更新 eyelib-material 条目添加 README 路径引用
- ✅ `docs/index/util.md` 重写，移除已删除的 `TexturePathHelper.java` 引用
- ✅ `docs/index/util.md` 更新为后 v1.3 状态，列出活跃的 `:eyelib-util` 包
- ✅ `docs/architecture/00-control-spec.md` 使用 `eyelib-preprocessing`
- ✅ `docs/architecture/01-module-boundaries.md` 使用 `eyelib-preprocessing`
- ✅ `docs/architecture/02-side-boundaries.md` 使用 `eyelib-preprocessing`
- ✅ `docs/architecture/03-generated-code-policy.md` 准确
- ✅ `docs/architecture/04-mc-debt-ledger.md` 准确
- ✅ `docs/architecture/ARCHITECTURE-BLUEPRINT.md` 使用 `eyelib-preprocessing`
- ✅ `docs/index/repo-map.md` 使用 `eyelib-preprocessing`
- **状态: PASS**

## Files Changed
1. `docs/index/util.md` — 重写过时章节
2. `eyelib-material/src/main/java/io/github/tt432/eyelibmaterial/README.md` — 新建
3. `MODULES.md` — 两处添加 README 路径引用
