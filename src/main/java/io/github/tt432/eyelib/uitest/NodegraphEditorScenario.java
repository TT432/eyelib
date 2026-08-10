//? if <26.1 {
package io.github.tt432.eyelib.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import io.github.tt432.eyelib.client.nodegraph.editor.ldlib2.Ldlib2NodegraphEditor;

/**
 * 节点图编辑器（LDLib2 UI 最大消费方）的 in-client UI 烟雾场景：
 * 打开编辑器 → 等画布与 root 节点建出 → 截图 → 关屏。
 *
 * <p>断言画布结构出现（节点元素计数），截图为人工目检交付物。
 * 26.1.2.33 尚无 uitest 框架（上游 26.1 分支停在 2.2.33），本类按版本守卫排除 26.1。
 */
@LDLRegisterClient(name = "nodegraph_editor", group = "eyelib", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class NodegraphEditorScenario implements UIScenario {

    @Override
    public void define(ScenarioBuilder s) {
        s.step("open nodegraph editor (new client_entity library)",
                        ctx -> Ldlib2NodegraphEditor.open(null))
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                // 节点异步构建：新库恰 1 个 entity.root 节点
                .waitUntil("root node element built", ctx -> ctx.count(".__node-element__") == 1)
                .check("exactly one entity.root node on canvas",
                        ctx -> ctx.count(".__node-element__") == 1)
                .settleMs(150)
                .screenshot("01_editor")
                .closeScreen();
    }
}
//?}
