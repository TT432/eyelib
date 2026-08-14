package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.type.MolangObject;
import io.github.tt432.eyelib.molang.type.MolangString;
import io.github.tt432.eyelib.molang.type.MolangStruct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * struct（对象值）契约测试。Oracle = Mojang 官方 syntax-guide「Structs」节：
 * 结构按使用隐式定义、可整体赋值、可任意深嵌套；文档明示的 5 个表达式均返回 1.23。
 *
 * <p>赋值整体传递为引用语义（深拷贝为 Deferred 兼容策略，见
 * docs/molang/design/compatibility-semantics-matrix.md）。
 */
class MolangStructTest {
    private final MolangCompilerImpl compiler = new MolangCompilerImpl();

    private MolangObject eval(String source, MolangScope scope) {
        return compiler.compile(source, CompileContext.defaults()).evaluate(scope);
    }

    @Nested
    @DisplayName("官方文档等价例（syntax-guide Structs：以下每个表达式返回 1.23）")
    class OfficialDocExamples {
        @Test
        void directMemberRead() {
            MolangScope scope = new MolangScope();
            MolangObject value = eval("""
                    v.cowcow.friend = v.pigpig; v.pigpig->v.test.a.b.c = 1.23; return v.cowcow.friend->v.test.a.b.c;
                    """, scope);
            assertEquals(1.23F, value.asFloat(), 0.0001F);
        }

        @Test
        void structAssignmentThenDeepRead() {
            MolangScope scope = new MolangScope();
            MolangObject value = eval("""
                    v.cowcow.friend = v.pigpig; v.pigpig->v.test.a.b.c = 1.23; v.moo = v.cowcow.friend->v.test; return v.moo.a.b.c;
                    """, scope);
            assertEquals(1.23F, value.asFloat(), 0.0001F);
        }

        @Test
        void partialStructAssignmentDepth1() {
            MolangScope scope = new MolangScope();
            MolangObject value = eval("""
                    v.cowcow.friend = v.pigpig; v.pigpig->v.test.a.b.c = 1.23; v.moo = v.cowcow.friend->v.test.a; return v.moo.b.c;
                    """, scope);
            assertEquals(1.23F, value.asFloat(), 0.0001F);
        }

        @Test
        void partialStructAssignmentDepth2() {
            MolangScope scope = new MolangScope();
            MolangObject value = eval("""
                    v.cowcow.friend = v.pigpig; v.pigpig->v.test.a.b.c = 1.23; v.moo = v.cowcow.friend->v.test.a.b; return v.moo.c;
                    """, scope);
            assertEquals(1.23F, value.asFloat(), 0.0001F);
        }

        @Test
        void fullPathAssignmentReadsBack() {
            MolangScope scope = new MolangScope();
            MolangObject value = eval("""
                    v.cowcow.friend = v.pigpig; v.pigpig->v.test.a.b.c = 1.23; v.moo = v.cowcow.friend->v.test.a.b.c; return v.moo;
                    """, scope);
            assertEquals(1.23F, value.asFloat(), 0.0001F);
        }
    }

    @Nested
    @DisplayName("对象作为值")
    class ObjectAsValue {
        @Test
        void memberWriteMakesParentReadableAsStruct() {
            // qpptaw 形态：逐成员写入后，variable.qpptaw 整体可读为对象
            MolangScope scope = new MolangScope();
            eval("variable.qpptaw.r = 1; variable.qpptaw.x = 2;", scope);
            MolangObject root = scope.get("variable.qpptaw");
            assertInstanceOf(MolangStruct.class, root);
            assertEquals(1F, scope.get("variable.qpptaw.r").asFloat(), 0.0001F);
            assertEquals(2F, scope.get("variable.qpptaw.x").asFloat(), 0.0001F);
        }

        @Test
        void structAssignmentSharesMembers() {
            // 引用语义：v.b = v.a 后经 v.b 写入对 v.a 可见
            MolangScope scope = new MolangScope();
            MolangObject value = eval("v.a.x = 1; v.b = v.a; v.b.y = 2; return v.a.y;", scope);
            assertEquals(2F, value.asFloat(), 0.0001F);
        }

        @Test
        void wholeStructOverwriteReplacesMembers() {
            MolangScope scope = new MolangScope();
            MolangObject value = eval("v.a.x = 1; v.a = 5; return v.a;", scope);
            assertEquals(5F, value.asFloat(), 0.0001F);
            // 覆盖后旧成员路径落进非标量 → 0
            assertEquals(0F, scope.get("variable.a.x").asFloat(), 0.0001F);
        }

        @Test
        void scalarOverwriteByMemberWrite() {
            // BE 隐式定义：标量被成员写覆盖为 struct
            MolangScope scope = new MolangScope();
            MolangObject value = eval("v.a = 5; v.a.x = 7; return v.a.x;", scope);
            assertEquals(7F, value.asFloat(), 0.0001F);
        }

        @Test
        void structMembersHoldStrings() {
            MolangScope scope = new MolangScope();
            eval("v.obj.name = 'hello';", scope);
            assertEquals("hello", scope.get("variable.obj.name").asString());
            assertInstanceOf(MolangString.class, scope.get("variable.obj.name"));
        }

        @Test
        void missingMemberReadsAsZero() {
            MolangScope scope = new MolangScope();
            assertEquals(0F, eval("v.obj.missing", scope).asFloat(), 0.0001F);
            assertEquals(0F, eval("v.obj", scope).asFloat(), 0.0001F);
        }
    }

    @Nested
    @DisplayName("scope 既有行为保持")
    class ScopeCompat {
        @Test
        void twoSegmentNamesStayFlat() {
            MolangScope scope = new MolangScope();
            scope.set("variable.foo", 3F);
            assertEquals(3F, scope.get("variable.foo").asFloat(), 0.0001F);
            // 两段名不产生 struct 根
            assertEquals(3F, scope.localEntries().get("variable.foo").asFloat(), 0.0001F);
        }

        @Test
        void tempStructClearedWithTempVariables() {
            MolangScope scope = new MolangScope();
            scope.set("temp.loc.x", 1.0f);
            scope.set("variable.keep", 2.0f);
            scope.clearTempVariables();
            assertEquals(0F, scope.get("temp.loc.x").asFloat(), 0.0001F);
            assertEquals(2.0F, scope.get("variable.keep").asFloat(), 0.0001F);
        }

        @Test
        void parentChainRootDelegation() {
            MolangScope parent = new MolangScope();
            parent.set("variable.obj.x", 9F);
            MolangScope child = new MolangScope();
            child.setParent(parent);
            assertEquals(9F, child.get("variable.obj.x").asFloat(), 0.0001F);
            // 子层同名根遮蔽 parent 整棵 struct
            child.set("variable.obj.y", 1F);
            assertEquals(0F, child.get("variable.obj.x").asFloat(), 0.0001F);
        }

        @Test
        void removeMemberLeaf() {
            MolangScope scope = new MolangScope();
            scope.set("variable.obj.x", 1F);
            scope.set("variable.obj.y", 2F);
            scope.remove("variable.obj.x");
            assertEquals(0F, scope.get("variable.obj.x").asFloat(), 0.0001F);
            assertEquals(2F, scope.get("variable.obj.y").asFloat(), 0.0001F);
        }
    }
}
