package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.github.tt432.eyelib.nodegraph.Diagnostic;
import io.github.tt432.eyelib.nodegraph.NodeInstance;
import io.github.tt432.eyelib.nodegraph.Wire;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 反编译单测共享辅助（与 AssemblyTestSupport 同风格：直接构造/断言 record，不走 MC）。 */
final class DecompileTestSupport {
    private DecompileTestSupport() {
    }

    private static final Pattern NUMBER_TOKEN = Pattern.compile("\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?");

    /**
     * molang 规范化比较：去全部空白/括号/分号（codegen 全括号化、语句序列仅以分号连接，
     * 均不承载语义）、数字格式归一（1.0 → 1）。
     */
    static String norm(String molang) {
        String stripped = molang.replaceAll("\\s+", "").replace("(", "").replace(")", "")
                .replace(";", "");
        Matcher m = NUMBER_TOKEN.matcher(stripped);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            double v = Double.parseDouble(m.group());
            m.appendReplacement(sb, Matcher.quoteReplacement(
                    v == Math.rint(v) && Math.abs(v) < 9.0e15
                            ? Long.toString((long) v)
                            : Double.toString(v)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static JsonObject parse(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
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

    static NodeInstance firstByType(List<NodeInstance> nodes, String type) {
        return nodes.stream().filter(n -> n.type().equals(type)).findFirst()
                .orElseThrow(() -> new AssertionError("no node of type " + type));
    }

    static List<NodeInstance> allByType(List<NodeInstance> nodes, String type) {
        return nodes.stream().filter(n -> n.type().equals(type)).toList();
    }

    /** 指向某输入端口的线（存在多条时取 from 字典序最小，与 codegen 同规则）。 */
    static Optional<Wire> wireInto(List<Wire> wires, String nodeUid, String portId) {
        return wires.stream()
                .filter(w -> w.to().node().equals(nodeUid) && w.to().port().equals(portId))
                .findFirst();
    }

    static String wireSource(List<Wire> wires, String nodeUid, String portId) {
        return wireInto(wires, nodeUid, portId)
                .map(w -> w.from().node())
                .orElseThrow(() -> new AssertionError("no wire into " + nodeUid + "." + portId));
    }

    /** 从某输出端口出发的线（v9 左读右写：set_var.target / ref write: 等写入通道输出）。 */
    static Optional<Wire> wireFrom(List<Wire> wires, String nodeUid, String portId) {
        return wires.stream()
                .filter(w -> w.from().node().equals(nodeUid) && w.from().port().equals(portId))
                .findFirst();
    }

    static String wireTarget(List<Wire> wires, String nodeUid, String portId) {
        return wireFrom(wires, nodeUid, portId)
                .map(w -> w.to().node())
                .orElseThrow(() -> new AssertionError("no wire from " + nodeUid + "." + portId));
    }

    static boolean hasCode(List<Diagnostic> diagnostics, String code) {
        return diagnostics.stream().anyMatch(d -> d.code().equals(code));
    }

    static long countCode(List<Diagnostic> diagnostics, String code) {
        return diagnostics.stream().filter(d -> d.code().equals(code)).count();
    }
}
