# Phase 17: Tier-1 Category Migration - Pattern Map

**Mapped:** 2026-05-10  
**Files analyzed:** 30 new/modified/deleted targets  
**Analogs found:** 30 / 30

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `build.gradle` | config | request-response | `build.gradle` dependency block for sibling modules | exact |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/time/SimpleTimer.java` | utility | transform | `src/main/java/io/github/tt432/eyelib/util/SimpleTimer.java` | exact self-move |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/time/FixedStepTimerState.java` | utility | transform | `src/main/java/io/github/tt432/eyelib/core/util/time/FixedStepTimerState.java` | exact self-move |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/color/ColorEncodings.java` | utility | transform | `src/main/java/io/github/tt432/eyelib/core/util/color/ColorEncodings.java` | exact self-move |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/loader/SharedLibraryLoader.java` | utility | file-I/O | `src/main/java/io/github/tt432/eyelib/util/SharedLibraryLoader.java` | exact self-move |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/Curves.java` | utility | transform | `src/main/java/io/github/tt432/eyelib/util/math/Curves.java` | exact self-move |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/EyeMath.java` | utility | transform | `src/main/java/io/github/tt432/eyelib/util/math/EyeMath.java` | exact self-move |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/MathHelper.java` | utility | transform | `src/main/java/io/github/tt432/eyelib/util/math/MathHelper.java` | exact self-move |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/FastColorHelper.java` | utility | transform | `src/main/java/io/github/tt432/eyelib/util/math/FastColorHelper.java` | exact self-move |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/math/Shapes.java` | utility | transform | `src/main/java/io/github/tt432/eyelib/util/math/Shapes.java` | exact self-move |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/search/Searchable.java` | utility | request-response | `src/main/java/io/github/tt432/eyelib/util/search/Searchable.java` | exact self-move |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/search/SearchResults.java` | utility | request-response | `src/main/java/io/github/tt432/eyelib/util/search/SearchResults.java` | exact self-move |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/Blackboard.java` | utility | CRUD | `src/main/java/io/github/tt432/eyelib/util/Blackboard.java` | exact self-move |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/Lists.java` | utility | transform | `src/main/java/io/github/tt432/eyelib/util/Lists.java` | exact self-move |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/Collectors.java` | utility | batch/transform | `src/main/java/io/github/tt432/eyelib/util/Collectors.java` | exact self-move |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/EntryStreams.java` | utility | streaming | `src/main/java/io/github/tt432/eyelib/util/EntryStreams.java` | exact self-move |
| `eyelib-util/src/main/java/io/github/tt432/eyelibutil/collection/ListAccessors.java` | utility | transform | `src/main/java/io/github/tt432/eyelib/core/util/collection/ListAccessors.java` | exact self-move |
| `src/main/java/io/github/tt432/eyelib/util/ListHelper.java` | utility deletion | transform | `src/main/java/io/github/tt432/eyelib/core/util/collection/ListAccessors.java` | replacement exact |
| `eyelib-util/src/test/java/io/github/tt432/eyelibutil/time/FixedStepTimerStateTest.java` | test | transform | `src/test/java/io/github/tt432/eyelib/core/util/time/FixedStepTimerStateTest.java` | exact self-move |
| `eyelib-util/src/test/java/io/github/tt432/eyelibutil/collection/ListAccessorsTest.java` or adapted seam coverage | test | transform | `src/test/java/io/github/tt432/eyelib/core/util/CoreUtilitySeamTest.java` | role-match |
| `eyelib-util/src/test/java/io/github/tt432/eyelibutil/color/ColorEncodingsTest.java` or adapted seam coverage | test | transform | `src/test/java/io/github/tt432/eyelib/core/util/CoreUtilitySeamTest.java` | role-match |
| Root import consumers listed in research (`BrBoneKeyFrame`, `ModelPartModel`, `NativeImageIO`, `BrAttachableLoader`, animation/render/gui math users) | route/consumer | request-response | Phase 15 moved-class imports in `AnimationApplier.java` / `Models.java` | role-match |
| `MODULES.md`, `docs/index/repo-map.md`, `docs/architecture/01-module-boundaries.md`, `eyelib-util/README.md`, package README | docs/config | transform | Phase 16 scaffold docs and verification | role-match |

## Pattern Assignments

### Root `build.gradle` dependency edge (config, request-response)

**Analog:** existing root sibling-module dependency block in `build.gradle`.

**Dependency pattern** (`build.gradle` lines 148-166):
```gradle
dependencies {
    api project(':eyelib-attachment')
    modImplementation project(':eyelib-attachment')
    jarJar project(':eyelib-attachment')
    api project(':eyelib-material')
    modImplementation project(':eyelib-material')
    jarJar project(':eyelib-material')
    api project(':eyelib-particle')
    modImplementation project(':eyelib-particle')
    jarJar project(':eyelib-particle')
    api project(':eyelib-processor')
    additionalRuntimeClasspath project(':eyelib-processor')
    jarJar project(':eyelib-processor')
    api project(':eyelib-importer')
    modImplementation project(':eyelib-importer')
    jarJar project(':eyelib-importer')
    api project(':eyelib-molang')
    additionalRuntimeClasspath project(':eyelib-molang')
    jarJar project(':eyelib-molang')
}
```

**Apply:** Add `api project(':eyelib-util')`, `modImplementation project(':eyelib-util')`, and `jarJar project(':eyelib-util')` before rewiring root consumers to `io.github.tt432.eyelibutil.*`. Prefer the Forge-module pattern (`api` + `modImplementation` + `jarJar`) used by attachment/material/particle/importer.

---

### `:eyelib-util` module/package scaffold (config, request-response)

**Analog:** `eyelib-util/build.gradle`, `EyelibUtilMod.java`, and util package docs created in Phase 16.

**Build scaffold** (`eyelib-util/build.gradle` lines 1-15, 45-57):
```gradle
plugins {
    id 'java-library'
    id 'io.freefair.lombok' version '8.6'
    id 'net.neoforged.moddev.legacyforge' version '2.0.91'
    id 'maven-publish'
}

version = rootProject.version
group = rootProject.group

base {
    archivesName = 'eyelib-util'
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

dependencies {
    implementation 'com.mojang:datafixerupper:6.0.8'
    implementation 'org.joml:joml:1.10.5'
    implementation 'org.slf4j:slf4j-api:2.0.7'
    compileOnly 'org.jspecify:jspecify:1.0.0'
}
```

**Bootstrap identity** (`eyelib-util/src/main/java/io/github/tt432/eyelibutil/bootstrap/EyelibUtilMod.java` lines 1-8):
```java
package io.github.tt432.eyelibutil.bootstrap;

import net.minecraftforge.fml.common.Mod;

@Mod(EyelibUtilMod.MOD_ID)
public class EyelibUtilMod {
    public static final String MOD_ID = "eyelibutil";
}
```

**Apply:** New packages must live below `io.github.tt432.eyelibutil` and must not import root or sibling project packages back into the leaf utility module.

---

### Time utilities: `SimpleTimer`, `FixedStepTimerState` (utility, transform)

**Analogs:** existing source classes; move without algorithm rewrite.

**`SimpleTimer` core pattern** (`src/main/java/io/github/tt432/eyelib/util/SimpleTimer.java` lines 1-35):
```java
package io.github.tt432.eyelib.util;

public class SimpleTimer {
    private long startNanoTime;
    private boolean paused;
    private long fullPausedNanoTime;
    private long pausedNanoTime;

    public SimpleTimer() {
        startNanoTime = System.nanoTime();
    }

    public void setPaused(boolean paused) {
        if (paused && !this.paused) {
            this.paused = true;
            pausedNanoTime = System.nanoTime();
        } else if (!paused && this.paused) {
            this.paused = false;
            fullPausedNanoTime += System.nanoTime() - pausedNanoTime;
        }
    }
}
```

**`FixedStepTimerState` validation/error pattern** (`src/main/java/io/github/tt432/eyelib/core/util/time/FixedStepTimerState.java` lines 13-18, 28-44):
```java
public FixedStepTimerState(int rate) {
    if (rate <= 0) {
        throw new IllegalArgumentException("rate must be > 0");
    }
    this.rate = rate;
}

public boolean canNextStep(int ticks, float partialTick) {
    float secondsSinceStart = realSeconds(ticks, partialTick);
    int currentFixed = (int) Math.floor(secondsSinceStart * rate);

    if (currentFixed > lastFixed) {
        lastFixed += 1;
        return true;
    }
    if (!init) {
        init = true;
        lastFixed = 1;
        return true;
    }
    return false;
}
```

**Testing pattern** (`src/test/java/io/github/tt432/eyelib/core/util/time/FixedStepTimerStateTest.java` lines 9-17, 28-44):
```java
class FixedStepTimerStateTest {
    @Test
    void firstStepIsImmediatelyAvailableAfterStart() {
        FixedStepTimerState timer = new FixedStepTimerState(30);
        timer.start(100, 0.25f);

        assertTrue(timer.canNextStep(100, 0.25f));
        assertFalse(timer.canNextStep(100, 0.25f));
    }

    @Test
    void catchUpIsOneFixedStepPerCall() {
        FixedStepTimerState timer = new FixedStepTimerState(30);
        timer.start(0, 0f);
        assertTrue(timer.canNextStep(0, 0f));
        // one fixed step accepted per canNextStep call
    }
}
```

**Apply:** Change only the package line to `io.github.tt432.eyelibutil.time`, move the test package to `io.github.tt432.eyelibutil.time`, and preserve the constructor guard.

---

### Color/math utilities: `ColorEncodings`, `FastColorHelper`, `Curves`, `EyeMath`, `MathHelper`, `Shapes` (utility, transform)

**Analogs:** existing utility source files and Phase 15 moved-class package rewrite style.

**Static utility + Lombok private constructor pattern** (`ColorEncodings.java` lines 1-15):
```java
package io.github.tt432.eyelib.core.util.color;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ColorEncodings {
    public static int argbToAbgr(int argb32) {
        int a = (argb32 >>> 24) & 0xFF;
        int r = (argb32 >>> 16) & 0xFF;
        int g = (argb32 >>> 8) & 0xFF;
        int b = argb32 & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }
}
```

**Internal import rewrite pattern** (`FastColorHelper.java` lines 1-9):
```java
package io.github.tt432.eyelib.util.math;

import io.github.tt432.eyelib.core.util.color.ColorEncodings;

public class FastColorHelper {
    public static int argbToAbgr(int argb32) {
        return ColorEncodings.argbToAbgr(argb32);
    }
}
```

**Apply as target:**
```java
package io.github.tt432.eyelibutil.math;

import io.github.tt432.eyelibutil.color.ColorEncodings;
```

**JOML/MC utility imports are already supported by util build** (`Shapes.java` lines 1-13):
```java
package io.github.tt432.eyelib.util.math;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Shapes {
    public static Vector3f getRandomPointInAABB(RandomSource random, boolean surfaceOnly, Vector3f offset, Vector3f halfDim) {
```

**Math constants/interpolation pattern** (`EyeMath.java` lines 9-28):
```java
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EyeMath {
    public static final float DEGREES_TO_RADIANS = 0.017453292519943295F;
    public static final float RADIANS_TO_DEGREES = 57.29577951308232F;

    public static float getWeight(float before, float after, float current) {
        return (current - before) / (after - before);
    }

    public static float lerp(float a, float b, float weight) {
        return a + (b - a) * weight;
    }
}
```

**Apply:** Preserve all implementations. Only adjust package declarations and the one `FastColorHelper -> ColorEncodings` import to `io.github.tt432.eyelibutil.color.ColorEncodings`.

---

### Search utilities: `Searchable`, `SearchResults` (utility, request-response)

**Analog:** existing `util/search` pair and `BrAttachableLoader` implementation.

**Interface and internal peer construction** (`Searchable.java` lines 1-15):
```java
package io.github.tt432.eyelib.util.search;

import java.util.Map;
import java.util.stream.Stream;

public interface Searchable<V> {
    Stream<Map.Entry<String, V>> search(String searchStr);

    default SearchResults<V> results() {
        return new SearchResults<>(this);
    }
}
```

**Stateful results holder** (`SearchResults.java` lines 13-22):
```java
@RequiredArgsConstructor
public class SearchResults<V> {
    private final Searchable<V> searchable;
    @Getter
    private final List<Map.Entry<String, V>> suggestions = new ArrayList<>();

    public void update(String searchString) {
        suggestions.clear();
        searchable.search(searchString).forEach(suggestions::add);
    }
}
```

**Consumer import rewrite pattern** (`BrAttachableLoader.java` lines 8, 24-56):
```java
import io.github.tt432.eyelib.util.search.Searchable;

@Slf4j
public class BrAttachableLoader extends BrResourcesLoader implements Searchable<BrClientEntity> {
    @Override
    public Stream<Map.Entry<String, BrClientEntity>> search(String searchStr) {
        return AttachableManager.readPort().getAllData().entrySet().stream()
                .filter(entry -> StringUtils.contains(entry.getKey(), searchStr))
                .map(entry -> Map.entry(entry.getKey(), entry.getValue()));
    }
}
```

**Apply as target:** `import io.github.tt432.eyelibutil.search.Searchable;` in root consumer and package `io.github.tt432.eyelibutil.search` in both moved util classes.

---

### Collection utilities: `Blackboard`, `Lists`, `Collectors`, `EntryStreams`, `ListAccessors` (utility, CRUD/streaming/batch)

**Analogs:** existing collection family plus `CoreUtilitySeamTest`.

**CRUD map holder pattern** (`Blackboard.java` lines 11-32):
```java
public final class Blackboard {
    private final Map<String, Object> data = new HashMap<>();

    public void put(String key, Object value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrCreate(String key, T defaultValue) {
        return (T) data.computeIfAbsent(key, k -> defaultValue);
    }
}
```

**List view pattern and FastUtil dependency watchpoint** (`Lists.java` lines 3-31):
```java
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import lombok.AllArgsConstructor;

import java.util.AbstractList;
import java.util.List;

public class Lists {
    public static  <E> List<E> asList(int count, Int2ObjectFunction<E> function) {
        return new AsListView<>(count, function);
    }

    @AllArgsConstructor
    private static final class AsListView<E> extends AbstractList<E> {
        int count;
        Int2ObjectFunction<E> function;
    }
}
```

**Custom collector duplicate-key error pattern** (`Collectors.java` lines 17-25, 51-56):
```java
public static <T, K, U>
Collector<T, ?, TreeMap<K, U>> toTreeMap(Comparator<? super K> comparator,
                                         Function<? super T, ? extends K> keyMapper,
                                         Function<? super T, ? extends U> valueMapper) {
    return new CollectorImpl<>(() -> new TreeMap<>(comparator),
            uniqKeysMapAccumulator(keyMapper, valueMapper),
            uniqKeysMapMerger(),
            CH_ID);
}

private static IllegalStateException duplicateKeyException(Object k, Object u, Object v) {
    return new IllegalStateException(String.format(
            "Duplicate key %s (attempted merging values %s and %s)", k, u, v));
}
```

**Entry stream helper pattern** (`EntryStreams.java` lines 17-27, 49-63):
```java
public static <K, V> Collector<Map.Entry<K, V>, ?, LinkedHashMap<K, V>> collectSequenced() {
    return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new);
}

public static <K, V, M extends Map<K, V>> Collector<Map.Entry<K, V>, ?, M> collect(Supplier<M> supplier) {
    return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v2, supplier);
}

public static <K1, V1, K2, V2> Function<Map.Entry<K1, V1>, Map.Entry<K2, V2>> mapEntry(
        BiFunction<K1, V1, K2> keyFunction, BiFunction<K1, V1, V2> valueFunction) {
    return entry -> Map.entry(keyFunction.apply(entry.getKey(), entry.getValue()), valueFunction.apply(entry.getKey(), entry.getValue()));
}
```

**Canonical list accessor pattern** (`ListAccessors.java` lines 8-17):
```java
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

**Apply:** Move these into `io.github.tt432.eyelibutil.collection`. Do not rewrite `java.util.stream.Collectors` or `com.google.common.collect.Lists` imports. `EntryStreams` should continue using JDK `java.util.stream.Collectors`, not Eyelib `Collectors`.

---

### `ListHelper` deletion and `BrBoneKeyFrame` rewrite (utility deletion, transform)

**Analog:** shim delegates to `ListAccessors`; delete shim after caller uses canonical names.

**Shim to delete** (`src/main/java/io/github/tt432/eyelib/util/ListHelper.java` lines 1-15):
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

**Consumer occurrences to rewrite** (`BrBoneKeyFrame.java` lines 15-19, 194-216, 229-234):
```java
import io.github.tt432.eyelib.util.ListHelper;
import io.github.tt432.eyelib.util.math.Curves;
import io.github.tt432.eyelib.util.math.EyeMath;

public static MolangValue3 getValue(BrBoneKeyFrameDefinition keyFrame, boolean isPre) {
    return isPre ? ListHelper.getFirst(keyFrame.dataPoints()) : ListHelper.getLast(keyFrame.dataPoints());
}

public MolangValue3 getPre() {
    return ListHelper.getFirst(dataPoints);
}

public MolangValue3 getPost() {
    return ListHelper.getLast(dataPoints);
}

// codec getters also call ListHelper.getFirst/getLast
```

**Apply as target:**
```java
import io.github.tt432.eyelibutil.collection.ListAccessors;
import io.github.tt432.eyelibutil.math.Curves;
import io.github.tt432.eyelibutil.math.EyeMath;

return isPre ? ListAccessors.first(keyFrame.dataPoints()) : ListAccessors.last(keyFrame.dataPoints());
```

---

### Consumer import rewiring (route/consumer, request-response)

**Analog:** Phase 15 moved helper files now live under functional-owner packages and import cross-module classes directly.

**Moved-class package/import pattern** (`AnimationApplier.java` lines 1-13):
```java
package io.github.tt432.eyelib.client.animation;

import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import lombok.experimental.UtilityClass;
import org.joml.Vector3f;

@UtilityClass
public class AnimationApplier {
```

**Moved-class package/import pattern with external deps** (`Models.java` lines 1-12):
```java
package io.github.tt432.eyelib.client.model;

import io.github.tt432.eyelibimporter.model.GlobalBoneIdHandler;
import io.github.tt432.eyelibimporter.model.Model;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
```

**Specific consumer rewrite examples:**

`NativeImageIO.java` color import and calls (`lines 3-6, 63-77`):
```java
import io.github.tt432.eyelib.core.util.color.ColorEncodings;

image.setPixelRGBA(x, y, ColorEncodings.argbToAbgr(bufferedImage.getRGB(x, y)));
image.setPixelRGBA(x, y, ColorEncodings.argbToAbgr(imageData.getPixelArgb(x, y)));
```
Target import: `io.github.tt432.eyelibutil.color.ColorEncodings`.

`ModelPartModel.java` entry-stream import and usage (`lines 8-10, 37-39, 129-139`):
```java
import io.github.tt432.eyelib.util.EntryStreams;

return new Model(name, allBones.int2ObjectEntrySet().stream()
        .map(e -> Map.entry(e.getIntKey(), e.getValue().createBone()))
        .collect(EntryStreams.collect(Int2ObjectOpenHashMap::new)));
```
Target import: `io.github.tt432.eyelibutil.collection.EntryStreams`.

**Apply:** Update only imports from old Eyelib utility packages named in Phase 17. Do not touch out-of-scope codec/resource/texture imports.

---

### Tests and static guards (test, transform)

**Analog:** `UtilModuleIdentityTest` and existing core seam tests.

**Leaf module identity guard** (`UtilModuleIdentityTest.java` lines 14-28):
```java
class UtilModuleIdentityTest {
    private static final Pattern PROJECT_DEPENDENCY_CALL = Pattern.compile("\\bproject\\s*\\(");

    @Test
    void buildScriptDeclaresLeafForgeUtilModule() throws IOException {
        String build = Files.readString(PROJECT_ROOT.resolve("build.gradle"));

        assertAll(
                () -> assertTrue(build.contains("net.neoforged.moddev.legacyforge")),
                () -> assertTrue(build.contains("eyelibutil")),
                () -> assertTrue(build.contains("sourceSet(sourceSets.main)")),
                () -> assertFalse(PROJECT_DEPENDENCY_CALL.matcher(build).find())
        );
    }
}
```

**Core seam behavior tests to adapt under util module** (`CoreUtilitySeamTest.java` lines 21-32):
```java
@Test
void argbToAbgrReordersChannelsWithoutMinecraftRuntime() {
    assertEquals(0x11443322, ColorEncodings.argbToAbgr(0x11223344));
    assertEquals(0xFFCCBBAA, ColorEncodings.argbToAbgr(0xFFAABBCC));
}

@Test
void listAccessorsMirrorLegacyListHelperBehavior() {
    List<String> values = List.of("first", "middle", "last");
    assertEquals("first", ListAccessors.first(values));
    assertEquals("last", ListAccessors.last(values));
}
```

**Apply:** Move `FixedStepTimerStateTest` to the util module. Add or adapt util-module tests for `ColorEncodings` and `ListAccessors`; leave Phase 18/19 assertions for `TexturePaths` and `Eithers` in root until their phases.

## Shared Patterns

### Package Migration
**Source:** Phase 15 moved-class analogs (`AnimationApplier.java`, `Models.java`) and Phase 16 util scaffold.  
**Apply to:** all moved utility source/test files.

```java
// Old package examples:
package io.github.tt432.eyelib.util.math;
package io.github.tt432.eyelib.core.util.time;

// New package examples:
package io.github.tt432.eyelibutil.math;
package io.github.tt432.eyelibutil.time;
```

### Import Rewiring
**Source:** consumer examples above and `17-RESEARCH.md` consumer matrix.  
**Apply to:** root source/test consumers of Phase 17 classes.

```java
import io.github.tt432.eyelibutil.math.EyeMath;
import io.github.tt432.eyelibutil.math.MathHelper;
import io.github.tt432.eyelibutil.collection.EntryStreams;
import io.github.tt432.eyelibutil.search.Searchable;
import io.github.tt432.eyelibutil.color.ColorEncodings;
import io.github.tt432.eyelibutil.collection.ListAccessors;
```

### Boundary Guard
**Source:** `eyelib-util/README.md` lines 14-22 and `UtilModuleIdentityTest.java` lines 14-28.  
**Apply to:** `eyelib-util/build.gradle`, moved util source, docs.

```java
private static final Pattern PROJECT_DEPENDENCY_CALL = Pattern.compile("\\bproject\\s*\\(");
assertFalse(PROJECT_DEPENDENCY_CALL.matcher(build).find());
```

### Verification Pattern
**Source:** `17-VALIDATION.md` lines 24-28 and Phase 16 verification lines 69-73.  
**Apply to:** every implementation wave.

```text
After each migration group: IDE diagnostics on moved files and direct consumers.
After each wave: JetBrains MCP :eyelib-util:build.
Final gate: full project build via JetBrains MCP and residual import scan = zero.
```

Recommended residual import regex from research/manifest:
```regex
import\s+io\.github\.tt432\.eyelib\.(?:core\.)?util\.(?:time|math|search|collection|color|Blackboard|Lists|Collectors|EntryStreams|SharedLibraryLoader|ListHelper)
```

### Deletion Pattern
**Source:** `utility-routing-manifest.md` lines 71-73 and `ListHelper.java` lines 7-14.  
**Apply to:** `src/main/java/io/github/tt432/eyelib/util/ListHelper.java` only.

```text
Replace ListHelper.getFirst(list) -> ListAccessors.first(list)
Replace ListHelper.getLast(list) -> ListAccessors.last(list)
Then delete ListHelper.java and scan for ListHelper.
```

## No Analog Found

None. Every Phase 17 file is either an exact self-move, a direct shim deletion with canonical replacement, or follows established Phase 15/16 module migration patterns.

## Metadata

**Analog search scope:** `.planning/phases/15-*`, `.planning/phases/16-*`, `docs/architecture/migration/`, `eyelib-util/**`, `src/main/java/io/github/tt432/eyelib/{util,core,client,mc}/**`, `src/test/java/io/github/tt432/eyelib/core/util/**`.  
**Files scanned/read:** 34.  
**Pattern extraction date:** 2026-05-10.  
**Tooling notes:** Java source exploration was preceded by IDE Index skill load and index readiness check; Gradle execution remains JetBrains MCP-only and was not run during pattern mapping.  
**Do-not-migrate in Phase 17:** `ResourceLocations.java`, `TexturePathHelper.java`, `TexturePaths.java`, `util/codec/**`, `ImmutableFloatTreeMap.java`, `EitherHelper.java`, `Eithers.java`, and submodule-centralized duplicate helpers.
