package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.port.ArrowHostInstaller;
import io.github.tt432.eyelib.molang.type.MolangObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 箭头访问（{@code ->}）宿主切换契约测试。
 *
 * @author TT432
 */
class MolangArrowAccessTest {
    private final MolangCompilerImpl compiler = new MolangCompilerImpl();
    private final List<MolangObject> installedHosts = new ArrayList<>();
    private Object restoredToken;

    @AfterEach
    void tearDown() {
        MolangRuntimeSupport.setArrowHostInstaller(null);
    }

    private void installRecordingInstaller() {
        MolangRuntimeSupport.setArrowHostInstaller(new ArrowHostInstaller() {
            @Override
            public Object install(MolangScope scope, MolangObject host) {
                installedHosts.add(host);
                // 宿主切换期间右式应能读到安装器写入的变量（同一作用域求值）
                scope.set("variable.arrow_host_installed", 7F);
                return "restore-token";
            }

            @Override
            public void restore(MolangScope scope, Object previous) {
                restoredToken = previous;
            }
        });
    }

    @Test
    void arrowDelegatesHostToInstallerAndReturnsRightValue() {
        installRecordingInstaller();

        MolangScope scope = new MolangScope();
        scope.set("variable.host", 42F);
        CompiledMolangExpression compiled = compiler.compile("v.host->v.target", CompileContext.defaults());
        MolangObject value = compiled.evaluate(scope);

        // 结果 = 右式求值（v.target 未定义 → 0）
        assertEquals(0F, value.asFloat());
        // 安装器收到左侧宿主值
        assertEquals(1, installedHosts.size());
        assertEquals(42F, installedHosts.get(0).asFloat());
        // 右式求值完成后恢复 token 被归还
        assertEquals("restore-token", restoredToken);
    }

    @Test
    void rightSideEvaluatesInSameScopeDuringHostSwitch() {
        installRecordingInstaller();

        MolangScope scope = new MolangScope();
        scope.set("variable.host", 1F);
        CompiledMolangExpression compiled = compiler.compile("v.host->v.arrow_host_installed", CompileContext.defaults());
        MolangObject value = compiled.evaluate(scope);

        // 安装器在切换期间写入的变量对右式可见
        assertEquals(7F, value.asFloat());
    }

    @Test
    void withoutInstallerArrowFallsBackToRightValue() {
        // 未注册安装器：退化为仅求值右式（与历史行为一致）
        MolangScope scope = new MolangScope();
        scope.set("variable.host", 3F);
        scope.set("variable.target", 5F);
        CompiledMolangExpression compiled = compiler.compile("v.host->v.target", CompileContext.defaults());
        MolangObject value = compiled.evaluate(scope);

        assertEquals(5F, value.asFloat());
    }

    @Test
    void nestedArrowsBalancePushPop() {
        // 嵌套箭头：外层先 push，内层再 push/pop，最后外层 pop——restore 依次收到内层 token
        List<String> restoreOrder = new ArrayList<>();
        MolangRuntimeSupport.setArrowHostInstaller(new ArrowHostInstaller() {
            @Override
            public Object install(MolangScope scope, MolangObject host) {
                return "token:" + host.asFloat();
            }

            @Override
            public void restore(MolangScope scope, Object previous) {
                restoreOrder.add(String.valueOf(previous));
            }
        });

        MolangScope scope = new MolangScope();
        scope.set("variable.a", 1F);
        scope.set("variable.b", 2F);
        scope.set("variable.c", 3F);
        CompiledMolangExpression compiled = compiler.compile("v.a->v.b->v.c", CompileContext.defaults());
        MolangObject value = compiled.evaluate(scope);

        assertEquals(3F, value.asFloat());
        // LIFO：内层箭头先恢复，外层后恢复
        assertEquals(List.of("token:1.0", "token:2.0"), restoreOrder);
    }
}
