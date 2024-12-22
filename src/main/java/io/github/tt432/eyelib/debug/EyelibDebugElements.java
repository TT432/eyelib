package io.github.tt432.eyelib.debug;

import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.imgui4mc.ImGuiWindowElements;
import io.github.tt432.imgui4mc.RegisterImGui;
import io.github.tt432.imgui4mc.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

/**
 * @author TT432
 */
@RegisterImGui
public class EyelibDebugElements implements ImGuiWindowElements {
    boolean openEyelibDebug = false;
    ImBoolean openEyelibDebugBool = new ImBoolean(false);

    UUID currentEntityId;
    ImString entityId = new ImString();
    ImString molang = new ImString();
    MolangValue cache;

    @Override
    public void init(Window window, long windowId) {

    }

    @Override
    public void process(Window window) {
        if (ImGui.checkbox("openEyelibDebug", openEyelibDebugBool)) {
            openEyelibDebug = openEyelibDebugBool.get();
        }

        if (openEyelibDebug) {
            if (ImGui.inputText("entityId", entityId)) {
                try {
                    currentEntityId = UUID.fromString(entityId.get());
                } catch (RuntimeException e) {
                }
            }

            if (ImGui.inputText("molang", molang)) {
                cache = new MolangValue(molang.get());
            }

            ClientLevel level = Minecraft.getInstance().level;
            if (level != null && currentEntityId != null && level.getEntities().get(currentEntityId) instanceof Entity entity) {
                RenderData<Object> component = RenderData.getComponent(entity);

                MolangScope scope = component.getScope();

                if (scope != null && cache != null) {
                    ImGui.textWrapped("molang: " + cache.getObject(scope).toString());
                } else {
                    ImGui.textWrapped("molang: not a valid molang");
                }
            } else {
                ImGui.textWrapped("molang: not a valid entity");
            }
        }
    }
}
