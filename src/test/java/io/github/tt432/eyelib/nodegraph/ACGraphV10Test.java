package io.github.tt432.eyelib.nodegraph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.assembly.AnimationControllerAssembler;
import io.github.tt432.eyelib.nodegraph.assembly.AssemblyResult;
import io.github.tt432.eyelib.nodegraph.assembly.ClientEntityAssembler;
import io.github.tt432.eyelib.nodegraph.decompile.ImportResult;
import io.github.tt432.eyelib.nodegraph.decompile.JsonGraphImporters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * format v10（AC 图边化 + 粒子/音效引用，规格 nodegraph-ac-graph-and-effects）单测：
 * 迁移、导入、导出的边形态与粒子/音效全链路。
 * （AssemblyTestSupport/DecompileTestSupport 均包私有不可复用，辅助函数本文件自带。）
 */
class ACGraphV10Test {

    // ---------- 自带构造辅助（同 AssemblyTestSupport 语义） ----------

    private static NodeInstance node(String uid, String type, Map<String, JsonElement> options) {
        return new NodeInstance(uid, type, 0, 0, options, Map.of());
    }

    private static NodeInstance node(String uid, String type) {
        return NodeInstance.of(uid, type, 0, 0);
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

    private static GraphLibrary lib(GraphKind kind, GraphData main) {
        return new GraphLibrary(1, kind, "root", Map.of("root", main));
    }

    private static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static long countCode(List<Diagnostic> diagnostics, String code) {
        return diagnostics.stream().filter(d -> d.code().equals(code)).count();
    }

    private static boolean hasWire(GraphData g, String fromNode, String fromPort,
                                   String toNode, String toPort) {
        return g.wires().stream().anyMatch(w -> w.from().node().equals(fromNode)
                && w.from().port().equals(fromPort)
                && w.to().node().equals(toNode) && w.to().port().equals(toPort));
    }

    // ---------- 迁移 v9 → v10 ----------

    @Test
    void migrateV9ToV10ConvertsOptionsToEdges() {
        GraphData data = graph(
                List.of(
                        node("root", "ac.root", opts(
                                "identifier", "controller.animation.test",
                                "initial_state", "idle")),
                        node("s1", "ac.state", opts("name", "idle")),
                        node("s2", "ac.state", opts("name", "walk")),
                        node("t1", "ac.transition", opts("target", "walk"))),
                List.of(wire("s1", "state", "root", "states"),
                        wire("s2", "state", "root", "states"),
                        wire("t1", "transition", "s1", "transitions")));
        GraphLibrary v9 = new GraphLibrary(9, GraphKind.ANIMATION_CONTROLLER, "root",
                java.util.Map.of("root", data));

        GraphLibrary migrated = GraphMigrations.migrate(v9);

        assertEquals(10, migrated.formatVersion());
        GraphData g = migrated.mainGraph();
        assertTrue(hasWire(g, "s1", "state", "root", "initial"), "initial_state 应迁移为 initial 图边");
        assertTrue(hasWire(g, "t1", "target", "s2", "incoming"), "target 应迁移为 incoming 图边");
        assertTrue(g.findNode("root").orElseThrow().options().get("initial_state") == null,
                "initial_state 选项应删除");
        assertTrue(g.findNode("t1").orElseThrow().options().get("target") == null,
                "target 选项应删除");
    }

    @Test
    void migrateV9ToV10DropsUnresolvableTarget() {
        GraphData data = graph(
                List.of(
                        node("root", "ac.root", opts(
                                "identifier", "controller.animation.test",
                                "initial_state", "ghost")),
                        node("s1", "ac.state", opts("name", "idle")),
                        node("t1", "ac.transition", opts("target", "nowhere"))),
                List.of(wire("s1", "state", "root", "states")));
        GraphLibrary v9 = new GraphLibrary(9, GraphKind.ANIMATION_CONTROLLER, "root",
                java.util.Map.of("root", data));

        GraphData g = GraphMigrations.migrate(v9).mainGraph();

        assertFalse(hasWire(g, "s1", "state", "root", "initial"), "解析失败的 initial_state 不建边");
        assertFalse(g.wires().stream().anyMatch(w -> w.from().port().equals("target")),
                "解析失败的 target 不建边");
        assertTrue(g.findNode("root").orElseThrow().options().get("initial_state") == null);
        assertTrue(g.findNode("t1").orElseThrow().options().get("target") == null);
    }

    // ---------- 导入 ----------

    private static final String AC_EFFECTS_JSON = """
            {
              "format_version": "1.10.0",
              "animation_controllers": {
                "controller.animation.fx": {
                  "initial_state": "idle",
                  "states": {
                    "idle": {
                      "animations": ["idle_anim"],
                      "particle_effects": [
                        {"effect": "dust", "locator": "head", "bind_to_actor": false,
                         "pre_effect_script": "query.health"}
                      ],
                      "sound_effects": [{"effect": "idle_sound"}],
                      "transitions": [{"walk": "query.modified_move_speed"}]
                    },
                    "walk": {"animations": ["walk_anim"]}
                  }
                }
              }
            }
            """;

    @Test
    void importInitialAndTransitionsBecomeEdges() {
        ImportResult imported = JsonGraphImporters.importAnimationControllers(
                parse(AC_EFFECTS_JSON), "controller.animation.fx");
        assertFalse(imported.hasErrors(), () -> imported.diagnostics().toString());
        GraphData g = imported.library().mainGraph();

        NodeInstance idle = g.nodes().stream()
                .filter(n -> n.type().equals("ac.state") && "idle".equals(n.optionString("name", "")))
                .findFirst().orElseThrow();
        NodeInstance walk = g.nodes().stream()
                .filter(n -> n.type().equals("ac.state") && "walk".equals(n.optionString("name", "")))
                .findFirst().orElseThrow();
        NodeInstance transition = g.nodes().stream()
                .filter(n -> n.type().equals("ac.transition")).findFirst().orElseThrow();

        assertTrue(hasWire(g, idle.uid(), "state", "root", "initial"), "initial_state → initial 图边");
        assertTrue(hasWire(g, transition.uid(), "target", walk.uid(), "incoming"),
                "transition.target → incoming 图边");
        assertFalse(transition.options().containsKey("target"), "target 不再是选项");
    }

    @Test
    void importStateParticleAndSoundEffects() {
        ImportResult imported = JsonGraphImporters.importAnimationControllers(
                parse(AC_EFFECTS_JSON), "controller.animation.fx");
        assertFalse(imported.hasErrors(), () -> imported.diagnostics().toString());
        GraphData g = imported.library().mainGraph();

        NodeInstance entry = g.nodes().stream()
                .filter(n -> n.type().equals("particle.entry")).findFirst().orElseThrow();
        NodeInstance particleRef = g.nodes().stream()
                .filter(n -> n.type().equals("ref.particle")).findFirst().orElseThrow();
        NodeInstance soundRef = g.nodes().stream()
                .filter(n -> n.type().equals("ref.sound")).findFirst().orElseThrow();

        assertEquals("head", entry.optionString("locator", ""));
        assertEquals("false", entry.options().get("bind_to_actor").getAsString());
        assertEquals("dust", particleRef.optionString("short_name", ""));
        assertEquals("idle_sound", soundRef.optionString("short_name", ""));
        assertTrue(hasWire(g, particleRef.uid(), "ref", entry.uid(), "ref"));
        assertTrue(g.wires().stream().anyMatch(w -> w.to().node().equals(entry.uid())
                && w.to().port().equals("script")), "pre_effect_script 应接 script 值槽");
        assertTrue(g.wires().stream().anyMatch(w -> w.from().node().equals(soundRef.uid())
                && w.from().port().equals("ref") && w.to().port().equals("sounds")),
                "ref.sound 应直插 ac.state.sounds");
    }

    // ---------- 导出 ----------

    @Test
    void assembleEmitsParticleAndSoundEffects() {
        GraphLibrary lib = lib(GraphKind.ANIMATION_CONTROLLER, graph(
                List.of(
                        node("root", "ac.root", opts("identifier", "controller.animation.fx")),
                        node("s1", "ac.state", opts("name", "idle")),
                        node("pe1", "particle.entry", opts("locator", "head", "bind_to_actor", false)),
                        node("rp1", "ref.particle", opts("short_name", "dust")),
                        node("rs1", "ref.sound", opts("short_name", "idle_sound")),
                        node("q1", "query.call", opts("function", "query.health", "arg_count", 0))),
                List.of(
                        wire("s1", "state", "root", "states"),
                        wire("s1", "state", "root", "initial"),
                        wire("rp1", "ref", "pe1", "ref"),
                        wire("q1", "out", "pe1", "script"),
                        wire("pe1", "entry", "s1", "particles"),
                        wire("rs1", "ref", "s1", "sounds"))));

        AssemblyResult r = AnimationControllerAssembler.assemble(lib);

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        JsonObject state = r.json().getAsJsonObject("animation_controllers")
                .getAsJsonObject("controller.animation.fx")
                .getAsJsonObject("states").getAsJsonObject("idle");
        JsonObject particle = state.getAsJsonArray("particle_effects").get(0).getAsJsonObject();
        assertEquals("dust", particle.get("effect").getAsString());
        assertEquals("head", particle.get("locator").getAsString());
        assertFalse(particle.get("bind_to_actor").getAsBoolean());
        assertTrue(particle.has("pre_effect_script"), "script 连线应发射 pre_effect_script");
        JsonObject sound = state.getAsJsonArray("sound_effects").get(0).getAsJsonObject();
        assertEquals("idle_sound", sound.get("effect").getAsString());
    }

    // ---------- 实体粒子/音效声明表往返 ----------

    private static final String ENTITY_FX_JSON = """
            {
              "format_version": "1.21.0",
              "minecraft:client_entity": {
                "description": {
                  "identifier": "example:fx",
                  "geometry": {"default": "geometry.fx"},
                  "textures": {"default": "textures/fx"},
                  "materials": {"default": "entity_alphatest"},
                  "particle_effects": {"dust": "example:dust_particle"},
                  "sound_effects": {"idle_sound": "example.idle"}
                }
              }
            }
            """;

    @Test
    void entityParticleSoundTablesRoundTrip() {
        ImportResult imported = JsonGraphImporters.importClientEntity(parse(ENTITY_FX_JSON));
        assertFalse(imported.hasErrors(), () -> imported.diagnostics().toString());
        GraphData g = imported.library().mainGraph();

        NodeInstance particleRef = g.nodes().stream()
                .filter(n -> n.type().equals("ref.particle")).findFirst().orElseThrow();
        NodeInstance soundRef = g.nodes().stream()
                .filter(n -> n.type().equals("ref.sound")).findFirst().orElseThrow();
        assertEquals("example:dust_particle", particleRef.optionString("identifier", ""));
        assertEquals("example.idle", soundRef.optionString("identifier", ""));
        assertTrue(hasWire(g, particleRef.uid(), "ref", "root", "particles"));
        assertTrue(hasWire(g, soundRef.uid(), "ref", "root", "sounds"));

        AssemblyResult assembled = ClientEntityAssembler.assemble(imported.library());
        assertFalse(assembled.hasErrors(), () -> assembled.diagnostics().toString());
        JsonObject description = assembled.json().getAsJsonObject("minecraft:client_entity")
                .getAsJsonObject("description");
        assertEquals("example:dust_particle",
                description.getAsJsonObject("particle_effects").get("dust").getAsString());
        assertEquals("example.idle",
                description.getAsJsonObject("sound_effects").get("idle_sound").getAsString());
    }

    @Test
    void assembleTransitionWithoutTargetEdgeIsUnknownState() {
        GraphLibrary lib = lib(GraphKind.ANIMATION_CONTROLLER, graph(
                List.of(
                        node("root", "ac.root", opts("identifier", "controller.animation.test")),
                        node("s1", "ac.state", opts("name", "idle")),
                        node("t1", "ac.transition")),
                List.of(wire("s1", "state", "root", "states"),
                        wire("s1", "state", "root", "initial"),
                        wire("t1", "transition", "s1", "transitions"))));

        AssemblyResult r = AnimationControllerAssembler.assemble(lib);

        assertEquals(1, countCode(r.diagnostics(), "UNKNOWN_STATE"),
                () -> r.diagnostics().toString());
    }
}
