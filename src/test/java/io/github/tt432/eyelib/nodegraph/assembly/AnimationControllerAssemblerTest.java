package io.github.tt432.eyelib.nodegraph.assembly;

import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.countCode;
import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.graph;
import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.lib;
import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.node;
import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.opts;
import static io.github.tt432.eyelib.nodegraph.assembly.AssemblyTestSupport.wire;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link AnimationControllerAssembler} 单测：最小图（单状态）、全字段双状态图、
 * DUPLICATE_STATE + UNKNOWN_STATE 错误。
 */
class AnimationControllerAssemblerTest {

    private static JsonObject schemaOf(AssemblyResult r, String identifier) {
        return r.json().getAsJsonObject("animation_controllers").getAsJsonObject(identifier);
    }

    @Test
    void minimalGraph() {
        GraphLibrary lib = lib(GraphKind.ANIMATION_CONTROLLER, graph(
                List.of(
                        node("root", "ac.root", opts(
                                "identifier", "controller.animation.test",
                                "initial_state", "default")),
                        node("s1", "ac.state", opts("name", "default"))),
                List.of(wire("s1", "state", "root", "states"))));

        AssemblyResult r = AnimationControllerAssembler.assemble(lib);

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        assertEquals("1.10.0", r.json().get("format_version").getAsString());
        JsonObject schema = schemaOf(r, "controller.animation.test");
        assertEquals("default", schema.get("initial_state").getAsString());
        JsonObject state = schema.getAsJsonObject("states").getAsJsonObject("default");
        // 数值/布尔选项恒输出，写 JSON 原始类型
        assertEquals(0.2, state.get("blend_transition").getAsDouble());
        assertFalse(state.get("blend_via_shortest_path").getAsBoolean());
        assertFalse(state.has("on_entry"));
        assertFalse(state.has("on_exit"));
        assertFalse(state.has("animations"));
        assertFalse(state.has("transitions"));
    }

    @Test
    void fullGraph() {
        GraphLibrary lib = lib(GraphKind.ANIMATION_CONTROLLER, graph(
                List.of(
                        node("root", "ac.root", opts(
                                "identifier", "controller.animation.full",
                                "initial_state", "idle")),
                        node("s1", "ac.state", opts(
                                "name", "idle",
                                "blend_transition", 0.5,
                                "blend_via_shortest_path", true)),
                        node("e1", "exec.call", opts("function", "query.foo", "arg_count", 0)),
                        node("ae1", "animate.entry"),
                        node("ra1", "ref.animation",
                                opts("short_name", "idle_anim", "identifier", "animation.test.idle")),
                        node("t1", "ac.transition", opts("target", "walk")),
                        node("q1", "query.call",
                                opts("function", "query.modified_distance_moved", "arg_count", 0)),
                        node("s2", "ac.state", opts("name", "walk")),
                        node("sv", "exec.set_var"),
                        node("svt", "variable", opts("name", "stopped")),
                        node("t2", "ac.transition", opts("target", "idle"))),
                List.of(
                        wire("s1", "state", "root", "states"),
                        wire("e1", "exec_out", "s1", "on_entry"),
                        wire("ae1", "entry", "s1", "animations"),
                        wire("ra1", "ref", "ae1", "ref"),
                        wire("t1", "transition", "s1", "transitions"),
                        wire("q1", "out", "t1", "condition"),
                        wire("s2", "state", "root", "states"),
                        wire("sv", "exec_out", "s2", "on_exit"),
                        wire("svt", "out", "sv", "target"),
                        wire("t2", "transition", "s2", "transitions"))));

        AssemblyResult r = AnimationControllerAssembler.assemble(lib);

        assertFalse(r.hasErrors(), () -> r.diagnostics().toString());
        JsonObject schema = schemaOf(r, "controller.animation.full");
        assertEquals("idle", schema.get("initial_state").getAsString());
        JsonObject states = schema.getAsJsonObject("states");
        assertEquals(2, states.entrySet().size());

        JsonObject idle = states.getAsJsonObject("idle");
        assertEquals("query.foo", idle.get("on_entry").getAsString());
        assertFalse(idle.has("on_exit"));
        // animations：weight 未连线 → 端口默认 "1"
        assertEquals("1", idle.getAsJsonObject("animations").get("idle_anim").getAsString());
        assertEquals("query.modified_distance_moved",
                idle.getAsJsonObject("transitions").get("walk").getAsString());
        assertEquals(0.5, idle.get("blend_transition").getAsDouble());
        assertTrue(idle.get("blend_via_shortest_path").getAsBoolean());

        JsonObject walk = states.getAsJsonObject("walk");
        assertEquals("variable.stopped = 0", walk.get("on_exit").getAsString());
        assertFalse(walk.has("on_entry"));
        assertFalse(walk.has("animations"));
        // condition 未连线 → 端口默认 "1"
        assertEquals("1", walk.getAsJsonObject("transitions").get("idle").getAsString());
        assertEquals(0.2, walk.get("blend_transition").getAsDouble());
        assertFalse(walk.get("blend_via_shortest_path").getAsBoolean());
    }

    @Test
    void duplicateAndUnknownState() {
        GraphLibrary lib = lib(GraphKind.ANIMATION_CONTROLLER, graph(
                List.of(
                        node("root", "ac.root", opts(
                                "identifier", "controller.animation.err",
                                "initial_state", "ghost")),
                        node("s1", "ac.state", opts("name", "a")),
                        node("s2", "ac.state", opts("name", "a")),
                        node("t1", "ac.transition", opts("target", "nowhere"))),
                List.of(
                        wire("s1", "state", "root", "states"),
                        wire("s2", "state", "root", "states"),
                        wire("t1", "transition", "s1", "transitions"))));

        AssemblyResult r = AnimationControllerAssembler.assemble(lib);

        assertTrue(r.hasErrors());
        assertEquals(1, countCode(r, AssemblySupport.DUPLICATE_STATE));
        // initial_state "ghost" + transition 目标 "nowhere"
        assertEquals(2, countCode(r, AssemblySupport.UNKNOWN_STATE));
        // 尽力而为：重名状态保留先者
        JsonObject states = schemaOf(r, "controller.animation.err").getAsJsonObject("states");
        assertEquals(1, states.entrySet().size());
        assertTrue(states.has("a"));
    }
}
