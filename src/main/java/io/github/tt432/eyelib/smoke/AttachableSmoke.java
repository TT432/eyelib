package io.github.tt432.eyelib.smoke;

import io.github.tt432.clientsmokeannotation.ClientSmoke;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.entity.AttachableResolver;
import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelib.client.render.AttachableItemRenderSetup;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 验证 attachable 资源从 assets/test/eyelib/ 下自动加载并端到端渲染。
 * @author TT432
 */
@ClientSmoke(
        description = "attachable 资源自动加载 + 端到端渲染链路",
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

        var all = AttachableManager.readPort().getAllData();
        LOGGER.info("[AttachableSmoke] auto-loaded attachables: {}", all.size());
        all.forEach((k, v) -> LOGGER.info("[AttachableSmoke]   {} item={}", k, v.item()));

        if (all.isEmpty()) {
            throw new AssertionError("No attachables auto-loaded");
        }

        ItemStack stick = new ItemStack(Items.STICK);
        player.getInventory().setItem(0, stick);

        BrClientEntity resolved = AttachableResolver.resolve(player, stick);
        if (resolved == null) {
            throw new AssertionError("Attachable not auto-loaded for minecraft:stick");
        }

        RenderData<ItemStack> rd = AttachableItemRenderSetup.getOrPrepare(player, InteractionHand.MAIN_HAND);
        if (rd == null) {
            throw new AssertionError("RenderData not created");
        }
        LOGGER.info("[AttachableSmoke] ModelComponents: {}", rd.getModelComponents().size());
    }
}
