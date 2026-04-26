# Corpus Three-Channel Metadata

## Purpose

Organize Molang corpus test cases into three memory channels,
inspired by the PhiLia three-channel graph memory architecture.
Each channel has distinct creation, retention, and evolution rules.

## Three Channels

### 1. Spec Cases (`spec/`)

**Definition**: Cases derived from the syntax baseline and compatibility matrix.
They define what the parser/binder/runtime MUST correctly handle.

- **Creation**: When a new syntax feature or compatibility requirement is documented
- **Retention**: Must stay in sync with design docs; updated when baseline changes
- **Evolution**: Replaced rather than accumulated; old spec cases may be archived
- **Naming**: `spec-<feature>-<variant>.molang`
- **Example**: `spec-ternary-conditional.molang`, `spec-null-coalesce-chained.molang`

### 2. Pattern Cases (`pattern/`)

**Definition**: Validated parse/bind/execute patterns that exercise proven
semantic pathways. These are the "programmatic knowledge" of the refactor.

- **Creation**: When a new binder family or execution path is validated
- **Retention**: Accumulated; part of the refactor's "procedural memory"
- **Evolution**: May be merged or generalized as Phase scope widens
- **Naming**: `pattern-<phase>-<family>-<variant>.molang`
- **Example**: `pattern-phase3-bind-alias-canonicalization.molang`,
  `pattern-phase4-query-variant-default.molang`

### 3. Regression Cases (`regression/`)

**Definition**: Cases that reproduce past bugs, ensuring anti-regression
protection. These are the "episodic memory" of the refactor.

- **Creation**: When a bug is found and fixed (one case per bug)
- **Retention**: Only added, never removed (acts as anti-regression anchor)
- **Evolution**: Annotated with the commit that introduced the fix
- **Naming**: `regression-<YYYYMMDD>-<short-description>.molang`
- **Example**: `regression-20260426-alias-normalization-v-lowercase.molang`

## Channel Metadata Tags

Each corpus case file SHOULD include a metadata comment header:

```
# channel: spec
# feature: ternary-conditional
# phases: parser, binder
# added: 2026-04-26
# related: refactor-plan/02-parser-and-ast.md §36
```

## Directory Structure (target)

```
src/test/resources/io/github/tt432/eyelibmolang/compiler/corpus/
├── phase1/                      (current flat structure — transition)
│   ├── spec/                    (spec cases, migration in progress)
│   ├── pattern/                 (pattern cases, migration in progress)
│   └── regression/              (regression cases)
├── phase2/                      (future)
└── ...
```

## Migration

Current phase1 corpus files live in a flat directory.
New cases should be placed in the appropriate channel subdirectory.
Existing cases will be classified and moved over time.
