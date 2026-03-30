# Bedrock And Blockbench Model Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a unified client-side model importer that reads Blockbench `.bbmodel` and Bedrock geometry JSON, normalizes them through importer-local conversion and post-processing, and publishes Eyelib `Model` instances without expanding the runtime model layer.

**Architecture:** Keep source parsing in `client/model/bbmodel` and `client/model/bedrock`, move conversion and texture repack into a new `client/model/importer` seam, and have `ManagerResourceImportPlanner` call that seam instead of a format-specific loader. The implementation should use TDD to pin current `bbmodel` regressions first, then replace the mixed parse/convert/repack flow with an importer pipeline that can host both Blockbench and Bedrock adapters.

**Tech Stack:** Java 17, Gradle, JUnit 5, Mojang Codec, Gson JSON parsing, Forge/NeoForge legacy moddev runtime, existing Eyelib model and registry types.

---

## Context

### Repository evidence
- `build.gradle`
- `src/main/java/io/github/tt432/eyelib/client/model/Model.java`
- `src/main/java/io/github/tt432/eyelib/client/model/bbmodel/BBModel.java`
- `src/main/java/io/github/tt432/eyelib/client/model/bbmodel/BBModelLoader.java`
- `src/main/java/io/github/tt432/eyelib/client/model/bbmodel/Element.java`
- `src/main/java/io/github/tt432/eyelib/client/model/bbmodel/Outliner.java`
- `src/main/java/io/github/tt432/eyelib/client/gui/manager/reload/ManagerResourceImportPlanner.java`
- `src/main/java/io/github/tt432/eyelib/client/registry/ClientAssetRegistry.java`
- `docs/superpowers/specs/2026-03-30-bedrock-blockbench-model-import-design.md`
- `test.geo.json`
- `../blockbench/java/src/main/java/com/blockbench/model/BBModelLoader.java`
- `../blockbench/java/src/test/java/com/blockbench/model/BBModelLoaderTest.java`

### Proposed file structure
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ModelImportException.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ModelImporter.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedModelData.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedBoneData.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedCubeData.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedFaceData.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedLocatorData.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/BlockbenchModelImporter.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/BedrockGeometryImporter.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedModelBuilder.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedModelTextureRepacker.java`
- Modify: `src/main/java/io/github/tt432/eyelib/client/model/bbmodel/BBModel.java:23-127`
- Modify: `src/main/java/io/github/tt432/eyelib/client/model/bbmodel/BBModelLoader.java:14-64`
- Modify: `src/main/java/io/github/tt432/eyelib/client/model/bbmodel/Element.java:69-232`
- Create or restore: `src/main/java/io/github/tt432/eyelib/client/model/bedrock/BedrockGeometryModel.java`
- Create or restore: `src/main/java/io/github/tt432/eyelib/client/model/bedrock/BedrockModelLoader.java`
- Modify: `src/main/java/io/github/tt432/eyelib/client/gui/manager/reload/ManagerResourceImportPlanner.java:40-212`
- Create: `src/test/java/io/github/tt432/eyelib/client/model/importer/BlockbenchModelImporterTest.java`
- Create: `src/test/java/io/github/tt432/eyelib/client/model/importer/BlockbenchModelCompatibilityTest.java`
- Create: `src/test/java/io/github/tt432/eyelib/client/model/importer/ImportedModelTextureRepackerTest.java`
- Create: `src/test/java/io/github/tt432/eyelib/client/model/importer/BedrockGeometryImporterTest.java`
- Create: `src/test/resources/io/github/tt432/eyelib/client/model/importer/blockbench/minimal.bbmodel`
- Create: `src/test/resources/io/github/tt432/eyelib/client/model/importer/blockbench/multi_texture.bbmodel`
- Create: `src/test/resources/io/github/tt432/eyelib/client/model/importer/bedrock/minimal.geometry.json`
- Use existing fixture: `test.geo.json`
- Modify: `MODULES.md`
- Create if missing: `src/main/java/io/github/tt432/eyelib/client/model/README.md`

## Tasks

### Task 1: Lock in Blockbench behavior with failing tests

**Files:**
- Create: `src/test/java/io/github/tt432/eyelib/client/model/importer/BlockbenchModelCompatibilityTest.java`
- Create: `src/test/java/io/github/tt432/eyelib/client/model/importer/BlockbenchModelImporterTest.java`
- Create: `src/test/resources/io/github/tt432/eyelib/client/model/importer/blockbench/minimal.bbmodel`
- Create: `src/test/resources/io/github/tt432/eyelib/client/model/importer/blockbench/multi_texture.bbmodel`

- [ ] **Step 1: Write the failing compatibility test for source parsing**

```java
package io.github.tt432.eyelib.client.model.importer;

import io.github.tt432.eyelib.client.model.bbmodel.BBModel;
import io.github.tt432.eyelib.client.model.bbmodel.BBModelLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BlockbenchModelCompatibilityTest {
    @Test
    void parsesMinimalFixtureWithExpectedSourceSemantics() throws Exception {
        BBModel model = new BBModelLoader().load(Path.of("src/test/resources/io/github/tt432/eyelib/client/model/importer/blockbench/minimal.bbmodel"));

        assertEquals("geometry.test", model.modelIdentifier());
        assertNotNull(model.meta());
        assertNotNull(model.resolution());
        assertEquals(1, model.elements().size());
        assertEquals(1, model.outliner().size());
    }
}
```

- [ ] **Step 2: Write the failing importer test for `bbmodel -> Model`**

```java
package io.github.tt432.eyelib.client.model.importer;

import io.github.tt432.eyelib.client.model.Model;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BlockbenchModelImporterTest {
    @Test
    void importsMinimalBlockbenchModelIntoRuntimeModel() throws Exception {
        Map<String, Model> imported = ModelImporter.importFile(Path.of("src/test/resources/io/github/tt432/eyelib/client/model/importer/blockbench/minimal.bbmodel"));

        assertEquals(1, imported.size());
        Model model = imported.get("geometry.test");
        assertNotNull(model);
        assertEquals(1, model.toplevelBones().size());
        assertEquals(1, model.allBones().size());
    }
}
```

- [ ] **Step 3: Write the failing importer regression test for multi-texture repack**

```java
@Test
void repacksMultiTextureBlockbenchModelWithoutSplittingRuntimeModelNames() throws Exception {
    Map<String, Model> imported = ModelImporter.importFile(Path.of("src/test/resources/io/github/tt432/eyelib/client/model/importer/blockbench/multi_texture.bbmodel"));

    assertEquals(1, imported.size());
    Model model = imported.get("geometry.multi_texture");
    assertNotNull(model);
    assertEquals(1, model.toplevelBones().size());
}
```

- [ ] **Step 4: Add the minimal fixture file**

```json
{
  "meta": {"format_version": "4.9", "model_format": "free", "box_uv": false},
  "name": "minimal",
  "model_identifier": "geometry.test",
  "visible_box": [2.0, 2.0, 2.0],
  "resolution": {"width": 16, "height": 16},
  "elements": [
    {
      "name": "cube",
      "box_uv": false,
      "render_order": "default",
      "locked": false,
      "allow_mirror_modeling": true,
      "from": [0, 0, 0],
      "to": [16, 16, 16],
      "autouv": 0,
      "color": 0,
      "origin": [8, 8, 8],
      "uv_offset": [0, 0],
      "inflate": 0,
      "faces": {
        "north": {"uv": [0, 0, 16, 16], "texture": 0, "rotation": 0},
        "east": {"uv": [0, 0, 16, 16], "texture": 0, "rotation": 0},
        "south": {"uv": [0, 0, 16, 16], "texture": 0, "rotation": 0},
        "west": {"uv": [0, 0, 16, 16], "texture": 0, "rotation": 0},
        "up": {"uv": [0, 0, 16, 16], "texture": 0, "rotation": 0},
        "down": {"uv": [0, 0, 16, 16], "texture": 0, "rotation": 0}
      },
      "type": "cube",
      "uuid": "cube-1",
      "rotation": [0, 0, 0]
    }
  ],
  "outliner": [
    {
      "name": "root",
      "origin": [8, 8, 8],
      "rotation": [0, 0, 0],
      "uuid": "root-1",
      "export": true,
      "isOpen": true,
      "locked": false,
      "visibility": true,
      "mirror_uv": false,
      "color": 0,
      "autouv": 0,
      "shade": false,
      "children": ["cube-1"]
    }
  ],
  "textures": [
    {"path": "test.png", "name": "test", "folder": "", "namespace": "eyelib", "id": "0", "group": "", "width": 16, "height": 16, "uv_width": 16, "uv_height": 16, "particle": false, "use_as_default": false, "layers_enabled": false, "sync_to_project": "", "render_mode": "default", "render_sides": "auto"}
  ],
  "groups": []
}
```

- [ ] **Step 5: Add the multi-texture fixture file**

```json
{
  "meta": {"format_version": "4.9", "model_format": "free", "box_uv": false},
  "name": "multi_texture",
  "model_identifier": "geometry.multi_texture",
  "visible_box": [2.0, 2.0, 2.0],
  "resolution": {"width": 16, "height": 16},
  "elements": [
    {
      "name": "cube_a",
      "box_uv": false,
      "render_order": "default",
      "locked": false,
      "allow_mirror_modeling": true,
      "from": [0, 0, 0],
      "to": [8, 8, 8],
      "autouv": 0,
      "color": 0,
      "origin": [4, 4, 4],
      "uv_offset": [0, 0],
      "inflate": 0,
      "faces": {"north": {"uv": [0, 0, 8, 8], "texture": 0, "rotation": 0}},
      "type": "cube",
      "uuid": "cube-a",
      "rotation": [0, 0, 0]
    },
    {
      "name": "cube_b",
      "box_uv": false,
      "render_order": "default",
      "locked": false,
      "allow_mirror_modeling": true,
      "from": [8, 8, 8],
      "to": [16, 16, 16],
      "autouv": 0,
      "color": 0,
      "origin": [12, 12, 12],
      "uv_offset": [0, 0],
      "inflate": 0,
      "faces": {"north": {"uv": [0, 0, 8, 8], "texture": 1, "rotation": 0}},
      "type": "cube",
      "uuid": "cube-b",
      "rotation": [0, 0, 0]
    }
  ],
  "outliner": [
    {
      "name": "root",
      "origin": [8, 8, 8],
      "rotation": [0, 0, 0],
      "uuid": "root-1",
      "export": true,
      "isOpen": true,
      "locked": false,
      "visibility": true,
      "mirror_uv": false,
      "color": 0,
      "autouv": 0,
      "shade": false,
      "children": ["cube-a", "cube-b"]
    }
  ],
  "textures": [
    {"path": "a.png", "name": "a", "folder": "", "namespace": "eyelib", "id": "0", "group": "", "width": 16, "height": 16, "uv_width": 16, "uv_height": 16, "particle": false, "use_as_default": false, "layers_enabled": false, "sync_to_project": "", "render_mode": "default", "render_sides": "auto"},
    {"path": "b.png", "name": "b", "folder": "", "namespace": "eyelib", "id": "1", "group": "", "width": 16, "height": 16, "uv_width": 16, "uv_height": 16, "particle": false, "use_as_default": false, "layers_enabled": false, "sync_to_project": "", "render_mode": "default", "render_sides": "auto"}
  ],
  "groups": []
}
```

- [ ] **Step 6: Run tests to verify they fail for the right reason**

Run: `./gradlew test --tests "io.github.tt432.eyelib.client.model.importer.BlockbenchModelCompatibilityTest" --tests "io.github.tt432.eyelib.client.model.importer.BlockbenchModelImporterTest"`

Expected: FAIL because `ModelImporter` does not exist yet or because current `BBModel` behavior still mixes parse and conversion semantics.

- [ ] **Step 7: Commit**

```bash
git add src/test/java/io/github/tt432/eyelib/client/model/importer/BlockbenchModelCompatibilityTest.java src/test/java/io/github/tt432/eyelib/client/model/importer/BlockbenchModelImporterTest.java src/test/resources/io/github/tt432/eyelib/client/model/importer/blockbench/minimal.bbmodel src/test/resources/io/github/tt432/eyelib/client/model/importer/blockbench/multi_texture.bbmodel
git commit -m "test: add blockbench importer regression coverage"
```

### Task 2: Split Blockbench parsing from runtime conversion

**Files:**
- Modify: `src/main/java/io/github/tt432/eyelib/client/model/bbmodel/BBModel.java:39-127`
- Modify: `src/main/java/io/github/tt432/eyelib/client/model/bbmodel/BBModelLoader.java:31-64`
- Modify: `src/main/java/io/github/tt432/eyelib/client/model/bbmodel/Element.java:69-232`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ModelImporter.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ModelImportException.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedModelData.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedBoneData.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedCubeData.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedFaceData.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedLocatorData.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/BlockbenchModelImporter.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedModelBuilder.java`

- [ ] **Step 1: Write the failing unit test for importer-owned conversion**

```java
@Test
void importerBuildsBoneHierarchyAndVisibleBoxFromBlockbenchSource() throws Exception {
    Model model = ModelImporter.importFile(Path.of("src/test/resources/io/github/tt432/eyelib/client/model/importer/blockbench/minimal.bbmodel")).get("geometry.test");

    assertNotNull(model.visibleBox());
    assertEquals(1, model.toplevelBones().size());
    assertEquals(1, model.allBones().values().iterator().next().cubes().size());
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests "io.github.tt432.eyelib.client.model.importer.BlockbenchModelImporterTest.importerBuildsBoneHierarchyAndVisibleBoxFromBlockbenchSource"`

Expected: FAIL because conversion still lives inside `BBModel` and the importer seam does not exist yet.

- [ ] **Step 3: Write the minimal importer seam and remove runtime side effects from `BBModel`**

```java
package io.github.tt432.eyelib.client.model.importer;

import io.github.tt432.eyelib.client.model.Model;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public final class ModelImporter {
    private ModelImporter() {
    }

    public static Map<String, Model> importFile(Path path) throws IOException {
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".bbmodel")) {
            return BlockbenchModelImporter.importFile(path);
        }
        if (fileName.endsWith(".json")) {
            return BedrockGeometryImporter.importFile(path);
        }
        throw new IOException("Unsupported model file: " + path);
    }
}
```

```java
public record BBModel(
        Meta meta,
        String name,
        String modelIdentifier,
        List<Double> visibleBox,
        Resolution resolution,
        List<Element> elements,
        List<Outliner> outliner,
        List<Texture> textures,
        List<Group> groups
) {
    public static final Codec<BBModel> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Meta.CODEC.fieldOf("meta").forGetter(BBModel::meta),
            Codec.STRING.fieldOf("name").forGetter(BBModel::name),
            Codec.STRING.fieldOf("model_identifier").forGetter(BBModel::modelIdentifier),
            Codec.DOUBLE.listOf().fieldOf("visible_box").forGetter(BBModel::visibleBox),
            Resolution.CODEC.fieldOf("resolution").forGetter(BBModel::resolution),
            Element.CODEC.listOf().fieldOf("elements").forGetter(BBModel::elements),
            Outliner.CODEC.listOf().fieldOf("outliner").forGetter(BBModel::outliner),
            Texture.CODEC.listOf().fieldOf("textures").forGetter(BBModel::textures),
            Group.CODEC.listOf().optionalFieldOf("groups", List.of()).forGetter(BBModel::groups)
    ).apply(ins, BBModel::new));
}
```

```java
public final class BlockbenchModelImporter {
    private BlockbenchModelImporter() {
    }

    public static Map<String, Model> importFile(Path path) throws IOException {
        BBModel source = new BBModelLoader().load(path);
        ImportedModelData imported = ImportedModelData.fromBlockbench(source);
        return Map.of(imported.name(), ImportedModelBuilder.build(imported));
    }
}
```

- [ ] **Step 4: Run the focused tests to verify they pass**

Run: `./gradlew test --tests "io.github.tt432.eyelib.client.model.importer.BlockbenchModelCompatibilityTest" --tests "io.github.tt432.eyelib.client.model.importer.BlockbenchModelImporterTest"`

Expected: PASS for the source parsing and basic importer conversion tests.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/io/github/tt432/eyelib/client/model/bbmodel/BBModel.java src/main/java/io/github/tt432/eyelib/client/model/bbmodel/BBModelLoader.java src/main/java/io/github/tt432/eyelib/client/model/bbmodel/Element.java src/main/java/io/github/tt432/eyelib/client/model/importer/ModelImporter.java src/main/java/io/github/tt432/eyelib/client/model/importer/ModelImportException.java src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedModelData.java src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedBoneData.java src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedCubeData.java src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedFaceData.java src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedLocatorData.java src/main/java/io/github/tt432/eyelib/client/model/importer/BlockbenchModelImporter.java src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedModelBuilder.java
git commit -m "refactor: separate blockbench parsing from model import"
```

### Task 3: Move multi-texture repack into importer post-processing

**Files:**
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedModelTextureRepacker.java`
- Create: `src/test/java/io/github/tt432/eyelib/client/model/importer/ImportedModelTextureRepackerTest.java`
- Modify: `src/main/java/io/github/tt432/eyelib/client/model/importer/BlockbenchModelImporter.java`
- Modify: `src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedModelData.java`

- [ ] **Step 1: Write the failing repack test**

```java
package io.github.tt432.eyelib.client.model.importer;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImportedModelTextureRepackerTest {
    @Test
    void repackerCollapsesMultipleBlockbenchTexturesIntoOneImportedModel() throws Exception {
        ImportedModelData data = BlockbenchModelImporter.importSource(Path.of("src/test/resources/io/github/tt432/eyelib/client/model/importer/blockbench/multi_texture.bbmodel"));
        ImportedModelData repacked = ImportedModelTextureRepacker.repack(data);

        assertEquals(1, repacked.textures().size());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew test --tests "io.github.tt432.eyelib.client.model.importer.ImportedModelTextureRepackerTest"`

Expected: FAIL because `ImportedModelTextureRepacker` and `importSource` do not exist yet.

- [ ] **Step 3: Implement importer-local repack and UV remap**

```java
public final class ImportedModelTextureRepacker {
    private ImportedModelTextureRepacker() {
    }

    public static ImportedModelData repack(ImportedModelData data) {
        if (data.textures().size() <= 1) {
            return data;
        }

        return data.repackTextures();
    }
}
```

```java
public final class BlockbenchModelImporter {
    private BlockbenchModelImporter() {
    }

    public static ImportedModelData importSource(Path path) throws IOException {
        BBModel source = new BBModelLoader().load(path);
        return ImportedModelData.fromBlockbench(source);
    }

    public static Map<String, Model> importFile(Path path) throws IOException {
        ImportedModelData source = importSource(path);
        ImportedModelData repacked = ImportedModelTextureRepacker.repack(source);
        return Map.of(repacked.name(), ImportedModelBuilder.build(repacked));
    }
}
```

- [ ] **Step 4: Run the repack and importer regression tests to verify they pass**

Run: `./gradlew test --tests "io.github.tt432.eyelib.client.model.importer.ImportedModelTextureRepackerTest" --tests "io.github.tt432.eyelib.client.model.importer.BlockbenchModelImporterTest"`

Expected: PASS and the multi-texture fixture now imports as one runtime model name.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedModelTextureRepacker.java src/main/java/io/github/tt432/eyelib/client/model/importer/BlockbenchModelImporter.java src/main/java/io/github/tt432/eyelib/client/model/importer/ImportedModelData.java src/test/java/io/github/tt432/eyelib/client/model/importer/ImportedModelTextureRepackerTest.java
git commit -m "feat: repack blockbench textures during import"
```

### Task 4: Add Bedrock geometry import and route manager tooling through the unified importer

**Files:**
- Create or restore: `src/main/java/io/github/tt432/eyelib/client/model/bedrock/BedrockGeometryModel.java`
- Create or restore: `src/main/java/io/github/tt432/eyelib/client/model/bedrock/BedrockModelLoader.java`
- Create: `src/main/java/io/github/tt432/eyelib/client/model/importer/BedrockGeometryImporter.java`
- Create: `src/test/java/io/github/tt432/eyelib/client/model/importer/BedrockGeometryImporterTest.java`
- Create: `src/test/resources/io/github/tt432/eyelib/client/model/importer/bedrock/minimal.geometry.json`
- Use existing fixture: `test.geo.json`
- Modify: `src/main/java/io/github/tt432/eyelib/client/gui/manager/reload/ManagerResourceImportPlanner.java:43-103`

- [ ] **Step 1: Write the failing Bedrock importer test**

```java
package io.github.tt432.eyelib.client.model.importer;

import io.github.tt432.eyelib.client.model.Model;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BedrockGeometryImporterTest {
    @Test
    void importsMinimalBedrockGeometryIntoRuntimeModel() throws Exception {
        Map<String, Model> imported = ModelImporter.importFile(Path.of("src/test/resources/io/github/tt432/eyelib/client/model/importer/bedrock/minimal.geometry.json"));

        assertEquals(1, imported.size());
        assertNotNull(imported.get("geometry.test"));
    }

    @Test
    void importsRealBedrockFixtureWithRotatedAndZeroDepthCubes() throws Exception {
        Map<String, Model> imported = ModelImporter.importFile(Path.of("test.geo.json"));

        assertEquals(1, imported.size());
        Model model = imported.get("geometry.test");
        assertNotNull(model);
        assertEquals(1, model.toplevelBones().size());
    }
}
```

- [ ] **Step 2: Add the failing Bedrock fixture**

```json
{
  "format_version": "1.12.0",
  "minecraft:geometry": [
    {
      "description": {
        "identifier": "geometry.test",
        "texture_width": 16,
        "texture_height": 16,
        "visible_bounds_width": 2,
        "visible_bounds_height": 2,
        "visible_bounds_offset": [0, 1, 0]
      },
      "bones": [
        {
          "name": "root",
          "pivot": [0, 0, 0],
          "cubes": [
            {
              "origin": [-8, 0, -8],
              "size": [16, 16, 16],
              "uv": [0, 0]
            }
          ]
        }
      ]
    }
  ]
}
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./gradlew test --tests "io.github.tt432.eyelib.client.model.importer.BedrockGeometryImporterTest"`

Expected: FAIL because the Bedrock source loader and importer adapter do not exist yet.

- [ ] **Step 4: Implement the Bedrock source loader and unified planner integration**

```java
public final class BedrockGeometryImporter {
    private BedrockGeometryImporter() {
    }

    public static Map<String, Model> importFile(Path path) throws IOException {
        BedrockGeometryModel source = new BedrockModelLoader().load(path);
        return source.toImportedModels();
    }
}
```

```java
if (relative.startsWith("models/")) {
    ClientAssetRegistry.publishModels(ModelImporter.importFile(file));
}
```

- [ ] **Step 5: Run the Bedrock and manager-facing model tests to verify they pass**

Run: `./gradlew test --tests "io.github.tt432.eyelib.client.model.importer.BedrockGeometryImporterTest" --tests "io.github.tt432.eyelib.client.model.importer.BlockbenchModelImporterTest"`

Expected: PASS and both source formats now enter runtime through `ModelImporter`, including the real-world `test.geo.json` fixture with rotated cubes, per-face `uv_size`, and zero-depth geometry.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/io/github/tt432/eyelib/client/model/bedrock/BedrockGeometryModel.java src/main/java/io/github/tt432/eyelib/client/model/bedrock/BedrockModelLoader.java src/main/java/io/github/tt432/eyelib/client/model/importer/BedrockGeometryImporter.java src/main/java/io/github/tt432/eyelib/client/gui/manager/reload/ManagerResourceImportPlanner.java src/test/java/io/github/tt432/eyelib/client/model/importer/BedrockGeometryImporterTest.java src/test/resources/io/github/tt432/eyelib/client/model/importer/bedrock/minimal.geometry.json
git commit -m "feat: unify bedrock and blockbench model import"
```

### Task 5: Update documentation and run full verification

**Files:**
- Modify: `MODULES.md`
- Create if missing: `src/main/java/io/github/tt432/eyelib/client/model/README.md`
- Modify: `src/main/java/io/github/tt432/eyelib/client/gui/manager/README.md`
- Modify: `src/main/java/io/github/tt432/eyelib/client/loader/README.md`

- [ ] **Step 1: Update module inventory and package guidance**

```md
| Client model domain | model structures, bake/runtime data, locators, source parsing, and importer-backed Blockbench/Bedrock -> intermediate-model conversion | `src/main/java/io/github/tt432/eyelib/client/model/` | used by render pipeline, loaders, and manager import flow |
```

```md
## Scope
- Path: `src/main/java/io/github/tt432/eyelib/client/model/`
- Covers runtime `Model`, locators, bake data, source format records, and importer-backed conversion from Blockbench and Bedrock model files.
```

- [ ] **Step 2: Run focused importer tests**

Run: `./gradlew test --tests "io.github.tt432.eyelib.client.model.importer.BlockbenchModelCompatibilityTest" --tests "io.github.tt432.eyelib.client.model.importer.BlockbenchModelImporterTest" --tests "io.github.tt432.eyelib.client.model.importer.ImportedModelTextureRepackerTest" --tests "io.github.tt432.eyelib.client.model.importer.BedrockGeometryImporterTest"`

Expected: PASS with all importer regression tests green.

- [ ] **Step 3: Run the full test task**

Run: `./gradlew test`

Expected: exit code `0`.

- [ ] **Step 4: Run compile verification**

Run: `./gradlew compileJava`

Expected: exit code `0`.

- [ ] **Step 5: Run null-safety verification if affected code triggers NullAway paths**

Run: `./gradlew nullawayMain`

Expected: exit code `0`.

- [ ] **Step 6: Commit**

```bash
git add MODULES.md src/main/java/io/github/tt432/eyelib/client/model/README.md src/main/java/io/github/tt432/eyelib/client/gui/manager/README.md src/main/java/io/github/tt432/eyelib/client/loader/README.md
git commit -m "docs: document unified model import boundary"
```

## Self-Review

### Spec coverage
- Unified importer boundary: covered by Tasks 2-4.
- Blockbench compatibility with `../blockbench`: covered by Task 1 source parsing tests.
- Texture repack as importer post-process: covered by Task 3.
- Bedrock geometry support: covered by Task 4.
- Real Bedrock regression coverage from repository fixture: covered by Task 4.
- Manager planner routing through importer: covered by Task 4.
- Docs and module boundary updates: covered by Task 5.

### Placeholder scan
- No `TODO`, `TBD`, or deferred “implement later” steps remain.
- Every validation step includes an exact Gradle command.
- Every task lists concrete files.

### Type consistency
- The plan consistently uses `ModelImporter.importFile(Path)` as the shared entrypoint.
- Blockbench source parsing stays in `BBModelLoader` and Bedrock source parsing stays in `BedrockModelLoader`.
- Runtime publication consistently remains in `ClientAssetRegistry`.
