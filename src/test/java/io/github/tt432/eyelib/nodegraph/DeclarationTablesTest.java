package io.github.tt432.eyelib.nodegraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * {@link DeclarationTables} 单测（规格 nodegraph-inline-render-controller §3.1）：
 * geo/tex/mat 声明表从主图 RC 锚点（rc.root / ref.rc）派生的三种来源与类别不匹配防御。
 * 图直接用 record 构造，不走 JSON。
 */
class DeclarationTablesTest {

    private static NodeInstance node(String uid, String type) {
        return NodeInstance.of(uid, type, 0, 0);
    }

    private static NodeInstance node(String uid, String type, Map<String, JsonElement> options) {
        return new NodeInstance(uid, type, 0, 0, options, Map.of());
    }

    private static Map<String, JsonElement> opts(Object... kv) {
        Map<String, JsonElement> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            Object v = kv[i + 1];
            JsonElement e = v instanceof String s ? new JsonPrimitive(s)
                    : v instanceof Number n ? new JsonPrimitive(n)
                    : v instanceof Boolean b ? new JsonPrimitive(b)
                    : (JsonElement) v;
            map.put((String) kv[i], e);
        }
        return map;
    }

    private static Wire wire(String fromNode, String fromPort, String toNode, String toPort) {
        return new Wire(new PortRef(fromNode, fromPort), new PortRef(toNode, toPort));
    }

    private static GraphData graph(List<NodeInstance> nodes, List<Wire> wires) {
        return new GraphData(nodes, wires, List.of(), List.of(), List.of(), Optional.empty());
    }

    @Test
    void rcAnchorsSortedByUid() {
        GraphData main = graph(
                List.of(node("rcr", "rc.root", opts("identifier", "controller.render.a")),
                        node("rrc", "ref.rc", opts("identifier", "controller.render.b")),
                        node("r", "entity.root"),
                        node("g1", "ref.geometry", opts("identifier", "geometry.a"))),
                List.of());
        List<NodeInstance> anchors = DeclarationTables.rcAnchors(main);
        assertEquals(2, anchors.size());
        assertEquals("rcr", anchors.get(0).uid());
        assertEquals("rrc", anchors.get(1).uid());
    }

    @Test
    void collectsFromDeclarationPorts() {
        // 来源一：锚点 geometries/textures/materials 声明端口的直连 ref（rc.root 与 ref.rc 均可）
        GraphData main = graph(
                List.of(node("rcr", "rc.root", opts("identifier", "controller.render.a")),
                        node("rrc", "ref.rc", opts("identifier", "controller.render.b")),
                        node("g1", "ref.geometry", opts("short_name", "ga", "identifier", "geometry.a")),
                        node("t1", "ref.texture", opts("short_name", "tb", "path", "textures/b")),
                        node("m1", "ref.material", opts("short_name", "mc", "material", "entity_alphatest"))),
                List.of(wire("g1", "ref", "rcr", "decl_geometries"),
                        wire("t1", "ref", "rrc", "decl_textures"),
                        wire("m1", "ref", "rrc", "decl_materials")));

        DeclarationTables.Tables tables = DeclarationTables.collect(main);

        assertEquals(Map.of("ga", "geometry.a"), tables.geometry());
        assertEquals(Map.of("tb", "textures/b"), tables.textures());
        assertEquals(Map.of("mc", "entity_alphatest"), tables.materials());
        assertTrue(DeclarationTables.invalidDeclarationSources(main).isEmpty());
    }

    @Test
    void collectsFromGeometryFieldPort() {
        // 来源二：rc.root geometry 字段端口上游的 ref.geometry（声明+引用一体）
        GraphData main = graph(
                List.of(node("rcr", "rc.root", opts("identifier", "controller.render.a")),
                        node("g1", "ref.geometry", opts("short_name", "default", "identifier", "geometry.a"))),
                List.of(wire("g1", "ref", "rcr", "geometry")));

        DeclarationTables.Tables tables = DeclarationTables.collect(main);

        assertEquals(Map.of("default", "geometry.a"), tables.geometry());
    }

    @Test
    void collectsRefsNestedInExpressionTrees() {
        // 可达性模型：ref 嵌在表达式树内部（悦灵 geometry = v.x ? geometry.a : geometry.b），
        // 不直连锚点但存在连线路径 → 入表
        GraphData main = graph(
                List.of(node("rcr", "rc.root", opts("identifier", "controller.render.a")),
                        node("cond", "query.call", opts("function", "query.x", "arg_count", 0)),
                        node("tern", "op.ternary"),
                        node("g1", "ref.geometry", opts("short_name", "a", "identifier", "geometry.a")),
                        node("g2", "ref.geometry", opts("short_name", "b", "identifier", "geometry.b"))),
                List.of(wire("cond", "out", "tern", "cond"),
                        wire("g1", "ref", "tern", "a"),
                        wire("g2", "ref", "tern", "b"),
                        wire("tern", "out", "rcr", "geometry")));

        DeclarationTables.Tables tables = DeclarationTables.collect(main);

        assertEquals(Map.of("a", "geometry.a", "b", "geometry.b"), tables.geometry());
    }

    @Test
    void collectsFromEntryValues() {
        // 来源三：rc.root 的 list.entry / material.entry 条目 value 上游的 ref
        GraphData main = graph(
                List.of(node("rcr", "rc.root", opts("identifier", "controller.render.a")),
                        node("le", "list.entry"),
                        node("t1", "ref.texture", opts("short_name", "skin", "path", "textures/a")),
                        node("me", "material.entry", opts("pattern", "*")),
                        node("m1", "ref.material", opts("short_name", "default", "material", "entity_alphatest"))),
                List.of(wire("t1", "ref", "le", "value"),
                        wire("le", "entry", "rcr", "textures"),
                        wire("m1", "ref", "me", "value"),
                        wire("me", "entry", "rcr", "materials")));

        DeclarationTables.Tables tables = DeclarationTables.collect(main);

        assertEquals(Map.of("skin", "textures/a"), tables.textures());
        assertEquals(Map.of("default", "entity_alphatest"), tables.materials());
    }

    @Test
    void entryValueRefNotCollectedWhenEntryNotWiredToRc() {
        // 条目未接入 rc.root 的 SLOT → 其 value 上游 ref 不入表
        GraphData main = graph(
                List.of(node("rcr", "rc.root", opts("identifier", "controller.render.a")),
                        node("le", "list.entry"),
                        node("t1", "ref.texture", opts("short_name", "skin", "path", "textures/a"))),
                List.of(wire("t1", "ref", "le", "value")));

        DeclarationTables.Tables tables = DeclarationTables.collect(main);

        assertTrue(tables.textures().isEmpty());
    }

    @Test
    void wrongCategoryDeclarationWireReported() {
        // 错类别声明线（ref.texture 接 decl_geometries）→ invalidDeclarationSources 列出
        // （组装器据此报 INVALID_DECLARATION_REF）；实现按「可达锚点」口径仍会将其收入本类别表
        GraphData main = graph(
                List.of(node("rcr", "rc.root", opts("identifier", "controller.render.a")),
                        node("t1", "ref.texture", opts("short_name", "bad", "path", "textures/a")),
                        node("g1", "ref.geometry", opts("short_name", "good", "identifier", "geometry.a"))),
                List.of(wire("t1", "ref", "rcr", "decl_geometries"),
                        wire("g1", "ref", "rcr", "decl_geometries")));

        List<DeclarationTables.InvalidRef> invalid = DeclarationTables.invalidDeclarationSources(main);

        assertEquals(1, invalid.size());
        assertEquals("rcr", invalid.get(0).anchorUid());
        assertEquals("t1", invalid.get(0).source().uid());
        assertEquals("decl_geometries", invalid.get(0).port());

        DeclarationTables.Tables tables = DeclarationTables.collect(main);
        assertEquals(Map.of("good", "geometry.a"), tables.geometry());
        // 注意：实现以「ref 能否沿连线到达任一 RC 锚点」派生，t1 仍进入 textures 表
        assertEquals(Map.of("bad", "textures/a"), tables.textures());
    }

    @Test
    void sameShortNameFirstWins() {
        // 同有效短名 putIfAbsent 保留先者（锚点 uid 序）；冲突由验证器 REF_CONFLICT 报告
        GraphData main = graph(
                List.of(node("rcr", "rc.root", opts("identifier", "controller.render.a")),
                        node("g1", "ref.geometry", opts("short_name", "default", "identifier", "geometry.a")),
                        node("g2", "ref.geometry", opts("short_name", "default", "identifier", "geometry.b"))),
                List.of(wire("g1", "ref", "rcr", "decl_geometries"),
                        wire("g2", "ref", "rcr", "decl_geometries")));

        DeclarationTables.Tables tables = DeclarationTables.collect(main);

        assertEquals(Map.of("default", "geometry.a"), tables.geometry());
    }

    @Test
    void entityRootDeclarationWiresAreNotCollected() {
        // v4：entity.root 不再有 geo/tex/mat 声明端口；手工构造的 entity.root 连线不入派生集合
        // （short_name 用非协议名——协议名 default/material 走 v6 carve-out 恒入表）
        GraphData main = graph(
                List.of(node("r", "entity.root"),
                        node("g1", "ref.geometry", opts("short_name", "foo", "identifier", "geometry.a"))),
                List.of(wire("g1", "ref", "r", "geometries")));

        DeclarationTables.Tables tables = DeclarationTables.collect(main);

        assertTrue(tables.geometry().isEmpty());
        assertTrue(DeclarationTables.rcAnchors(main).isEmpty());
    }

    @Test
    void protocolShortNameRefsCollectedWithoutWires() {
        // v6 carve-out（规格 §3.2）：协议短名 ref（default / texture.material）在图中出现即入表，
        // 无需接线；非协议短名不接线则不入表
        GraphData main = graph(
                List.of(node("rcr", "rc.root", opts("identifier", "controller.render.a")),
                        node("g1", "ref.geometry", opts("short_name", "default", "identifier", "geometry.a")),
                        node("t1", "ref.texture", opts("short_name", "material", "path", "textures/mat")),
                        node("m1", "ref.material", opts("short_name", "fancy", "material", "entity_alphatest"))),
                List.of());

        DeclarationTables.Tables tables = DeclarationTables.collect(main);

        assertEquals(Map.of("default", "geometry.a"), tables.geometry());
        assertEquals(Map.of("material", "textures/mat"), tables.textures());
        assertTrue(tables.materials().isEmpty());
    }
}
