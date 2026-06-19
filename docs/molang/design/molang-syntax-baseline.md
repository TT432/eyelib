# Molang Syntax Baseline

## Purpose
- This document defines the current baseline for discussing a future `molang` package rewrite.
- It is **not** a formal grammar.
- It separates:
  - **officially documented constructs**,
  - **community-observed behavior/quirks**,
  - **public parser implementation differences**.

## Source Priority
1. Official Microsoft Learn / Mojang sample docs
2. Mojang `bedrock-samples` examples and metadata docs
3. Bedrock Wiki / community references
4. Public parser/compiler implementations

## Important Constraint
- Public Molang documentation is descriptive, not a complete formal grammar.
- Future parser/AST work should treat this file as a **baseline acceptance/reference document**, not as an authoritative EBNF.

---

## 1. Officially Documented Baseline

### Primary Sources
- Microsoft Learn: <https://learn.microsoft.com/en-us/minecraft/creator/documents/molang/syntax-guide?view=minecraft-bedrock-stable>
- Microsoft Learn: <https://learn.microsoft.com/en-us/minecraft/creator/documents/molang/introduction?view=minecraft-bedrock-stable>
- Microsoft Learn: <https://learn.microsoft.com/en-us/minecraft/creator/documents/molang/practical-molang?view=minecraft-bedrock-stable>
- Microsoft Learn Molang reference root: <https://learn.microsoft.com/en-us/minecraft/creator/reference/content/molangreference/?view=minecraft-bedrock-stable>
- Mojang samples: <https://github.com/Mojang/bedrock-samples>
- Mojang samples rendered docs: <https://mojang.github.io/bedrock-samples/Molang.html>

### Officially documented constructs
| Construct | Status | Notes |
|---|---|---|
| Simple expression | Explicit | Expression directly returns a value |
| Complex expression | Explicit | Statement list separated by `;`, final `return` allowed/expected |
| Arithmetic / comparison / logical operators | Explicit | Standard Molang operator set and precedence are documented |
| Ternary `?:` | Explicit | Supported and version-sensitive in some historical behavior |
| Null coalescing `??` | Explicit | Used for missing/invalid variables or stale entity refs |
| Arrow operator `->` | Explicit | Cross-object/entity access |
| Arrays and indexing `[]` | Explicit | Index conversion and out-of-range behavior are documented |
| Variables / aliases | Explicit | `query/q`, `temp/t`, `variable/v`, `context/c` |
| Structs / nested member chains | Partly explicit | Clearly shown in examples, but no formal chain grammar published |
| `loop`, `for_each`, `break`, `continue` | Explicit | Flow control is documented |
| Strings | Explicit | Single-quoted; no normal escape model like many mainstream languages |
| Case-insensitivity | Explicit | Molang is case-insensitive except string contents |

### Curated official legal examples

#### Simple expression
```text
math.sin(query.anim_time * 1.23)
```
Source: Microsoft Learn syntax guide.

#### Assignment + statement sequence + return
```text
variable.is_blinking = 1; variable.return_from_blink = query.life_time; return query.all_animations_finished && (query.life_time > (variable.return_from_blink ?? 0.2));
```
Source: <https://github.com/Mojang/bedrock-samples/blob/f5b651000f52b66334c968f3ccf1aca7950cd6ee/resource_pack/animation_controllers/persona.animation_controllers.json#L7-L20>

#### Ternary
```text
query.get_name == 'Toast' ? Texture.toast : Array.skins[query.variant]
```
Source: <https://github.com/Mojang/bedrock-samples/blob/f5b651000f52b66334c968f3ccf1aca7950cd6ee/resource_pack/render_controllers/rabbit.render_controllers.json#L5-L12>

#### Loop
```text
loop(10, {
    t.x = v.x + v.y;
    v.x = v.y;
    v.y = t.x;
});
```
Source: Microsoft Learn syntax guide.

#### Break / continue
```text
loop(10, {t.x = v.x + v.y; v.x = v.y; v.y = t.x; (v.y > 20) ? break;});
```
Source: Microsoft Learn syntax guide.

#### Struct / member access / arrow
```text
v.location.x = 1;
v.location.y = 2;
v.location.z = 3;
v.another_mobs_location = v.another_mob_set_elsewhere->v.location;
```
Source: <https://github.com/Mojang/bedrock-samples/blob/f5b651000f52b66334c968f3ccf1aca7950cd6ee/metadata/doc_modules/molang.json#L520-L529>

#### Deep chain via arrow
```text
v.cowcow.friend = v.pigpig; v.pigpig->v.test.a.b.c = 1.23; return v.cowcow.friend->v.test.a.b.c;
```
Source: <https://github.com/Mojang/bedrock-samples/blob/f5b651000f52b66334c968f3ccf1aca7950cd6ee/metadata/doc_modules/molang.json#L545-L550>

#### Array indexing
```text
Array.skins[query.variant]
```
Source: <https://github.com/Mojang/bedrock-samples/blob/f5b651000f52b66334c968f3ccf1aca7950cd6ee/resource_pack/render_controllers/fox.render_controllers.json#L5-L20>

#### Null coalescing
```text
variable.rolled_up_time = variable.is_rolled_up ? ((variable.rolled_up_time ?? 0.0) + query.delta_time) : 0.0;
```
Source: <https://github.com/Mojang/bedrock-samples/blob/f5b651000f52b66334c968f3ccf1aca7950cd6ee/resource_pack/entity/armadillo.entity.json#L20-L31>

#### for_each
```text
for_each(t.pig, query.get_nearby_entities(4, 'minecraft:pig'), {
    v.x = v.x + 1;
});
```
Source: <https://github.com/Mojang/bedrock-samples/blob/f5b651000f52b66334c968f3ccf1aca7950cd6ee/metadata/doc_modules/molang.json#L1267-L1276>

### Official design takeaways
- Molang is best treated as **expression-first** with support for complex statement blocks.
- `query/temp/variable/context` should be treated as language roots/aliases, but member/struct chaining is clearly a real language feature.
- `->`, `??`, `[]`, `loop`, `for_each`, `break`, `continue`, and `return` all belong in the baseline acceptance set.

---

## 2. Community-Observed Behavior And Quirks

### Community Sources
- Bedrock Wiki Molang: <https://wiki.bedrock.dev/concepts/molang>
- Bedrock Wiki advanced Molang: <https://wiki.bedrock.dev/documentation/advanced-molang.html>
- Bedrock Wiki queries: <https://wiki.bedrock.dev/documentation/queries.html>
- bedrock.dev Molang docs: <https://bedrock.dev/docs/stable/Molang>

### High-confidence community guidance
- Aliases are commonly used: `q/t/v/c`.
- Zero-arg queries often omit `()`.
- Structs and nested chains are widely treated as legal and expected.
- `??` is the preferred fallback operator for variables/stale refs.
- `loop`, `for_each`, brace scopes, and `return` are part of practical Molang usage.

### Community-observed quirks to track separately from syntax
- `temp.` variables are described as ephemeral and **do not support structs** in community docs.
- `->` short-circuits on invalid left-hand values.
- Community references warn that multiple `->` uses inside a single statement may be restricted/problematic.
- Array subscripts can have awkward behavior in arithmetic-heavy contexts.
- `initialize` and `pre_animation` are described as lazily concatenated in practice.
- Many runtime failures degrade to `0.0`-like values.
- Strings are single-quoted and escaping behavior is limited/odd.
- Historical version-specific behavior changes exist, including ternary associativity fixes.

### Good community corpus examples
```text
v.buff_timer = (v.buff_timer ?? 0) + q.delta_time;
v.location.x = 1; v.location.y = 2; v.location.z = 3;
context.other->query.remaining_durability
loop(10, { ... })
for_each(t.pig, query.get_nearby_entities(4, 'minecraft:pig'), { ... })
v.cowcow.friend = v.pigpig; v.pigpig->v.test.a.b.c = 1.23; return v.cowcow.friend->v.test.a.b.c;
```

### Community design takeaways
- Community docs are useful for **acceptance tests and runtime compatibility notes**, not for defining the formal core grammar.
- Community quirks should likely become a separate compatibility/semantics layer in the rewrite.

---

## 3. Public Parser / Compiler Implementation Survey

### 3.1 bridge-core/molang
- Repo: <https://github.com/bridge-core/molang>
- Broadest public accepted surface among sampled projects.
- Supports numbers, identifiers, booleans, single-quoted strings, comments, dot/array access, calls, assignment, `??`, ternary, brace scopes, `loop`, `for_each`, `break`, `continue`, `return`, `->`.
- Also adds custom `function('name', ...)` syntax and AST transforms.
- Close to official surface, but extension-heavy.

Representative examples:
```text
query.x + query.get(3) == 7
context.other->query.test
function('sq', 'base', { return math.pow(a.base, 2); });
```

### 3.2 JannisX11/MolangJS
- Repo: <https://github.com/JannisX11/MolangJS>
- Supports assignments, `??`, ternary, `loop`, `break`, `continue`, dot access, calls, arithmetic, comparisons.
- Adds custom helpers like `query.approx_eq`, `query.all`, `query.any`, and many easing helpers.
- Notable divergence: lowercases whole input during parsing, so it is not a safe reference for exact string/case behavior.
- `for_each` was not clearly evidenced in parser/runtime support from the sampled code.

Representative examples:
```text
query.has_rider ? Math.sin(query.anim_time) : -44 * 3
loop(3, { v.x = v.x + 1; })
query.approx_eq(q.a, q.b)
```

### 3.3 unnamed/mocha
- Repo: <https://github.com/unnamed/mocha>
- Supports numbers, booleans, single-quoted strings, `=`, `??`, `->`, `[]`, calls, ternary, brace scopes, `loop`, `for_each`, `break`, `continue`, `return`.
- Relatively close to official Molang surface.
- Also has compilation support.
- Sampled code did not show rich comment support or mature string escape handling.

Representative examples:
```text
math.sqrt(3 * 3 + 4 * 4)
a > b
loop(10, { v.x = v.x + 1; })
for_each(v.item, arr, { ... })
```

### 3.4 hollow-cube/mql
- Repo: <https://github.com/hollow-cube/mql>
- Much smaller subset: numeric literals, identifiers, dotted access, calls, unary minus, arithmetic, comparisons, `??`, ternary, parentheses.
- No clear support in sampled parser for strings, booleans, assignments, loops, or statement blocks.
- Useful as a subset reference, not as a full Molang compatibility target.

Representative examples:
```text
1.234
math.pi + 1
my.add(1, 2)
a ?? b
a > b ? 1 : 0
```

### Public implementation survey takeaways
- Closest sampled implementations to official surface: **bridge-core/molang** and **unnamed/mocha**.
- Most extension-heavy: **bridge-core/molang** and **MolangJS**.
- Smallest subset: **hollow-cube/mql**.
- Public implementations are useful as compatibility references, but none should be treated as the specification.

---

## 4. Baseline Acceptance Set For Rewrite Discussion

The following should be treated as the current minimum target syntax set for discussing a new frontend:

- literals: number, boolean, single-quoted string, `this`
- identifiers and namespace aliases: `query/q`, `temp/t`, `variable/v`, `context/c`
- member access via `.`
- arrow access via `->`
- indexing via `[]`
- calls
- unary operators
- arithmetic/comparison/logical operators
- ternary `?:`
- null coalescing `??`
- assignment
- statement lists separated by `;`
- brace blocks `{ ... }`
- `return`
- `loop`
- `for_each`
- `break`, `continue`

## 5. Out Of Scope For This Document
- Formal grammar definition
- Exact AST node design
- Type system design
- Host binding / query injection design
- Versioned semantics matrix

Those should be written as follow-up design docs under this directory.
