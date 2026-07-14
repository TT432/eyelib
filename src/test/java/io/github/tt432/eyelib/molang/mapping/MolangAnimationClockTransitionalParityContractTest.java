package io.github.tt432.eyelib.molang.mapping;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.HostRole;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 4 transitional parity: 验证 HostRole&lt;T&gt; API 和 deprecated Class&lt;?&gt; API
 * 在 MolangScope.HostContext 中的互操作性。
 *
 * <p>Migration 将所有静态 Class&lt;?&gt; put 改为 HostRole put（commit 98ed1de9）。
 * 桥接层确保 Class&lt;?&gt; get（用于 callable 参数解析等动态路径）仍能找到 HostRole-put 的值。
 *
 * <p>验证场景：
 * <ul>
 *   <li>HostRole put → Class&lt;?&gt; get 可见（桥接方向，migration 后的兼容性）</li>
 *   <li>HostRole put → HostRole get 一致</li>
 *   <li>Class&lt;?&gt; put → Class&lt;?&gt; get 仍功能正常（遗留路径）</li>
 *   <li>不同类型的 HostRole 不互相干扰</li>
 * </ul>
 *
 * @author TT432
 */
class MolangAnimationClockTransitionalParityContractTest {

    @Test
    void hostRolePutIsVisibleViaClassGet() {
        MolangScope scope = new MolangScope();
        HostRole<TestClock> role = HostRole.of("test_clock", TestClock.class);
        TestClock clock = new TestClock(42.0f);

        scope.getHostContext().put(role, clock);

        // Class<?> get 应通过桥接找到 HostRole-put 的值
        Optional<TestClock> viaClass = scope.getHostContext().get(TestClock.class);
        assertTrue(viaClass.isPresent(), "Class<?> get should find HostRole-put value via bridge");
        assertEquals(42.0f, viaClass.get().time(), 0.001f);
    }

    @Test
    void hostRolePutAndHostRoleGetAreConsistent() {
        MolangScope scope = new MolangScope();
        HostRole<TestClock> role = HostRole.of("test_clock", TestClock.class);
        TestClock clock = new TestClock(7.0f);

        scope.getHostContext().put(role, clock);

        Optional<TestClock> viaRole = scope.getHostContext().get(role);
        assertTrue(viaRole.isPresent());
        assertEquals(7.0f, viaRole.get().time(), 0.001f);
    }

    @Test
    void classPutAndClassGetRemainFunctional() {
        MolangScope scope = new MolangScope();
        TestClock clock = new TestClock(99.0f);

        scope.getHostContext().put(TestClock.class, clock);

        Optional<TestClock> viaClass = scope.getHostContext().get(TestClock.class);
        assertTrue(viaClass.isPresent());
        assertEquals(99.0f, viaClass.get().time(), 0.001f);
    }

    @Test
    void hostRolePutDifferentTypesDoNotInterfere() {
        MolangScope scope = new MolangScope();
        HostRole<TestClock> clockRole = HostRole.of("clock", TestClock.class);
        HostRole<TestSignal> signalRole = HostRole.of("signal", TestSignal.class);

        scope.getHostContext().put(clockRole, new TestClock(1.0f));
        scope.getHostContext().put(signalRole, new TestSignal(true));

        Optional<TestClock> clock = scope.getHostContext().get(TestClock.class);
        Optional<TestSignal> signal = scope.getHostContext().get(TestSignal.class);

        assertTrue(clock.isPresent());
        assertTrue(signal.isPresent());
        assertEquals(1.0f, clock.get().time(), 0.001f);
        assertTrue(signal.get().active());
    }

    record TestClock(float time) {}
    record TestSignal(boolean active) {}
}
