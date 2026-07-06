package io.github.tt432.eyelib.smoke;

import io.github.tt432.clientsmokeannotation.ClientSmoke;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.entity.AttachableResolver;
import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelib.client.render.AttachableItemRenderSetup;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
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
        description = "attachable 资源自动加载 + 端到端渲染链路（手持 + 盔甲槽）",
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

        var all = AttachableManager.INSTANCE.all();
        LOGGER.info("[AttachableSmoke] auto-loaded attachables: {}", all.size());
        all.forEach((k, v) -> LOGGER.info("[AttachableSmoke]   {} item={}", k, v.item()));

        if (all.isEmpty()) {
            throw new AssertionError("No attachables auto-loaded");
        }

        verifyHandAttachable(player);
        verifyArmorAttachable(player);
    }

    private static void verifyHandAttachable(Player player) {
        ItemStack stick = new ItemStack(Items.STICK);
        player.getInventory().setItem(0, stick);

        BrClientEntity resolved = AttachableResolver.resolve(player, stick);
        if (resolved == null) {
            throw new AssertionError("Attachable not auto-loaded for minecraft:stick");
        }

        RenderData<ItemStack> rd = AttachableItemRenderSetup.getOrPrepare(player, InteractionHand.MAIN_HAND, true);
        if (rd == null) {
            throw new AssertionError("RenderData not created for MAIN_HAND");
        }
        LOGGER.info("[AttachableSmoke] hand ModelComponents: {}", rd.getModelComponents().size());
    }

    private static void verifyArmorAttachable(Player player) {
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        player.setItemSlot(EquipmentSlot.HEAD, helmet);

        BrClientEntity resolved = AttachableResolver.resolve(player, helmet);
        if (resolved == null) {
            throw new AssertionError("Attachable not auto-loaded for minecraft:iron_helmet");
        }

        RenderData<ItemStack> rd = AttachableItemRenderSetup.getOrPrepare(player, EquipmentSlot.HEAD, false);
        if (rd == null) {
            throw new AssertionError("RenderData not created for HEAD slot");
        }
        LOGGER.info("[AttachableSmoke] helmet ModelComponents: {}", rd.getModelComponents().size());
    }
}
