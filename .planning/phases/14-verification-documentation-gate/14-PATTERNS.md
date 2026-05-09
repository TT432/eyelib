---
phase: 14
slug: verification-documentation-gate
status: complete
created: 2026-05-09
---

# Phase 14 Pattern Map

## Pattern Summary

Phase 14 should follow existing repository patterns for JUnit 5 static/boundary tests, stable documentation drift tests, real codec/fixture parity tests, JetBrains MCP-only verification, and planning-artifact evidence capture.

## Test Patterns

### Flat JUnit 5 Classes

- Use package-private `class *Test` and package-private `@Test void descriptiveMethodName()` methods.
- Use JUnit assertions directly; no mocking framework is present.
- Keep helper records/classes at the bottom of the test file.

Analog files:
- `src/test/java/io/github/tt432/eyelib/client/particle/ParticleApiDelegationBoundaryTest.java`
- `src/test/java/io/github/tt432/eyelib/docs/ParticleCommandNetworkDocumentationTest.java`
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/runtime/ParticleDefinitionAdapterTest.java`
- `eyelib-particle/src/test/java/io/github/tt432/eyelibparticle/loading/ParticleResourcePublicationTest.java`

### Source-Scan Boundary Tests

Use `Files.walk(Path.of(...))` and `Files.readString(Path.of(...))` with explicit forbidden fragment lists. Existing tests scan for root/Minecraft/Forge imports and check documentation wording with small `SourceCheck` helpers.

Required Phase 14 refinements:
- Pure particle areas must reject `io.github.tt432.eyelib.client`, `io.github.tt432.eyelib.network`, `io.github.tt432.eyelib.capability`, `io.github.tt432.eyelib.mc.impl`, `net.minecraft`, and `net.minecraftforge` imports.
- `eyelib-particle/.../client/**` is the documented client integration exception and must be checked for `Dist.CLIENT`/client-layer ownership instead of treated as pure runtime.
- Normal tests must not read `.planning/` files.

### Documentation Drift Tests

Existing doc tests read stable repository files only:

```java
private static String readDocs(String... paths) throws IOException {
    StringBuilder docs = new StringBuilder();
    for (String path : paths) {
        docs.append(Files.readString(Path.of(path))).append('\n');
    }
    return docs.toString();
}
```

Phase 14 docs tests should include:
- `MODULES.md`
- `docs/index/repo-map.md`
- `docs/architecture/01-module-boundaries.md`
- `docs/architecture/02-side-boundaries.md`
- `eyelib-particle/src/main/java/io/github/tt432/eyelibparticle/README.md`
- `src/main/java/io/github/tt432/eyelib/client/particle/README.md`
- `src/main/java/io/github/tt432/eyelib/network/README.md`

They must assert final ownership anchors: `ParticleDefinitionRegistry`, `ParticleResourcePublication`, `ParticleDefinition.identifier()`, `ParticleDefinitionAdapter`, `ParticleCommandRuntime`, `mc/impl/common/command`, `mc/impl/network/packet`, `SpawnParticlePacket(String spawnId, String particleId, Vector3f position)`, `RemoveParticlePacket(String removeId)`, `ClientSmoke`, `hardware`, `PFUT-02`, and `PFUT-03`.

### Codec/Fixture Parity Tests

Use real importer codecs and existing fixtures, as in `ParticleDefinitionAdapterTest`:
- `BrParticle.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))`
- `ParticleDefinitionAdapter.fromSchema(schema)`
- Assertions over identifier, format version, material, texture, curves, events, raw components, billboard flipbook, and loud DataResult failures.

### Publication Key Tests

Use `LinkedHashMap` fixture inputs and assert active registry keys, as in `ParticleResourcePublicationTest`:
- Source ids are report metadata.
- Active keys are `particle_effect.description.identifier` via `ParticleDefinition.identifier()`.
- Full replacement removes stale entries.
- Invalid resources are reported and skipped while valid entries replace the store.

## Documentation Patterns

Docs should state exact ownership rather than vague consistency language:

| Owner | Final Phase 14 wording target |
|-------|-------------------------------|
| `:eyelib-particle` | Owns module APIs, `ParticleDefinition`, `ParticleDefinitionAdapter`, executable runtime, client integration, render manager, loading/publication, and active `ParticleStore<ParticleDefinition>`. |
| root runtime | Owns Forge/resource adapter `BrParticleLoader`, command adapter `mc/impl/common/command`, packet DTO/codecs under `mc/impl/network/packet`, transport, network handler delegation, and transitional compatibility facades. |
| importer | Owns raw Bedrock particle schema/codec `io.github.tt432.eyelibimporter.particle.BrParticle`. |
| `.planning` | May contain evidence; normal source tests must not depend on `.planning` artifacts. |

## Verification Patterns

All Gradle verification must be expressed as JetBrains MCP tasks. Use these command shapes in plan verification and evidence:

- `jetbrain_run_gradle_tasks` taskNames=`[":eyelib-particle:test", ":eyelib-particle:compileJava", ":compileJava"]`
- `jetbrain_run_gradle_tasks` taskNames=`[":test"]` scriptParameters with targeted `--tests` filters for root particle tests.
- Optional broad root `:test` may be recorded with triage; unrelated fixture failures are residual unless they block particle-gate evidence.

## Evidence Artifact Patterns

Planning/evidence files are allowed to summarize final gate status, but production/source tests cannot read them.

Recommended evidence files:
- `.planning/phases/14-verification-documentation-gate/14-HARDWARE-CHECKLIST.md`
- `.planning/phases/14-verification-documentation-gate/14-FINAL-GATE-EVIDENCE.md`
- `.planning/phases/14-verification-documentation-gate/14-MCP-VERIFICATION-MATRIX.md`
- `.planning/phases/14-verification-documentation-gate/14-MILESTONE-CLOSURE.md`

Each evidence file should list exact task names, result status, residual risks, and whether the item is automated, ClientSmoke-applicable, or manual/hardware-only.

## Pattern Map Complete

Executors can implement Phase 14 by copying these test/documentation/evidence shapes without broad exploration or runtime feature work.
