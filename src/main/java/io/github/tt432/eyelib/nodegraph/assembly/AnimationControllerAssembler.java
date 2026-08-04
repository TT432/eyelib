package io.github.tt432.eyelib.nodegraph.assembly;

import com.google.gson.JsonObject;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.NodeTypes;
import io.github.tt432.eyelib.nodegraph.ShortNames;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * AnimationController 文档组装器（规格 §2.6/T6）：
 * kind=animation_controller 的图库 → Bedrock animation_controller 文档 JSON。
 *
 * <p>产出 {@code {"format_version":"1.10.0","animation_controllers":{"<identifier>":
 * {"initial_state":...,"states":{...}}}}}。states：ac.root 的 states 槽连的 ac.state
 * （uid 字典序，键=name 选项，重名 → DUPLICATE_STATE）；每状态 on_entry/on_exit
 * （EXEC 槽未连线跳过）、animations（animate.entry → {short_name: weight 表达式}）、
 * transitions（ac.transition → {target: condition 表达式}）、blend_transition/
 * blend_via_shortest_path（数值/布尔选项恒输出，写 JSON 原始类型）。
 * 引用完整性：initial_state 与各 transition.target 必须存在于 states 键集，否则 UNKNOWN_STATE。
 */
public final class AnimationControllerAssembler {
    private AnimationControllerAssembler() {
    }

    public static AssemblyResult assemble(GraphLibrary library) {
        JsonObject schema = new JsonObject();
        String identifier = "";
        List<Diagnostic> diagnostics = new ArrayList<>();
        GraphData main = library.graphs().get(library.main());
        if (main == null) {
            diagnostics.add(Diagnostic.error(AssemblySupport.MISSING_ROOT, "主图 '" + library.main() + "' 不存在"));
        } else {
            AssemblySupport.Ctx ctx = new AssemblySupport.Ctx(library, main);
            Optional<NodeInstance> root = AssemblySupport.findRoot(ctx, "ac.root");
            if (root.isPresent()) {
                NodeInstance rootNode = root.get();
                identifier = AssemblySupport.optionString(rootNode, NodeTypes.AC_ROOT, "identifier");
                assembleSchema(ctx, rootNode, schema);
            }
            diagnostics.addAll(ctx.diagnostics);
        }
        JsonObject controllers = new JsonObject();
        controllers.add(identifier, schema);
        JsonObject doc = new JsonObject();
        doc.addProperty("format_version", "1.10.0");
        doc.add("animation_controllers", controllers);
        return new AssemblyResult(doc, diagnostics);
    }

    private static void assembleSchema(AssemblySupport.Ctx ctx, NodeInstance root, JsonObject schema) {
        String initialState = AssemblySupport.optionString(root, NodeTypes.AC_ROOT, "initial_state");
        schema.addProperty("initial_state", initialState);
        JsonObject states = new JsonObject();
        Set<String> stateNames = new LinkedHashSet<>();
        List<NodeInstance> transitions = new ArrayList<>();
        for (NodeInstance state : AssemblySupport.wiredSources(ctx.main, root.uid(), "states")) {
            String name = AssemblySupport.optionString(state, NodeTypes.AC_STATE, "name");
            if (!stateNames.add(name)) {
                ctx.error(AssemblySupport.DUPLICATE_STATE, "状态重名：'" + name + "'（保留先者）", state.uid());
                continue;
            }
            states.add(name, assembleState(ctx, state, transitions));
        }
        schema.add("states", states);
        if (!stateNames.contains(initialState)) {
            ctx.error(AssemblySupport.UNKNOWN_STATE,
                    "initial_state '" + initialState + "' 不在 states 键集中", root.uid());
        }
        for (NodeInstance transition : transitions) {
            String target = AssemblySupport.optionString(transition, NodeTypes.AC_TRANSITION, "target");
            if (!stateNames.contains(target)) {
                ctx.error(AssemblySupport.UNKNOWN_STATE,
                        "transition 目标 '" + target + "' 不在 states 键集中", transition.uid());
            }
        }
    }

    private static JsonObject assembleState(AssemblySupport.Ctx ctx, NodeInstance state,
                                            List<NodeInstance> transitionsOut) {
        JsonObject obj = new JsonObject();
        if (AssemblySupport.hasWire(ctx.main, state.uid(), "on_entry")) {
            obj.addProperty("on_entry", ctx.emitStatements(state.uid(), "on_entry"));
        }
        if (AssemblySupport.hasWire(ctx.main, state.uid(), "on_exit")) {
            obj.addProperty("on_exit", ctx.emitStatements(state.uid(), "on_exit"));
        }

        // 同 short_name 后来者覆盖；先归并再发射
        Map<String, NodeInstance> animations = new LinkedHashMap<>();
        for (NodeInstance entry : AssemblySupport.wiredSources(ctx.main, state.uid(), "animations")) {
            NodeInstance refNode = AssemblySupport.resolveEntryRef(ctx, entry, "ref");
            if (refNode == null) {
                continue;
            }
            String shortName = switch (refNode.type()) {
                case "ref.animation" -> ShortNames.effective(refNode, NodeTypes.REF_ANIMATION);
                case "ref.ac" -> ShortNames.effective(refNode, NodeTypes.REF_AC);
                default -> null;
            };
            if (shortName == null) {
                ctx.error(AssemblySupport.INVALID_ENTRY_REF,
                        "ac.state animations 条目的 ref 槽只能连接 ref.animation / ref.ac，实际 "
                                + refNode.type(), entry.uid());
                continue;
            }
            animations.put(shortName, entry);
        }
        if (!animations.isEmpty()) {
            JsonObject map = new JsonObject();
            animations.forEach((shortName, entry) ->
                    map.addProperty(shortName, ctx.emitExpression(entry.uid(), "weight")));
            obj.add("animations", map);
        }

        Map<String, NodeInstance> transitions = new LinkedHashMap<>();
        for (NodeInstance t : AssemblySupport.wiredSources(ctx.main, state.uid(), "transitions")) {
            transitionsOut.add(t);
            transitions.put(AssemblySupport.optionString(t, NodeTypes.AC_TRANSITION, "target"), t);
        }
        if (!transitions.isEmpty()) {
            JsonObject map = new JsonObject();
            transitions.forEach((target, t) ->
                    map.addProperty(target, ctx.emitExpression(t.uid(), "condition")));
            obj.add("transitions", map);
        }

        obj.add("blend_transition", AssemblySupport.optionValue(state, NodeTypes.AC_STATE, "blend_transition"));
        obj.add("blend_via_shortest_path",
                AssemblySupport.optionValue(state, NodeTypes.AC_STATE, "blend_via_shortest_path"));
        return obj;
    }
}
