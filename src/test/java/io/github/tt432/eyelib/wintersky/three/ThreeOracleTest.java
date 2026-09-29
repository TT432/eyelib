package io.github.tt432.eyelib.wintersky.three;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * three.js r134 移植层 vs Node 端 three 的差分测试。
 *
 * <p>用例由 scripts/wintersky-oracle/three_cases.mjs 生成并冻结在
 * src/test/resources/wintersky/three_cases.json。数值容差 1e-9
 * （吸收 V8 与 JVM 三角函数的末位 ulp 差）；float32 数组逐位比较。
 */
class ThreeOracleTest {

    private static final double EPS = 1e-9;

    @Test
    void threeMatchesNodeOracle() throws Exception {
        JsonArray cases;
        try (Reader reader = new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream("/wintersky/three_cases.json"),
                        "three_cases.json missing"),
                StandardCharsets.UTF_8)) {
            cases = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("cases");
        }
        assertTrue(cases.size() > 50, "oracle cases should be loaded, got " + cases.size());

        for (JsonElement caseElement : cases) {
            JsonObject c = caseElement.getAsJsonObject();
            String kind = c.get("kind").getAsString();
            JsonObject input = c.getAsJsonObject("input");
            JsonElement expected = c.get("expected");

            try {
                runCase(kind, input, expected);
            } catch (AssertionError e) {
                fail("case " + kind + " input=" + input + " failed: " + e.getMessage());
            }
        }
    }

    private static void runCase(String kind, JsonObject input, JsonElement expected) {
        switch (kind) {
            case "vec3_apply_quat" -> assertVec(expected,
                    vec(input, "v").applyQuaternion(quat(input, "q")));
            case "vec3_apply_euler_xyz", "vec3_apply_euler_yxz" -> assertVec(expected,
                    vec(input, "v").applyEuler(euler(input, "e")));
            case "vec3_reflect", "vec3_reflect_tilted" -> assertVec(expected,
                    vec(input, "v").reflect(vec(input, "n")));
            case "vec3_add_scaled" -> assertVec(expected,
                    vec(input, "a").addScaledVector(vec(input, "b"), input.get("s").getAsDouble()));
            case "vec3_normalize_zero" -> assertVec(expected, vec(input, "v").normalize());
            case "vec3_cross" -> assertVec(expected, vec(input, "a").cross(vec(input, "b")));

            case "quat_from_euler" -> assertQuat(expected,
                    new Quaternion().setFromEuler(new Euler(
                            arr(input, "e")[0], arr(input, "e")[1], arr(input, "e")[2],
                            input.get("order").getAsString())));
            case "quat_from_unit_vectors", "quat_from_unit_vectors_opposite", "quat_from_unit_vectors_opposite_x" ->
                    assertQuat(expected, new Quaternion().setFromUnitVectors(vec(input, "from"), vec(input, "to")));
            case "quat_multiply" -> assertQuat(expected, quat(input, "a").multiply(quat(input, "b")));
            case "quat_premultiply" -> assertQuat(expected, quat(input, "a").premultiply(quat(input, "b")));
            case "quat_invert" -> assertQuat(expected, quat(input, "q").invert());
            case "quat_from_rotation_matrix", "quat_from_rotation_matrix2" ->
                    assertQuat(expected, new Quaternion().setFromRotationMatrix(mat(input, "m")));

            case "euler_from_matrix", "euler_from_matrix_gimbal" -> {
                Euler e = new Euler().setFromRotationMatrix(mat(input, "m"), input.get("order").getAsString(), true);
                assertEuler(expected, e);
            }
            case "euler_reorder" -> assertEuler(expected, euler(input, "e").reorder(input.get("order").getAsString()));

            case "mat4_compose" -> assertMat(expected,
                    new Matrix4().compose(vec(input, "pos"), quat(input, "quat"), vec(input, "scale")));
            case "mat4_decompose" -> {
                Vector3 pos = new Vector3();
                Quaternion quat = new Quaternion();
                Vector3 scale = new Vector3();
                mat(input, "m").decompose(pos, quat, scale);
                JsonObject exp = expected.getAsJsonObject();
                assertVec(exp.get("pos"), pos);
                assertQuat(exp.get("quat"), quat);
                assertVec(exp.get("scale"), scale);
            }
            case "mat4_multiply" -> assertMat(expected, mat(input, "a").multiply(mat(input, "b")));
            case "mat4_invert" -> assertMat(expected, mat(input, "m").invert());
            case "mat4_determinant" -> assertEquals(expected.getAsDouble(), mat(input, "m").determinant(), EPS);
            case "mat4_look_at", "mat4_look_at_degenerate" -> assertMat(expected,
                    new Matrix4().lookAt(vec(input, "eye"), vec(input, "target"), vec(input, "up")));
            case "mat4_extract_rotation" -> assertMat(expected, new Matrix4().extractRotation(mat(input, "m")));

            case "object3d_world_matrix", "object3d_world_position", "object3d_world_quaternion",
                 "object3d_local_to_world", "object3d_world_to_local", "object3d_look_at",
                 "object3d_look_at_euler_sync" -> runObject3dCase(kind, input, expected);
            case "object3d_rotation_sync_quat" -> {
                Object3D o = new Object3D();
                Euler e = euler(input, "e");
                o.rotation.set(e.getX(), e.getY(), e.getZ(), "YXZ");
                assertQuat(expected, o.quaternion);
            }
            case "object3d_rotation_z_increment" -> {
                Object3D o = new Object3D();
                o.rotation.set(0.3, -1.1, 2.2, "YXZ");
                o.rotation.setZ(o.rotation.getZ() + 0.5);
                assertQuat(expected, o.quaternion);
            }

            case "plane_intersects_line" -> {
                Plane plane = plane(input);
                Line3 line = line(input);
                assertEquals(expected.getAsBoolean(), plane.intersectsLine(line));
            }
            case "plane_intersect_line_hit", "plane_intersect_line_miss", "plane_intersect_line_tilted" -> {
                Plane plane = plane(input);
                Line3 line = line(input);
                Vector3 target = plane.intersectLine(line, new Vector3());
                if (expected.isJsonNull()) {
                    assertEquals(null, target, "expected no intersection");
                } else {
                    assertVec(expected, target);
                }
            }
            case "plane_intersects_sphere", "plane_intersects_sphere_far" -> {
                Plane plane = plane(input);
                Sphere sphere = new Sphere(vec(input, "center"), input.get("radius").getAsDouble());
                assertEquals(expected.getAsBoolean(), plane.intersectsSphere(sphere));
            }
            case "plane_set_components_not_normalized" -> {
                double[] p = arr(input, "plane");
                Plane plane = new Plane().setComponents(p[0], p[1], p[2], p[3]);
                double[] exp = toArray(expected);
                assertEquals(exp[0], plane.normal.x, EPS);
                assertEquals(exp[1], plane.normal.y, EPS);
                assertEquals(exp[2], plane.normal.z, EPS);
                assertEquals(exp[3], plane.constant, EPS);
            }

            case "spline_curve" -> {
                List<Vector2> points = new ArrayList<>();
                for (JsonElement p : input.getAsJsonArray("points")) {
                    double[] xy = toArray(p);
                    points.add(new Vector2(xy[0], xy[1]));
                }
                Vector2 r = new SplineCurve(points).getPoint(input.get("t").getAsDouble());
                double[] exp = toArray(expected);
                assertEquals(exp[0], r.x, EPS, "spline x");
                assertEquals(exp[1], r.y, EPS, "spline y");
            }
            case "cubic_bezier" -> {
                CubicBezierCurve curve = new CubicBezierCurve(
                        vec2(input, "v0"), vec2(input, "v1"), vec2(input, "v2"), vec2(input, "v3"));
                Vector2 r = curve.getPoint(input.get("t").getAsDouble());
                double[] exp = toArray(expected);
                assertEquals(exp[0], r.x, EPS, "bezier x");
                assertEquals(exp[1], r.y, EPS, "bezier y");
            }

            case "plane_geometry" -> {
                PlaneGeometry geo = new PlaneGeometry(input.get("width").getAsDouble(),
                        input.get("height").getAsDouble(), 1, 1);
                JsonObject exp = expected.getAsJsonObject();
                assertFloatArray(toArray(exp.get("position")), geo.getAttribute("position").array.data);
                assertFloatArray(toArray(exp.get("normal")), geo.getAttribute("normal").array.data);
                assertFloatArray(toArray(exp.get("uv")), geo.getAttribute("uv").array.data);
                int[] expIndex = new int[exp.getAsJsonArray("index").size()];
                for (int i = 0; i < expIndex.length; i++) {
                    expIndex[i] = exp.getAsJsonArray("index").get(i).getAsInt();
                }
                org.junit.jupiter.api.Assertions.assertArrayEquals(expIndex, geo.getIndex());
            }

            default -> fail("unknown case kind: " + kind);
        }
    }

    /** 重建 oracle 脚本中的 root→mid→leaf→watcher 场景图并回放指定观测。 */
    private static void runObject3dCase(String kind, JsonObject input, JsonElement expected) {
        Object3D root = new Object3D();
        root.position.set(1, 2, 3);
        root.rotation.set(0.1, 0.2, 0.3);
        root.scale.set(2, 2, 2);
        Object3D mid = new Object3D();
        mid.position.set(0.5, -1, 2);
        mid.rotation.set(-0.4, 0.9, 0.1, "YXZ");
        mid.scale.set(0.5, 1, 1.5);
        Object3D leaf = new Object3D();
        leaf.position.set(1, 1, 1);
        leaf.rotation.set(2.0, -0.3, 0.8);
        root.add(mid);
        mid.add(leaf);
        root.updateMatrixWorld(true);

        switch (kind) {
            case "object3d_world_matrix" -> assertMat(expected, leaf.matrixWorld);
            case "object3d_world_position" -> assertVec(expected, leaf.getWorldPosition(new Vector3()));
            case "object3d_world_quaternion" -> assertQuat(expected, leaf.getWorldQuaternion(new Quaternion()));
            case "object3d_local_to_world" -> assertVec(expected, leaf.localToWorld(vec(input, "v")));
            case "object3d_world_to_local" -> assertVec(expected, leaf.worldToLocal(vec(input, "v")));
            case "object3d_look_at", "object3d_look_at_euler_sync" -> {
                Object3D watcher = new Object3D();
                watcher.position.set(0.2, 0.3, 0.4);
                mid.add(watcher);
                root.updateMatrixWorld(true);
                watcher.lookAt(new Vector3(10, 0, -5));
                if (kind.equals("object3d_look_at")) {
                    assertQuat(expected, watcher.quaternion);
                } else {
                    assertEuler(expected, watcher.rotation);
                }
            }
            default -> fail("unknown object3d case: " + kind);
        }
    }

    // ---- helpers ----

    private static double[] arr(JsonObject obj, String key) {
        return toArray(obj.get(key));
    }

    private static double[] toArray(JsonElement element) {
        JsonArray a = element.getAsJsonArray();
        double[] out = new double[a.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = a.get(i).getAsDouble();
        }
        return out;
    }

    private static Vector3 vec(JsonObject obj, String key) {
        double[] v = arr(obj, key);
        return new Vector3(v[0], v[1], v[2]);
    }

    private static Vector2 vec2(JsonObject obj, String key) {
        double[] v = arr(obj, key);
        return new Vector2(v[0], v[1]);
    }

    private static Quaternion quat(JsonObject obj, String key) {
        double[] q = arr(obj, key);
        return new Quaternion(q[0], q[1], q[2], q[3]);
    }

    private static Euler euler(JsonObject obj, String key) {
        JsonArray a = obj.getAsJsonArray(key);
        return new Euler(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble(),
                a.get(3).getAsString());
    }

    private static Matrix4 mat(JsonObject obj, String key) {
        return new Matrix4().fromArray(arr(obj, key));
    }

    private static Plane plane(JsonObject input) {
        double[] p = arr(input, "plane");
        Plane plane = new Plane().setComponents(p[0], p[1], p[2], p[3]);
        if (input.has("normalize") && input.get("normalize").getAsBoolean()) {
            plane.normalize();
        }
        return plane;
    }

    private static Line3 line(JsonObject input) {
        JsonArray l = input.getAsJsonArray("line");
        double[] s = toArray(l.get(0));
        double[] e = toArray(l.get(1));
        return new Line3(new Vector3(s[0], s[1], s[2]), new Vector3(e[0], e[1], e[2]));
    }

    private static void assertVec(JsonElement expected, Vector3 actual) {
        double[] exp = toArray(expected);
        assertEquals(exp[0], actual.x, EPS, "x");
        assertEquals(exp[1], actual.y, EPS, "y");
        assertEquals(exp[2], actual.z, EPS, "z");
    }

    private static void assertQuat(JsonElement expected, Quaternion actual) {
        double[] exp = toArray(expected);
        assertEquals(exp[0], actual.getX(), EPS, "qx");
        assertEquals(exp[1], actual.getY(), EPS, "qy");
        assertEquals(exp[2], actual.getZ(), EPS, "qz");
        assertEquals(exp[3], actual.getW(), EPS, "qw");
    }

    private static void assertEuler(JsonElement expected, Euler actual) {
        JsonArray a = expected.getAsJsonArray();
        assertEquals(a.get(0).getAsDouble(), actual.getX(), EPS, "ex");
        assertEquals(a.get(1).getAsDouble(), actual.getY(), EPS, "ey");
        assertEquals(a.get(2).getAsDouble(), actual.getZ(), EPS, "ez");
        assertEquals(a.get(3).getAsString(), actual.getOrder(), "order");
    }

    private static void assertMat(JsonElement expected, Matrix4 actual) {
        double[] exp = toArray(expected);
        for (int i = 0; i < 16; i++) {
            assertEquals(exp[i], actual.elements[i], EPS, "m[" + i + "]");
        }
    }

    private static void assertFloatArray(double[] expected, float[] actual) {
        assertEquals(expected.length, actual.length, "array length");
        for (int i = 0; i < expected.length; i++) {
            assertEquals((float) expected[i], actual[i], "float32[" + i + "]");
        }
    }
}
