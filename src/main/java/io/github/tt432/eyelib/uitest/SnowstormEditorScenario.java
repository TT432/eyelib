//? if <26.1 {
package io.github.tt432.eyelib.uitest;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.registry.RegistrationEnvironment;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegisterClient;
import com.lowdragmc.lowdraglib2.uitest.ScenarioBuilder;
import com.lowdragmc.lowdraglib2.uitest.UIScenario;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormEditorGate;
import io.github.tt432.eyelib.client.gui.snowstorm.inputs.InputViewFactory;
import io.github.tt432.eyelib.snowstorm.editor.EditorRuntime;
import io.github.tt432.eyelib.snowstorm.io.SnowstormImport;

/**
 * Snowstorm 编辑器（ADR-0036）in-client UI 烟雾场景：
 * 打开编辑器 → 结构断言（menubar/sidebar/stage 就位）→ 加载 fire 预设 →
 * Config identifier 断言 + Sidebar 重建 → 截图 → 关屏。
 *
 * <p>26.1.2.33 无 uitest 框架（上游 26.1 停在 2.2.33），本类按版本守卫排除 26.1。
 * 截图为人工目检交付物。
 */
@LDLRegisterClient(name = "snowstorm_editor", group = "eyelib", registry = UIScenario.REGISTRY,
        environment = RegistrationEnvironment.DEV_ONLY)
public class SnowstormEditorScenario implements UIScenario {

    @Override
    public void define(ScenarioBuilder s) {
        s.step("open snowstorm editor", ctx -> SnowstormEditorGate.openEditor())
                .awaitScreen(ModularUIScreen.class)
                .awaitModularUI()
                .awaitElement("#sidebar")
                .check("editor structure present",
                        ctx -> ctx.count("#menubar") == 1
                                && ctx.count("#sidebar") == 1
                                && ctx.count("#stage") == 1)
                .step("load fire preset", ctx -> {
                    SnowstormImport.loadPreset("fire");
                    InputViewFactory.notifyChanged();
                })
                .waitUntil("config identifier is snowstorm:fire",
                        ctx -> "snowstorm:fire".equals(EditorRuntime.Config.identifier))
                .settleMs(200)
                .screenshot("01_editor_fire")
                .closeScreen()
                .check("screen closed", ctx -> net.minecraft.client.Minecraft.getInstance().screen == null);
    }
}
//?}
