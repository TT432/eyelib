# Phase 10: Schema/Runtime Ownership & Adapter - Pattern Map

**Mapped:** 2026-05-09  
**Files analyzed:** 11 likely new/modified files  
**Analogs found:** 11 / 11

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `eyelib-particle/build.gradle` | config | batch | `eyelib-particle/build.gradle` current dependency/test blocks | exact-existing-file |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinition.java` | model | transform | `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/particle/BrParticle.java` | exact role+codec model |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapter.java` | utility/service | transform | `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/particle/BrParticle.java` | exact transform/error-channel |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/package-info.java` | config/docs | boundary/documentation | `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/package-info.java` | exact package-doc pattern |
| `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapterTest.java` | test | transform/parity | `eyelib-processor/src/test/java/io/github/tt432/eyelibprocessor/particle/flipbook/ParticleFlipbookSummaryOpsTest.java` | role-match |
| `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionBoundaryTest.java` | test | batch/static-source-scan | `src/test/java/io/github/tt432/eyelib/client/particle/ParticleApiDelegationBoundaryTest.java` | exact static-boundary pattern |
| `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionDocumentationTest.java` | test | batch/static-source-scan | `src/test/java/io/github/tt432/eyelib/client/particle/ParticleApiDelegationBoundaryTest.java` | role-match |
| `eyelib-particle/src/test/resources/io/github/tt432/eyelibparticle/runtime/fixtures/witchspell.json` | test fixture | file-I/O | `eyelib-importer/src/test/resources/.../particles/witchspell.json` | exact fixture source |
| `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md` | docs | boundary/documentation | existing same file | exact-existing-file |
| `src/main/java/io/github/tt432/eyelib/client/particle/README.md` | docs | boundary/documentation | existing same file | exact-existing-file |
| `MODULES.md` / `docs/architecture/01-module-boundaries.md` / `docs/architecture/02-side-boundaries.md` | docs | boundary/documentation | existing module/architecture rows | exact-existing-file |

## Pattern Assignments

### `eyelib-particle/build.gradle` (config, batch)

**Analog:** `eyelib-particle/build.gradle`

**Gradle project/test baseline** (lines 45-59):
```groovy
dependencies {
    compileOnly 'org.jspecify:jspecify:1.0.0'

    testImplementation platform('org.junit:junit-bom:5.10.2')
    testImplementation 'org.junit.jupiter:junit-jupiter'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}

tasks.named('test').configure {
    useJUnitPlatform()
}
```

**Apply:** add only the narrow project dependencies needed by the adapter (`project(':eyelib-importer')`, and `project(':eyelib-molang')` if not already available transitively for public types). Preserve the existing JUnit/JSpecify pattern and do not add `project(':')`.

---

### `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinition.java` (model, transform)

**Analog:** `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/particle/BrParticle.java`

**Imports pattern** (lines 5-18):
```java
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibimporter.addon.BedrockResourceValue;
import io.github.tt432.eyelibimporter.util.ImporterCodecUtil;
import io.github.tt432.eyelibmolang.MolangValue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
```

**Record/codec shape to mirror, without naming it `BrParticle`** (lines 20-44):
```java
public record BrParticle(
        String formatVersion,
        ParticleEffect particleEffect
) {
    private static final Codec<JsonElement> JSON_ELEMENT_CODEC = ImporterCodecUtil.JSON_ELEMENT_CODEC;

    public static final Codec<BrParticle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("format_version").forGetter(BrParticle::formatVersion),
            ParticleEffect.CODEC.fieldOf("particle_effect").forGetter(BrParticle::particleEffect)
    ).apply(instance, BrParticle::new));

    public record ParticleEffect(
            Description description,
            Map<String, Curve> curves,
            Events events,
            Map<String, BedrockResourceValue> components
    ) {
```

**Description/render params model** (lines 165-183):
```java
public record Description(
        String identifier,
        BasicRenderParameters basicRenderParameters
) {
    public static final Codec<Description> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("identifier").forGetter(Description::identifier),
            BasicRenderParameters.CODEC.fieldOf("basic_render_parameters").forGetter(Description::basicRenderParameters)
    ).apply(instance, Description::new));
}

public record BasicRenderParameters(
        String material,
        String texture
) {
```

**Apply:** define `ParticleDefinition` as a module-owned runtime definition record with `String` identifier/render texture, importer `BedrockResourceValue` raw components, importer curve/event-compatible data, and optional billboard flipbook summary. Use package docs to state it is canonical runtime definition, not raw Bedrock schema.

---

### `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapter.java` (utility/service, transform)

**Analog:** `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/particle/BrParticle.java`

**Loud failure pattern using `DataResult`** (lines 232-248):
```java
private static DataResult<ChainNode> decode(JsonElement jsonElement) {
    if (!jsonElement.isJsonObject()) {
        return DataResult.error(() -> "Chain node must be a JSON object");
    }
    JsonObject jsonObject = jsonElement.getAsJsonObject();
    try {
        Float value = readOptionalFloat(jsonObject, "value");
        Float slope = readOptionalFloat(jsonObject, "slope");
        return DataResult.success(new ChainNode(
                firstNonNull(readOptionalFloat(jsonObject, "left_value"), value, "left_value/value"),
                firstNonNull(readOptionalFloat(jsonObject, "right_value"), value, "right_value/value"),
                firstNonNull(readOptionalFloat(jsonObject, "left_slope"), slope, "left_slope/slope"),
                firstNonNull(readOptionalFloat(jsonObject, "right_slope"), slope, "right_slope/slope")
        ));
    } catch (RuntimeException exception) {
        return DataResult.error(() -> exception.getMessage());
    }
}
```

**Required-value helper pattern** (lines 265-273):
```java
private static float firstNonNull(Float primary, Float alternative, String label) {
    if (primary != null) {
        return primary;
    }
    if (alternative != null) {
        return alternative;
    }
    throw new IllegalArgumentException("Missing required curve value: " + label);
}
```

**Raw + derived flipbook extraction pattern** (lines 46-82):
```java
public Optional<BillboardFlipbook> billboardFlipbook() {
    BedrockResourceValue appearance = Optional.ofNullable(components.get("minecraft:particle_appearance_billboard"))
            .orElse(components.get("particle_appearance_billboard"));
    if (!(appearance instanceof BedrockResourceValue.ObjectValue appearanceObject)) {
        return Optional.empty();
    }

    BedrockResourceValue uvValue = appearanceObject.values().get("uv");
    if (!(uvValue instanceof BedrockResourceValue.ObjectValue uvObject)) {
        return Optional.empty();
    }

    BedrockResourceValue flipbookValue = uvObject.values().get("flipbook");
    if (!(flipbookValue instanceof BedrockResourceValue.ObjectValue flipbookObject)) {
        return Optional.empty();
    }
```

**Apply:** expose a named static adapter such as `fromSchema(BrParticle schema): DataResult<ParticleDefinition>`. Use `Objects.requireNonNull` for null schema, explicit blank/missing identifier/render-param errors, and copy/preserve all parity-critical maps instead of silently filtering unknown components.

---

### `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/package-info.java` (config/docs, boundary)

**Analog:** `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/package-info.java`

**Package documentation and NullMarked pattern** (lines 1-12):
```java
/**
 * Root-consumed particle API contracts for lookup, store, publication, lifecycle, and spawn seams.
 * <p>
 * The root runtime may consume this package, but this package must not depend back on root runtime
 * packages, root managers, root registries, root packets, root capability helpers, Minecraft,
 * Forge, or root platform implementation classes. Platform-specific validation and
 * lifecycle wiring belong in explicitly documented adapters outside this pure API boundary.
 */
@NullMarked
package io.github.tt432.eyelibparticle.api;

import org.jspecify.annotations.NullMarked;
```

**Apply:** copy this structure for `io.github.tt432.eyelibparticle.runtime`; update prose to name importer `BrParticle` as canonical raw schema input, `ParticleDefinition` as canonical module runtime definition, and root `client/particle/bedrock/BrParticle` as legacy/non-canonical until later phases.

---

### `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapterTest.java` (test, transform/parity)

**Analog:** `eyelib-processor/src/test/java/io/github/tt432/eyelibprocessor/particle/flipbook/ParticleFlipbookSummaryOpsTest.java`

**Imports/assertion pattern** (lines 1-13):
```java
package io.github.tt432.eyelibprocessor.particle.flipbook;

import io.github.tt432.eyelibimporter.addon.BedrockResourceValue;
import io.github.tt432.eyelibimporter.particle.BrParticle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
```

**Inline particle fixture builder** (lines 117-163):
```java
private static BrParticle particleWithFlipbook(
        String baseX,
        String baseY,
        String sizeX,
        String sizeY,
        String stepX,
        String stepY,
        String framesPerSecond,
        String maxFrame,
        boolean stretchToLifetime,
        boolean loop
) {
    LinkedHashMap<String, BedrockResourceValue> components = new LinkedHashMap<>();
    components.put("minecraft:particle_appearance_billboard", object(
            entry("size", array(number("1"), number("1"))),
            entry("facing_camera_mode", string("lookat_xyz")),
            entry("uv", object(
```

**Real fixture content to copy/reference** (`eyelib-importer/.../witchspell.json`, lines 1-10 and 53-71):
```json
{
  "format_version": "1.10.0",
  "particle_effect": {
    "description": {
      "identifier": "sample:witchspell_emitter",
      "basic_render_parameters": {
        "material": "particles_alpha",
        "texture": "textures/particle/particles"
      }
    },
...
      "minecraft:particle_appearance_billboard": {
        "size": [0.125, 0.125],
        "facing_camera_mode": "lookat_xyz",
        "uv": {
          "texture_width": 128,
          "texture_height": 128,
          "flipbook": {
            "base_UV": [64, 72],
            "size_UV": [8, 8],
            "step_UV": [-8, 0],
```

**Apply:** include one real fixture parity test that decodes importer `BrParticle.CODEC`, adapts to `ParticleDefinition`, and asserts identifier, format version, render material/texture, curve/event/raw-component keys, raw component values, and `billboardFlipbook()` summary. Add small inline invalid cases for blank/missing required data if codecs allow constructing records directly.

---

### `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionBoundaryTest.java` (test, batch/static-source-scan)

**Analog:** `src/test/java/io/github/tt432/eyelib/client/particle/ParticleApiDelegationBoundaryTest.java`

**Forbidden import scan** (lines 53-71):
```java
@Test
void particleModuleMainSourcesRemainFreeOfRootMinecraftAndForgeImports() throws IOException {
    Path sourceRoot = Path.of("eyelib-particle/src/main/java");
    List<String> forbiddenFragments = List.of(
            "import io.github.tt432.eyelib.client.",
            "import io.github.tt432.eyelib.network.",
            "import io.github.tt432.eyelib.capability.",
            "import io.github.tt432.eyelib.mc.impl.",
            "import net.minecraft.",
            "import net.minecraftforge."
    );

    try (var paths = Files.walk(sourceRoot)) {
        List<Path> violatingFiles = paths
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> containsAnyForbiddenImport(path, forbiddenFragments))
                .toList();

        assertTrue(violatingFiles.isEmpty(), () -> "Forbidden particle module imports: " + violatingFiles);
    }
}
```

**SourceCheck helper** (lines 83-95):
```java
private static SourceCheck source(String path) throws IOException {
    return new SourceCheck(path, Files.readString(Path.of(path)));
}

private record SourceCheck(String path, String content) {
    void assertContains(String expected) {
        assertTrue(content.contains(expected), () -> path + " should contain: " + expected);
    }

    void assertNotContains(String unexpected) {
        assertFalse(content.contains(unexpected), () -> path + " should not contain: " + unexpected);
    }
}
```

**Apply:** copy the static scan, add assertions that no file under `eyelib-particle/src/main/java` declares `class BrParticle`, `record BrParticle`, or imports root runtime/Minecraft/Forge. Permit imports from `io.github.tt432.eyelibimporter.` and `io.github.tt432.eyelibmolang.` only for the documented adapter/runtime seam.

---

### `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionDocumentationTest.java` (test, batch/static-source-scan)

**Analog:** `src/test/java/io/github/tt432/eyelib/client/particle/ParticleApiDelegationBoundaryTest.java`

**Documentation invariant pattern** (lines 15-27, 44-49):
```java
void retainedRootFacadesDelegateToParticleModuleApiAndDocumentTransition() throws IOException {
    SourceCheck lookup = source("src/main/java/io/github/tt432/eyelib/client/particle/ParticleLookup.java");
    SourceCheck spawnService = source("src/main/java/io/github/tt432/eyelib/client/particle/ParticleSpawnService.java");
    SourceCheck registry = source("src/main/java/io/github/tt432/eyelib/client/registry/ParticleAssetRegistry.java");
    SourceCheck particleReadme = source("src/main/java/io/github/tt432/eyelib/client/particle/README.md");
    SourceCheck registryReadme = source("src/main/java/io/github/tt432/eyelib/client/registry/README.md");
...
    particleReadme.assertContains("transitional root runtime adapter");
    particleReadme.assertContains("removal condition");
    particleReadme.assertContains("do not add a duplicate root request type");
    assertTrue(Files.notExists(obsoleteRootRequest), () -> obsoleteRootRequest + " should not be reintroduced");
    registryReadme.assertContains("transitional root facade");
```

**Apply:** assert owner docs contain exact strings for canonical importer schema, canonical particle runtime definition, and root legacy/non-canonical `BrParticle` status. Candidate docs: `MODULES.md`, `docs/architecture/01-module-boundaries.md`, `docs/architecture/02-side-boundaries.md`, `eyelib-particle/.../README.md`, `eyelib-particle/.../runtime/package-info.java`, and `src/main/.../client/particle/README.md`.

---

### Documentation files (docs, boundary/documentation)

**Analogs:** existing `MODULES.md`, architecture docs, particle READMEs.

**Particle module row pattern** (`MODULES.md`, lines 38-42):
```markdown
| Particle subproject | First-class particle module boundary for particle API/store/spawn contracts, future particle core/runtime definitions, and explicit integration seams | `eyelib-particle/build.gradle`, `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/`, `eyelib-particle/src/main/resources/META-INF/mods.toml`, `eyelib-particle/src/test/` | consumed directly by root through `project(':eyelib-particle')` one-way dependencies; must not depend back on root runtime packages, root managers, root registries, root packets, root capability helpers, or root `mc/impl` classes; current executable particle runtime remains under `src/main/java/io/github/tt432/eyelib/client/particle/` until later phases move it through explicit seams |
```

**Architecture ownership map pattern** (`docs/architecture/01-module-boundaries.md`, lines 70-72):
```markdown
| `eyelib-molang/**` | `molang.engine` | Own Molang value/runtime, compile/type/scope/mapping-api, and built-in mappings without depending on root runtime packages |
| `:eyelib-particle/**` | `client.particle.module` | Own particle module contracts and future particle-module APIs/core/runtime definitions; root may consume it, but it must not depend on root runtime packages, root managers, root registries, root packets, root capability helpers, or root mc/impl; Phase 8 is skeleton/boundary only and current executable runtime remains in root client particle packages until later phase plans move it through explicit seams |
| `network/` | `sync` | Own packet registration and side-aware routing |
```

**Side boundary rule pattern** (`docs/architecture/02-side-boundaries.md`, lines 21-31):
```markdown
- `:eyelib-particle` is a particle module zone: it may own particle module contracts and future pure particle core/runtime definitions, must not depend on root runtime packages or `mc/impl`, and any Minecraft/Forge-facing integration requires documented adapters before introduction.
...
- Pure particle core in :eyelib-particle must remain root/MC/Forge-clean; platform-specific concerns require documented adapters before any Minecraft/Forge-facing dependency is introduced.
```

**Apply:** update only wording needed to change “future runtime definitions” into current Phase 10 ownership: importer owns raw schema/codecs, particle owns `ParticleDefinition`/adapter seam, root `client/particle/bedrock/BrParticle` is legacy/non-canonical until Phase 11/12.

## Shared Patterns

### Root-clean particle module boundary
**Source:** `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md` lines 12-19  
**Apply to:** all new `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/runtime/*` files
```markdown
## Dependency Direction
- Root runtime may depend on :eyelib-particle, but :eyelib-particle must not depend on root runtime packages, root managers, root registries, root packets, root capability helpers, or root mc/impl classes.
- Do not add `project(':')` or imports from `io.github.tt432.eyelib.client`, `io.github.tt432.eyelib.network`, `io.github.tt432.eyelib.capability`, or `io.github.tt432.eyelib.mc.impl` here.

## Integration Rule
- Pure particle core stays free of root, Minecraft, and Forge contamination.
- Minecraft/Forge-facing integration must live in explicitly documented adapters before introduction.
```

### Nullness/package docs
**Source:** `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/api/package-info.java` lines 1-12  
**Apply to:** `runtime/package-info.java`
```java
@NullMarked
package io.github.tt432.eyelibparticle.api;

import org.jspecify.annotations.NullMarked;
```

### DataResult loud failure
**Source:** `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/particle/BrParticle.java` lines 232-248  
**Apply to:** `ParticleDefinitionAdapter.fromSchema(...)`
```java
if (!jsonElement.isJsonObject()) {
    return DataResult.error(() -> "Chain node must be a JSON object");
}
...
} catch (RuntimeException exception) {
    return DataResult.error(() -> exception.getMessage());
}
```

### Preserve raw components and prefixed/unprefixed billboard lookup
**Source:** `eyelib-importer/src/main/java/io/github/tt432/eyelibimporter/particle/BrParticle.java` lines 46-48  
**Apply to:** `ParticleDefinition` and adapter parity tests
```java
BedrockResourceValue appearance = Optional.ofNullable(components.get("minecraft:particle_appearance_billboard"))
        .orElse(components.get("particle_appearance_billboard"));
```

### Static source-scan boundary tests
**Source:** `src/test/java/io/github/tt432/eyelib/client/particle/ParticleApiDelegationBoundaryTest.java` lines 53-71  
**Apply to:** boundary and documentation tests
```java
try (var paths = Files.walk(sourceRoot)) {
    List<Path> violatingFiles = paths
            .filter(path -> path.toString().endsWith(".java"))
            .filter(path -> containsAnyForbiddenImport(path, forbiddenFragments))
            .toList();

    assertTrue(violatingFiles.isEmpty(), () -> "Forbidden particle module imports: " + violatingFiles);
}
```

## No Analog Found

All likely new/modified Phase 10 files have usable analogs. The closest analog for `ParticleDefinitionDocumentationTest` is a role-match static documentation test rather than an existing dedicated documentation-test class; planner should still use the `SourceCheck` pattern above.

## Metadata

**Analog search scope:** provided phase context files; `eyelib-particle/src/main/java`, `eyelib-particle/src/test/java`, `eyelib-importer/src/main/java`, `eyelib-importer/src/test/resources`, root particle/loader docs and tests, `MODULES.md`, `docs/architecture/*`.  
**Files scanned/read:** 30+ candidate files across docs, runtime, importer schema, particle API, fixtures, tests, and build config.  
**Pattern extraction date:** 2026-05-09
