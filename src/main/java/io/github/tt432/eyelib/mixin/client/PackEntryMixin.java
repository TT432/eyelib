//? if <26.1 {
package io.github.tt432.eyelib.mixin.client;

import io.github.tt432.eyelib.bridge.client.gui.adapter.BedrockPackSettingsScreen;
import io.github.tt432.eyelib.bridge.client.loader.adapter.BedrockPackResources;
import io.github.tt432.eyelib.importer.addon.BedrockPackSettingsCatalog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.TransferableSelectionList;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

/**
 * 资源包列表条目右侧的「包设置」齿轮（对应基岩版资源包条目上的齿轮按钮）。
 * <p>
 * 仅对带 subpacks/settings 的 Bedrock 附加包（{@link BedrockPackResources} 注册的
 * {@code file/*.mcpack|mcaddon}）显示；点击打开 {@link BedrockPackSettingsScreen}。
 * 齿轮矩形与 {@code render} 的几何同源：render 的 {@code left/width} 即
 * {@code parent.getRowLeft()/getRowWidth()}（{@code AbstractSelectionList.renderList}）。
 * <p>
 * 26.1 未适配（输入/渲染体系重写），与 BedrockPackSettingsScreen 同步排除。
 *
 * @author TT432
 */
@Mixin(TransferableSelectionList.PackEntry.class)
public abstract class PackEntryMixin {
    @Unique
    private static final ResourceLocation EYELIB_GEAR =
            io.github.tt432.eyelib.bridge.material.ResourceLocationBridge
                    .fromParts("eyelib", "textures/gui/pack_settings_gear.png");
    /** 齿轮矩形（条目本地坐标）：[width-20, width-4] × [2, 18]。 */
    @Unique
    private static final int EYELIB_GEAR_RIGHT_MARGIN = 20;
    @Unique
    private static final int EYELIB_GEAR_SIZE = 16;
    @Unique
    private static final int EYELIB_GEAR_TOP = 2;

    @Shadow
    @Final
    private PackSelectionModel.Entry pack;
    @Shadow
    @Final
    protected Minecraft minecraft;
    @Shadow
    @Final
    private TransferableSelectionList parent;

    @Unique
    private BedrockPackSettingsCatalog eyelib$catalog;
    @Unique
    private boolean eyelib$catalogResolved;

    @Inject(method = "render", at = @At("TAIL"))
    private void eyelib$renderSettingsGear(GuiGraphics guiGraphics, int index, int top, int left, int width,
                                           int height, int mouseX, int mouseY, boolean hovering,
                                           float partialTick, CallbackInfo ci) {
        if (eyelib$settingsCatalog() == null) {
            return;
        }
        int gearX = left + width - EYELIB_GEAR_RIGHT_MARGIN;
        int gearY = top + EYELIB_GEAR_TOP;
        boolean gearHovered = mouseX >= gearX && mouseX < gearX + EYELIB_GEAR_SIZE
                && mouseY >= gearY && mouseY < gearY + EYELIB_GEAR_SIZE;
        if (gearHovered) {
            guiGraphics.fill(gearX - 1, gearY - 1, gearX + EYELIB_GEAR_SIZE + 1,
                    gearY + EYELIB_GEAR_SIZE + 1, 0x40FFFFFF);
        }
        guiGraphics.blit(EYELIB_GEAR, gearX, gearY, 0.0F, 0.0F,
                EYELIB_GEAR_SIZE, EYELIB_GEAR_SIZE, EYELIB_GEAR_SIZE, EYELIB_GEAR_SIZE);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void eyelib$clickSettingsGear(double mouseX, double mouseY, int button,
                                          CallbackInfoReturnable<Boolean> cir) {
        BedrockPackSettingsCatalog catalog = eyelib$settingsCatalog();
        if (catalog == null || button != 0) {
            return;
        }
        double localX = mouseX - this.parent.getRowLeft();
        double localY = mouseY - ((AbstractSelectionListAccessor) this.parent)
                .eyelib$getRowTop(this.parent.children().indexOf(this));
        int rowWidth = this.parent.getRowWidth();
        if (localX >= rowWidth - EYELIB_GEAR_RIGHT_MARGIN
                && localX < rowWidth - EYELIB_GEAR_RIGHT_MARGIN + EYELIB_GEAR_SIZE
                && localY >= EYELIB_GEAR_TOP && localY < EYELIB_GEAR_TOP + EYELIB_GEAR_SIZE) {
            this.minecraft.setScreen(new BedrockPackSettingsScreen(this.minecraft.screen, catalog));
            cir.setReturnValue(true);
        }
    }

    /** 惰性解析设置目录（每条目实例一次 zip 读取；无可配置内容 → null）。 */
    @Unique
    private BedrockPackSettingsCatalog eyelib$settingsCatalog() {
        if (!eyelib$catalogResolved) {
            eyelib$catalogResolved = true;
            String id = pack.getId();
            if (id.startsWith(BedrockPackResources.ID_PREFIX)) {
                Path file = minecraft.getResourcePackDirectory().resolve(id.substring(BedrockPackResources.ID_PREFIX.length()));
                if (BedrockPackResources.isBedrockAddonFile(file)) {
                    BedrockPackSettingsCatalog loaded = BedrockPackSettingsCatalog.load(file);
                    eyelib$catalog = loaded != null && loaded.hasSettings() ? loaded : null;
                }
            }
        }
        return eyelib$catalog;
    }
}
//?}
