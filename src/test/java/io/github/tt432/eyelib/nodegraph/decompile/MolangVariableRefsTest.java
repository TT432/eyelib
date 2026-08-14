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

    @Test
    void assignmentLhsIsWriteOtherOccurrencesAreReads() {
        var json = JsonParser.parseString("\"v.a=5; v.b=v.a*2; v.c=(v.a==5); variable.d=v.c\"");
        MolangVariableRefs.Refs refs = MolangVariableRefs.collectWithAccess(json);
        assertEquals(Set.of("a", "b", "c", "d"), refs.writes());
        assertEquals(Set.of("a", "c"), refs.reads());
    }

    @Test
    void equalityComparisonIsNotAWrite() {
        var json = JsonParser.parseString("\"v.x==5 && v.y!=3\"");
        MolangVariableRefs.Refs refs = MolangVariableRefs.collectWithAccess(json);
        assertTrue(refs.writes().isEmpty());
        assertEquals(Set.of("x", "y"), refs.reads());
    }

    @Test
    void unionPreservesOccurrenceOrder() {
        var json = JsonParser.parseString("[\"v.b=v.a\", \"v.c=v.b\"]");
        assertEquals(Set.of("a", "b", "c"), MolangVariableRefs.collect(json));
    }

    @Test
    void memberChainsKeepDottedName() {
        // variable.qpptaw.r = 1 → 成员写 "qpptaw.r"；v.qpptaw.x = 2 → 写；v.qpptaw.y 读取 → 读
        var json = JsonParser.parseString(
                "\"variable.qpptaw.r=1; v.qpptaw.x=2; v.out=v.qpptaw.y + 1\"");
        MolangVariableRefs.Refs refs = MolangVariableRefs.collectWithAccess(json);
        assertEquals(Set.of("qpptaw.r", "qpptaw.x", "out"), refs.writes());
        assertEquals(Set.of("qpptaw.y"), refs.reads());
    }

    @Test
    void deepMemberChainAndWholeObjectCoexist() {
        var json = JsonParser.parseString("\"v.a.b.c=1; v.a=v.b\"");
        MolangVariableRefs.Refs refs = MolangVariableRefs.collectWithAccess(json);
        assertEquals(Set.of("a.b.c", "a"), refs.writes());
        assertEquals(Set.of("b"), refs.reads());
    }
}
