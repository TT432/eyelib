package io.github.tt432.eyelib.nodegraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonPrimitive;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * ShortNameOps 图改写（规格 nodegraph-shortname-elimination D4）单测。
 */
class ShortNameOpsTest {

    private static NodeInstance ref(String uid, String type, Map<String, com.google.gson.JsonElement> options) {
        return new NodeInstance(uid, type, 0, 0, options, Map.of());
    }

    private static Map<String, com.google.gson.JsonElement> opts(Object... kv) {
        Map<String, com.google.gson.JsonElement> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], new JsonPrimitive(String.valueOf(kv[i + 1])));
        }
        return map;
    }

    private static GraphLibrary lib(GraphData... graphs) {
        Map<String, GraphData> map = new LinkedHashMap<>();
        for (int i = 0; i < graphs.length; i++) {
            map.put(i == 0 ? "root" : "g" + i, graphs[i]);
        }
        return new GraphLibrary(1, GraphKind.CLIENT_ENTITY, "root", map);
    }

    private static GraphData graph(List<NodeInstance> nodes) {
        return new GraphData(nodes, List.of(), List.of(), List.of(), List.of(), Optional.empty());
    }

    // ---------- normalize ----------

    @Test
    void normalizeStripsExplicitShortNames() {
        GraphLibrary in = lib(graph(List.of(
                ref("a", "ref.geometry", opts("short_name", "main_geo", "identifier", "geometry.x")),
                ref("b", "ref.texture", opts("path", "textures/a")), // 无显式 → 不动
                ref("c", "ref.animation", opts("short_name", "walk", "identifier", "animation.w")),
                new NodeInstance("d", "const.number", 0, 0, opts("value", "1"), Map.of()))));

        ShortNameOps.RewriteResult r = ShortNameOps.normalize(in);

        assertEquals(2, r.changed());
        var nodes = r.library().graphs().get("root").nodes();
        assertFalse(nodes.get(0).options().containsKey("short_name"));
        assertEquals("geometry.x", nodes.get(0).options().get("identifier").getAsString());
        assertFalse(nodes.get(1).options().containsKey("short_name"));
        assertFalse(nodes.get(2).options().containsKey("short_name"));
        // 非 ref 节点不动
        assertEquals("1", nodes.get(3).options().get("value").getAsString());
        // 入参不可变
        assertTrue(in.graphs().get("root").nodes().get(0).options().containsKey("short_name"));
    }

    @Test
    void normalizeIsIdempotent() {
        GraphLibrary in = lib(graph(List.of(
                ref("a", "ref.geometry", opts("short_name", "default", "identifier", "geometry.x")))));
        ShortNameOps.RewriteResult once = ShortNameOps.normalize(in);
        ShortNameOps.RewriteResult twice = ShortNameOps.normalize(once.library());
        assertEquals(0, once.changed()); // default 是协议名，不剥
        assertEquals(0, twice.changed());
    }

    @Test
    void normalizePreservesProtocolShortNames() {
        // default（运行时回退协议）与 texture.material（Bedrock 动态材质纹理协议字）保留
        GraphLibrary in = lib(graph(List.of(
                ref("a", "ref.geometry", opts("short_name", "default", "identifier", "geometry.x")),
                ref("b", "ref.texture", opts("short_name", "material", "path", "")),
                ref("c", "ref.texture", opts("short_name", "default", "path", "textures/a")),
                ref("d", "ref.material", opts("short_name", "material", "material", "entity_alphatest")),
                ref("e", "ref.animation", opts("short_name", "walk", "identifier", "animation.w")))));

        ShortNameOps.RewriteResult r = ShortNameOps.normalize(in);

        assertEquals(2, r.changed()); // d（ref.material 的 "material" 非协议字）与 e 被剥
        var nodes = r.library().graphs().get("root").nodes();
        assertTrue(nodes.get(0).options().containsKey("short_name"));
        assertTrue(nodes.get(1).options().containsKey("short_name"));
        assertTrue(nodes.get(2).options().containsKey("short_name"));
        assertFalse(nodes.get(3).options().containsKey("short_name"));
        assertFalse(nodes.get(4).options().containsKey("short_name"));
    }

    // ---------- backfillIdentifiers ----------

    @Test
    void backfillFillsIdentifierKeepsShortName() {
        ShortNameOps.KnownTables known = new ShortNameOps.KnownTables.Builder()
                .put("ref.geometry", "default", "geometry.test.model")
                .put("ref.texture", "skin", "textures/entity/a")
                .build();
        GraphLibrary in = lib(graph(List.of(
                ref("g", "ref.geometry", opts("short_name", "default")),       // 裸 → 回填
                ref("t", "ref.texture", opts("short_name", "skin")),          // 裸 → 回填
                ref("m", "ref.material", opts("short_name", "mat")),          // 未命中 → 不动
                ref("g2", "ref.geometry",                                     // 已有标识符 → 不动
                        opts("short_name", "default", "identifier", "geometry.other")))));

        ShortNameOps.RewriteResult r = ShortNameOps.backfillIdentifiers(in, known);

        assertEquals(2, r.changed());
        var nodes = r.library().graphs().get("root").nodes();
        assertEquals("geometry.test.model", nodes.get(0).options().get("identifier").getAsString());
        assertEquals("default", nodes.get(0).options().get("short_name").getAsString());
        assertEquals("textures/entity/a", nodes.get(1).options().get("path").getAsString());
        assertFalse(nodes.get(2).options().containsKey("material"));
        assertEquals("geometry.other", nodes.get(3).options().get("identifier").getAsString());
    }

    @Test
    void backfillAnimationFallsBackToAcTable() {
        // animate 裸短名 ref.animation 可查 AC 表（D6 合并命名空间）
        ShortNameOps.KnownTables known = new ShortNameOps.KnownTables.Builder()
                .put("ref.ac", "main", "controller.animation.test.main")
                .build();
        GraphLibrary in = lib(graph(List.of(
                ref("a", "ref.animation", opts("short_name", "main")))));

        ShortNameOps.RewriteResult r = ShortNameOps.backfillIdentifiers(in, known);

        assertEquals(1, r.changed());
        assertEquals("controller.animation.test.main",
                r.library().graphs().get("root").nodes().get(0).options().get("identifier").getAsString());
    }

    @Test
    void backfillEmptyTablesIsNoop() {
        GraphLibrary in = lib(graph(List.of(
                ref("g", "ref.geometry", opts("short_name", "default")))));
        ShortNameOps.RewriteResult r = ShortNameOps.backfillIdentifiers(in, ShortNameOps.KnownTables.EMPTY);
        assertEquals(0, r.changed());
        assertEquals(in, r.library());
    }

    @Test
    void backfillCaseInsensitiveLookup() {
        // 运行时 scope 键 lowercase：大小写不敏感命中
        ShortNameOps.KnownTables known = new ShortNameOps.KnownTables.Builder()
                .put("ref.geometry", "default", "geometry.test.model")
                .build();
        GraphLibrary in = lib(graph(List.of(
                ref("g", "ref.geometry", opts("short_name", "Default")))));

        ShortNameOps.RewriteResult r = ShortNameOps.backfillIdentifiers(in, known);

        assertEquals(1, r.changed());
    }
}
