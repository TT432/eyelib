package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.type.MolangFloat;
import io.github.tt432.eyelib.molang.type.MolangNull;
import io.github.tt432.eyelib.molang.type.MolangObject;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MolangScope 作用域测试，验证变量存取、作用域链和线程安全。
 *
 * @author TT432
 */
@NullMarked
class MolangScopeTest {

    @Test
    void 新建scopeGet不存在key返回Null() {
        MolangScope scope = new MolangScope();
        assertSame(MolangNull.INSTANCE, scope.get("nonexistent"));
    }

    @Test
    void set后Get返回设置的值() {
        MolangScope scope = new MolangScope();
        MolangObject obj = scope.set("x", 42.0f);
        assertSame(obj, scope.get("x"));
        assertEquals(42.0f, scope.get("x").asFloat(), 0.0001f);
    }

    @Test
    void parentScope链子ScopeGet不到时Fallback到Parent() {
        MolangScope parent = new MolangScope();
        parent.set("parentKey", 99.0f);

        MolangScope child = new MolangScope();
        child.setParent(parent);

        // 子 scope 没有 parentKey，应 fallback 到 parent
        assertEquals(99.0f, child.get("parentKey").asFloat(), 0.0001f);

        // 子 scope 设置同名 key 后优先返回子 scope 的值
        child.set("parentKey", 1.0f);
        assertEquals(1.0f, child.get("parentKey").asFloat(), 0.0001f);
    }

    @Test
    void volatileParent字段简单线程安全测试() throws Exception {
        MolangScope parent = new MolangScope();
        parent.set("key", 42.0f);

        MolangScope child = new MolangScope();
        child.setParent(parent);

        Thread t = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                child.get("key");
            }
        });
        t.start();
        t.join();

        // 并发读取后 parent 值仍然正确
        assertEquals(42.0f, child.get("key").asFloat(), 0.0001f);
    }

    @Test
    void set各类型重载正常工作() {
        MolangScope scope = new MolangScope();

        scope.set("f", 3.14f);
        assertEquals(3.14f, scope.get("f").asFloat(), 0.0001f);

        scope.set("d", 2.71);
        assertEquals(2.71f, scope.get("d").asFloat(), 0.0001f);

        scope.set("b_true", true);
        assertEquals(1.0f, scope.get("b_true").asFloat(), 0.0001f);

        scope.set("b_false", false);
        assertEquals(0.0f, scope.get("b_false").asFloat(), 0.0001f);
    }

    @Test
    void remove从缓存中移除变量() {
        MolangScope scope = new MolangScope();
        scope.set("key", 10.0f);
        assertEquals(10.0f, scope.get("key").asFloat(), 0.0001f);

        scope.remove("key");
        assertSame(MolangNull.INSTANCE, scope.get("key"));
    }

    @Test
    void setFloatSupplier延迟求值() {
        MolangScope scope = new MolangScope();
        scope.set("lazy", () -> 42.0f);
        assertEquals(42.0f, scope.get("lazy").asFloat(), 0.0001f);
    }

    @Test
    void contains检查变量存在() {
        MolangScope scope = new MolangScope();
        assertFalse(scope.contains("key"));
        scope.set("key", 1.0f);
        assertTrue(scope.contains("key"));
    }

    @Test
    void contains在ParentScope中查找() {
        MolangScope parent = new MolangScope();
        parent.set("pKey", 1.0f);

        MolangScope child = new MolangScope();
        child.setParent(parent);

        assertTrue(child.contains("pKey"));
        assertFalse(child.contains("nonexistent"));
    }
}
