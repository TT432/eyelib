package io.github.tt432.eyelib.nodegraph.decompile;

import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.allByType;
import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.firstByType;
import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.hasCode;
import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.norm;
import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.opts;
import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.parse;
import static io.github.tt432.eyelib.nodegraph.decompile.DecompileTestSupport.wireSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.ShortNameOps;
import io.github.tt432.eyelib.nodegraph.Wire;
import io.github.tt432.eyelib.nodegraph.assembly.AnimationControllerAssembler;
import io.github.tt432.eyelib.nodegraph.assembly.AssemblyResult;
import io.github.tt432.eyelib.nodegraph.assembly.ClientEntityAssembler;
import io.github.tt432.eyelib.nodegraph.assembly.RenderControllerAssembler;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * {@link JsonGraphImporters} 单测：三种资产的 导入→组装 往返语义（molang 规范化比较）、
 * import(build(graph)) 结构断言、多语句 initialize 往返、不支持结构/未知字段/缺失目标诊断。
 */
class JsonGraphImportersTest {

    // ---------- ClientEntity 新增形态覆盖 ----------

    @Test
    void animateArrayWeightValuesImport() {
        // allay 实测形态：weight 为字符串数组（ExprSet），单元素等价字符串直译
        JsonObject json = parse("""
                {
                  "minecraft:client_entity": {
                    "description": {
                      "identifier": "test:arrayweight",
                      "animations": {"walk": "animation.test.walk"},
                      "scripts": {"animate": [{"walk": ["q.is_alive"]}]}
                    }
                  }
                }
                """);
        ImportResult imported = JsonGraphImporters.importClientEntity(json);
        assertFalse(hasCode(imported.diagnostics(), DecompileDiagnostics.INVALID_FIELD));
        // weight 经反编译连线（query 别名归一）
        NodeInstance entry = firstByType(imported.library().mainGraph().nodes(), "animate.entry");
        NodeInstance queryCall = firstByType(imported.library().mainGraph().nodes(), "query.call");
        assertEquals(queryCall.uid(), wireSource(imported.library().mainGraph().wires(), entry.uid(), "weight"));
        // 往返：组装回出 weight 表达式（单字符串，语义等价）
        AssemblyResult assembled = ClientEntityAssembler.assemble(imported.library());
        JsonArray animate = assembled.json().getAsJsonObject("minecraft:client_entity")
                .getAsJsonObject("description").getAsJsonObject("scripts").getAsJsonArray("animate");
        assertEquals("query.is_alive",
                norm(animate.get(0).getAsJsonObject().get("walk").getAsString()));
    }

    @Test
    void renderControllerConditionsMapImport() {
        // map 形态条件表 → rc.condition_entry（与内联同构，往返以数组内联回出，语义等价）
        JsonObject json = parse("""
                {
                  "minecraft:client_entity": {
                    "description": {
                      "identifier": "test:rcc",
                      "render_controller_conditions": {"controller.render.test.a": "query.is_baby"}
                    }
                  }
                }
                """);
        ImportResult imported = JsonGraphImporters.importClientEntity(json);
        assertFalse(hasCode(imported.diagnostics(), DecompileDiagnostics.UNKNOWN_FIELD));
        NodeInstance entry = firstByType(imported.library().mainGraph().nodes(), "rc.condition_entry");
        NodeInstance queryCall = firstByType(imported.library().mainGraph().nodes(), "query.call");
        assertEquals(queryCall.uid(), wireSource(imported.library().mainGraph().wires(), entry.uid(), "condition"));
        AssemblyResult assembled = ClientEntityAssembler.assemble(imported.library());
        JsonArray rcs = assembled.json().getAsJsonObject("minecraft:client_entity")
                .getAsJsonObject("description").getAsJsonArray("render_controllers");
        assertEquals("query.is_baby",
                norm(rcs.get(0).getAsJsonObject().get("controller.render.test.a").getAsString()));
    }

    @Test
    void renderControllerArrayAndConditionsMapMergeById() {
        // 数组与 map 含同一 id：导入必须合并为单个 rc.condition_entry（否则组装回出重复条目、
        // 运行时同 RC 渲染两遍——悦灵实机暴露的保真缺陷）
        JsonObject json = parse("""
                {
                  "minecraft:client_entity": {
                    "description": {
                      "identifier": "test:rcmerge",
                      "render_controllers": ["controller.render.test.a", "controller.render.test.b"],
                      "render_controller_conditions": {"controller.render.test.b": "query.is_baby"}
                    }
                  }
                }
                """);
        ImportResult imported = JsonGraphImporters.importClientEntity(json);
        assertFalse(imported.hasErrors(), () -> imported.diagnostics().toString());
        // 恰 2 个条目；b 的条件来自 map
        assertEquals(2, allByType(imported.library().mainGraph().nodes(), "rc.condition_entry").size());
        assertEquals(2, allByType(imported.library().mainGraph().nodes(), "ref.rc").size());

        AssemblyResult assembled = ClientEntityAssembler.assemble(imported.library());
        JsonArray rcs = assembled.json().getAsJsonObject("minecraft:client_entity")
                .getAsJsonObject("description").getAsJsonArray("render_controllers");
        assertEquals(2, rcs.size());
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (JsonElement el : rcs) {
            if (el.isJsonPrimitive()) {
                seen.add(el.getAsString());
            } else {
                var obj = el.getAsJsonObject();
                String id = obj.keySet().iterator().next();
                seen.add(id);
                assertEquals("query.is_baby", norm(obj.get(id).getAsString()));
            }
        }
        assertEquals(java.util.Set.of("controller.render.test.a", "controller.render.test.b"), seen);
    }

    @Test
    void conditionalAssignmentInInitialize() {
        // 条件赋值脱糖：v.a?{v.x=1;} → initialize 往返语义 = variable.x = variable.a ? 1 : variable.x
        JsonObject json = parse("""
                {
                  "minecraft:client_entity": {
                    "description": {
                      "identifier": "test:condassign",
                      "scripts": {"initialize": "v.a?{v.x=1;}"}
                    }
                  }
                }
                """);
        ImportResult imported = JsonGraphImporters.importClientEntity(json);
        assertFalse(hasCode(imported.diagnostics(), DecompileDiagnostics.UNSUPPORTED_IMPORT));
        AssemblyResult assembled = ClientEntityAssembler.assemble(imported.library());
        String initialize = assembled.json().getAsJsonObject("minecraft:client_entity")
                .getAsJsonObject("description").getAsJsonObject("scripts").get("initialize").getAsString();
        assertEquals(norm("variable.x = variable.a ? 1 : variable.x"), norm(initialize));
    }

    // ---------- ClientEntity 往返 ----------

    private static final String ENTITY_JSON = """
            {
              "format_version": "1.10.0",
              "minecraft:client_entity": {
                "description": {
                  "identifier": "test:roundtrip",
                  "geometry": {"default": "geometry.test.model"},
                  "textures": {"default": "textures/entity/test", "angry": "textures/entity/test_angry"},
                  "materials": {"default": "entity_alphatest"},
                  "animations": {"walk": "animation.test.walk", "idle": "animation.test.idle"},
                  "animation_controllers": [{"main": "controller.animation.test.main"}],
                  "scripts": {
                    "initialize": "variable.base = 2; variable.speed = variable.base * 3;",
                    "pre_animation": ["temp.tick = query.anim_time;", "temp.done = 1;"],
                    "animate": ["walk", {"idle": "query.modified_move_speed"}, {"main": "1"}],
                    "scale": "1.5",
                    "scaleX": "variable.base"
                  },
                  "render_controllers": [
                    "controller.render.test.a",
                    {"controller.render.test.b": "query.is_baby"}
                  ]
                }
              }
            }
            """;

    @Test
    void clientEntityRoundTrip() {
        JsonObject original = parse(ENTITY_JSON);
        ImportResult imported = JsonGraphImporters.importClientEntity(original);
        assertFalse(imported.hasErrors(), () -> imported.diagnostics().toString());

        AssemblyResult assembled = ClientEntityAssembler.assemble(imported.library());
        assertFalse(assembled.hasErrors(), () -> assembled.diagnostics().toString());

        JsonObject originalDesc = original.getAsJsonObject("minecraft:client_entity")
                .getAsJsonObject("description");
        JsonObject desc = assembled.json().getAsJsonObject("minecraft:client_entity")
                .getAsJsonObject("description");

        assertEquals(originalDesc.get("identifier").getAsString(), desc.get("identifier").getAsString());
        // 声明表逐值相等
        assertEquals(stringMap(originalDesc.getAsJsonObject("geometry")), stringMap(desc.getAsJsonObject("geometry")));
        assertEquals(stringMap(originalDesc.getAsJsonObject("textures")), stringMap(desc.getAsJsonObject("textures")));
        assertEquals(stringMap(originalDesc.getAsJsonObject("materials")), stringMap(desc.getAsJsonObject("materials")));
        // D6：animations 与 animation_controllers 是同一命名空间（Bedrock animate 解析两表合并）；
        // 重组装统一发进 animations 表，不再发 animation_controllers
        Map<String, String> originalAnimations = new java.util.LinkedHashMap<>(
                stringMap(originalDesc.getAsJsonObject("animations")));
        originalAnimations.putAll(singleKeyArrayToMap(originalDesc.getAsJsonArray("animation_controllers")));
        assertEquals(originalAnimations, stringMap(desc.getAsJsonObject("animations")));
        assertFalse(desc.has("animation_controllers"));

        JsonObject originalScripts = originalDesc.getAsJsonObject("scripts");
        JsonObject scripts = desc.getAsJsonObject("scripts");
        assertEquals(norm(originalScripts.get("initialize").getAsString()),
                norm(scripts.get("initialize").getAsString()));
        // pre_animation：string[] 顺序拼接
        assertEquals(norm("temp.tick = query.anim_time; temp.done = 1;"),
                norm(scripts.get("pre_animation").getAsString()));
        assertEquals(norm(originalScripts.get("scale").getAsString()), norm(scripts.get("scale").getAsString()));
        assertEquals(norm(originalScripts.get("scaleX").getAsString()), norm(scripts.get("scaleX").getAsString()));
        assertEquals(animateToMap(originalScripts.get("animate")), animateToMap(scripts.get("animate")));
        assertEquals(rcToMap(originalDesc.get("render_controllers")), rcToMap(desc.get("render_controllers")));
    }

    /** (e) initialize 多语句（含 loop）exec 链往返。 */
    @Test
    void multiStatementInitializeRoundTrip() {
        JsonObject json = parse("""
                {
                  "minecraft:client_entity": {
                    "description": {
                      "identifier": "test:exec",
                      "scripts": {
                        "initialize": "variable.a = 1; variable.b = variable.a + 2; loop(2, { temp.c = variable.b * 3; }); return variable.b;"
                      }
                    }
                  }
                }
                """);
        ImportResult imported = JsonGraphImporters.importClientEntity(json);
        assertFalse(imported.hasErrors(), () -> imported.diagnostics().toString());

        // exec 链：set_var → set_var → loop → return，链尾接 root.initialize
        GraphData graph = imported.library().mainGraph();
        assertEquals("exec.return", wireSourceNode(graph, "root", "initialize").type());

        AssemblyResult assembled = ClientEntityAssembler.assemble(imported.library());
        assertFalse(assembled.hasErrors(), () -> assembled.diagnostics().toString());
        String initialize = assembled.json().getAsJsonObject("minecraft:client_entity")
                .getAsJsonObject("description").getAsJsonObject("scripts")
                .get("initialize").getAsString();
        assertEquals(norm("variable.a = 1; variable.b = variable.a + 2; loop(2, { temp.c = variable.b * 3; }); return variable.b;"),
                norm(initialize));
    }

    /** (c) import(build(graph)) 结构断言 + 再 build JSON 相等。 */
    @Test
    void buildThenImportStructure() {
        GraphLibrary lib = new GraphLibrary(1, GraphKind.CLIENT_ENTITY, "root", Map.of("root",
                new GraphData(
                        List.of(
                                node("root", "entity.root", opts("identifier", "test:cycle"),
                                        opts("scale_x", 2)),
                                node("s1", "exec.set_var", Map.of(),
                                        opts("value", 1)),
                                node("s1t", "variable", opts("name", "foo")),
                                node("cscale", "const.number", opts("value", 1.5)),
                                node("ae1", "animate.entry"),
                                node("w1", "const.number", opts("value", 0.5)),
                                node("ra1", "ref.animation",
                                        opts("short_name", "walk", "identifier", "animation.test.walk")),
                                node("rce1", "rc.condition_entry"),
                                node("rrc1", "ref.rc", opts("identifier", "controller.render.test.a")),
                                node("rce2", "rc.condition_entry"),
                                node("rrc2", "ref.rc", opts("identifier", "controller.render.test.b")),
                                node("q1", "query.call", opts("function", "query.is_baby", "arg_count", 0)),
                                node("rg1", "ref.geometry",
                                        opts("short_name", "default", "identifier", "geometry.test.model"))),
                        List.of(
                                wire("s1", "exec_out", "root", "initialize"),
                                wire("s1t", "out", "s1", "target"),
                                wire("cscale", "out", "root", "scale"),
                                wire("ra1", "ref", "ae1", "ref"),
                                wire("w1", "out", "ae1", "weight"),
                                wire("ae1", "entry", "root", "animate"),
                                wire("rrc1", "ref", "rce1", "rc"),
                                wire("rce1", "entry", "root", "render_controllers"),
                                wire("rrc2", "ref", "rce2", "rc"),
                                wire("q1", "out", "rce2", "condition"),
                                wire("rce2", "entry", "root", "render_controllers")),
                        List.of(), List.of(), List.of(), Optional.empty())));

        AssemblyResult built = ClientEntityAssembler.assemble(lib);
        assertFalse(built.hasErrors(), () -> built.diagnostics().toString());

        ImportResult imported = JsonGraphImporters.importClientEntity(built.json());
        assertFalse(imported.hasErrors(), () -> imported.diagnostics().toString());
        GraphData graph = imported.library().mainGraph();

        // 结构断言：root + 选项、ref.geometry、exec 链、animate/rc 条目
        NodeInstance root = graph.findNode("root").orElseThrow();
        assertEquals("entity.root", root.type());
        assertEquals("test:cycle", root.options().get("identifier").getAsString());
        NodeInstance geo = firstByType(graph.nodes(), "ref.geometry");
        assertEquals("default", geo.options().get("short_name").getAsString());
        assertEquals("geometry.test.model", geo.options().get("identifier").getAsString());
        NodeInstance setVar = firstByType(graph.nodes(), "exec.set_var");
        NodeInstance setTarget = graph.findNode(wireSource(graph.wires(), setVar.uid(), "target")).orElseThrow();
        assertEquals("variable", setTarget.type());
        assertEquals("foo", setTarget.options().get("name").getAsString());
        assertEquals(setVar.uid(), wireSource(graph.wires(), "root", "initialize"));
        assertEquals(1, allByType(graph.nodes(), "animate.entry").size());
        assertEquals(2, allByType(graph.nodes(), "rc.condition_entry").size());
        assertEquals(1, allByType(graph.nodes(), "query.call").size());

        // 再 build：JSON 与首次 build 相等
        AssemblyResult rebuilt = ClientEntityAssembler.assemble(imported.library());
        assertFalse(rebuilt.hasErrors(), () -> rebuilt.diagnostics().toString());
        assertEquals(built.json(), rebuilt.json());
    }

    /** (d) 不支持结构：诊断 + 便签 + const 0 占位，其余字段正常导入。 */
    @Test
    void unsupportedStructureKeepsEverythingElse() {
        JsonObject json = parse("""
                {
                  "minecraft:client_entity": {
                    "description": {
                      "identifier": "test:unsupported",
                      "scripts": {
                        "initialize": "variable.a = 1;",
                        "scale": "variable.a->query.b + 1"
                      }
                    }
                  }
                }
                """);
        ImportResult imported = JsonGraphImporters.importClientEntity(json);
        assertTrue(hasCode(imported.diagnostics(), DecompileDiagnostics.UNSUPPORTED_IMPORT));
        GraphData graph = imported.library().mainGraph();
        assertTrue(graph.stickyNotes().stream().anyMatch(s -> s.text().contains("variable.a->query.b")));

        // scale：op.binary 的 a 侧为 const 0 占位，b 侧 const.int 1 正常
        NodeInstance op = firstByType(graph.nodes(), "op.binary");
        assertEquals(op.uid(), wireSource(graph.wires(), "root", "scale"));
        NodeInstance aSource = graph.findNode(wireSource(graph.wires(), op.uid(), "a")).orElseThrow();
        assertEquals("const.number", aSource.type());
        assertEquals(0.0, aSource.options().get("value").getAsDouble());
        NodeInstance bSource = graph.findNode(wireSource(graph.wires(), op.uid(), "b")).orElseThrow();
        assertEquals("const.int", bSource.type());
        assertEquals(1, bSource.options().get("value").getAsInt());

        // initialize 照常导入
        NodeInstance setVar = firstByType(graph.nodes(), "exec.set_var");
        assertEquals(setVar.uid(), wireSource(graph.wires(), "root", "initialize"));
    }

    @Test
    void unknownFieldsBecomeStickyNotes() {
        JsonObject json = parse("""
                {
                  "minecraft:client_entity": {
                    "description": {
                      "identifier": "test:unknown",
                      "spawn_egg": {"base_color": "#ffffff"}
                    }
                  }
                }
                """);
        ImportResult imported = JsonGraphImporters.importClientEntity(json);
        assertTrue(hasCode(imported.diagnostics(), DecompileDiagnostics.UNKNOWN_FIELD));
        assertTrue(imported.library().mainGraph().stickyNotes().stream()
                .anyMatch(s -> s.text().contains("spawn_egg") && s.text().contains("#ffffff")));
    }

    // ---------- RenderController 往返 ----------

    private static final String RC_JSON = """
            {
              "format_version": "1.8.0",
              "render_controllers": {
                "controller.render.test": {
                  "geometry": "geometry.default",
                  "textures": ["texture.default", "texture.variant"],
                  "materials": [{"*": "material.default"}, {"head*": "material.special"}],
                  "part_visibility": [{"leg*": "query.is_baby"}, {"arm*": "1"}],
                  "arrays": {"bones": {"head": ["a", "b"]}},
                  "ignore_lighting": true,
                  "color": {"r": 1.0, "g": "query.is_baby", "b": 0, "a": 1},
                  "overlay_color": {"r": 0.5, "g": 0.5, "b": 0.5, "a": 0.5}
                }
              }
            }
            """;

    @Test
    void renderControllerRoundTrip() {
        JsonObject original = parse(RC_JSON);
        ImportResult imported = JsonGraphImporters.importRenderController(original, "controller.render.test");
        assertFalse(imported.hasErrors(), () -> imported.diagnostics().toString());

        AssemblyResult assembled = RenderControllerAssembler.assemble(imported.library());
        assertFalse(assembled.hasErrors(), () -> assembled.diagnostics().toString());

        JsonObject originalEntry = original.getAsJsonObject("render_controllers")
                .getAsJsonObject("controller.render.test");
        JsonObject entry = assembled.json().getAsJsonObject("render_controllers")
                .getAsJsonObject("controller.render.test");

        assertEquals(norm(originalEntry.get("geometry").getAsString()), norm(entry.get("geometry").getAsString()));
        assertEquals(2, entry.getAsJsonArray("textures").size());
        for (int i = 0; i < 2; i++) {
            assertEquals(norm(originalEntry.getAsJsonArray("textures").get(i).getAsString()),
                    norm(entry.getAsJsonArray("textures").get(i).getAsString()));
        }
        assertEquals(singleKeyArrayToNormMap(originalEntry.get("materials")),
                singleKeyArrayToNormMap(entry.get("materials")));
        assertEquals(singleKeyArrayToNormMap(originalEntry.get("part_visibility")),
                singleKeyArrayToNormMap(entry.get("part_visibility")));
        assertEquals(originalEntry.getAsJsonObject("arrays"), entry.getAsJsonObject("arrays"));
        assertTrue(entry.get("ignore_lighting").getAsBoolean());
        assertColorEquals(originalEntry.getAsJsonObject("color"), entry.getAsJsonObject("color"));
        assertColorEquals(originalEntry.getAsJsonObject("overlay_color"), entry.getAsJsonObject("overlay_color"));
    }

    /** D4：RC 导入携已知短名表 → 裸短名 ref 回填标识符（显式短名保留）。 */
    @Test
    void rcImportBackfillsIdentifiersFromKnownTables() {
        JsonObject original = parse(RC_JSON);
        ShortNameOps.KnownTables known = new ShortNameOps.KnownTables.Builder()
                .put("ref.geometry", "default", "geometry.test.model")
                .put("ref.texture", "default", "textures/entity/test")
                .put("ref.material", "default", "entity_alphatest")
                .build();

        ImportResult imported = JsonGraphImporters.importRenderController(
                original, "controller.render.test", known);
        assertFalse(imported.hasErrors(), () -> imported.diagnostics().toString());
        GraphData graph = imported.library().mainGraph();

        NodeInstance geo = firstByType(graph.nodes(), "ref.geometry");
        assertEquals("geometry.test.model", geo.options().get("identifier").getAsString());
        assertEquals("default", geo.options().get("short_name").getAsString());

        List<NodeInstance> texRefs = allByType(graph.nodes(), "ref.texture");
        assertEquals(2, texRefs.size());
        NodeInstance texDefault = texRefs.stream()
                .filter(n -> n.options().get("short_name").getAsString().equals("default"))
                .findFirst().orElseThrow();
        assertEquals("textures/entity/test", texDefault.options().get("path").getAsString());
        NodeInstance texVariant = texRefs.stream()
                .filter(n -> n.options().get("short_name").getAsString().equals("variant"))
                .findFirst().orElseThrow();
        assertFalse(texVariant.options().containsKey("path"));

        // 重组装仍按显式短名回出（保留策略）
        AssemblyResult assembled = RenderControllerAssembler.assemble(imported.library());
        assertFalse(assembled.hasErrors(), () -> assembled.diagnostics().toString());
        JsonObject entry = assembled.json().getAsJsonObject("render_controllers")
                .getAsJsonObject("controller.render.test");
        assertEquals("geometry.default", entry.get("geometry").getAsString());
    }

    /** (c) RC：import(build(graph)) 结构断言 + 再 build JSON 相等。 */
    @Test
    void rcBuildThenImportStructure() {
        GraphLibrary lib = new GraphLibrary(1, GraphKind.RENDER_CONTROLLER, "root", Map.of("root",
                new GraphData(
                        List.of(
                                node("root", "rc.root", opts("identifier", "controller.render.cycle",
                                        "ignore_lighting", true)),
                                node("g1", "const.string", opts("value", "geometry.custom")),
                                node("t1", "list.entry"),
                                node("tc1", "const.string", opts("value", "texture.a")),
                                node("m1", "material.entry", opts("pattern", "*")),
                                node("rm1", "ref.material", opts("short_name", "default")),
                                node("pv1", "part_visibility.entry", opts("bone_pattern", "head*")),
                                node("pb1", "const.bool", opts("value", true))),
                        List.of(
                                wire("g1", "out", "root", "geometry"),
                                wire("tc1", "out", "t1", "value"),
                                wire("t1", "entry", "root", "textures"),
                                wire("rm1", "ref", "m1", "value"),
                                wire("m1", "entry", "root", "materials"),
                                wire("pb1", "out", "pv1", "condition"),
                                wire("pv1", "entry", "root", "part_visibility")),
                        List.of(), List.of(), List.of(), Optional.empty())));

        AssemblyResult built = RenderControllerAssembler.assemble(lib);
        assertFalse(built.hasErrors(), () -> built.diagnostics().toString());

        ImportResult imported = JsonGraphImporters.importRenderController(built.json(), "controller.render.cycle");
        assertFalse(imported.hasErrors(), () -> imported.diagnostics().toString());
        GraphData graph = imported.library().mainGraph();

        NodeInstance root = graph.findNode("root").orElseThrow();
        assertEquals("rc.root", root.type());
        assertEquals("controller.render.cycle", root.options().get("identifier").getAsString());
        assertTrue(root.options().get("ignore_lighting").getAsBoolean());
        assertEquals(1, allByType(graph.nodes(), "list.entry").size());
        assertEquals(1, allByType(graph.nodes(), "material.entry").size());
        assertEquals(1, allByType(graph.nodes(), "part_visibility.entry").size());
        assertEquals(1, allByType(graph.nodes(), "ref.material").size());

        AssemblyResult rebuilt = RenderControllerAssembler.assemble(imported.library());
        assertFalse(rebuilt.hasErrors(), () -> rebuilt.diagnostics().toString());
        assertEquals(built.json(), rebuilt.json());
    }

    @Test
    void missingRenderController() {
        ImportResult imported = JsonGraphImporters.importRenderController(parse(RC_JSON), "controller.render.absent");
        assertTrue(imported.hasErrors());
        assertTrue(hasCode(imported.diagnostics(), DecompileDiagnostics.MISSING_ENTRY));
    }

    // ---------- AnimationController 往返 ----------

    private static final String AC_JSON = """
            {
              "format_version": "1.10.0",
              "animation_controllers": {
                "controller.animation.test": {
                  "initial_state": "idle",
                  "states": {
                    "idle": {
                      "animations": {"idle_anim": "1"},
                      "transitions": [{"walk": "query.modified_move_speed > 0.1"}],
                      "on_entry": ["variable.entered = 1;"],
                      "blend_transition": 0.3,
                      "blend_via_shortest_path": true
                    },
                    "walk": {
                      "animations": ["walk_anim"],
                      "transitions": [{"idle": "!query.modified_move_speed"}]
                    }
                  }
                }
              }
            }
            """;

    @Test
    void animationControllerRoundTrip() {
        JsonObject original = parse(AC_JSON);
        ImportResult imported = JsonGraphImporters.importAnimationControllers(original, "controller.animation.test");
        assertFalse(imported.hasErrors(), () -> imported.diagnostics().toString());

        AssemblyResult assembled = AnimationControllerAssembler.assemble(imported.library());
        assertFalse(assembled.hasErrors(), () -> assembled.diagnostics().toString());

        JsonObject originalSchema = original.getAsJsonObject("animation_controllers")
                .getAsJsonObject("controller.animation.test");
        JsonObject schema = assembled.json().getAsJsonObject("animation_controllers")
                .getAsJsonObject("controller.animation.test");

        assertEquals("idle", schema.get("initial_state").getAsString());
        JsonObject originalStates = originalSchema.getAsJsonObject("states");
        JsonObject states = schema.getAsJsonObject("states");
        assertEquals(originalStates.keySet(), states.keySet());

        JsonObject idle = states.getAsJsonObject("idle");
        assertEquals(norm("variable.entered = 1;"), norm(idle.get("on_entry").getAsString()));
        assertFalse(idle.has("on_exit"));
        assertEquals(animationsToMap(originalStates.getAsJsonObject("idle").get("animations")),
                animationsToMap(idle.get("animations")));
        assertEquals(singleKeyArrayToNormMap(originalStates.getAsJsonObject("idle").get("transitions")),
                transitionsToMap(idle.get("transitions")));
        assertEquals(0.3, idle.get("blend_transition").getAsDouble());
        assertTrue(idle.get("blend_via_shortest_path").getAsBoolean());

        JsonObject walk = states.getAsJsonObject("walk");
        assertEquals(animationsToMap(originalStates.getAsJsonObject("walk").get("animations")),
                animationsToMap(walk.get("animations")));
        assertEquals(singleKeyArrayToNormMap(originalStates.getAsJsonObject("walk").get("transitions")),
                transitionsToMap(walk.get("transitions")));
        // 缺省选项：assembler 恒输出（blend_transition 类型默认 0.2 / shortest_path 默认 false）
        assertEquals(0.2, walk.get("blend_transition").getAsDouble());
        assertFalse(walk.get("blend_via_shortest_path").getAsBoolean());
    }

    @Test
    void missingAnimationController() {
        ImportResult imported = JsonGraphImporters.importAnimationControllers(parse(AC_JSON), "controller.animation.absent");
        assertTrue(imported.hasErrors());
        assertTrue(hasCode(imported.diagnostics(), DecompileDiagnostics.MISSING_ENTRY));
    }

    // ---------- 构造/比较辅助 ----------

    private static NodeInstance node(String uid, String type) {
        return NodeInstance.of(uid, type, 0, 0);
    }

    private static NodeInstance node(String uid, String type, Map<String, JsonElement> options) {
        return new NodeInstance(uid, type, 0, 0, options, Map.of());
    }

    private static NodeInstance node(String uid, String type, Map<String, JsonElement> options,
                                     Map<String, JsonElement> constants) {
        return new NodeInstance(uid, type, 0, 0, options, constants);
    }

    private static Wire wire(String fromNode, String fromPort, String toNode, String toPort) {
        return new Wire(new PortRef(fromNode, fromPort), new PortRef(toNode, toPort));
    }

    private static NodeInstance wireSourceNode(GraphData graph, String nodeUid, String portId) {
        String sourceUid = wireSource(graph.wires(), nodeUid, portId);
        return graph.findNode(sourceUid).orElseThrow();
    }

    private static Map<String, String> stringMap(JsonObject obj) {
        Map<String, String> out = new LinkedHashMap<>();
        obj.entrySet().forEach(e -> out.put(e.getKey(), e.getValue().getAsString()));
        return out;
    }

    /** single-key object 数组 → map（值原文）。 */
    private static Map<String, String> singleKeyArrayToMap(JsonArray array) {
        Map<String, String> out = new LinkedHashMap<>();
        array.forEach(el -> el.getAsJsonObject().entrySet()
                .forEach(e -> out.put(e.getKey(), e.getValue().getAsString())));
        return out;
    }

    /** object 或 single-key object 数组 → map（值 molang 规范化）。 */
    private static Map<String, String> singleKeyArrayToNormMap(JsonElement value) {
        Map<String, String> out = new LinkedHashMap<>();
        if (value.isJsonObject()) {
            value.getAsJsonObject().entrySet()
                    .forEach(e -> out.put(e.getKey(), norm(e.getValue().getAsString())));
        } else {
            value.getAsJsonArray().forEach(el -> el.getAsJsonObject().entrySet()
                    .forEach(e -> out.put(e.getKey(), norm(e.getValue().getAsString()))));
        }
        return out;
    }

    private static Map<String, String> transitionsToMap(JsonElement value) {
        return singleKeyArrayToNormMap(value);
    }

    /** animate / animations：字符串（weight 缺省 1）或 {short: weight} 条目 → map（weight 规范化）。 */
    private static Map<String, String> animateToMap(JsonElement value) {
        Map<String, String> out = new LinkedHashMap<>();
        if (value.isJsonObject()) {
            value.getAsJsonObject().entrySet()
                    .forEach(e -> out.put(e.getKey(), norm(e.getValue().getAsString())));
            return out;
        }
        for (JsonElement el : value.getAsJsonArray()) {
            if (el.isJsonPrimitive()) {
                out.put(el.getAsString(), "1");
            } else {
                el.getAsJsonObject().entrySet()
                        .forEach(e -> out.put(e.getKey(), norm(e.getValue().getAsString())));
            }
        }
        return out;
    }

    private static Map<String, String> animationsToMap(JsonElement value) {
        return animateToMap(value);
    }

    /** render_controllers：字符串（condition 恒 1）或 {identifier: condition} → map（condition 规范化）。 */
    private static Map<String, String> rcToMap(JsonElement value) {
        return animateToMap(value);
    }

    private static void assertColorEquals(JsonObject expected, JsonObject actual) {
        for (String channel : List.of("r", "g", "b", "a")) {
            assertEquals(norm(channelText(expected.get(channel))), norm(channelText(actual.get(channel))),
                    "channel " + channel);
        }
    }

    /** JSON 原始值 → 文本：字符串取内容，其余取 toString（数字）。 */
    private static String channelText(JsonElement el) {
        return el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()
                ? el.getAsString()
                : el.toString();
    }
}
