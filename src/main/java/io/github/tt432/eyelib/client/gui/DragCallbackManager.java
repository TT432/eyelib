package io.github.tt432.eyelib.client.gui;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.glfwSetDropCallback;
import static org.lwjgl.system.Pointer.POINTER_SIZE;

/**
 * @author TT432
 */
@Slf4j
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DragCallbackManager {
    @SubscribeEvent
    public static void onEvent(FMLClientSetupEvent event) {
        glfwSetDropCallback(Minecraft.getInstance().getWindow().getWindow(), (window, count, names) -> {
            if (Minecraft.getInstance().screen instanceof DragTargetScreen dragTargetScreen) {
                List<String> nameList = new ArrayList<>();

                for (int i = 0; i < count; i++) {
                    nameList.add(MemoryUtil.memUTF8(MemoryUtil.memGetAddress(names + (long) i * POINTER_SIZE)));
                }

                dragTargetScreen.onDragEnter(nameList);
            }
        });
    }
}
