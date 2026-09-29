package io.github.tt432.eyelib.client.gui.manager;

import io.github.tt432.eyelib.bridge.particle.WinterskyParticlePort;
import io.github.tt432.eyelib.bridge.ui.UiPort;
import io.github.tt432.eyelib.ui.UIGraphics;
import io.github.tt432.eyelib.ui.UIScreen;
import io.github.tt432.eyelib.ui.UIScreenContext;
import io.github.tt432.eyelib.ui.UIWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * wintersky 粒子预览屏幕：列出资源包内全部 Bedrock 粒子（identifier），
 * 点击在玩家眼前 3m 处 spawn（wintersky 运行时，与 Blockbench/Snowstorm 同引擎）。
 *
 * @author TT432
 */
public final class WinterskyParticleScreen implements UIScreen {
    @Nullable
    private UIScreenContext ctx;

    @Nullable
    private ParticleListPanel panel;

    private List<String> allIdentifiers = List.of();

    @Override
    public void onInit(UIScreenContext ctx) {
        this.ctx = ctx;
        allIdentifiers = WinterskyParticlePort.identifiers();

        int border = Math.round(ctx.height() * 0.1F);
        int inputHeight = Math.round(ctx.fontHeight() / 0.614F);
        int listWidth = Math.min(240, ctx.width() - border * 2);

        var input = ctx.addTextField(border, border, listWidth, inputHeight);
        input.setMaxLength(256);
        input.setBordered(true);
        input.setResponder(this::onEdited);
        input.setCanLoseFocus(true);

        int padding = Math.round(inputHeight * .1F);
        panel = ctx.addWidget(new ParticleListPanel(
                border,
                border + inputHeight + padding,
                listWidth,
                Math.round(ctx.height() * 0.8F) - (inputHeight + padding)));
        panel.setEntries(allIdentifiers);

        int buttonY = Math.round(ctx.height() * 0.95F) - 20;
        ctx.addButton("清除全部粒子", border, buttonY, 80, 20, WinterskyParticlePort::clear);
        ctx.addButton("返回", border + 84, buttonY, 50, 20,
                () -> Minecraft.getInstance().setScreen(UiPort.wrap(EyelibManagerScreen.create())));
    }

    @Override
    public void onRender(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
        UIScreenContext currentCtx = ctx;
        if (currentCtx == null) {
            return;
        }
        int border = Math.round(currentCtx.height() * 0.1F);
        gfx.drawText("粒子预览（wintersky）· 点击在面前生成 · 共 " + allIdentifiers.size() + " 个",
                border, border - gfx.fontHeight() - 2, 0xFFFFFFFF);
        if (allIdentifiers.isEmpty()) {
            gfx.drawText("未发现粒子文件（assets/*/particles/*.json）", border, border + 40, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        return panel != null && panel.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double delta) {
        return panel != null && panel.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void onEdited(String input) {
        if (panel == null) {
            return;
        }
        if (input.isEmpty()) {
            panel.setEntries(allIdentifiers);
            return;
        }
        String lower = input.toLowerCase();
        List<String> filtered = new ArrayList<>();
        for (String id : allIdentifiers) {
            if (id.toLowerCase().contains(lower)) {
                filtered.add(id);
            }
        }
        panel.setEntries(filtered);
    }

    /** 在玩家眼前 3m spawn 并关闭屏幕（便于观察世界内效果）。 */
    private void spawnAndClose(String identifier) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            Vec3 eye = minecraft.player.getEyePosition();
            Vec3 look = minecraft.player.getViewVector(1.0F);
            WinterskyParticlePort.spawn(identifier,
                    eye.x + look.x * 3, eye.y + look.y * 3, eye.z + look.z * 3);
        }
        minecraft.setScreen(null);
    }

    /** 简单滚动列表：行高 = 字高 + 6，hover 高亮，点击 spawn。 */
    private final class ParticleListPanel implements UIWidget {
        private final int x;
        private final int y;
        private final int w;
        private final int h;
        private List<String> entries = List.of();
        private int scrollOffset = 0;

        ParticleListPanel(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        void setEntries(List<String> entries) {
            this.entries = entries;
            this.scrollOffset = 0;
        }

        private int rowHeight(UIGraphics gfx) {
            return gfx.fontHeight() + 6;
        }

        @Override
        public void render(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
            int rowHeight = rowHeight(gfx);
            gfx.fill(x, y, x + w, y + h, 0x66000000);
            gfx.enableScissor(x, y, w, h);
            for (int i = 0; i * rowHeight < h; i++) {
                int index = i + scrollOffset / rowHeight;
                if (index >= entries.size()) {
                    break;
                }
                int rowY = y + i * rowHeight - scrollOffset % rowHeight;
                boolean hover = mouseX > x && mouseX < x + w && mouseY > rowY && mouseY < rowY + rowHeight
                        && mouseY >= y && mouseY < y + h;
                if (hover) {
                    gfx.fill(x, rowY, x + w, rowY + rowHeight, 0xAA555555);
                }
                gfx.drawText(entries.get(index), x + 4, rowY + 3, 0xFFFFFFFF);
            }
            gfx.disableScissor();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!(mouseX > x && mouseX < x + w && mouseY > y && mouseY < y + h)) {
                return false;
            }
            UIScreenContext currentCtx = ctx;
            if (currentCtx == null) {
                return false;
            }
            int rowHeight = currentCtx.fontHeight() + 6;
            int index = (int) ((mouseY - y + scrollOffset) / rowHeight);
            if (index >= 0 && index < entries.size()) {
                spawnAndClose(entries.get(index));
            }
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            UIScreenContext currentCtx = ctx;
            if (currentCtx == null) {
                return false;
            }
            int rowHeight = currentCtx.fontHeight() + 6;
            int maxScroll = Math.max(0, entries.size() * rowHeight - h);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (delta * rowHeight)));
            return true;
        }

        @Override
        public int getWidth() {
            return w;
        }

        @Override
        public int getHeight() {
            return h;
        }
    }
}
