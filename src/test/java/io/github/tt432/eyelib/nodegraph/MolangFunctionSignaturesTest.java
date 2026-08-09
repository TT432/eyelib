package io.github.tt432.eyelib.nodegraph;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link MolangFunctionSignatures} 与签名驱动端口（{@code NodeTypes#callArgPorts}）测试。
 */
class MolangFunctionSignaturesTest {

    private static NodeInstance call(String type, String function) {
        return new NodeInstance("u1", type, 0, 0,
                Map.of("function", new com.google.gson.JsonPrimitive(function)),
                Map.of());
    }

    private static List<PortDef> inputs(NodeType type, NodeInstance instance) {
        return type.inputsOf(instance, name -> Optional.empty());
    }

    @Test
    void 定长签名mathClamp() {
        MolangFunctionSignatures.Signature sig = MolangFunctionSignatures.find("math.clamp");
        assertNotNull(sig);
        assertNull(sig.varArg());
        assertEquals(3, sig.fixedArity());
        assertEquals(List.of("value", "min", "max"),
                sig.fixed().stream().map(MolangFunctionSignatures.Arg::name).toList());
        assertTrue(sig.fixed().stream()
                .allMatch(a -> a.kind() == MolangFunctionSignatures.ArgKind.NUMBER));
    }

    @Test
    void 变长签名isItemNameAny() {
        MolangFunctionSignatures.Signature sig = MolangFunctionSignatures.find("query.is_item_name_any");
        assertNotNull(sig);
        assertNotNull(sig.varArg());
        assertEquals(-1, sig.fixedArity());
        assertEquals(List.of("hand"), sig.fixed().stream().map(MolangFunctionSignatures.Arg::name).toList());
        assertEquals(MolangFunctionSignatures.ArgKind.STRING, sig.fixed().get(0).kind());
        assertEquals(MolangFunctionSignatures.ArgKind.STRING, sig.varArg().kind());
    }

    @Test
    void 未知函数无签名() {
        assertNull(MolangFunctionSignatures.find("query.not_a_function"));
        assertFalse(MolangFunctionSignatures.isFixedArity("query.not_a_function"));
        assertTrue(MolangFunctionSignatures.isFixedArity("math.clamp"));
        assertFalse(MolangFunctionSignatures.isFixedArity("query.is_name_any"));
    }

    @Test
    void 自定义函数注册覆盖与清理() {
        MolangFunctionSignatures.registerCustom("query.my_custom",
                new MolangFunctionSignatures.Signature(
                        List.of(new MolangFunctionSignatures.Arg("x", MolangFunctionSignatures.ArgKind.NUMBER)), null));
        assertTrue(MolangFunctionSignatures.isFixedArity("query.my_custom"));
        assertEquals(List.of("query.my_custom"), MolangFunctionSignatures.customNames("query"));
        assertEquals(List.of(), MolangFunctionSignatures.customNames("math"));
        MolangFunctionSignatures.clearCustoms();
        assertNull(MolangFunctionSignatures.find("query.my_custom"));
    }

    @Test
    void 定长函数端口按签名生成() {
        // v11：端口完全由签名决定（arg_count 选项已删除）
        List<PortDef> ports = inputs(NodeTypes.MATH_CALL, call("math.call", "math.clamp"));
        assertEquals(List.of("arg1", "arg2", "arg3"), ports.stream().map(PortDef::id).toList());
        assertEquals(List.of("value", "min", "max"),
                ports.stream().map(p -> p.label().orElseThrow()).toList());
        // 物理类型保持 ANY（行内字面值编辑器依赖）
        assertTrue(ports.stream().allMatch(p -> p.type() == PortType.ANY));
    }

    @Test
    void 变长函数只有固定前缀端口() {
        // v11：变长尾参不产生端口，走 args 列表选项；query.is_item_name_any 固定前缀 = [hand]
        List<PortDef> ports = inputs(NodeTypes.QUERY_CALL, call("query.call", "query.is_item_name_any"));
        assertEquals(List.of("arg1"), ports.stream().map(PortDef::id).toList());
        assertEquals("hand: str", ports.get(0).label().orElseThrow());
        // args 列表行标签：变长 → 名[...]（string 元素带 : str[]）
        assertEquals("items[...]: str[]",
                MolangFunctionSignatures.variadicListLabel("query.is_item_name_any"));
    }

    @Test
    void 未知函数无端口() {
        // v11：未知函数（含零参内建，签名表只收录带参函数）无任何 arg 端口，参数全走 args 列表
        List<PortDef> ports = inputs(NodeTypes.QUERY_CALL, call("query.call", "query.custom_thing"));
        assertTrue(ports.isEmpty());
        // 列表行标签：未知 → args[...]；定长 → null（编辑器隐藏列表行）
        assertEquals("args[...]", MolangFunctionSignatures.variadicListLabel("query.custom_thing"));
        assertNull(MolangFunctionSignatures.variadicListLabel("math.clamp"));
    }

    @Test
    void execCall同样签名驱动且保留exec端口() {
        List<PortDef> ports = inputs(NodeTypes.EXEC_CALL, call("exec.call", "math.clamp"));
        assertEquals("exec_in", ports.get(0).id());
        assertEquals(List.of("arg1", "arg2", "arg3"),
                ports.subList(1, 4).stream().map(PortDef::id).toList());
    }
}
