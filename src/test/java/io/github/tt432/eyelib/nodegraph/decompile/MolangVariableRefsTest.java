package io.github.tt432.eyelib.nodegraph.decompile;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MolangVariableRefs} 单测：v./variable. 别名、temp 排除、残缺语法跳过、嵌套递归。
 */
class MolangVariableRefsTest {

    @Test
    void collectsVariableAndAliasNames() {
        var json = JsonParser.parseString(
                "{\"a\": \"variable.foo + v.bar\", \"b\": [\"q.x * variable.foo\"]}");
        assertEquals(Set.of("foo", "bar"), MolangVariableRefs.collect(json));
    }

    @Test
    void tempAndQueryAreNotEntityVariables() {
        var json = JsonParser.parseString(
                "\"temp.t + t.y + variable.real + q.anim_time + context.other\"");
        assertEquals(Set.of("real"), MolangVariableRefs.collect(json));
    }

    @Test
    void brokenSyntaxIsSkippedSilently() {
        var json = JsonParser.parseString("[\"variable.a +\", \"v.b\"]");
        assertTrue(MolangVariableRefs.collect(json).contains("b"));
    }

    @Test
    void nestedDocumentIsWalkedRecursively() {
        var json = JsonParser.parseString(
                "{\"animations\": {\"anim.x\": {\"timeline\": {\"1.0\": [\"v.deep=1\"]}}}}");
        assertEquals(Set.of("deep"), MolangVariableRefs.collect(json));
    }

    @Test
    void noVariablesYieldsEmpty() {
        assertTrue(MolangVariableRefs.collect(JsonParser.parseString("{\"k\": 5}")).isEmpty());
    }
}
