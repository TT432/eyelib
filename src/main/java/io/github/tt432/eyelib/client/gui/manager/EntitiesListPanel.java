package io.github.tt432.eyelib.client.gui.manager;

import io.github.tt432.eyelib.bridge.client.ClientTickHandler;
import io.github.tt432.eyelib.bridge.client.entity.EntityRegistryBridge;
import io.github.tt432.eyelib.ui.UIGraphics;
import io.github.tt432.eyelib.ui.UIScrollPanel;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 客户端实体类型选择列表。
 *
 * @author TT432
 */
public final class EntitiesListPanel extends UIScrollPanel {
    static @Nullable EntityButton lastSelected;

    private volatile List<EntityButton> allEntitiesList = List.of();
    private volatile List<EntityButton> filteredEntitiesList = List.of();
    private volatile boolean finished;
    private volatile String lastSearch = "";
    private @Nullable EntityButton hoverButton;

    public EntitiesListPanel(int x, int y, int width, int height) {
        this(x, y, width, height, 4);
    }

    public EntitiesListPanel(int x, int y, int width, int height, int border) {
        super(x, y, width, height);
        this.border = border;
        CompletableFuture.supplyAsync(EntitiesListPanel::loadEntityButtons)
                .thenAccept(buttons -> {
                    allEntitiesList = buttons;
                    applyFilter(lastSearch);
                    finished = true;
                });
    }

    void onEdited(String input) {
        if (lastSearch.equals(input)) {
            return;
        }

        lastSearch = input;
        applyFilter(input);
        setScrollDistance(0);
    }

    @Override
    protected void renderContent(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
        hoverButton = null;

        if (!finished) {
            gfx.drawCenteredText("正在加载", x + width / 2, y + height / 2, 0xFFFFFFFF);
            return;
        }

        int slotSize = slotSize();
        int renderLine = (height - border * 2) / slotSize + 2;
        float iconBorder = slotSize / 30F;
        float iconSize = slotSize - iconBorder * 2;
        int line = (int) (scrollDistance / slotSize);

        if (lineAmount() < line) {
            return;
        }

        for (int i = 0; i < renderLine; i++) {
            for (int column = 0; column < buttonPreLine(); column++) {
                int index = (line + i) * buttonPreLine() + column;
                if (index >= filteredEntitiesList.size()) {
                    break;
                }

                int buttonX = Math.round(x + border + column * slotSize + iconBorder);
                int buttonY = Math.round(y + border + line * slotSize + i * slotSize + iconBorder);
                int size = Math.round(iconSize);
                EntityButton entityButton = filteredEntitiesList.get(index);
                boolean hover = EyelibManagerScreen.hover(buttonX, buttonY, slotSize, slotSize, mouseX, mouseY);
                float alpha = lastSelected == entityButton ? 1 : entityButton.animator().getTime(
                        ClientTickHandler.getTick(), partialTick, hover);

                EntityButtonRenderer.render(gfx, buttonX, buttonY, size, alpha, entityButton);
                if (EyelibManagerScreen.hover(buttonX, buttonY, iconSize, iconSize, mouseX, mouseY)) {
                    hoverButton = entityButton;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }

        if (hoverButton != null) {
            lastSelected = hoverButton;
            return true;
        }

        return false;
    }

    @Override
    public int getContentHeight() {
        return Math.max(height, (lineAmount() + 1) * slotSize() + border * 2);
    }

    @Override
    protected void renderOverlay(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
        if (hoverButton != null) {
            gfx.renderTooltip(hoverButton.name(), mouseX, mouseY);
        }
    }

    private void applyFilter(String input) {
        List<EntityButton> filtered = new java.util.ArrayList<>();
        for (EntityButton entityButton : allEntitiesList) {
            if (StringUtils.contains(entityButton.key(), input) || StringUtils.contains(entityButton.name(), input)) {
                filtered.add(entityButton);
            }
        }
        filteredEntitiesList = List.copyOf(filtered);
        setScrollDistance(scrollDistance);
    }

    private static List<EntityButton> loadEntityButtons() {
        List<EntityButton> buttons = new java.util.ArrayList<>();
        for (EntityRegistryBridge.EntityTypeEntry entry : EntityRegistryBridge.getEntries()) {
            buttons.add(new EntityButton(
                    entry.id(),
                    entry.description(),
                    "eyelib:icons/entities/" + entry.id().replace(":", "/")
            ));
        }
        return List.copyOf(buttons);
    }

    private int buttonPreLine() {
        return 4;
    }

    private int lineAmount() {
        if (filteredEntitiesList.isEmpty()) {
            return 0;
        }
        return (filteredEntitiesList.size() - 1) / buttonPreLine();
    }

    private int slotSize() {
        return (width - border * 2) / buttonPreLine();
    }

}
