# Phase 15: Pre-Migration Audit & Routing - Pattern Map

**Mapped:** 2026-05-10  
**Files analyzed:** 10 likely created/modified artifacts plus 37 util/core-util inventory entries  
**Analogs found:** 10 / 10

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `docs/architecture/migration/utility-routing-manifest.md` | documentation / manifest | batch audit + routing decision table | `docs/architecture/migration/utility-mc-bridges.md` | role-match |
| `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntry.java` | model / codec | transform + serialization | `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrBoneKeyFrame.java` | exact for explicit import style |
| `src/main/java/io/github/tt432/eyelib/util/codec/TupleCodec.java` | utility / codec | serialization transform | `src/main/java/io/github/tt432/eyelib/util/codec/ChinExtraCodecs.java` | role-match |
| `src/main/java/io/github/tt432/eyelib/util/client/AnimationApplier.java` -> `src/main/java/io/github/tt432/eyelib/client/animation/AnimationApplier.java` or delete | utility | transform | `src/main/java/io/github/tt432/eyelib/client/render/PoseCopies.java` | role-match for utility relocation |
| `src/main/java/io/github/tt432/eyelib/util/client/Models.java` -> `src/main/java/io/github/tt432/eyelib/client/model/Models.java` or delete | utility | transform | `src/main/java/io/github/tt432/eyelib/client/model/README.md` + `Models.java` current implementation | role-match |
| `src/main/java/io/github/tt432/eyelib/util/modbridge/ModBridgeServer.java` -> `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/ModBridgeServer.java` or delete | service / integration adapter | event-driven + file/network I/O | `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/ModBridgeModelUpdateEvent.java` | role-match by destination owner |
| `src/main/java/io/github/tt432/eyelib/util/modbridge/BBModelSink.java` -> `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/BBModelSink.java` or delete | provider / integration port | event-driven | `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/ModBridgeModelUpdateEvent.java` | partial |
| `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/README.md` (optional) | documentation / package guide | boundary documentation | `src/main/java/io/github/tt432/eyelib/mc/README.md` | role-match |
| `src/main/java/io/github/tt432/eyelib/util/ListHelper.java` (catalog only unless Phase 15 migrates caller) | compatibility shim | transform | `src/main/java/io/github/tt432/eyelib/core/util/collection/ListAccessors.java` | exact canonical backend |
| `src/main/java/io/github/tt432/eyelib/util/codec/EitherHelper.java` (catalog only unless Phase 15 migrates callers) | compatibility shim / codec utility | transform | `src/main/java/io/github/tt432/eyelib/core/util/codec/Eithers.java` | exact canonical backend |

## Pattern Assignments

### `docs/architecture/migration/utility-routing-manifest.md` (documentation, batch audit)

**Analog:** `docs/architecture/migration/utility-mc-bridges.md`

**Scope + main paths pattern** (lines 3-6):
```markdown
## Scope
- Utilities that currently mix pure helpers with Minecraft-facing helpers.
- Main paths: `util/client/`, `util/codec/`, `util/modbridge/`, selected `util/math/`, selected top-level `util/*.java`
```

**Status / target-state pattern** (lines 10-20):
```markdown
## Final isolation status
- First-wave seam status: complete.
- Hard-quarantine wave 2 status (timer + modbridge event): complete.
- Final functional-ownership cleanup status: in progress.
- Expected final state for this module: only truly cross-cutting helpers remain in `util`/`core`; MC codecs, resource-location helpers, render helpers, bridge events, and other Minecraft/Forge-aware utility behavior should live with the feature that owns the behavior.

## Target seam
- Move pure helpers toward `core`.
- Keep MC codec primitives, render helpers, and bridge/event integration with their functional owners once identified.
```

**Inventory/history pattern** (lines 33-43):
```markdown
## Implementation changes
- Added new core package documentation: `src/main/java/io/github/tt432/eyelib/core/README.md`.
- Updated adapter-style utility classes to delegate into core seams:
  - `util/ListHelper` → `core/util/collection/ListAccessors`
  - `util/client/texture/TexturePathHelper` → `core/util/texture/TexturePaths`
  - `util/math/FastColorHelper` → `core/util/color/ColorEncodings`
  - `util/codec/EitherHelper` → `core/util/codec/Eithers`
- Migrated direct internal callers where low-risk:
  - `client/render/controller/RenderControllerEntry` now uses `TexturePaths` directly.
  - `client/render/texture/NativeImageIO` now uses `ColorEncodings` directly.
```

**Apply to Phase 15:** create a manifest table with one row for every current `src/main/java/io/github/tt432/eyelib/util/**/*.java` and `src/main/java/io/github/tt432/eyelib/core/util/**/*.java` file, including consumer count, 0/1/N class, route, phase, and evidence. Use current inventory: 32 root util Java files + 5 core util Java files from fresh glob.

---

### `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrAnimationEntry.java` (model/codec, transform)

**Analog:** `src/main/java/io/github/tt432/eyelib/client/animation/bedrock/BrBoneKeyFrame.java`

**Explicit import pattern** (lines 3-20):
```java
import io.github.tt432.eyelibimporter.animation.bedrock.BrBoneKeyFrameSchema;
import io.github.tt432.eyelibprocessor.animation.baked.BakedBoneKeyFrame;
import io.github.tt432.eyelibprocessor.animation.baked.BoneAnimationBaker;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import io.github.tt432.eyelibmolang.MolangValue3;
import io.github.tt432.eyelib.util.ListHelper;
import io.github.tt432.eyelib.util.codec.ChinExtraCodecs;
import io.github.tt432.eyelib.util.codec.CodecHelper;
import io.github.tt432.eyelib.util.math.Curves;
import io.github.tt432.eyelib.util.math.EyeMath;
```

**Current wildcard to eliminate** (`BrAnimationEntry.java` lines 3-15):
```java
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.*;
import io.github.tt432.eyelib.client.animation.*;
import io.github.tt432.eyelib.client.model.*;
import io.github.tt432.eyelib.util.*;
import io.github.tt432.eyelib.util.codec.*;
import io.github.tt432.eyelibimporter.animation.bedrock.*;
import io.github.tt432.eyelibimporter.model.GlobalBoneIdHandler;
import io.github.tt432.eyelibmolang.*;
import it.unimi.dsi.fastutil.ints.*;
import org.jspecify.annotations.*;
```

**Core codec usage requiring explicit imports** (`BrAnimationEntry.java` lines 64-67, 103-105):
```java
public static Codec<BrAnimationEntry> codec(String name) {
    return RecordCodecBuilder.create(ins -> {
        final Codec<List<MolangValue>> elementCodec = ChinExtraCodecs.singleOrList(MolangValue.CODEC);
        Comparator<Float> comparator = Comparator.comparingDouble(k -> k);
```
```java
private static final Codec<TreeMap<Float, List<BrEffectsKeyFrameDefinition>>> EFFECTS_CODEC = CodecHelper.dispatchedMap(
        Codec.STRING,
        f -> ChinExtraCodecs.singleOrList(BrEffectsKeyFrameDefinition.Factory.CODEC).xmap(
```

**Apply to Phase 15:** replace wildcard imports with class-level imports. At minimum preserve explicit imports for `Codec`, `RecordCodecBuilder`, animation/model classes used in signatures, `ChinExtraCodecs`, `CodecHelper`, importer definitions, `MolangScope`, `MolangValue`, `Int2ObjectMap`, `Int2ObjectOpenHashMap`, and `Nullable`. Let IDE optimize imports if possible.

---

### `src/main/java/io/github/tt432/eyelib/util/codec/TupleCodec.java` (codec utility, transform)

**Analog:** `src/main/java/io/github/tt432/eyelib/util/codec/ChinExtraCodecs.java`

**Explicit imports pattern** (`ChinExtraCodecs.java` lines 1-12):
```java
package io.github.tt432.eyelib.util.codec;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
```

**Current wildcard to eliminate** (`TupleCodec.java` lines 3-14):
```java
import com.mojang.datafixers.util.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.ListBuilder;
import io.github.tt432.eyelib.util.codec.Tuple.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
```

**Nested tuple usage pattern** (`TupleCodec.java` lines 92-94, 108-110):
```java
public <T> Codec<T> bmap(BiFunction<A, B, T> to, Function<T, T2<A, B>> from) {
    return xmap(l -> to.apply((A) l.get(0), (B) l.get(1)), from.andThen(Tuple::asList));
}
```
```java
public <T> Codec<T> bmap(Function3<A, B, C, T> to, Function<T, T3<A, B, C>> from) {
    return xmap(l -> to.apply((A) l.get(0), (B) l.get(1), (C) l.get(2)), from.andThen(Tuple::asList));
}
```

**Apply to Phase 15:** replace `com.mojang.datafixers.util.*` with `Pair`; replace `Tuple.*` with explicit nested imports (`T2`, `T3`, ..., `T16`, `Function3`, ..., `Function16`) or qualify as `Tuple.T2` / `Tuple.Function3`. Prefer explicit nested imports if IDE keeps readability.

---

### `AnimationApplier` relocation/delete decision (utility transform)

**Analog:** `src/main/java/io/github/tt432/eyelib/client/render/PoseCopies.java`; destination guidance from `src/main/java/io/github/tt432/eyelib/client/animation/README.md`

**Functional utility class pattern** (`PoseCopies.java` lines 1-13):
```java
package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PoseCopies {
    public static PoseStack.Pose copy(PoseStack.Pose pose) {
        return new PoseStack.Pose(new Matrix4f(pose.pose()), new Matrix3f(pose.normal()));
    }
}
```

**Current implementation to preserve if moved** (`AnimationApplier.java` lines 1-38):
```java
package io.github.tt432.eyelib.util.client;

import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import lombok.experimental.UtilityClass;
import org.joml.Vector3f;

@UtilityClass
public class AnimationApplier {
    public void apply(ModelRuntimeData.Entry entry, Model.Bone model, ModelRuntimeData data) {
        var initPosition = model.position();
        Vector3f renderPosition = entry.position;
        data.position(model, renderPosition.x + initPosition.x(), renderPosition.y + initPosition.y(), renderPosition.z + initPosition.z());
        var initRotation = model.rotation();
        Vector3f renderRotation = entry.rotation;
        data.rotation(model, renderRotation.x + initRotation.x(), renderRotation.y + initRotation.y(), renderRotation.z + initRotation.z());
        var initScale = model.scale();
        Vector3f renderScala = entry.scale;
        data.scale(model, renderScala.x * initScale.x(), renderScala.y * initScale.y(), renderScala.z * initScale.z());
    }
}
```

**Destination ownership pattern** (`client/animation/README.md` lines 3-10):
```markdown
## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/animation/`
- Contains runtime animation/controller execution, playback state, lookup seams, and transitional classes that currently still mix schema with runtime hooks.

## Boundary intent
- Parsed animation/controller schema and codecs are moving toward `:eyelib-importer`.
- Pure animation state and deterministic transition logic may remain outside `mc/impl` if they stay platform-type-free and plain-JVM-testable.
- Runtime animation hooks, controller execution tied to entity/particle/model runtime state, and schema-to-runtime adaptation stay root-owned and must not move into `:eyelib-importer` unchanged.
```

**Apply to Phase 15:** first verify references. Current text scan found only the class declaration, so planner should require a Wave 0 semantic reference check. If kept, move with IDE-aware refactor into `client/animation`, and consider converting to final class + private constructor pattern used by `PoseCopies` instead of extending `util/client` surface. If zero-consumer deletion is chosen, record `0/delete` in the manifest before deleting.

---

### `Models` relocation/delete decision (utility transform)

**Analog:** `src/main/java/io/github/tt432/eyelib/client/model/README.md`; current `Models.java` implementation

**Destination ownership pattern** (`client/model/README.md` lines 3-10):
```markdown
## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/model/`
- Contains root-side runtime model helpers, bake helpers, model-part-facing abstractions, and adapters that consume model definitions from `:eyelib-importer`.

## Boundary intent
- Platform-free definition/state structures may remain outside `mc/impl` only if they do not expose Minecraft model/runtime types.
- Bake/runtime/render integration, texture-backed model processing, and model-part-facing implementation code should move toward `mc/impl` during final quarantine.
```

**Current core merge pattern** (`Models.java` lines 17-43):
```java
public class Models {
    public static @Nullable Model merge(List<Model> models) {
        if (models.isEmpty()) return null;

        Model result = models.get(0);

        for (int i = 1; i < models.size(); i++) {
            result = add(result, models.get(i));
        }

        return result;
    }

    public static Model add(Model modelA, Model modelB) {
        Int2ObjectMap<Model.Bone> bonesMap = new Int2ObjectOpenHashMap<>(modelA.allBones());
        Int2IntMap idRemap = new Int2IntOpenHashMap();
        idRemap.defaultReturnValue(-1);
        // ...
        return new Model(modelA.name(), bonesMap, modelA.locator(), modelA.visibleBox());
    }
```

**Current subtract pattern** (`Models.java` lines 84-99):
```java
public static Model sub(Model modelA, Model modelB) {
    Int2ObjectMap<Model.Bone> newBones = new Int2ObjectOpenHashMap<>();

    modelA.allBones().int2ObjectEntrySet().forEach(entry -> {
        if (modelB.allBones().containsKey(entry.getIntKey())) {
            newBones.put(entry.getIntKey(), entry.getValue().withCubes(new ArrayList<>()).withChildren(new Int2ObjectOpenHashMap<>()));
        } else {
            newBones.put(entry.getIntKey(), entry.getValue().withChildren(new Int2ObjectOpenHashMap<>()));
        }
    });

    return new Model(modelA.name(), newBones, modelA.locator(), modelA.visibleBox());
}
```

**Apply to Phase 15:** verify whether any semantic references exist beyond text false positives for local `models` variables. If kept, move to `client/model` because it consumes importer `Model` definitions and manipulates root runtime model helper data. If zero-consumer deletion is chosen, record it in the manifest.

---

### `ModBridgeServer` + `BBModelSink` relocation/delete decision (service/provider, event-driven + network I/O)

**Analog:** `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/ModBridgeModelUpdateEvent.java`; package guidance from `src/main/java/io/github/tt432/eyelib/mc/README.md`

**Destination owner pattern** (`ModBridgeModelUpdateEvent.java` lines 1-16):
```java
package io.github.tt432.eyelib.mc.impl.modbridge;

import net.minecraftforge.eventbus.api.Event;

/**
 * fire on client side.
 *
 * @author TT432
 */
public class ModBridgeModelUpdateEvent extends Event {
    public final String json;

    public ModBridgeModelUpdateEvent(String json) {
        this.json = json;
    }
}
```

**Current service lifecycle + error handling** (`ModBridgeServer.java` lines 17-50):
```java
@Slf4j
public class ModBridgeServer {
    private final int port;
    private final BBModelSink sink;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void start() {
        if (running.get()) return;
        running.set(true);
        executor.submit(this::runServer);
        log.info("ModBridge Test server started on TCP 127.0.0.1:{}", port);
    }

    private void runServer() {
        try (ServerSocket server = new ServerSocket(port)) {
            while (running.get()) {
                try (Socket client = server.accept()) {
                    handleClient(client);
                }
            }
        } catch (IOException e) {
            log.error("ModBridge server error", e);
        }
    }
}
```

**Payload bound / validation pattern** (`ModBridgeServer.java` lines 53-69):
```java
private void handleClient(Socket client) throws IOException {
    client.setTcpNoDelay(true);
    DataInputStream in = new DataInputStream(new BufferedInputStream(client.getInputStream()));
    while (running.get()) {
        int len;
        try {
            len = readIntLE(in);
        } catch (IOException eof) {
            break;
        }
        if (len <= 0 || len > 50_000_000) break;
        byte[] payload = in.readNBytes(len);
        if (payload.length != len) break;
        String json = new String(payload, StandardCharsets.UTF_8);

        sink.onModelUpdate(json);
    }
}
```

**Sink port pattern** (`BBModelSink.java` lines 1-5):
```java
package io.github.tt432.eyelib.util.modbridge;

public interface BBModelSink {
    void onModelUpdate(String bbmodelJson);
}
```

**MC package rule** (`mc/README.md` lines 19-21):
```markdown
## Ownership Rule
- Minecraft/Forge imports are allowed wherever the owning functional module needs them.
- Use this package only when code genuinely coordinates multiple functional modules or has not yet received a better owner.
```

**Apply to Phase 15:** if kept, move both files together into `mc/impl/modbridge` and update package declarations/imports via IDE-aware move. Preserve `50_000_000` payload bound, `AtomicBoolean` lifecycle, single-thread executor, and `Slf4j` logging. Current text scan found no production constructor call for `ModBridgeServer`, so deletion needs explicit `0/delete` manifest evidence.

---

### Optional `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/README.md` (package guide)

**Analog:** `src/main/java/io/github/tt432/eyelib/mc/README.md`

**README shape** (`mc/README.md` lines 3-10):
```markdown
## Scope
- Path: `src/main/java/io/github/tt432/eyelib/mc/`
- Transitional home for root-level Minecraft/Forge wiring that has not yet been assigned to a functional module.
- This package is not a boundary rule. Feature-owned Minecraft/Forge code should live with its feature module.

## Layout
- `impl/`: remaining root mod startup, shared technical wiring, and compatibility adapters that still need functional ownership decisions.
- `api/`: legacy transitional namespace; do not add new contracts here.
```

**Apply to Phase 15:** only create this README if `ModBridgeServer`/`BBModelSink` are physically moved and local ownership needs documentation. State that modbridge TCP/event integration is transitional MC implementation wiring and not a general util destination.

---

### `ListHelper` shim catalog (compatibility shim, transform)

**Analog:** `src/main/java/io/github/tt432/eyelib/core/util/collection/ListAccessors.java`

**Current shim** (`ListHelper.java` lines 1-15):
```java
package io.github.tt432.eyelib.util;

import io.github.tt432.eyelib.core.util.collection.ListAccessors;

import java.util.List;

public class ListHelper {
    public static <T> T getFirst(List<T> list) {
        return ListAccessors.first(list);
    }

    public static <T> T getLast(List<T> list) {
        return ListAccessors.last(list);
    }
}
```

**Canonical backend** (`ListAccessors.java` lines 1-17):
```java
package io.github.tt432.eyelib.core.util.collection;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ListAccessors {
    public static <T> T first(List<T> list) {
        return list.get(0);
    }

    public static <T> T last(List<T> list) {
        return list.get(list.size() - 1);
    }
}
```

**Current consumer evidence** (`BrBoneKeyFrame.java` lines 194-203):
```java
public static MolangValue3 getValue(BrBoneKeyFrameDefinition keyFrame, boolean isPre) {
    return isPre ? ListHelper.getFirst(keyFrame.dataPoints()) : ListHelper.getLast(keyFrame.dataPoints());
}

public MolangValue3 getPre() {
    return ListHelper.getFirst(dataPoints);
}

public MolangValue3 getPost() {
    return ListHelper.getLast(dataPoints);
}
```

**Apply to Phase 15:** catalog as compatibility shim in manifest with deletion timing tied to Phase 17. If Phase 15 also rewrites the single consumer, use `ListAccessors.first/last` directly and preserve behavior.

---

### `EitherHelper` shim catalog (compatibility shim, codec transform)

**Analog:** `src/main/java/io/github/tt432/eyelib/core/util/codec/Eithers.java`

**Current shim** (`EitherHelper.java` lines 1-10):
```java
package io.github.tt432.eyelib.util.codec;

import com.mojang.datafixers.util.Either;
import io.github.tt432.eyelib.core.util.codec.Eithers;

public class EitherHelper {
    public static <U> U unwrap(final Either<? extends U, ? extends U> either) {
        return Eithers.unwrap(either);
    }
}
```

**Canonical backend** (`Eithers.java` lines 1-14):
```java
package io.github.tt432.eyelib.core.util.codec;

import com.mojang.datafixers.util.Either;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Eithers {
    public static <U> U unwrap(final Either<? extends U, ? extends U> either) {
        return either.map(Function.identity(), Function.identity());
    }
}
```

**Current consumers** (`CodecHelper.java` lines 9-15; `ChinExtraCodecs.java` lines 25-27, 54-57):
```java
public class CodecHelper {
    public static <T> Codec<T> withAlternative(final Codec<T> primary, final Codec<? extends T> alternative) {
        return Codec.either(primary, alternative).xmap(EitherHelper::unwrap, Either::left);
    }
```
```java
public static <A> Codec<List<A>> singleOrList(Codec<A> codec) {
    return Codec.either(codec.xmap(List::of, l -> l.get(0)), codec.listOf()).xmap(EitherHelper::unwrap, Either::right);
}
```
```java
public static <T> MapCodec<T> withAlternative(final MapCodec<T> primary,
                                              final MapCodec<? extends T> alternative) {
    return Codec.mapEither(primary, alternative).xmap(EitherHelper::unwrap, Either::left);
}
```

**Apply to Phase 15:** catalog as codec compatibility shim in manifest with deletion timing tied to Phase 19 atomic codec migration. Do not migrate codec infrastructure early.

## Shared Patterns

### Ownership routing before package moves
**Source:** `AGENTS.md` lines 14-21; `MODULES.md` lines 151-156  
**Apply to:** all package moves and manifest decisions
```markdown
- Prefer narrow, stage-scoped edits over broad package churn.
- Document ownership and dependency rules before moving code across subsystem boundaries.
- Do not add new code to ambiguous catch-all areas like `src/main/java/io/github/tt432/eyelib/util/client/` without first documenting the destination responsibility.
- Before each change, identify which modules in `MODULES.md` are affected.
```

### Existing utility split rule
**Source:** `src/main/java/io/github/tt432/eyelib/util/README.md` lines 23-40  
**Apply to:** manifest route decisions, `AnimationApplier`, `Models`, shims
```markdown
## First-Wave Core Extractions
- `../core/util/texture/TexturePaths.java`: platform-free texture-path derivation
- `../core/util/color/ColorEncodings.java`: platform-free color channel transforms
- `../core/util/collection/ListAccessors.java`: platform-free list edge accessors
- `../core/util/codec/Eithers.java`: platform-free `Either` unwrap helper
- `../core/util/time/FixedStepTimerState.java`: platform-free fixed-step timer state math
- Existing `util/*` classes remain as compatibility adapters while callers migrate incrementally.

## Boundary Reminder
- Keep only truly cross-cutting helpers in `util/`.
- Do not add unrelated new code to `util/client/` without a documented destination responsibility.
```

### Phase 15 success criteria
**Source:** `.planning/ROADMAP.md` lines 68-76  
**Apply to:** all Phase 15 plans
```markdown
1. Maintainer can inspect a routing manifest listing every root/util/* and core/util/* file with its verified consumer count (0/1/N) and target destination (eyelib-util / functional owner / delete).
2. `grep` for wildcard imports like `import io.github.tt432.eyelib.util.*` or `import io.github.tt432.eyelib.util.codec.*` across root source files returns zero results — all are replaced with explicit class-level imports.
3. Single-consumer utility classes (AnimationApplier → client/animation, Models → client/model, ModBridgeServer/BBModelSink → mc/impl/modbridge) reside in their functional owner packages and compile successfully with zero residual references to old util/ paths.
4. Identified compatibility shims (ListHelper, EitherHelper) are cataloged with consumer counts and a deletion plan tied to their respective migration phases.
```

### Verification pattern
**Source:** `15-VALIDATION.md` lines 37-44  
**Apply to:** planner verification actions
```markdown
| 15-01-01 | routing manifest | Compare current `src/main/java/io/github/tt432/eyelib/util/**/*.java` and `src/main/java/io/github/tt432/eyelib/core/util/**/*.java` inventory against `docs/architecture/migration/utility-routing-manifest.md` rows |
| 15-02-01 | wildcard cleanup | `jetbrain_search_regex` pattern `import\s+io\.github\.tt432\.eyelib\.util(?:\.[A-Za-z0-9_]+)*\.\*;` returns zero results |
| 15-03-01 | single-consumer routing | IDE reference checks for `AnimationApplier`, `Models`, `ModBridgeServer`, `BBModelSink`; residual old path import scan returns zero; JetBrains build succeeds |
```

### No shell Gradle
**Source:** `AGENTS.md` lines 28-31  
**Apply to:** all verification
```markdown
- All Gradle commands must use JetBrains MCP (`jetbrain_build_project`, `jetbrain_run_gradle_tasks`). Never run `./gradlew` in shell.
```

## No Analog Found

No likely Phase 15 artifact lacks an analog. The routing manifest has migration-doc analogs; source moves have prior functional-owner relocation analogs; shims have canonical core backend analogs.

## Metadata

**Analog search scope:** `docs/architecture/migration/`, `src/main/java/io/github/tt432/eyelib/util/**`, `src/main/java/io/github/tt432/eyelib/core/util/**`, `src/main/java/io/github/tt432/eyelib/client/animation/`, `src/main/java/io/github/tt432/eyelib/client/model/`, `src/main/java/io/github/tt432/eyelib/mc/impl/modbridge/`  
**Files scanned:** 37 util/core-util Java files plus targeted docs/source analogs  
**Pattern extraction date:** 2026-05-10
