package io.github.tt432.eyelib.ui;

/**
 * MC 无关的滚动面板基类。
 * 自实现滚动逻辑（不依赖 Forge ScrollPanel），解决 NeoForge 1.21.1 中 ScrollPanel 已删除的问题。
 *
 * @author TT432
 */
public abstract class UIScrollPanel implements UIWidget {
    protected double scrollDistance;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected int border;

    protected UIScrollPanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public final void render(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.enableScissor(x, y, width, height);
        var pose = gfx.pose();
        pose.pushPose();
        pose.translate(0, (float) -scrollDistance, 0);
        renderContent(gfx, mouseX, (int) Math.round(mouseY + scrollDistance), partialTick);
        pose.popPose();
        gfx.disableScissor();
        renderOverlay(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }

        setScrollDistance(scrollDistance - delta * 20);
        return true;
    }

    protected boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void setScrollDistance(double distance) {
        scrollDistance = Math.max(0, Math.min(distance, Math.max(0, getContentHeight() - height)));
    }

    public double getScrollDistance() {
        return scrollDistance;
    }

    @Override
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    protected abstract void renderContent(UIGraphics gfx, int mouseX, int mouseY, float partialTick);

    protected void renderOverlay(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {}

    public abstract int getContentHeight();
}
