package io.github.tt432.eyelib.client.model.importer;

import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.client.model.bedrock.BedrockGeometryModel;
import io.github.tt432.eyelib.client.model.bedrock.BedrockModelLoader;
import org.junit.jupiter.api.Test;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockImportedModelDataTest {
    private static final float EPSILON = 1.0e-6F;

    @Test
    void metadataFixtureNormalizesBoneNamesAndHierarchy() throws Exception {
        ImportedModelData data = importedData("bedrock/metadata.geometry.json");

        assertEquals("geometry.metadata", data.name());
        assertEquals(2, data.bones().size());

        ImportedBoneData root = boneByName(data, "root");
        ImportedBoneData child = boneByName(data, "child");

        assertEquals("root", boneName(root));
        assertEquals(-1, root.parentId());
        assertEquals("child", boneName(child));
        assertEquals("root", boneName(child.parentId()));
    }

    @Test
    void metadataFixtureNormalizesRootBoneTransformsAndFlags() throws Exception {
        ImportedBoneData root = boneByName(importedData("bedrock/metadata.geometry.json"), "root");

        assertVector(root.pivot(), -0.0625F, 0.125F, 0.1875F);
        assertVector(root.rotation(), radians(-10), radians(-20), radians(30));
        assertEquals("entity_alphatest", root.material());
        assertEquals("q.body", root.binding());
        assertTrue(root.reset());
        assertFalse(root.mirrorUv());
    }

    @Test
    void metadataFixtureNormalizesLocatorCoordinatesAndNullObjects() throws Exception {
        ImportedBoneData root = boneByName(importedData("bedrock/metadata.geometry.json"), "root");

        assertEquals(2, root.locators().size());
        ImportedLocatorData hand = locatorByName(root, "hand");
        ImportedLocatorData aim = locatorByName(root, "aim");

        assertEquals("hand", hand.name());
        assertVector(hand.offset(), -0.0625F, 0.125F, 0.1875F);
        assertVector(hand.rotation(), 0, 0, 0);
        assertFalse(hand.ignoreInheritedScale());
        assertFalse(hand.isNullObject());

        assertEquals("aim", aim.name());
        assertVector(aim.offset(), -0.25F, 0.3125F, 0.375F);
        assertVector(aim.rotation(), radians(-7), radians(-8), radians(9));
        assertTrue(aim.ignoreInheritedScale());
        assertTrue(aim.isNullObject());
    }

    @Test
    void metadataFixtureNormalizesTextureMeshCoordinatesAndZeroScaleFallback() throws Exception {
        ImportedBoneData root = boneByName(importedData("bedrock/metadata.geometry.json"), "root");

        assertEquals(1, root.textureMeshes().size());
        ImportedTextureMeshData textureMesh = root.textureMeshes().get(0);
        assertEquals("tex_mesh", textureMesh.texture());
        assertVector(textureMesh.position(), 0.125F, -1F, 0.25F);
        assertVector(textureMesh.rotation(), radians(5), radians(6), radians(7));
        assertVector(textureMesh.localPivot(), -0.0625F, 0.125F, -0.1875F);
        assertVector(textureMesh.scale(), 1, 1, 1);
    }

    @Test
    void metadataFixturePreservesPerFaceUvBoxesAndMaterialInstances() throws Exception {
        ImportedBoneData root = boneByName(importedData("bedrock/metadata.geometry.json"), "root");

        assertEquals(1, root.cubes().size());
        assertEquals(6, root.cubes().get(0).faces().size());

        ImportedFaceData rootNorth = faceByMaterialInstance(root.cubes().get(0), "mat_n");
        ImportedFaceData rootWest = faceByUvBounds(root.cubes().get(0), 0.09375F, 0F, 0.125F, 0.03125F);
        ImportedFaceData rootDown = faceByUvBounds(root.cubes().get(0), 0.03125F, 0.0625F, 0.0625F, 0.09375F);

        assertEquals("mat_n", rootNorth.materialInstance());
        assertUvBox(rootNorth.uvs(), 0F, 0F, 0.03125F, 0.03125F);
        assertUvBox(rootWest.uvs(), 0.09375F, 0F, 0.125F, 0.03125F);
        assertUvBox(rootDown.uvs(), 0.03125F, 0.0625F, 0.0625F, 0.09375F);
    }

    @Test
    void metadataFixtureKeepsChildMirrorFlagAndCubeOverrideBehavior() throws Exception {
        ImportedBoneData child = boneByName(importedData("bedrock/metadata.geometry.json"), "child");

        assertVector(child.pivot(), 0, 0.5F, 0);
        assertVector(child.rotation(), 0, 0, 0);
        assertNull(child.material());
        assertNull(child.binding());
        assertFalse(child.reset());
        assertTrue(child.mirrorUv());

        assertEquals(1, child.cubes().size());
        assertEquals(6, child.cubes().get(0).faces().size());
        ImportedFaceData northFace = faceByUvBounds(child.cubes().get(0), 0.1875F, 0.21875F, 0.25F, 0.34375F);
        assertUvBox(northFace.uvs(), 0.1875F, 0.21875F, 0.25F, 0.34375F);
        assertUvSequence(northFace.uvs(),
                0.1875F, 0.21875F,
                0.25F, 0.21875F,
                0.25F, 0.34375F,
                0.1875F, 0.34375F);
    }

    @Test
    void realFixtureRetainsZeroDepthCubeAndDegenerateNormals() throws Exception {
        ImportedBoneData bone = boneByName(importedData("bedrock/test.geo.json"), "bone");
        ImportedCubeData zeroDepthCube = bone.cubes().get(5);

        assertEquals(6, zeroDepthCube.faces().size());
        assertContainsUvBox(zeroDepthCube, 0.1875F, 0.25F, 0.375F, 0.5F);
        assertContainsUvBox(zeroDepthCube, 0.71875F, 0.65625F, 0.90625F, 0.65625F);

        List<ImportedFaceData> degenerateFaces = zeroDepthCube.faces().stream()
                .filter(face -> approximatelyEquals(face.normal().lengthSquared(), 0F))
                .toList();
        assertEquals(4, degenerateFaces.size());

        Set<String> degenerateUvBoxes = degenerateFaces.stream()
                .map(face -> uvBoundsKey(face.uvs()))
                .collect(Collectors.toSet());
        assertEquals(Set.of(
                uvBoundsKey(0.65625F, 0.65625F, 0.65625F, 0.96875F),
                uvBoundsKey(0.84375F, 0.0625F, 0.84375F, 0.375F),
                uvBoundsKey(0.71875F, 0.65625F, 0.90625F, 0.65625F),
                uvBoundsKey(0.15625F, 0.84375F, 0.34375F, 0.84375F)
        ), degenerateUvBoxes);
    }

    @Test
    void realFixturePreservesRotatedCubeUvBoxes() throws Exception {
        ImportedBoneData bone = boneByName(importedData("bedrock/test.geo.json"), "bone");

        ImportedCubeData rotatedCube = bone.cubes().get(2);
        assertEquals(6, rotatedCube.faces().size());
        assertContainsUvBox(rotatedCube, 0.0625F, 0.3125F, 0.09375F, 0.40625F);
        assertContainsUvBox(rotatedCube, 0.40625F, 0.09375F, 0.4375F, 0.1875F);
    }

    @Test
    void mirrorFixtureUsesBoneMirrorWhenCubeDoesNotOverride() throws Exception {
        ImportedBoneData root = boneByName(importedData("bedrock/mirror.geometry.json"), "mirror_root");
        ImportedCubeData inheritedMirrorCube = root.cubes().get(0);
        ImportedFaceData northFace = faceByUvBounds(inheritedMirrorCube, 0.125F, 0.125F, 0.25F, 0.25F);

        assertUvSequence(northFace.uvs(),
                0.25F, 0.125F,
                0.125F, 0.125F,
                0.125F, 0.25F,
                0.25F, 0.25F);
    }

    @Test
    void mirrorFixtureAllowsCubeMirrorOverrideToDisableMirroring() throws Exception {
        ImportedBoneData root = boneByName(importedData("bedrock/mirror.geometry.json"), "mirror_root");
        ImportedCubeData cubeOverrideMirrorFalse = root.cubes().get(1);
        ImportedFaceData northFace = faceByUvBounds(cubeOverrideMirrorFalse, 0.125F, 0.125F, 0.25F, 0.25F);

        assertUvSequence(northFace.uvs(),
                0.125F, 0.125F,
                0.25F, 0.125F,
                0.25F, 0.25F,
                0.125F, 0.25F);
    }

    @Test
    void alignmentFixtureMapsBoxUvSixFaceSlotsUsingBlockbenchStripOrder() throws Exception {
        ImportedBoneData boxSlots = boneByName(importedData("bedrock/alignment.geometry.json"), "box_slots");
        ImportedCubeData cube = boxSlots.cubes().get(0);

        assertEquals(6, cube.faces().size());
        assertContainsUvBox(cube, 0.171875F, 0.109375F, 0.234375F, 0.140625F); // north
        assertContainsUvBox(cube, 0.125F, 0.109375F, 0.171875F, 0.140625F); // east
        assertContainsUvBox(cube, 0.234375F, 0.109375F, 0.28125F, 0.140625F); // west
        assertContainsUvBox(cube, 0.28125F, 0.109375F, 0.34375F, 0.140625F); // south
        assertContainsUvBox(cube, 0.171875F, 0.0625F, 0.234375F, 0.109375F); // up
        assertContainsUvBox(cube, 0.234375F, 0.0625F, 0.296875F, 0.109375F); // down
    }

    @Test
    void alignmentFixturePreservesSparseFacesAndKeepsNonRightAngleUvRotationUnchanged() throws Exception {
        ImportedBoneData sparse = boneByName(importedData("bedrock/alignment.geometry.json"), "sparse_faces");
        ImportedCubeData cube = sparse.cubes().get(0);

        assertEquals(2, cube.faces().size());

        ImportedFaceData northFace = faceByMaterialInstance(cube, "mat_sparse_n");
        assertUvSequence(northFace.uvs(),
                0.3125F, 0.15625F,
                0.375F, 0.15625F,
                0.375F, 0.203125F,
                0.3125F, 0.203125F);
    }

    @Test
    void alignmentPlusFixturePreservesRightAngleUvRotationsIncludingUpDown270() throws Exception {
        ImportedBoneData bone = boneByName(importedData("bedrock/alignment_plus.geometry.json"), "uv_rotation_cases");
        ImportedCubeData cube = bone.cubes().get(0);

        assertEquals(6, cube.faces().size());

        ImportedFaceData north270 = faceByMaterialInstance(cube, "mat_n270");
        assertUvSequence(north270.uvs(),
                0.0625F, 0.1875F,
                0.0625F, 0.125F,
                0.1875F, 0.125F,
                0.1875F, 0.1875F);

        ImportedFaceData east90 = faceByMaterialInstance(cube, "mat_e90");
        assertUvSequence(east90.uvs(),
                0.28125F, 0.125F,
                0.28125F, 0.25F,
                0.21875F, 0.25F,
                0.21875F, 0.125F);

        ImportedFaceData up270 = faceByMaterialInstance(cube, "mat_u270");
        assertUvSequence(up270.uvs(),
                0.6875F, 0.125F,
                0.6875F, 0.25F,
                0.5625F, 0.25F,
                0.5625F, 0.125F);

        ImportedFaceData down270Neg = faceByMaterialInstance(cube, "mat_d270_neg");
        assertUvSequence(down270Neg.uvs(),
                0.875F, 0.25F,
                0.875F, 0.125F,
                0.75F, 0.125F,
                0.75F, 0.25F);
    }

    @Test
    void alignmentPlusFixturePreservesNegativeUvSizeVertexOrderIncludingUpDownFaces() throws Exception {
        ImportedBoneData bone = boneByName(importedData("bedrock/alignment_plus.geometry.json"), "negative_uv_size_cases");
        ImportedCubeData cube = bone.cubes().get(0);

        assertEquals(3, cube.faces().size());

        ImportedFaceData northNegative = faceByMaterialInstance(cube, "mat_neg_n");
        assertUvSequence(northNegative.uvs(),
                0F, 0F,
                -0.125F, 0F,
                -0.125F, 0.0625F,
                0F, 0.0625F);

        ImportedFaceData upNegative = faceByMaterialInstance(cube, "mat_neg_up");
        assertUvSequence(upNegative.uvs(),
                0.375F, 0.1875F,
                0.25F, 0.1875F,
                0.25F, 0.25F,
                0.375F, 0.25F);

        ImportedFaceData downNegative = faceByMaterialInstance(cube, "mat_neg_down");
        assertUvSequence(downNegative.uvs(),
                0.25F, 0.3125F,
                0.375F, 0.3125F,
                0.375F, 0.375F,
                0.25F, 0.375F);
    }

    @Test
    void alignmentPlusFixtureRetainsZeroDimensionXAndYCubeAxes() throws Exception {
        ImportedModelData data = importedData("bedrock/alignment_plus.geometry.json");

        ImportedCubeData xCube = boneByName(data, "zero_dimension_x").cubes().get(0);
        ImportedCubeData yCube = boneByName(data, "zero_dimension_y").cubes().get(0);

        assertEquals(6, xCube.faces().size());
        assertEquals(4, xCube.faces().stream().filter(face -> approximatelyEquals(face.normal().lengthSquared(), 0F)).count());
        assertContainsUvBox(xCube, 0.03125F, 0.53125F, 0.03125F, 0.59375F);

        assertEquals(6, yCube.faces().size());
        assertEquals(4, yCube.faces().stream().filter(face -> approximatelyEquals(face.normal().lengthSquared(), 0F)).count());
        assertContainsUvBox(yCube, 0.3125F, 0.5625F, 0.34375F, 0.5625F);
    }

    @Test
    void alignmentPlusFixtureCombinesBoneMirrorWithCubeRotationAndUpDownFaces() throws Exception {
        ImportedBoneData bone = boneByName(importedData("bedrock/alignment_plus.geometry.json"), "mirror_rotated");
        ImportedCubeData cube = bone.cubes().get(0);

        assertTrue(bone.mirrorUv());
        assertVector(bone.rotation(), 0, radians(-30), 0);

        ImportedFaceData northFace = faceByMaterialInstance(cube, "mat_m_n90");
        assertUvSequence(northFace.uvs(),
                0F, 0.625F,
                0F, 0.6875F,
                0.125F, 0.6875F,
                0.125F, 0.625F);

        ImportedFaceData upFace = faceByMaterialInstance(cube, "mat_m_u270");
        assertUvSequence(upFace.uvs(),
                0.5F, 0.625F,
                0.5F, 0.75F,
                0.625F, 0.75F,
                0.625F, 0.625F);

        ImportedFaceData downFace = faceByMaterialInstance(cube, "mat_m_d90_neg");
        assertUvSequence(downFace.uvs(),
                0.75F, 0.625F,
                0.75F, 0.75F,
                0.625F, 0.75F,
                0.625F, 0.625F);
    }

    @Test
    void skeletonGapFixtureExtendsNegativeUvSizeCoverageToEastWestAndMixedSigns() throws Exception {
        ImportedBoneData bone = boneByName(importedData("bedrock/skeleton_gaps.geometry.json"), "negative_east_west");
        ImportedCubeData cube = bone.cubes().get(0);

        assertEquals(3, cube.faces().size());

        ImportedFaceData eastNegative = faceByMaterialInstance(cube, "mat_neg_east");
        assertUvSequence(eastNegative.uvs(),
                0.375F, 0.125F,
                0.25F, 0.125F,
                0.25F, 0.1875F,
                0.375F, 0.1875F);

        ImportedFaceData westMixedNegative = faceByMaterialInstance(cube, "mat_neg_west_mixed");
        assertUvSequence(westMixedNegative.uvs(),
                0.5F, 0.125F,
                0.375F, 0.125F,
                0.375F, 0.0625F,
                0.5F, 0.0625F);
    }

    @Test
    void skeletonGapFixtureKeepsDegeneratePerFaceUvRectanglesWhenUvSizeHasZeroComponents() throws Exception {
        ImportedBoneData bone = boneByName(importedData("bedrock/skeleton_gaps.geometry.json"), "zero_uv_size_components");
        ImportedCubeData cube = bone.cubes().get(0);

        assertEquals(6, cube.faces().size());

        ImportedFaceData eastZeroWidth = faceByMaterialInstance(cube, "mat_zero_east");
        assertUvSequence(eastZeroWidth.uvs(),
                0.125F, 0.375F,
                0.125F, 0.375F,
                0.125F, 0.46875F,
                0.125F, 0.46875F);
        assertUvBox(eastZeroWidth.uvs(), 0.125F, 0.375F, 0.125F, 0.46875F);

        ImportedFaceData westZeroWidth = faceByMaterialInstance(cube, "mat_zero_west");
        assertUvSequence(westZeroWidth.uvs(),
                0.25F, 0.375F,
                0.25F, 0.375F,
                0.25F, 0.46875F,
                0.25F, 0.46875F);
        assertUvBox(westZeroWidth.uvs(), 0.25F, 0.375F, 0.25F, 0.46875F);

        ImportedFaceData upZeroHeight = faceByMaterialInstance(cube, "mat_zero_up");
        assertUvSequence(upZeroHeight.uvs(),
                0.46875F, 0.375F,
                0.375F, 0.375F,
                0.375F, 0.375F,
                0.46875F, 0.375F);
        assertUvBox(upZeroHeight.uvs(), 0.375F, 0.375F, 0.46875F, 0.375F);

        ImportedFaceData downZeroHeight = faceByMaterialInstance(cube, "mat_zero_down");
        assertUvSequence(downZeroHeight.uvs(),
                0.59375F, 0.375F,
                0.5F, 0.375F,
                0.5F, 0.375F,
                0.59375F, 0.375F);
        assertUvBox(downZeroHeight.uvs(), 0.5F, 0.375F, 0.59375F, 0.375F);
    }

    @Test
    void skeletonGapFixtureAppliesComplexTriAxisRotationToFaceNormals() throws Exception {
        ImportedBoneData bone = boneByName(importedData("bedrock/skeleton_gaps.geometry.json"), "tri_axis_rotation");
        ImportedCubeData cube = bone.cubes().get(0);

        assertEquals(4, cube.faces().size());

        ImportedFaceData rotatedEast = faceByMaterialInstance(cube, "mat_rot_east");
        assertVector(rotatedEast.normal(), -0.19562931F, -0.974559F, -0.10937977F);
    }

    @Test
    void hierarchyMeshFixtureNormalizesDeepHierarchyAndChildLocators() throws Exception {
        ImportedModelData data = importedData("bedrock/hierarchy_mesh.geometry.json");

        ImportedBoneData root = boneByName(data, "root");
        ImportedBoneData childA = boneByName(data, "child_a");
        ImportedBoneData childB = boneByName(data, "child_b");
        ImportedBoneData childC = boneByName(data, "child_c");

        assertEquals(4, data.bones().size());
        assertEquals(-1, root.parentId());
        assertEquals("root", boneName(childA.parentId()));
        assertEquals("child_a", boneName(childB.parentId()));
        assertEquals("child_b", boneName(childC.parentId()));

        assertVector(childA.pivot(), -0.125F, 0.25F, 0F);
        assertVector(childA.rotation(), radians(-15), 0F, radians(5));
        assertVector(childC.pivot(), 0.0625F, 0.0625F, 0.0625F);
        assertVector(childC.rotation(), radians(-5), 0F, radians(35));

        assertEquals(2, childA.locators().size());
        ImportedLocatorData childHit = locatorByName(childA, "child_a_hit");
        assertVector(childHit.offset(), 0F, 0.5F, 0F);
        assertFalse(childHit.isNullObject());

        ImportedLocatorData childScope = locatorByName(childA, "child_a_scope");
        assertVector(childScope.offset(), -0.125F, 0.625F, 0.0625F);
        assertVector(childScope.rotation(), 0F, radians(-90), 0F);
        assertTrue(childScope.isNullObject());

        assertEquals(2, childB.locators().size());
        ImportedLocatorData childSocket = locatorByName(childB, "child_b_socket");
        assertVector(childSocket.offset(), 0F, 0.1875F, 0.0625F);
        assertVector(childSocket.rotation(), radians(-15), 0F, 0F);
        assertTrue(childSocket.ignoreInheritedScale());
    }

    @Test
    void hierarchyMeshFixtureNormalizesTextureMeshesForDefaultsOverridesAndZeroScaleFallback() throws Exception {
        ImportedModelData data = importedData("bedrock/hierarchy_mesh.geometry.json");
        ImportedBoneData root = boneByName(data, "root");
        ImportedBoneData childA = boneByName(data, "child_a");

        assertEquals(3, root.textureMeshes().size());
        ImportedTextureMeshData rootDefault = root.textureMeshes().get(0);
        ImportedTextureMeshData rootCustom = root.textureMeshes().get(1);
        ImportedTextureMeshData rootZeroScale = root.textureMeshes().get(2);

        assertEquals("mesh_default", rootDefault.texture());
        assertVector(rootDefault.position(), 0F, 0F, 0F);
        assertVector(rootDefault.rotation(), 0F, 0F, 0F);
        assertVector(rootDefault.localPivot(), 0F, 0F, 0F);
        assertVector(rootDefault.scale(), 1F, 1F, 1F);

        assertEquals("mesh_rot_scale", rootCustom.texture());
        assertVector(rootCustom.position(), 0.125F, -1.125F, 0.25F);
        assertVector(rootCustom.rotation(), radians(10), radians(20), radians(30));
        assertVector(rootCustom.localPivot(), -0.0625F, 0.125F, -0.1875F);
        assertVector(rootCustom.scale(), 2F, 1F, 0.5F);

        assertEquals("mesh_zero", rootZeroScale.texture());
        assertVector(rootZeroScale.position(), -0.125F, -1F, 0F);
        assertVector(rootZeroScale.scale(), 1F, 1F, 1F);

        assertEquals(1, childA.textureMeshes().size());
        ImportedTextureMeshData childMesh = childA.textureMeshes().get(0);
        assertEquals("mesh_child_a", childMesh.texture());
        assertVector(childMesh.position(), 0.0625F, -0.625F, -0.125F);
        assertVector(childMesh.rotation(), 0F, radians(45), 0F);
        assertVector(childMesh.localPivot(), 0F, 0F, 0F);
        assertVector(childMesh.scale(), 1F, 1F, 1F);
    }

    private static ImportedModelData importedData(String relativePath) throws Exception {
        BedrockGeometryModel source = new BedrockModelLoader().load(fixturePath(relativePath));
        return ImportedModelData.fromBedrock(source.geometries().get(0));
    }

    private static ImportedBoneData boneByName(ImportedModelData data, String expectedName) {
        for (ImportedBoneData bone : data.bones()) {
            if (expectedName.equals(boneName(bone.id()))) {
                return bone;
            }
        }
        throw new AssertionError("Missing bone named " + expectedName + " in imported model " + data.name());
    }

    private static ImportedLocatorData locatorByName(ImportedBoneData bone, String expectedName) {
        for (ImportedLocatorData locator : bone.locators()) {
            if (expectedName.equals(locator.name())) {
                return locator;
            }
        }
        throw new AssertionError("Missing locator named " + expectedName + " in bone " + boneName(bone));
    }

    private static ImportedFaceData faceByMaterialInstance(ImportedCubeData cube, String materialInstance) {
        for (ImportedFaceData face : cube.faces()) {
            if (materialInstance.equals(face.materialInstance())) {
                return face;
            }
        }
        throw new AssertionError("Missing face with material_instance=" + materialInstance);
    }

    private static ImportedFaceData faceByUvBounds(ImportedCubeData cube, float u0, float v0, float u1, float v1) {
        for (ImportedFaceData face : cube.faces()) {
            if (uvBoundsMatches(face.uvs(), u0, v0, u1, v1)) {
                return face;
            }
        }
        throw new AssertionError("Missing face with UV bounds [" + u0 + ", " + v0 + ", " + u1 + ", " + v1 + "]");
    }

    private static void assertContainsUvBox(ImportedCubeData cube, float u0, float v0, float u1, float v1) {
        assertNotNull(faceByUvBounds(cube, u0, v0, u1, v1));
    }

    private static Path fixturePath(String relativePath) throws URISyntaxException {
        return Path.of(Objects.requireNonNull(BedrockImportedModelDataTest.class.getResource(relativePath)).toURI());
    }

    private static String boneName(ImportedBoneData bone) {
        return boneName(bone.id());
    }

    private static String boneName(int boneId) {
        return boneId < 0 ? "" : GlobalBoneIdHandler.get(boneId);
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    private static boolean approximatelyEquals(float actual, float expected) {
        return Math.abs(actual - expected) <= EPSILON;
    }

    private static boolean uvBoundsMatches(List<Vector2f> uvs, float u0, float v0, float u1, float v1) {
        return approximatelyEquals(actualMinU(uvs), u0)
                && approximatelyEquals(actualMinV(uvs), v0)
                && approximatelyEquals(actualMaxU(uvs), u1)
                && approximatelyEquals(actualMaxV(uvs), v1);
    }

    private static float actualMinU(List<Vector2f> uvs) {
        float actual = Float.POSITIVE_INFINITY;
        for (Vector2f uv : uvs) {
            actual = Math.min(actual, uv.x);
        }
        return actual;
    }

    private static float actualMinV(List<Vector2f> uvs) {
        float actual = Float.POSITIVE_INFINITY;
        for (Vector2f uv : uvs) {
            actual = Math.min(actual, uv.y);
        }
        return actual;
    }

    private static float actualMaxU(List<Vector2f> uvs) {
        float actual = Float.NEGATIVE_INFINITY;
        for (Vector2f uv : uvs) {
            actual = Math.max(actual, uv.x);
        }
        return actual;
    }

    private static float actualMaxV(List<Vector2f> uvs) {
        float actual = Float.NEGATIVE_INFINITY;
        for (Vector2f uv : uvs) {
            actual = Math.max(actual, uv.y);
        }
        return actual;
    }

    private static String uvBoundsKey(List<Vector2f> uvs) {
        return uvBoundsKey(actualMinU(uvs), actualMinV(uvs), actualMaxU(uvs), actualMaxV(uvs));
    }

    private static String uvBoundsKey(float u0, float v0, float u1, float v1) {
        return String.format("%.6f,%.6f,%.6f,%.6f", u0, v0, u1, v1);
    }

    private static void assertUvSequence(List<Vector2f> uvs, float... expectedUvPairs) {
        assertNotNull(uvs);
        assertEquals(expectedUvPairs.length / 2, uvs.size(), "Unexpected number of UV vertices");
        for (int i = 0; i < uvs.size(); i++) {
            int offset = i * 2;
            assertEquals(expectedUvPairs[offset], uvs.get(i).x, EPSILON, "Unexpected U at index " + i);
            assertEquals(expectedUvPairs[offset + 1], uvs.get(i).y, EPSILON, "Unexpected V at index " + i);
        }
    }

    private static void assertVector(Vector3f actual, float x, float y, float z) {
        assertEquals(x, actual.x, EPSILON);
        assertEquals(y, actual.y, EPSILON);
        assertEquals(z, actual.z, EPSILON);
    }

    private static void assertUvBox(List<Vector2f> uvs, float u0, float v0, float u1, float v1) {
        assertNotNull(uvs);
        assertEquals(4, uvs.size());
        assertEquals(u0, actualMinU(uvs), EPSILON);
        assertEquals(v0, actualMinV(uvs), EPSILON);
        assertEquals(u1, actualMaxU(uvs), EPSILON);
        assertEquals(v1, actualMaxV(uvs), EPSILON);
    }
}
