package io.github.tt432.eyelib.smoke;

import io.github.tt432.clientsmokeannotation.ClientSmoke;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.entity.AttachableResolver;
import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelib.client.manager.RenderControllerManager;
import io.github.tt432.eyelib.client.render.AttachableItemRenderSetup;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibmolang.MolangValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 验证 attachable 端到端链路：解析→RenderData→模型组件创建。
 * @author TT432
 */
@ClientSmoke(
        description = "attachable 端到端渲染链路验证",
        priority = 50
)
public class AttachableSmoke {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttachableSmoke.class);

    public AttachableSmoke() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) {
            throw new AssertionError("Player not available");
        }

        registerTestRenderController();
        registerTestAttachable();

        ItemStack stick = new ItemStack(Items.STICK);
        player.getInventory().setItem(0, stick);

        BrClientEntity resolved = AttachableResolver.resolve(player, stick);
        if (resolved == null) {
            throw new AssertionError("Attachable not resolved");
        }
        LOGGER.info("[AttachableSmoke] resolved: {}", resolved.identifier());

        RenderData<ItemStack> rd = AttachableItemRenderSetup.getOrPrepare(player, InteractionHand.MAIN_HAND);
        if (rd == null) {
            throw new AssertionError("RenderData not created");
        }
        LOGGER.info("[AttachableSmoke] ModelComponents: {}", rd.getModelComponents().size());
    }

    private static void registerTestAttachable() {
        BrClientEntity attachable = new BrClientEntity(
                "eyelib:test_stick",
                Optional.empty(),
                Map.of("default", "entity_alphatest"),
                Map.of("default", "minecraft:textures/item/stick"),
                Map.of("default", "geometry.eyelib.test_stick"),
                Map.of(),
                List.of(),
                Map.of(),
                Map.of(),
                List.of("controller.render.eyelib.test_stick"),
                Optional.empty(),
                Optional.empty(),
                Map.of("minecraft:stick", "1.0"),
                false
        );
        AttachableManager.writePort().put(attachable.identifier(), attachable);
    }

    private static void registerTestRenderController() {
        RenderControllerEntry entry = new RenderControllerEntry(
                new MolangValue("geometry.default"),
                List.of(new MolangValue("texture.default")),
                Map.of(),
                Map.of("*", new MolangValue("material.default")),
                Map.of()
        );
        RenderControllerManager.writePort().put("controller.render.eyelib.test_stick", entry);
    }
}
