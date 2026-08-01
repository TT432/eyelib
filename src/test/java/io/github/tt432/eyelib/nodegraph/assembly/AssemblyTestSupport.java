package io.github.tt432.eyelib.nodegraph.assembly;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.GraphData;
import io.github.tt432.eyelib.nodegraph.GraphKind;
import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.PortRef;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 组装器单测共享构造辅助（与 GraphValidatorTest 同风格：图直接用 record 构造，不走 JSON）。 */
final class AssemblyTestSupport {
    private AssemblyTestSupport() {
    }

    static NodeInstance node(String uid, String type) {
        return NodeInstance.of(uid, type, 0, 0);
    }

    static NodeInstance node(String uid, String type, Map<String, JsonElement> options) {
        return new NodeInstance(uid, type, 0, 0, options, Map.of());
    }

    static NodeInstance node(String uid, String type, Map<String, JsonElement> options,
                             Map<String, JsonElement> constants) {
        return new NodeInstance(uid, type, 0, 0, options, constants);
    }

    static Map<String, JsonElement> opts(Object... kv) {
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

    static Wire wire(String fromNode, String fromPort, String toNode, String toPort) {
        return new Wire(new PortRef(fromNode, fromPort), new PortRef(toNode, toPort));
    }

    static GraphData graph(List<NodeInstance> nodes, List<Wire> wires) {
        return new GraphData(nodes, wires, List.of(), List.of(), List.of(), Optional.empty());
    }

    static GraphLibrary lib(GraphKind kind, GraphData main) {
        return new GraphLibrary(1, kind, "root", Map.of("root", main));
    }

    static boolean hasCode(AssemblyResult r, String code) {
        return r.diagnostics().stream().anyMatch(d -> d.code().equals(code));
    }

    static long countCode(AssemblyResult r, String code) {
        return r.diagnostics().stream().filter(d -> d.code().equals(code)).count();
    }
}
