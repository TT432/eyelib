package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelibimporter.model.GlobalBoneIdHandler;
import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.client.model.importer.ModelImporter;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitContext;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitor;
import org.jspecify.annotations.Nullable;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RenderGeometryDumpParityTest {
    private static final float POS_EPS = 1.0e-3F;
    private static final float DOT_EPS = 0.98F;
    private static final float EDGE_TOL = 0.0035F;
    private static final float UV_TOL = 0.002F;

    @Test
    void skeletonRenderVisitorOutputCanBeReconstructedByGeometryCsvStyleSegmentation() throws Exception {
        Model model = importedModelAtPath(Path.of("test_resources", "eyelib", "models", "skeleton.geo.json"), "geometry.unknown");

        List<SourceCube> sourceCubes = collectSourceCubes(model);
        List<FaceChunk> capturedFaces = captureGpuReadyFaces(model);
        List<SegmentCube> segmented = segmentFaces(capturedFaces);

        assertFalse(capturedFaces.isEmpty(), "Expected captured render faces");
        assertEquals(totalFaceCount(sourceCubes), capturedFaces.size(), "Render visitor should preserve face count before reverse-style segmentation");
        assertEquals(sourceCubes.size(), segmented.size(), "Geometry.csv-style segmentation should recover the same cube count as the source model traversal");

        List<String> unmatchedSource = new ArrayList<>();
        List<String> ambiguousSource = new ArrayList<>();
        for (SourceCube sourceCube : sourceCubes) {
            List<Integer> candidates = findMatchingSegments(sourceCube, segmented);
            if (candidates.isEmpty()) {
                unmatchedSource.add(sourceCube.label());
            } else if (candidates.size() > 1) {
                ambiguousSource.add(sourceCube.label() + " -> " + candidates.subList(0, Math.min(8, candidates.size())));
            }
        }

        assertTrue(unmatchedSource.isEmpty(),
                "Python-aligned reverse matching should leave no unmatched source cubes. unmatchedSource=" + unmatchedSource.stream().limit(20).toList()
                        + " ambiguousSample=" + ambiguousSource.stream().limit(20).toList());
    }


    private static int totalFaceCount(List<SourceCube> cubes) {
        int total = 0;
        for (SourceCube cube : cubes) {
            total += cube.faceCount();
        }
        return total;
    }


    private static List<Integer> findMatchingSegments(SourceCube sourceCube, List<SegmentCube> segmented) {
        List<Integer> matches = new ArrayList<>();
        for (int i = 0; i < segmented.size(); i++) {
            if (cubeMatches(sourceCube.features(), segmented.get(i).features())) {
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
        if (left.edgeProfile().size() != right.edgeProfile().size() || left.uvBox().size() != right.uvBox().size()) {
            return false;
        }
        for (int i = 0; i < left.edgeProfile().size(); i++) {
            float diff = Math.abs(left.edgeProfile().get(i) - right.edgeProfile().get(i));
            if (diff > EDGE_TOL) {
                return false;
            }
        }
        for (int i = 0; i < left.uvBox().size(); i++) {
            float diff = Math.abs(left.uvBox().get(i) - right.uvBox().get(i));
            if (diff > UV_TOL) {
                return false;
            }
        }
        return true;
    }

    private static Model importedModelAtPath(Path path, String name) throws Exception {
        Map<String, Model> imported = ModelImporter.importFile(path.toAbsolutePath().normalize());
        assertEquals(1, imported.size());
        Model model = imported.get(name);
        assertNotNull(model, "Expected model " + name + " in fixture " + path);
        return model;
    }

    private static List<SourceCube> collectSourceCubes(Model model) {
        SourceCubeVisitor visitor = new SourceCubeVisitor();
        visitor.visitModel(RenderParams.noRender(new PoseStack()), new ModelVisitContext(), ModelRuntimeData.EMPTY, model);
        return visitor.finish();
    }

    private static List<FaceChunk> captureGpuReadyFaces(Model model) {
        CaptureVisitor visitor = new CaptureVisitor();
        visitor.visitModel(RenderParams.noRender(new PoseStack()), new ModelVisitContext(), ModelRuntimeData.EMPTY, model);
        return visitor.finish();
    }

    private static List<SegmentCube> segmentFaces(List<FaceChunk> faces) {
        List<SegmentCube> out = new ArrayList<>();
        int i = 0;
        while (i < faces.size()) {
            List<FaceChunk> current = new ArrayList<>();
            current.add(faces.get(i));
            int j = i + 1;
            while (j < faces.size() && canExtend(current, faces.get(j))) {
                current.add(faces.get(j));
                j++;
            }
            out.add(SegmentCube.fromFaces(current));
            i = j;
        }
        return out;
    }

    private static boolean canExtend(List<FaceChunk> current, FaceChunk next) {
        List<FaceChunk> trial = new ArrayList<>(current);
        trial.add(next);
        if (trial.size() > 6) {
            return false;
        }
        return uniquePointCount(trial) <= 8 && normalDirCount(trial) <= 3;
    }

    private static int uniquePointCount(List<FaceChunk> faces) {
        List<CapturedVertex> unique = new ArrayList<>();
        for (FaceChunk face : faces) {
            for (CapturedVertex vertex : face.vertices()) {
                if (unique.stream().noneMatch(existing -> samePosition(existing, vertex))) {
                    unique.add(vertex);
                }
            }
        }
        return unique.size();
    }

    private static int normalDirCount(List<FaceChunk> faces) {
        List<Vector3f> dirs = new ArrayList<>();
        for (FaceChunk face : faces) {
            Vector3f normal = face.derivedNormal();
            if (normal.lengthSquared() == 0) {
                continue;
            }
            boolean matched = false;
            for (Vector3f existing : dirs) {
                if (Math.abs(normal.dot(existing)) >= DOT_EPS) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                dirs.add(normal);
            }
        }
        return dirs.size();
    }

    private static boolean samePosition(CapturedVertex left, CapturedVertex right) {
        return Math.abs(left.x() - right.x()) <= POS_EPS
                && Math.abs(left.y() - right.y()) <= POS_EPS
                && Math.abs(left.z() - right.z()) <= POS_EPS;
    }

    private static float round3(float value) {
        return Math.round(value * 1000F) / 1000F;
    }

    private static float round4(float value) {
        return Math.round(value * 10000F) / 10000F;
    }

    private static float distance(CapturedVertex a, CapturedVertex b) {
        float dx = a.x() - b.x();
        float dy = a.y() - b.y();
        float dz = a.z() - b.z();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private record FaceFeature(List<Float> edgeProfile, List<Float> uvBox) {
    }

    private interface CubeFeatureCarrier {
        int faceCount();

        List<FaceFeature> features();
    }

    private record SourceCube(String label, int faceCount, List<FaceFeature> features) implements CubeFeatureCarrier {
        static SourceCube fromCube(String label, Model.Cube cube) {
            List<FaceFeature> features = new ArrayList<>();
            for (Model.Face face : cube.faces()) {
                List<CapturedVertex> vertices = new ArrayList<>();
                for (Model.Vertex vertex : face.vertexes()) {
                    vertices.add(new CapturedVertex(
                            vertex.position().x(), vertex.position().y(), vertex.position().z(),
                            vertex.uv().x(), vertex.uv().y(),
                            vertex.normal().x(), vertex.normal().y(), vertex.normal().z()
                    ));
                }
                features.add(FaceChunk.fromVertices(vertices).feature());
            }
            features.sort(RenderGeometryDumpParityTest::compareFaceFeature);
            return new SourceCube(label, cube.faces().size(), List.copyOf(features));
        }

    }

    private record SegmentCube(int faceCount, List<FaceFeature> features) implements CubeFeatureCarrier {
        static SegmentCube fromFaces(List<FaceChunk> faces) {
            List<FaceFeature> features = new ArrayList<>();
            for (FaceChunk face : faces) {
                features.add(face.feature());
            }
            features.sort(RenderGeometryDumpParityTest::compareFaceFeature);
            return new SegmentCube(faces.size(), List.copyOf(features));
        }
    }

    private static int compareFaceFeature(FaceFeature left, FaceFeature right) {
        for (int i = 0; i < Math.min(left.edgeProfile().size(), right.edgeProfile().size()); i++) {
            int cmp = Float.compare(left.edgeProfile().get(i), right.edgeProfile().get(i));
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
        return Integer.compare(left.edgeProfile().size() + left.uvBox().size(), right.edgeProfile().size() + right.uvBox().size());
    }

    private record CapturedVertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
    }

    private record FaceChunk(List<CapturedVertex> vertices) {
        static FaceChunk fromVertices(List<CapturedVertex> vertices) {
            return new FaceChunk(List.copyOf(vertices));
        }

        FaceFeature feature() {
            List<Float> edges = new ArrayList<>();
            for (int i = 0; i < vertices.size(); i++) {
                for (int j = i + 1; j < vertices.size(); j++) {
                    edges.add(round4(distance(vertices.get(i), vertices.get(j))));
                }
            }
            Collections.sort(edges);

            float minU = Float.POSITIVE_INFINITY;
            float minV = Float.POSITIVE_INFINITY;
            float maxU = Float.NEGATIVE_INFINITY;
            float maxV = Float.NEGATIVE_INFINITY;
            for (CapturedVertex vertex : vertices) {
                minU = Math.min(minU, round3(vertex.u()));
                minV = Math.min(minV, round3(vertex.v()));
                maxU = Math.max(maxU, round3(vertex.u()));
                maxV = Math.max(maxV, round3(vertex.v()));
            }
            return new FaceFeature(List.copyOf(edges), List.of(minU, minV, maxU, maxV));
        }

        Vector3f derivedNormal() {
            if (vertices.size() < 3) {
                return new Vector3f();
            }
            Vector3f a = new Vector3f(vertices.get(1).x() - vertices.get(0).x(), vertices.get(1).y() - vertices.get(0).y(), vertices.get(1).z() - vertices.get(0).z());
            Vector3f b = new Vector3f(vertices.get(2).x() - vertices.get(0).x(), vertices.get(2).y() - vertices.get(0).y(), vertices.get(2).z() - vertices.get(0).z());
            Vector3f normal = a.cross(b, new Vector3f());
            return normal.lengthSquared() == 0 ? normal.zero() : normal.normalize();
        }
    }

    private static final class SourceCubeVisitor extends ModelVisitor {
        private final List<SourceCube> cubes = new ArrayList<>();
        private final Map<String, Integer> indices = new LinkedHashMap<>();
        private String currentBoneName = "";

        @Override
        public void visitPreBone(RenderParams renderParams, ModelVisitContext context, Model.Bone bone, ModelRuntimeData data) {
            super.visitPreBone(renderParams, context, bone, data);
            currentBoneName = GlobalBoneIdHandler.get(bone.id());
        }

        @Override
        public void visitCube(RenderParams renderParams, ModelVisitContext context, Model.Cube cube) {
            int idx = indices.merge(currentBoneName, 0, (oldValue, ignored) -> oldValue + 1);
            String label = currentBoneName + "#" + idx;
            cubes.add(SourceCube.fromCube(label, cube));
            super.visitCube(renderParams, context, cube);
        }

        List<SourceCube> finish() {
            return List.copyOf(cubes);
        }
    }

    private static final class CaptureVisitor extends ModelVisitor {
        private final Vector4f transformedPosition = new Vector4f();
        private final Vector3f transformedNormal = new Vector3f();
        private final List<List<CapturedVertex>> faces = new ArrayList<>();
        private @Nullable List<CapturedVertex> currentFace;

        @Override
        public void visitFace(RenderParams renderParams, ModelVisitContext context, Model.Cube cube,
                              List<Vector3fc> vertexes, List<Vector2fc> uvs, Vector3fc normal) {
            currentFace = new ArrayList<>(vertexes.size());
            faces.add(currentFace);
        }

        @Override
        public void visitVertex(RenderParams renderParams, ModelVisitContext context, Model.Cube cube,
                                Vector3fc vertex, Vector2fc uv, Vector3fc normal) {
            PoseStack.Pose last = renderParams.poseStack().last();
            last.pose().transformAffine(vertex.x(), vertex.y(), vertex.z(), 1, transformedPosition);
            last.normal().transform(normal, transformedNormal);

            assertTrue(currentFace != null, "visitVertex called before visitFace");
            currentFace.add(new CapturedVertex(
                    transformedPosition.x, transformedPosition.y, transformedPosition.z,
                    uv.x(), uv.y(),
                    transformedNormal.x, transformedNormal.y, transformedNormal.z
            ));
        }

        List<FaceChunk> finish() {
            List<FaceChunk> out = new ArrayList<>();
            for (List<CapturedVertex> face : faces) {
                out.add(FaceChunk.fromVertices(face));
            }
            return List.copyOf(out);
        }
    }
}

