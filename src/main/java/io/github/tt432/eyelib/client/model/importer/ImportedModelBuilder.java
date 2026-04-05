package io.github.tt432.eyelib.client.model.importer;

import io.github.tt432.eyelib.client.model.Model;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.molang.MolangValue;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

final class ImportedModelBuilder {
    private ImportedModelBuilder() {
    }

    public static Model build(ImportedModelData data) {
        Int2ObjectMap<Model.Bone> bones = new Int2ObjectOpenHashMap<>();
        for (ImportedBoneData bone : data.bones()) {
            bones.put(bone.id(), buildBone(bone));
        }
        return new Model(data.name(), bones, data.visibleBox());
    }

    private static Model.Bone buildBone(ImportedBoneData bone) {
        MolangValue binding = bone.binding() == null || bone.binding().isBlank()
                ? MolangValue.FALSE_VALUE
                : new MolangValue(bone.binding());
        List<Model.TextureMesh> textureMeshes = bone.textureMeshes().stream()
                .map(tm -> new Model.TextureMesh(
                        tm.texture(),
                        new Vector3f(tm.position()),
                        new Vector3f(tm.rotation()),
                        new Vector3f(tm.localPivot()),
                        new Vector3f(tm.scale())
                ))
                .toList();
        return new Model.Bone(
                bone.id(),
                bone.parentId(),
                new Vector3f(bone.pivot()),
                new Vector3f(bone.rotation()),
                new Vector3f(),
                new Vector3f(1),
                binding,
                new Int2ObjectOpenHashMap<>(),
                bone.cubes().stream().map(ImportedModelBuilder::buildCube).toList(),
                new GroupLocator(new Int2ObjectOpenHashMap<>(), locatorEntries(bone.locators())),
                bone.reset(),
                bone.material(),
                textureMeshes
        );
    }

    private static List<LocatorEntry> locatorEntries(List<ImportedLocatorData> locators) {
        List<LocatorEntry> entries = new ArrayList<>();
        for (ImportedLocatorData locator : locators) {
            entries.add(new LocatorEntry(
                    locator.name(),
                    new Vector3f(locator.offset()),
                    new Vector3f(locator.rotation()),
                    locator.ignoreInheritedScale(),
                    locator.isNullObject()
            ));
        }
        return entries;
    }

    private static Model.Cube buildCube(ImportedCubeData cube) {
        return new Model.Cube(cube.faces().stream().map(ImportedModelBuilder::buildFace).toList());
    }

    /**
     * Builds a {@link Model.Face} from imported face data, correcting vertex winding order
     * and face normal direction for OpenGL rendering.
     *
     * <h3>Why reversal is needed</h3>
     * <p>The vertex positions produced by the Bedrock / BBModel import layer
     * ({@link ImportedModelData}) follow Blockbench's winding convention, which produces
     * <b>clockwise (CW)</b> triangles when a face is viewed from the outside of the cube.
     * Blockbench renders with {@code THREE.DoubleSide} (no face culling), so the winding
     * direction is irrelevant there.</p>
     *
     * <p>eyelib renders with OpenGL's default {@code GL_CCW} front-face convention and
     * {@code GL_BACK} culling. Under this convention CW-wound faces are treated as
     * back-faces and culled, making them invisible from the outside.</p>
     *
     * <h3>What this method does</h3>
     * <ul>
     *   <li>Reverses the vertex iteration order (last-to-first), converting the winding
     *       from CW to CCW so OpenGL treats the face as a front-face.</li>
     *   <li>Negates the imported normal vector. The imported normal is inward-facing
     *       (derived from the original CW winding via cross product); negating it produces
     *       the correct outward-facing normal for the CCW-wound face.</li>
     *   <li>Preserves the original position-to-UV pairing by iterating both
     *       {@code positions} and {@code uvs} with the same reversed index.</li>
     * </ul>
     *
     * @param face the imported face data with CW-wound positions and inward-facing normal
     * @return a model face with CCW-wound vertices and outward-facing normal
     */
    private static Model.Face buildFace(ImportedFaceData face) {
        List<Model.Vertex> vertexes = new ArrayList<>();
        Vector3f outwardNormal = new Vector3f(face.normal()).negate();
        for (int i = face.positions().size() - 1; i >= 0; i--) {
            vertexes.add(new Model.Vertex(face.positions().get(i), face.uvs().get(i), outwardNormal));
        }
        return new Model.Face(vertexes, outwardNormal, face.materialInstance());
    }
}
