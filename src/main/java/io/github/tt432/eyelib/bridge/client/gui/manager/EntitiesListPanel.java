package io.github.tt432.eyelib.bridge.client.gui.manager;

import com.mojang.blaze3d.vertex.Tesselator;
import io.github.tt432.eyelib.bridge.Eyelib;
import io.github.tt432.eyelib.bridge.client.ClientTickHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if <26.1 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
//? if <1.20.6 {
import net.minecraftforge.client.gui.widget.ScrollPanel;
//?}
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author TT432
 */
//? if <1.20.6 {
public class EntitiesListPanel extends ScrollPanel {

    static EyelibManagerScreen.@Nullable EntityButton lastSelected;

    private final List<EyelibManagerScreen.EntityButton> allEntitiesList = new ArrayList<>();
    private final List<EyelibManagerScreen.EntityButton> filtedEntitiesList = new ArrayList<>();
    private boolean finished = false;

    {
        CompletableFuture.runAsync(() -> {
            for (ResourceLocation resourceLocation : BuiltInRegistries.ENTITY_TYPE.keySet()) {
                allEntitiesList.add(new EyelibManagerScreen.EntityButton(
                        resourceLocation.toString(),
                        BuiltInRegistries.ENTITY_TYPE.get(resourceLocation).getDescription(),
                        //? if <1.20.6 {
                        new ResourceLocation(Eyelib.MOD_ID, "icons/entities/" + resourceLocation.toString()
                                                                                                 .replace(":", "/"))
                        //?} else {
                        ResourceLocation.fromNamespaceAndPath(Eyelib.MOD_ID, "icons/entities/" + resourceLocation.toString()
                                                                                                 .replace(":", "/"))
                        //?}
                ));
            }
            filtedEntitiesList.addAll(allEntitiesList);
            finished = true;
        });
    }

    public EntitiesListPanel(Minecraft client, int width, int height, int top, int left) {
        super(client, width, height, top, left);
    }

    public EntitiesListPanel(Minecraft client, int width, int height, int top, int left, int border) {
        super(client, width, height, top, left, border);
    }

    public EntitiesListPanel(Minecraft client, int width, int height, int top, int left, int border, int barWidth) {
        super(client, width, height, top, left, border, barWidth);
    }

    public EntitiesListPanel(Minecraft client, int width, int height, int top, int left, int border, int barWidth, int barBgColor, int barColor, int barBorderColor) {
        super(client, width, height, top, left, border, barWidth, 0xC0101010, 0xD0101010, barBgColor, barColor, barBorderColor);
    }

    String lastSearch = "";

    void onEdited(String input) {
        if (lastSearch.equals(input)) return;
        filtedEntitiesList.clear();
        for (EyelibManagerScreen.EntityButton entityButton : allEntitiesList) {
            if (StringUtils.contains(entityButton.key, input) || StringUtils.contains(entityButton.name.getString(), input)) {
                filtedEntitiesList.add(entityButton);
            }
        }
        scrollDistance = 0;
        lastSearch = input;
    }

    private int buttonPreLine() {
        return 4;
    }

    private int lineAmount() {
        if (filtedEntitiesList.isEmpty()) {
            return 0;
        }
        return (filtedEntitiesList.size() - 1) / buttonPreLine();
    }

    private int slotSize() {
        return (width - (border * 2)) / buttonPreLine();
    }

    @Override
    protected int getContentHeight() {
        return lineAmount() * slotSize();
    }

    private EyelibManagerScreen.@Nullable EntityButton hoverButton;

    //? if <26.1 {
    @Override
    protected void drawPanel(GuiGraphics guiGraphics, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY) {
        if (!finished) {
            centeredString(guiGraphics, "正在加载", left, top, width, height);
        } else {
            var slotSize = slotSize();
            var renderLine = (height - border * 2) / slotSize + 2;
            var iconBorder = slotSize / 30F;
            var iconSize = slotSize - (iconBorder * 2);
            int line = (int) (scrollDistance / slotSize);

            hoverButton = null;

            if (lineAmount() >= line) {
                for (int i = 0; i < renderLine; i++) {
                    int index;
                    for (int i1 = 0; i1 < buttonPreLine() && (index = (line + i) * buttonPreLine() + i1) < filtedEntitiesList.size(); i1++) {
                        var x = Math.round(left + border + i1 * slotSize + iconBorder);
                        var y = Math.round(top + border - (scrollDistance % slotSize) + i * slotSize + iconBorder);
                        var s = Math.round(iconSize);

                        EyelibManagerScreen.EntityButton entityButton = filtedEntitiesList.get(index);
                        boolean hover = EyelibManagerScreen.hover(x, y, slotSize, slotSize, mouseX, mouseY);
                        float a;

                        if (lastSelected != entityButton) {
                            a = entityButton.animator.getTime(ClientTickHandler.getTick(),
                                    //? if <1.20.6 {
                                    Minecraft.getInstance().timer.partialTick
                                    //?} else {
                                    Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true)
                                    //?}
                                    , hover);
                        } else {
                            a = 1;
                        }

                        EyelibManagerScreen.renderEntityButton(guiGraphics, x, y, s, a, entityButton);
                        if (EyelibManagerScreen.hover(x, y, iconSize, iconSize, mouseX, mouseY)) {
                            hoverButton = entityButton;
                        }
                    }
                }
            }
        }
    }
    //?} else {
    protected void drawPanel() {
        throw new UnsupportedOperationException("26.1 GUI rendering not yet supported");
    }
    //?}

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button))
            return true;

        if (hoverButton != null) {
            lastSelected = hoverButton;
            return true;
        }
        return false;
    }

    //? if <26.1 {
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (hoverButton != null) {
            guiGraphics.renderTooltip(Minecraft.getInstance().font, hoverButton.name, mouseX, mouseY);
        }
    }
    //?} else {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphicsExtractor, int mouseX, int mouseY, float partialTick) {
        throw new UnsupportedOperationException("26.1 GUI rendering not yet supported");
    }
    //?}

    //? if <26.1 {
    private static void centeredString(GuiGraphics guiGraphics, String string, int x, int y, int w, int h) {
        Font font = Minecraft.getInstance().font;
        int th = font.lineHeight;

        int tw = font.width(string);
        guiGraphics.drawString(font, string, x + w / 2 - tw / 2, y + h / 2 - th / 2, 0xFFFFFFFF);
    }
    //?}

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {

    }
}
//?} else {
public class EntitiesListPanel extends AbstractWidget {
    static EyelibManagerScreen.@Nullable EntityButton lastSelected;

    public EntitiesListPanel(Minecraft client, int width, int height, int top, int left) {
        this(client, width, height, top, left, 0);
    }

    public EntitiesListPanel(Minecraft client, int width, int height, int top, int left, int border) {
        super(left, top, width, height, Component.empty());
        throw new UnsupportedOperationException("NeoForge 1.21.1 ScrollPanel 已删除，需用 vanilla 方案重写");
    }

    public EntitiesListPanel(Minecraft client, int width, int height, int top, int left, int border, int barWidth) {
        this(client, width, height, top, left, border);
    }

    public EntitiesListPanel(Minecraft client, int width, int height, int top, int left, int border, int barWidth, int barBgColor, int barColor, int barBorderColor) {
        this(client, width, height, top, left, border);
    }

    void onEdited(String input) {
        throw new UnsupportedOperationException("NeoForge 1.21.1 ScrollPanel 已删除，需用 vanilla 方案重写");
    }

    //? if <26.1 {
    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // TODO: NeoForge 1.21.1 ScrollPanel 已删除，需用 vanilla 方案重写
    }
    //?} else {
    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphicsExtractor, int mouseX, int mouseY, float partialTick) {
        throw new UnsupportedOperationException("26.1 GUI rendering not yet supported");
    }
    //?}

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}
//?}
