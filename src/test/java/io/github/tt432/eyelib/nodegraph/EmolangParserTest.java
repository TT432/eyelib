package io.github.tt432.eyelib.nodegraph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link EmolangParser} 单测：函数头/类型标注/注释剥离/return 约束/语法校验。
 */
class EmolangParserTest {

    @Test
    void 基本解析() {
        var r = EmolangParser.parse("custom1.emolang",
                "function custom1(arg1: int, arg2: float) {\n"
                        + "    temp.doubled = arg1 * 2;\n"
                        + "    return temp.doubled + arg2;\n"
                        + "}\n");
        assertNull(r.error());
        EmolangFunction fn = r.function();
        assertNotNull(fn);
        assertEquals("custom1", fn.name());
        assertEquals(2, fn.params().size());
        assertEquals("arg1", fn.params().get(0).name());
        assertEquals(MolangFunctionSignatures.ArgKind.NUMBER, fn.params().get(0).kind());
        assertEquals("arg2", fn.params().get(1).name());
        assertTrue(fn.returnIndex() > 0);
    }

    @Test
    void 类型省略与string标注() {
        var r = EmolangParser.parse("f.emolang",
                "function f(a, b: string) { return a; }");
        assertNull(r.error());
        assertNotNull(r.function());
        assertEquals(MolangFunctionSignatures.ArgKind.NUMBER, r.function().params().get(0).kind());
        assertEquals(MolangFunctionSignatures.ArgKind.STRING, r.function().params().get(1).kind());
    }

    @Test
    void 注释剥离但字符串内双斜杠保留() {
        var r = EmolangParser.parse("f.emolang",
                "// 头部注释\nfunction f(a) { // 行尾注释\n return 'x//y'; }\n");
        assertNull(r.error(), () -> r.error());
        assertNotNull(r.function());
    }

    @Test
    void 缺return报错() {
        var r = EmolangParser.parse("f.emolang", "function f(a) { temp.x = a; }");
        assertNull(r.function());
        assertNotNull(r.error());
        assertTrue(r.error().contains("return"));
    }

    @Test
    void 多个顶层return报错() {
        var r = EmolangParser.parse("f.emolang", "function f(a) { return a; return a; }");
        assertNull(r.function());
        assertNotNull(r.error());
    }

    @Test
    void return不是最后语句报错() {
        var r = EmolangParser.parse("f.emolang", "function f(a) { return a; temp.x = 1; }");
        assertNull(r.function());
        assertNotNull(r.error());
    }

    @Test
    void 保留字参数名报错() {
        var r = EmolangParser.parse("f.emolang", "function f(temp) { return temp; }");
        assertNull(r.function());
        assertNotNull(r.error());
    }

    @Test
    void 未知类型报错() {
        var r = EmolangParser.parse("f.emolang", "function f(a: object) { return a; }");
        assertNull(r.function());
        assertNotNull(r.error());
    }

    @Test
    void 体语法错误报错() {
        var r = EmolangParser.parse("f.emolang", "function f(a) { return a +; }");
        assertNull(r.function());
        assertNotNull(r.error());
    }

    @Test
    void 体后多余内容报错() {
        var r = EmolangParser.parse("f.emolang", "function f(a) { return a; } 1");
        assertNull(r.function());
        assertNotNull(r.error());
    }

    @Test
    void 零参函数() {
        var r = EmolangParser.parse("f.emolang", "function answer() { return 42; }");
        assertNull(r.error(), () -> r.error());
        assertNotNull(r.function());
        assertEquals(0, r.function().params().size());
    }

    @Test
    void 体中花括号嵌套配平() {
        var r = EmolangParser.parse("f.emolang",
                "function f(a) { temp.c = 0; loop(3, { temp.c = temp.c + a; }); return temp.c; }");
        assertNull(r.error(), () -> r.error());
        assertNotNull(r.function());
    }
}
