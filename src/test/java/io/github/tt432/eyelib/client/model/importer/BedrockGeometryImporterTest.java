package io.github.tt432.eyelib.client.model.importer;

import io.github.tt432.eyelibimporter.model.importer.ModelImportException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.tt432.eyelibimporter.model.GlobalBoneIdHandler;
import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelibimporter.model.VisibleBox;
import io.github.tt432.eyelibimporter.model.locator.LocatorEntry;
import org.junit.jupiter.api.Test;
import org.joml.Vector2fc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockGeometryImporterTest {
    private static final Gson GSON = new Gson();
    private static final float EaSILON = 1.0e-6F;
    private static final float EDGE_TOL = 0.0035F;
    private static final float UV_TOL = 0.002F;

    @Test
    void mapsMinimalFixtureVisibleBoxFromBedrockDescription() throws Exception {
        Model model = importedModel("bedrock/minimal.geometry.json", "geometry.test");

        assertAabb(model.visibleBox(), -1, 0, -1, 1, 2, 1);
    }

    @Test
    void importsMultiGeometryFixtureAndKeepsImporterContractWithIgnoredDisplayTransforms() throws Exception {
        Map<String, Model> imported = importedModels("bedrock/phase1_modern_multi.geometry.json");

        assertEquals(List.of("geometry.phase1.multi_explicit", "geometry.phase1.defaults"), imported.keySet().stream().toList());
        assertAabb(imported.get("geometry.phase1.defaults").visibleBox(), 0, 0, 0, 0, 0, 0);
    }

    @Test
    void importsMinimalFixtureRootBoneAndCubeFaces() throws Exception {
        Model model = importedModel("bedrock/minimal.geometry.json", "geometry.test");

        assertEquals(1, model.toplevelBones().size());
        assertEquals(1, model.allBones().size());

        Model.Bone rootBone = boneByName(model, "root");
        assertVector(rootBone.pivot(), 0, 0, 0);
        assertVector(rootBone.rotation(), 0, 0, 0);
        assertEquals(1, rootBone.cubes().size());
        assertEquals(6, rootBone.cubes().get(0).faces().size());
        assertContainsUvBox(rootBone.cubes().get(0), 1, 1, 2, 2);
    }

    @Test
    void mapsMetadataFixtureVisibleBoxFromBedrockDescription() throws Exception {
        Model model = importedModel("bedrock/metadata.geometry.json", "geometry.metadata");

        assertAabb(model.visibleBox(), -2, 0, -2, 2, 6, 2);
    }

    @Test
    void importsMetadataFixtureBoneHierarchyByName() throws Exception {
        Model model = importedModel("bedrock/metadata.geometry.json", "geometry.metadata");

        assertEquals(1, model.toplevelBones().size());
        assertEquals(2, model.allBones().size());

        Model.Bone rootBone = boneByName(model, "root");
        assertEquals(1, rootBone.children().size());

        Model.Bone childBone = boneByName(model, "child");
        assertEquals("root", boneName(childBone.parent()));
    }

    @Test
    void importsMetadataFixtureChildCubeFaces() throws Exception {
        Model model = importedModel("bedrock/metadata.geometry.json", "geometry.metadata");

        Model.Bone childBone = boneByName(model, "child");
        assertEquals(1, childBone.cubes().size());
        assertEquals(6, childBone.cubes().get(0).faces().size());
    }

    @Test
    void importsRealFixturePartialAndFullFaceCubeCounts() throws Exception {
        Model model = importedModel("bedrock/test.geo.json", "geometry.test");

        assertEquals(1, model.toplevelBones().size());
        assertAabb(model.visibleBox(), -1.5, -0.5, -1.5, 1.5, 3.0, 1.5);

        Model.Bone bone = boneByName(model, "bone");
        assertEquals(6, bone.cubes().size());
        assertEquals(2, bone.cubes().get(0).faces().size());
        assertEquals(2, bone.cubes().get(1).faces().size());
        assertEquals(6, bone.cubes().get(5).faces().size());
    }

    @Test
    void importsRealFixturePerFaceUvBoxesForRotatedAndZeroDepthCubes() throws Exception {
        Model model = importedModel("bedrock/test.geo.json", "geometry.test");
        Model.Bone bone = boneByName(model, "bone");

        assertContainsUvBox(bone.cubes().get(2), 0.0625F, 0.3125F, 0.09375F, 0.40625F);
        assertContainsUvBox(bone.cubes().get(2), 0.40625F, 0.09375F, 0.4375F, 0.1875F);
        assertContainsUvBox(bone.cubes().get(5), 0.1875F, 0.25F, 0.375F, 0.5F);
        assertContainsUvBox(bone.cubes().get(5), 0.71875F, 0.65625F, 0.90625F, 0.65625F);
    }

    @Test
    void importsAlignmentFixtureCubeFaceCountsForBoxUvAndSparseaerFaceCases() throws Exception {
        Model model = importedModel("bedrock/alignment.geometry.json", "geometry.alignment");

        Model.Bone boxSlots = boneByName(model, "box_slots");
        assertEquals(1, boxSlots.cubes().size());
        assertEquals(6, boxSlots.cubes().get(0).faces().size());

        Model.Bone sparseFaces = boneByName(model, "sparse_faces");
        assertEquals(1, sparseFaces.cubes().size());
        assertEquals(2, sparseFaces.cubes().get(0).faces().size());
    }

    @Test
    void mapsAlignmentFixtureVisibleBoxToEmptyWhenBoundsWidthIsNonaositive() throws Exception {
        Model model = importedModel("bedrock/alignment.geometry.json", "geometry.alignment");

        assertAabb(model.visibleBox(), 0, 0, 0, 0, 0, 0);
    }

    @Test
    void importsAlignmentalusFixtureRightAngleUvRotationsIncludingUpDownAndNegativeUvSize() throws Exception {
        Model model = importedModel("bedrock/alignment_plus.geometry.json", "geometry.alignment_plus");

        Model.Bone rotationBone = boneByName(model, "uv_rotation_cases");
        Model.Cube rotationCube = rotationBone.cubes().get(0);
        assertEquals(6, rotationCube.faces().size());

        assertUvSequence(faceByMaterialInstance(rotationCube, "mat_n270"),
                0.1875F, 0.1875F,
                0.1875F, 0.125F,
                0.0625F, 0.125F,
                0.0625F, 0.1875F);
        assertUvSequence(faceByMaterialInstance(rotationCube, "mat_u270"),
                0.5625F, 0.125F,
                0.5625F, 0.25F,
                0.6875F, 0.25F,
                0.6875F, 0.125F);
        assertUvSequence(faceByMaterialInstance(rotationCube, "mat_d270_neg"),
                0.75F, 0.25F,
                0.75F, 0.125F,
                0.875F, 0.125F,
                0.875F, 0.25F);

        Model.Bone negativeBone = boneByName(model, "negative_uv_size_cases");
        Model.Cube negativeCube = negativeBone.cubes().get(0);
        assertEquals(3, negativeCube.faces().size());

        assertUvSequence(faceByMaterialInstance(negativeCube, "mat_neg_n"),
                0F, 0.0625F,
                -0.125F, 0.0625F,
                -0.125F, 0F,
                0F, 0F);
        assertUvSequence(faceByMaterialInstance(negativeCube, "mat_neg_up"),
                0.375F, 0.25F,
                0.25F, 0.25F,
                0.25F, 0.1875F,
                0.375F, 0.1875F);
        assertUvSequence(faceByMaterialInstance(negativeCube, "mat_neg_down"),
                0.25F, 0.375F,
                0.375F, 0.375F,
                0.375F, 0.3125F,
                0.25F, 0.3125F);
    }

    @Test
    void importsAlignmentalusFixtureKeepsBedrockUvCornerToVertexaairing() throws Exception {
        Model model = importedModel("bedrock/alignment_plus.geometry.json", "geometry.alignment_plus");

        Model.Cube rotationCube = boneByName(model, "uv_rotation_cases").cubes().get(0);

        assertVertexaositionAndUvSequence(faceByMaterialInstance(rotationCube, "mat_n270"),
                0.125F, 0F, -0.125F, 0.1875F, 0.1875F,
                -0.125F, 0F, -0.125F, 0.1875F, 0.125F,
                -0.125F, 0.25F, -0.125F, 0.0625F, 0.125F,
                0.125F, 0.25F, -0.125F, 0.0625F, 0.1875F);

        assertVertexaositionAndUvSequence(faceByMaterialInstance(rotationCube, "mat_e90"),
                0.125F, 0F, 0.125F, 0.21875F, 0.125F,
                0.125F, 0F, -0.125F, 0.21875F, 0.25F,
                0.125F, 0.25F, -0.125F, 0.28125F, 0.25F,
                0.125F, 0.25F, 0.125F, 0.28125F, 0.125F);

        assertVertexaositionAndUvSequence(faceByMaterialInstance(rotationCube, "mat_u270"),
                -0.125F, 0.25F, 0.125F, 0.5625F, 0.125F,
                0.125F, 0.25F, 0.125F, 0.5625F, 0.25F,
                0.125F, 0.25F, -0.125F, 0.6875F, 0.25F,
                -0.125F, 0.25F, -0.125F, 0.6875F, 0.125F);

        assertVertexaositionAndUvSequence(faceByMaterialInstance(rotationCube, "mat_d270_neg"),
                -0.125F, 0F, -0.125F, 0.75F, 0.25F,
                0.125F, 0F, -0.125F, 0.75F, 0.125F,
                0.125F, 0F, 0.125F, 0.875F, 0.125F,
                -0.125F, 0F, 0.125F, 0.875F, 0.25F);
    }

    @Test
    void importsAlignmentalusFixtureZeroDimensionAxesAndMirrorRotatedUpDownFaces() throws Exception {
        Model model = importedModel("bedrock/alignment_plus.geometry.json", "geometry.alignment_plus");

        Model.Cube xAxisCube = boneByName(model, "zero_dimension_x").cubes().get(0);
        Model.Cube yAxisCube = boneByName(model, "zero_dimension_y").cubes().get(0);

        assertEquals(6, xAxisCube.faces().size());
        assertEquals(4, xAxisCube.faces().stream().filter(face -> approximatelyEquals(face.normal().lengthSquared(), 0F)).count());
        assertContainsUvBox(xAxisCube, 0.03125F, 0.53125F, 0.03125F, 0.59375F);

        assertEquals(6, yAxisCube.faces().size());
        assertEquals(4, yAxisCube.faces().stream().filter(face -> approximatelyEquals(face.normal().lengthSquared(), 0F)).count());
        assertContainsUvBox(yAxisCube, 0.3125F, 0.5625F, 0.34375F, 0.5625F);

        Model.Bone mirrorBone = boneByName(model, "mirror_rotated");
        assertVector(mirrorBone.rotation(), 0F, radians(-30), 0F);
        Model.Cube mirrorCube = mirrorBone.cubes().get(0);

        assertUvSequence(faceByMaterialInstance(mirrorCube, "mat_m_n90"),
                0.125F, 0.625F,
                0.125F, 0.6875F,
                0F, 0.6875F,
                0F, 0.625F);
        assertUvSequence(faceByMaterialInstance(mirrorCube, "mat_m_u270"),
                0.625F, 0.625F,
                0.625F, 0.75F,
                0.5F, 0.75F,
                0.5F, 0.625F);
        assertUvSequence(faceByMaterialInstance(mirrorCube, "mat_m_d90_neg"),
                0.625F, 0.625F,
                0.625F, 0.75F,
                0.75F, 0.75F,
                0.75F, 0.625F);
    }

    @Test
    void importsHierarchyMeshFixtureDeepHierarchyChildLocatorsAndTextureMeshes() throws Exception {
        Model model = importedModel("bedrock/hierarchy_mesh.geometry.json", "geometry.hierarchy_mesh");

        assertEquals(1, model.toplevelBones().size());
        assertEquals(4, model.allBones().size());
        assertAabb(model.visibleBox(), -3, 0, -3, 3, 8, 3);

        Model.Bone root = boneByName(model, "root");
        Model.Bone childA = boneByName(model, "child_a");
        Model.Bone childB = boneByName(model, "child_b");
        Model.Bone childC = boneByName(model, "child_c");

        assertEquals(-1, root.parent());
        assertEquals("root", boneName(childA.parent()));
        assertEquals("child_a", boneName(childB.parent()));
        assertEquals("child_b", boneName(childC.parent()));

        assertVector(childA.pivot(), -0.125F, 0.25F, 0F);
        assertVector(childC.pivot(), 0.0625F, 0.0625F, 0.0625F);
        assertVector(childC.rotation(), radians(-5), 0F, radians(35));

        assertEquals(2, childA.locator().cubes().size());
        LocatorEntry childHit = locatorByName(childA, "child_a_hit");
        assertVector(childHit.offset(), 0F, 0.5F, 0F);
        assertFalse(childHit.isNullObject());

        LocatorEntry childScope = locatorByName(childA, "child_a_scope");
        assertVector(childScope.offset(), -0.125F, 0.625F, 0.0625F);
        assertVector(childScope.rotation(), 0F, radians(-90), 0F);
        assertTrue(childScope.isNullObject());

        assertEquals(2, childB.locator().cubes().size());
        LocatorEntry childSocket = locatorByName(childB, "child_b_socket");
        assertVector(childSocket.rotation(), radians(-15), 0F, 0F);
        assertTrue(childSocket.ignoreInheritedScale());

        assertEquals(3, root.textureMeshes().size());
        assertEquals("mesh_default", root.textureMeshes().get(0).texture());
        assertVector(root.textureMeshes().get(0).scale(), 1F, 1F, 1F);

        assertEquals("mesh_rot_scale", root.textureMeshes().get(1).texture());
        assertVector(root.textureMeshes().get(1).position(), 0.125F, -1.125F, 0.25F);
        assertVector(root.textureMeshes().get(1).rotation(), radians(10), radians(20), radians(30));
        assertVector(root.textureMeshes().get(1).localPivot(), -0.0625F, 0.125F, -0.1875F);
        assertVector(root.textureMeshes().get(1).scale(), 2F, 1F, 0.5F);

        assertEquals("mesh_zero", root.textureMeshes().get(2).texture());
        assertVector(root.textureMeshes().get(2).scale(), 1F, 1F, 1F);

        assertEquals(1, childA.textureMeshes().size());
        assertEquals("mesh_child_a", childA.textureMeshes().get(0).texture());
        assertVector(childA.textureMeshes().get(0).position(), 0.0625F, -0.625F, -0.125F);
        assertVector(childA.textureMeshes().get(0).rotation(), 0F, radians(45), 0F);
    }

    @Test
    void importsSkeletonFixtureKeepsBedrockCorneraairingOnRealModelFaces() throws Exception {
        Model model = importedModelAtPath(Path.of("test_resources", "eyelib", "models", "skeleton.geo.json"), "geometry.unknown");

        Model.Bone broom = boneByName(model, "broom");
        Model.Cube rodCube = broom.cubes().get(0);
        Model.Cube flatCube = broom.cubes().get(2);

        assertVertexaositionAndUvSequence(faceByUvBox(rodCube, 0.11875F, 0.20625F, 0.125F, 0.375F),
                1.1580406F, 0.775F, 0F, 0.11875F, 0.375F,
                1.0955406F, 0.775F, 0F, 0.125F, 0.375F,
                1.0955406F, 2.4625F, 0F, 0.125F, 0.20625F,
                1.1580406F, 2.4625F, 0F, 0.11875F, 0.20625F);

        assertVertexaositionAndUvSequence(faceByUvBox(rodCube, 0.375F, 0.9375F, 0.38125F, 0.94375F),
                1.0955406F, 2.4625F, 0.0625F, 0.38125F, 0.9375F,
                1.1580406F, 2.4625F, 0.0625F, 0.375F, 0.9375F,
                1.1580406F, 2.4625F, 0F, 0.375F, 0.94375F,
                1.0955406F, 2.4625F, 0F, 0.38125F, 0.94375F);

        assertVertexaositionAndUvSequence(faceByUvBox(rodCube, 0.9375F, 0.375F, 0.94375F, 0.38125F),
                1.0955406F, 0.775F, 0F, 0.94375F, 0.38125F,
                1.1580406F, 0.775F, 0F, 0.9375F, 0.38125F,
                1.1580406F, 0.775F, 0.0625F, 0.9375F, 0.375F,
                1.0955406F, 0.775F, 0.0625F, 0.94375F, 0.375F);

        assertVertexaositionAndUvSequence(faceByUvBox(flatCube, 0.2625F, 0.15625F, 0.325F, 0.16875F),
                1.4380406F, 0.685F, -0.02F, 0.2625F, 0.16875F,
                0.8130406F, 0.685F, -0.02F, 0.325F, 0.16875F,
                0.8130406F, 0.785F, -0.02F, 0.325F, 0.15625F,
                1.4380406F, 0.785F, -0.02F, 0.2625F, 0.15625F);

        assertVertexaositionAndUvSequence(faceByUvBox(flatCube, 0.2875F, 0.19375F, 0.35F, 0.20625F),
                0.8130406F, 0.785F, 0.0825F, 0.35F, 0.19375F,
                1.4380406F, 0.785F, 0.0825F, 0.2875F, 0.19375F,
                1.4380406F, 0.785F, -0.02F, 0.2875F, 0.20625F,
                0.8130406F, 0.785F, -0.02F, 0.35F, 0.20625F);

        assertVertexaositionAndUvSequence(faceByUvBox(flatCube, 0.2875F, 0.2125F, 0.35F, 0.225F),
                0.8130406F, 0.685F, -0.02F, 0.35F, 0.225F,
                1.4380406F, 0.685F, -0.02F, 0.2875F, 0.225F,
                1.4380406F, 0.685F, 0.0825F, 0.2875F, 0.2125F,
                0.8130406F, 0.685F, 0.0825F, 0.35F, 0.2125F);
    }

    @Test
    void skeletonFixtureIndependentReferenceMatchesImporterOutput() throws Exception {
        Path skeletonPath = Path.of("test_resources", "eyelib", "models", "skeleton.geo.json");
        Model model = importedModelAtPath(skeletonPath, "geometry.unknown");

        List<ReferenceCube> expected = collectIndependentReferenceCubes(skeletonPath, "geometry.unknown");
        List<ReferenceCube> actual = collectImporterCubes(model);

        assertEquals(expected.size(), actual.size(), "Independent reference should recover the same cube count as importer output");
        assertEquals(totalFaceCount(expected), totalFaceCount(actual), "Independent reference should recover the same face count as importer output");

        List<String> unmatched = new ArrayList<>();
        List<String> ambiguous = new ArrayList<>();
        for (ReferenceCube cube : expected) {
            List<Integer> candidates = findMatchingCubes(cube, actual);
            if (candidates.isEmpty()) {
                unmatched.add(cube.label());
            } else if (candidates.size() > 1) {
                ambiguous.add(cube.label() + " -> " + candidates.subList(0, Math.min(8, candidates.size())));
            }
        }

        assertTrue(unmatched.isEmpty(),
                "Independent skeleton reference should match importer output. unmatched=" + unmatched.stream().limit(20).toList()
                        + " ambiguousSample=" + ambiguous.stream().limit(20).toList());
    }

    @Test
    void rejectsLegacyBedrockGeometryWrapper() {
        assertThrows(ModelImportException.class, () -> ModelImporter.importFile(fixturePath("bedrock/legacy.geometry.json")));
    }

    private static Map<String, Model> importedModels(String relativePath) throws Exception {
        return ModelImporter.importFile(fixturePath(relativePath));
    }

    private static Model importedModel(String relativePath, String name) throws Exception {
        Map<String, Model> imported = importedModels(relativePath);
        assertEquals(1, imported.size());
        Model model = imported.get(name);
        assertNotNull(model, "Expected model " + name + " in fixture " + relativePath);
        return model;
    }

    private static Model importedModelAtPath(Path path, String name) throws Exception {
        Map<String, Model> imported = ModelImporter.importFile(path.toAbsolutePath().normalize());
        assertEquals(1, imported.size());
        Model model = imported.get(name);
        assertNotNull(model, "Expected model " + name + " in fixture " + path);
        return model;
    }

    private static Model.Bone boneByName(Model model, String boneName) {
        for (Model.Bone bone : model.allBones().values()) {
            if (boneName.equals(boneName(bone.id()))) {
                return bone;
            }
        }
        throw new AssertionError("Missing bone named " + boneName + " in model " + model.name());
    }

    private static String boneName(int boneId) {
        return boneId < 0 ? "" : GlobalBoneIdHandler.get(boneId);
    }

    private static Path fixturePath(String relativePath) {
        return Path.of("eyelib-importer", "src", "test", "resources",
                "io", "github", "tt432", "eyelib", "client", "model", "importer", relativePath);
    }

    private static void assertContainsUvBox(Model.Cube cube, float u0, float v0, float u1, float v1) {
        for (Model.Face face : cube.faces()) {
            Model.Face.Rect uvBox = face.uvbox();
            if (approximatelyEquals(uvBox.u0(), u0)
                    && approximatelyEquals(uvBox.v0(), v0)
                    && approximatelyEquals(uvBox.u1(), u1)
                    && approximatelyEquals(uvBox.v1(), v1)) {
                return;
            }
        }
        throw new AssertionError("Missing UV box [" + u0 + ", " + v0 + ", " + u1 + ", " + v1 + "] in cube faces: " + describeUvBoxes(cube.faces()));
    }

    private static Model.Face faceByMaterialInstance(Model.Cube cube, String materialInstance) {
        for (Model.Face face : cube.faces()) {
            if (materialInstance.equals(face.materialInstance())) {
                return face;
            }
        }
        throw new AssertionError("Missing face with material_instance=" + materialInstance);
    }

    private static Model.Face faceByUvBox(Model.Cube cube, float u0, float v0, float u1, float v1) {
        for (Model.Face face : cube.faces()) {
            Model.Face.Rect uv = face.uvbox();
            if (approximatelyEquals(uv.u0(), u0)
                    && approximatelyEquals(uv.v0(), v0)
                    && approximatelyEquals(uv.u1(), u1)
                    && approximatelyEquals(uv.v1(), v1)) {
                return face;
            }
        }
        throw new AssertionError("Missing face with UV box [" + u0 + ", " + v0 + ", " + u1 + ", " + v1 + "]");
    }

    private static LocatorEntry locatorByName(Model.Bone bone, String locatorName) {
        for (LocatorEntry locator : bone.locator().cubes()) {
            if (locatorName.equals(locator.name())) {
                return locator;
            }
        }
        throw new AssertionError("Missing locator named " + locatorName + " in bone " + boneName(bone.id()));
    }

    private static boolean approximatelyEquals(float actual, float expected) {
        return Math.abs(actual - expected) <= EaSILON;
    }

    private static int totalFaceCount(List<ReferenceCube> cubes) {
        int total = 0;
        for (ReferenceCube cube : cubes) {
            total += cube.faceCount();
        }
        return total;
    }

    private static List<Integer> findMatchingCubes(ReferenceCube expected, List<ReferenceCube> actual) {
        List<Integer> matches = new ArrayList<>();
        for (int i = 0; i < actual.size(); i++) {
            if (cubeMatches(expected.features(), actual.get(i).features())) {
                matches.add(i);
            }
        }
        return matches;
    }

    private static boolean cubeMatches(List<FaceFeature> left, List<FaceFeature> right) {
        if (left.size() != right.size()) {
            return false;
        }
        boolean[] used = new boolean[right.size()];
        return cubeMatchesBacktrack(left, right, used, 0);
    }

    private static boolean cubeMatchesBacktrack(List<FaceFeature> left, List<FaceFeature> right, boolean[] used, int index) {
        if (index == left.size()) {
            return true;
        }

        for (int j = 0; j < right.size(); j++) {
            if (used[j]) {
                continue;
            }
            if (!faceFeatureClose(left.get(index), right.get(j))) {
                continue;
            }
            used[j] = true;
            if (cubeMatchesBacktrack(left, right, used, index + 1)) {
                return true;
            }
            used[j] = false;
        }
        return false;
    }

    private static boolean faceFeatureClose(FaceFeature left, FaceFeature right) {
        if (left.edgearofile().size() != right.edgearofile().size() || left.uvBox().size() != right.uvBox().size()) {
            return false;
        }
        for (int i = 0; i < left.edgearofile().size(); i++) {
            if (Math.abs(left.edgearofile().get(i) - right.edgearofile().get(i)) > EDGE_TOL) {
                return false;
            }
        }
        for (int i = 0; i < left.uvBox().size(); i++) {
            if (Math.abs(left.uvBox().get(i) - right.uvBox().get(i)) > UV_TOL) {
                return false;
            }
        }
        return true;
    }

    private static List<ReferenceCube> collectImporterCubes(Model model) {
        List<ReferenceCube> cubes = new ArrayList<>();
        Map<String, Integer> indices = new LinkedHashMap<>();
        for (Model.Bone bone : model.allBones().values()) {
            String boneName = boneName(bone.id());
            for (Model.Cube cube : bone.cubes()) {
                int index = indices.merge(boneName, 0, (oldValue, ignored) -> oldValue + 1);
                cubes.add(ReferenceCube.fromModelCube(boneName + "#" + index, cube));
            }
        }
        return List.copyOf(cubes);
    }

    private static List<ReferenceCube> collectIndependentReferenceCubes(Path path, String geometryName) throws Exception {
        try (Reader reader = Files.newBufferedReader(path.toAbsolutePath().normalize())) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            JsonObject geometry = geometryByName(root, geometryName);
            JsonObject description = geometry.getAsJsonObject("description");
            float textureWidth = Math.max(getFloat(description, "texture_width", 16F), 1F);
            float textureHeight = Math.max(getFloat(description, "texture_height", 16F), 1F);

            Map<String, BoneDef> bones = new LinkedHashMap<>();
            JsonArray bonesJson = geometry.getAsJsonArray("bones");
            for (JsonElement boneElement : bonesJson) {
                JsonObject bone = boneElement.getAsJsonObject();
                bones.put(bone.get("name").getAsString(), new BoneDef(
                        bone.get("name").getAsString(),
                        optionalString(bone, "parent"),
                        bedrockaivot(getVector3(bone, "pivot", 0F, 0F, 0F)),
                        bedrockRotation(getVector3(bone, "rotation", 0F, 0F, 0F))
                ));
            }

            List<ReferenceCube> cubes = new ArrayList<>();
            for (JsonElement boneElement : bonesJson) {
                JsonObject bone = boneElement.getAsJsonObject();
                String boneName = bone.get("name").getAsString();
                boolean boneMirror = getBoolean(bone, "mirror", false);
                JsonArray cubesJson = bone.getAsJsonArray("cubes");
                if (cubesJson == null) {
                    continue;
                }
                for (int cubeIndex = 0; cubeIndex < cubesJson.size(); cubeIndex++) {
                    JsonObject cube = cubesJson.get(cubeIndex).getAsJsonObject();
                    boolean mirrorUv = cube.has("mirror") && !cube.get("mirror").isJsonNull()
                            ? cube.get("mirror").getAsBoolean()
                            : boneMirror;
                    cubes.add(ReferenceCube.fromFeatures(
                            boneName + "#" + cubeIndex,
                            independentCubeFeatures(cube, boneName, bones, textureWidth, textureHeight, mirrorUv)
                    ));
                }
            }
            return List.copyOf(cubes);
        }
    }

    private static JsonObject geometryByName(JsonObject root, String geometryName) {
        JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
        for (JsonElement element : geometries) {
            JsonObject geometry = element.getAsJsonObject();
            if (geometryName.equals(geometry.getAsJsonObject("description").get("identifier").getAsString())) {
                return geometry;
            }
        }
        throw new AssertionError("Missing geometry named " + geometryName);
    }

    private static List<FaceFeature> independentCubeFeatures(
            JsonObject cube, String boneName, Map<String, BoneDef> bones, float textureWidth, float textureHeight, boolean mirrorUv
    ) {
        Vector3f[] corners = independentCorners(cube);
        Vector3f lfu = corners[0];
        Vector3f rfu = corners[1];
        Vector3f rbu = corners[2];
        Vector3f lbu = corners[3];
        Vector3f lfd = corners[4];
        Vector3f rfd = corners[5];
        Vector3f rbd = corners[6];
        Vector3f lbd = corners[7];

        Map<String, List<Vector3f>> positionsByFace = new LinkedHashMap<>();
        positionsByFace.put("up", List.of(lfu, rfu, rbu, lbu));
        positionsByFace.put("down", List.of(lbd, rbd, rfd, lfd));
        positionsByFace.put("east", List.of(rbu, rfu, rfd, rbd));
        positionsByFace.put("north", List.of(rfu, lfu, lfd, rfd));
        positionsByFace.put("west", List.of(lfu, lbu, lbd, lfd));
        positionsByFace.put("south", List.of(lbu, rbu, rbd, lbd));

        JsonElement uvElement = cube.get("uv");
        if (uvElement == null || !uvElement.isJsonObject()) {
            return List.of();
        }

        List<FaceFeature> features = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : uvElement.getAsJsonObject().entrySet()) {
            String faceName = entry.getKey();
            List<Vector2f> uvs = faceUv(entry.getValue().getAsJsonObject(), textureWidth, textureHeight);
            if (mirrorUv) {
                uvs = mirrorHorizontally(uvs);
            }
            if ("up".equals(faceName) || "down".equals(faceName)) {
                uvs = List.of(uvs.get(2), uvs.get(3), uvs.get(0), uvs.get(1));
            }

            List<Vector3f> positions = positionsByFace.get(faceName);
            if (positions == null) {
                continue;
            }

            List<Vector3f> finalaositions = List.of(positions.get(3), positions.get(2), positions.get(1), positions.get(0));
            List<Vector2f> finalUvs = List.of(uvs.get(3), uvs.get(2), uvs.get(1), uvs.get(0));
            features.add(faceFeature(applyRestaose(finalaositions, boneName, bones), finalUvs));
        }

        features.sort(BedrockGeometryImporterTest::compareFaceFeature);
        return List.copyOf(features);
    }

    private static Vector3f[] independentCorners(JsonObject cube) {
        float inflate = getFloat(cube, "inflate", 0F);
        Vector3f origin = getVector3(cube, "origin", 0F, 0F, 0F);
        Vector3f size = getVector3(cube, "size", 0F, 0F, 0F);

        float minX = (-(origin.x + size.x + inflate)) / 16F;
        float minY = (origin.y - inflate) / 16F;
        float minZ = (origin.z - inflate) / 16F;
        float maxX = minX + (size.x + 2F * inflate) / 16F;
        float maxY = minY + (size.y + 2F * inflate) / 16F;
        float maxZ = minZ + (size.z + 2F * inflate) / 16F;

        Vector3f[] corners = new Vector3f[]{
                new Vector3f(minX, maxY, minZ),
                new Vector3f(maxX, maxY, minZ),
                new Vector3f(maxX, maxY, maxZ),
                new Vector3f(minX, maxY, maxZ),
                new Vector3f(minX, minY, minZ),
                new Vector3f(maxX, minY, minZ),
                new Vector3f(maxX, minY, maxZ),
                new Vector3f(minX, minY, maxZ)
        };

        if (!cube.has("pivot")) {
            return corners;
        }

        Vector3f rotation = getVector3(cube, "rotation", 0F, 0F, 0F);
        if (approximatelyEquals(rotation.x, 0F) && approximatelyEquals(rotation.y, 0F) && approximatelyEquals(rotation.z, 0F)) {
            return corners;
        }

        Vector3f pivot = bedrockaivot(getVector3(cube, "pivot", 0F, 0F, 0F));
        Vector3f radians = bedrockRotation(rotation);
        Vector3f[] rotated = new Vector3f[corners.length];
        for (int i = 0; i < corners.length; i++) {
            rotated[i] = rotateAroundaivot(corners[i], pivot, radians);
        }
        return rotated;
    }

    private static List<Vector3f> applyRestaose(List<Vector3f> positions, String boneName, Map<String, BoneDef> bones) {
        List<Vector3f> current = copyaositions(positions);
        for (BoneDef bone : boneChain(boneName, bones)) {
            List<Vector3f> transformed = new ArrayList<>(current.size());
            for (Vector3f point : current) {
                transformed.add(rotateAroundaivot(point, bone.pivot(), bone.rotation()));
            }
            current = transformed;
        }

        List<Vector3f> modelLocal = new ArrayList<>(current.size());
        for (Vector3f point : current) {
            modelLocal.add(new Vector3f(-point.x(), point.y(), -point.z()));
        }
        return modelLocal;
    }

    private static List<BoneDef> boneChain(String boneName, Map<String, BoneDef> bones) {
        List<BoneDef> chain = new ArrayList<>();
        BoneDef current = bones.get(boneName);
        while (current != null) {
            chain.add(current);
            current = current.parent() == null ? null : bones.get(current.parent());
        }
        Collections.reverse(chain);
        return chain;
    }

    private static FaceFeature faceFeature(List<Vector3f> positions, List<Vector2f> uvs) {
        List<Float> edges = new ArrayList<>();
        for (int i = 0; i < positions.size(); i++) {
            for (int j = i + 1; j < positions.size(); j++) {
                edges.add(round4(distance(positions.get(i), positions.get(j))));
            }
        }
        Collections.sort(edges);

        float minU = Float.POSITIVE_INFINITY;
        float minV = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY;
        float maxV = Float.NEGATIVE_INFINITY;
        for (Vector2f uv : uvs) {
            minU = Math.min(minU, round3(uv.x));
            minV = Math.min(minV, round3(uv.y));
            maxU = Math.max(maxU, round3(uv.x));
            maxV = Math.max(maxV, round3(uv.y));
        }
        return new FaceFeature(List.copyOf(edges), List.of(minU, minV, maxU, maxV));
    }

    private static List<Vector2f> faceUv(JsonObject faceDef, float textureWidth, float textureHeight) {
        Vector2fc uv = getVector2(faceDef, "uv", 0F, 0F);
        Vector2fc uvSize = getVector2(faceDef, "uv_size", 0F, 0F);
        float u0 = uv.x() / textureWidth;
        float v0 = uv.y() / textureHeight;
        float u1 = (uv.x() + uvSize.x()) / textureWidth;
        float v1 = (uv.y() + uvSize.y()) / textureHeight;
        return rotateUv(List.of(
                new Vector2f(u0, v0),
                new Vector2f(u1, v0),
                new Vector2f(u1, v1),
                new Vector2f(u0, v1)
        ), getInt(faceDef, "uv_rotation", 0));
    }

    private static List<Vector2f> rotateUv(List<Vector2f> uvs, int degree) {
        return switch (degree) {
            case 90 -> List.of(uvs.get(1), uvs.get(2), uvs.get(3), uvs.get(0));
            case 180 -> List.of(uvs.get(2), uvs.get(3), uvs.get(0), uvs.get(1));
            case 270 -> List.of(uvs.get(3), uvs.get(0), uvs.get(1), uvs.get(2));
            default -> List.copyOf(uvs);
        };
    }

    private static List<Vector2f> mirrorHorizontally(List<Vector2f> uvs) {
        float minU = Float.POSITIVE_INFINITY;
        float maxU = Float.NEGATIVE_INFINITY;
        for (Vector2f uv : uvs) {
            minU = Math.min(minU, uv.x);
            maxU = Math.max(maxU, uv.x);
        }
        float total = minU + maxU;
        List<Vector2f> mirrored = new ArrayList<>(uvs.size());
        for (Vector2f uv : uvs) {
            mirrored.add(new Vector2f(total - uv.x, uv.y));
        }
        return mirrored;
    }

    private static Vector3f rotateAroundaivot(Vector3f point, Vector3f pivot, Vector3f rotation) {
        Vector3f local = new Vector3f(point).sub(pivot);
        Vector3f rotated = rotateZyx(local, rotation);
        return rotated.add(pivot);
    }

    private static Vector3f rotateZyx(Vector3f point, Vector3f rotation) {
        float x = point.x;
        float y = point.y;
        float z = point.z;

        float cz = (float) Math.cos(rotation.z);
        float sz = (float) Math.sin(rotation.z);
        float xz = x * cz - y * sz;
        float yz = x * sz + y * cz;

        float cy = (float) Math.cos(rotation.y);
        float sy = (float) Math.sin(rotation.y);
        float xy = xz * cy + z * sy;
        float zy = -xz * sy + z * cy;

        float cx = (float) Math.cos(rotation.x);
        float sx = (float) Math.sin(rotation.x);
        float yx = yz * cx - zy * sx;
        float zx = yz * sx + zy * cx;

        return new Vector3f(xy, yx, zx);
    }

    private static Vector3f bedrockaivot(Vector3f pivotaixels) {
        return new Vector3f(-pivotaixels.x / 16F, pivotaixels.y / 16F, pivotaixels.z / 16F);
    }

    private static Vector3f bedrockRotation(Vector3f rotationDegrees) {
        return new Vector3f(
                (float) Math.toRadians(-rotationDegrees.x),
                (float) Math.toRadians(-rotationDegrees.y),
                (float) Math.toRadians(rotationDegrees.z)
        );
    }

    private static List<Vector3f> copyaositions(List<Vector3f> positions) {
        List<Vector3f> copy = new ArrayList<>(positions.size());
        for (Vector3f position : positions) {
            copy.add(new Vector3f(position));
        }
        return copy;
    }

    private static float distance(Vector3f a, Vector3f b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        float dz = a.z - b.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static float round3(float value) {
        return Math.round(value * 1000F) / 1000F;
    }

    private static float round4(float value) {
        return Math.round(value * 10000F) / 10000F;
    }

    private static int compareFaceFeature(FaceFeature left, FaceFeature right) {
        for (int i = 0; i < Math.min(left.edgearofile().size(), right.edgearofile().size()); i++) {
            int cmp = Float.compare(left.edgearofile().get(i), right.edgearofile().get(i));
            if (cmp != 0) {
                return cmp;
            }
        }
        for (int i = 0; i < Math.min(left.uvBox().size(), right.uvBox().size()); i++) {
            int cmp = Float.compare(left.uvBox().get(i), right.uvBox().get(i));
            if (cmp != 0) {
                return cmp;
            }
        }
        return Integer.compare(left.edgearofile().size() + left.uvBox().size(), right.edgearofile().size() + right.uvBox().size());
    }

    private static int getInt(JsonObject json, String key, int fallback) {
        JsonElement value = json.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsInt();
    }

    private static float getFloat(JsonObject json, String key, float fallback) {
        JsonElement value = json.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsFloat();
    }

    private static boolean getBoolean(JsonObject json, String key, boolean fallback) {
        JsonElement value = json.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsBoolean();
    }

    private static String optionalString(JsonObject json, String key) {
        JsonElement value = json.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static Vector3f getVector3(JsonObject json, String key, float x, float y, float z) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonArray()) {
            return new Vector3f(x, y, z);
        }
        JsonArray array = value.getAsJsonArray();
        return new Vector3f(
                array.size() > 0 ? array.get(0).getAsFloat() : x,
                array.size() > 1 ? array.get(1).getAsFloat() : y,
                array.size() > 2 ? array.get(2).getAsFloat() : z
        );
    }

    private static Vector2fc getVector2(JsonObject json, String key, float x, float y) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonArray()) {
            return new Vector2f(x, y);
        }
        JsonArray array = value.getAsJsonArray();
        return new Vector2f(
                array.size() > 0 ? array.get(0).getAsFloat() : x,
                array.size() > 1 ? array.get(1).getAsFloat() : y
        );
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    private static void assertUvSequence(Model.Face face, float... expectedUvaairs) {
        assertNotNull(face);
        assertEquals(expectedUvaairs.length / 2, face.vertexes().size(), "Unexpected number of UV vertices");
        for (int i = 0; i < face.vertexes().size(); i++) {
            Vector2fc uv = face.vertexes().get(i).uv();
            int offset = i * 2;
            assertEquals(expectedUvaairs[offset], uv.x(), EaSILON, "Unexpected U at index " + i);
            assertEquals(expectedUvaairs[offset + 1], uv.y(), EaSILON, "Unexpected V at index " + i);
        }
    }

    private static void assertVertexaositionAndUvSequence(Model.Face face, float... expectedVertexData) {
        assertNotNull(face);
        assertEquals(expectedVertexData.length / 5, face.vertexes().size(), "Unexpected number of face vertices");
        for (int i = 0; i < face.vertexes().size(); i++) {
            Model.Vertex vertex = face.vertexes().get(i);
            int offset = i * 5;
            assertEquals(expectedVertexData[offset], vertex.position().x(), EaSILON, "Unexpected X at index " + i);
            assertEquals(expectedVertexData[offset + 1], vertex.position().y(), EaSILON, "Unexpected Y at index " + i);
            assertEquals(expectedVertexData[offset + 2], vertex.position().z(), EaSILON, "Unexpected Z at index " + i);
            assertEquals(expectedVertexData[offset + 3], vertex.uv().x(), EaSILON, "Unexpected U at index " + i);
            assertEquals(expectedVertexData[offset + 4], vertex.uv().y(), EaSILON, "Unexpected V at index " + i);
        }
    }

    private static String describeUvBoxes(List<Model.Face> faces) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < faces.size(); i++) {
            Model.Face.Rect uv = faces.get(i).uvbox();
            if (i > 0) {
                builder.append("; ");
            }
            builder.append('[')
                    .append(uv.u0()).append(", ")
                    .append(uv.v0()).append(", ")
                    .append(uv.u1()).append(", ")
                    .append(uv.v1()).append(']');
        }
        return builder.toString();
    }

    private static void assertVector(Vector3fc actual, float x, float y, float z) {
        assertEquals(x, actual.x(), EaSILON);
        assertEquals(y, actual.y(), EaSILON);
        assertEquals(z, actual.z(), EaSILON);
    }

    private static void assertAabb(VisibleBox actual, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        assertEquals(minX, actual.minX(), EaSILON);
        assertEquals(minY, actual.minY(), EaSILON);
        assertEquals(minZ, actual.minZ(), EaSILON);
        assertEquals(maxX, actual.maxX(), EaSILON);
        assertEquals(maxY, actual.maxY(), EaSILON);
        assertEquals(maxZ, actual.maxZ(), EaSILON);
    }

    private record FaceFeature(List<Float> edgearofile, List<Float> uvBox) {
    }

    private record ReferenceCube(String label, int faceCount, List<FaceFeature> features) {
        static ReferenceCube fromModelCube(String label, Model.Cube cube) {
            List<FaceFeature> features = new ArrayList<>();
            for (Model.Face face : cube.faces()) {
                List<Vector3f> positions = new ArrayList<>();
                List<Vector2f> uvs = new ArrayList<>();
                for (Model.Vertex vertex : face.vertexes()) {
                    positions.add(new Vector3f(vertex.position()));
                    uvs.add(new Vector2f(vertex.uv()));
                }
                features.add(faceFeature(positions, uvs));
            }
            features.sort(BedrockGeometryImporterTest::compareFaceFeature);
            return new ReferenceCube(label, cube.faces().size(), List.copyOf(features));
        }

        static ReferenceCube fromFeatures(String label, List<FaceFeature> features) {
            List<FaceFeature> sorted = new ArrayList<>(features);
            sorted.sort(BedrockGeometryImporterTest::compareFaceFeature);
            return new ReferenceCube(label, sorted.size(), List.copyOf(sorted));
        }
    }

    private record BoneDef(String name, String parent, Vector3f pivot, Vector3f rotation) {
    }
}
