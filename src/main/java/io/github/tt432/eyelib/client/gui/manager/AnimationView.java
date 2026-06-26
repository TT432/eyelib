package io.github.tt432.eyelib.client.gui.manager;

import io.github.tt432.eyelib.animation.AnimationEffects;
import io.github.tt432.eyelib.animation.AnimationLookup;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.ui.UIGraphics;
import io.github.tt432.eyelib.ui.UIScreen;
import io.github.tt432.eyelib.ui.UIScreenContext;
import io.github.tt432.eyelib.ui.UIScrollPanel;
import io.github.tt432.eyelib.ui.UIWidget;
import io.github.tt432.eyelib.util.math.EyeMath;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 动画曲线调试视图。
 *
 * @author TT432
 */
public final class AnimationView implements UIScreen {
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int PANEL_BACKGROUND = 0x66000000;
    private static final int SELECTED_BACKGROUND = 0xAA555555;
    private static final int HOVER_BACKGROUND = 0x66333333;
    private static final int TAB_BACKGROUND = 0xAA202020;
    private static final int TAB_SELECTED_BACKGROUND = 0xAA666666;

    @Nullable
    private String selectedAnimationName;
    @Nullable
    private String selectedBoneName;
    @Nullable
    private StringsScrollPanel animationListPanel;
    @Nullable
    private StringsScrollPanel boneListPanel;
    @Nullable
    private ButtonGroup tabGroup;
    @Nullable
    private ButtonGroup channelGroup;
    @Nullable
    private ButtonGroup axisGroup;
    @Nullable
    private CurvePanel curveWidget;
    @Nullable
    private UIScreenContext ctx;

    @Override
    public void onInit(UIScreenContext ctx) {
        this.ctx = ctx;
        selectedAnimationName = null;
        selectedBoneName = null;

        int buttonHeight = 20;
        int initPosY = 30;
        int interval = 22;
        int rightPanelStartX = 112;
        int curveTop = initPosY + interval * 3;
        int curveLeft = rightPanelStartX + 105;

        animationListPanel = ctx.addWidget(new StringsScrollPanel(
                100,
                ctx.height() - 40,
                30,
                10,
                1,
                name -> {
                    selectedAnimationName = name;
                    selectedBoneName = null;
                    if (boneListPanel != null) {
                        boneListPanel.refresh();
                    }
                },
                () -> AnimationLookup.names().stream()
        ));
        boneListPanel = ctx.addWidget(new StringsScrollPanel(
                100,
                ctx.height() - initPosY - interval - 10,
                initPosY + interval,
                rightPanelStartX,
                1,
                name -> selectedBoneName = name,
                this::bones
        ));
        tabGroup = ctx.addWidget(new ButtonGroup(
                List.of("curve"),
                50,
                buttonHeight,
                rightPanelStartX,
                55,
                initPosY
        ));
        channelGroup = ctx.addWidget(new ButtonGroup(
                List.of("position", "rotation", "scale"),
                50,
                buttonHeight,
                rightPanelStartX + 100,
                55,
                initPosY + interval
        ));
        axisGroup = ctx.addWidget(new ButtonGroup(
                List.of("x", "y", "z"),
                buttonHeight,
                buttonHeight,
                rightPanelStartX + 100,
                buttonHeight + 5,
                initPosY + interval * 2
        ));
        curveWidget = ctx.addWidget(new CurvePanel(
                curveLeft,
                curveTop,
                Math.max(0, ctx.width() - curveLeft - 10),
                Math.max(0, ctx.height() - curveTop - 10)
        ));

        animationListPanel.refresh();
        boneListPanel.refresh();
    }

    @Override
    public void onRender(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.drawText("动画视图", 5, 5, TEXT_COLOR);
        gfx.drawText("动画列表", 10, 20, TEXT_COLOR);
        gfx.drawText("当前动画：" + (selectedAnimationName != null ? selectedAnimationName : "空"), 110, 20, TEXT_COLOR);
    }

    @Override
    public boolean onMouseClick(double mouseX, double mouseY, int button) {
        return click(animationListPanel, mouseX, mouseY, button)
                || click(tabGroup, mouseX, mouseY, button)
                || click(boneListPanel, mouseX, mouseY, button)
                || click(channelGroup, mouseX, mouseY, button)
                || click(axisGroup, mouseX, mouseY, button)
                || click(curveWidget, mouseX, mouseY, button);
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double delta) {
        return scroll(animationListPanel, mouseX, mouseY, delta) || scroll(boneListPanel, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static boolean click(@Nullable UIWidget widget, double mouseX, double mouseY, int button) {
        return widget != null && widget.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean scroll(@Nullable UIWidget widget, double mouseX, double mouseY, double delta) {
        return widget != null && widget.mouseScrolled(mouseX, mouseY, delta);
    }

    private Stream<String> bones() {
        if (selectedAnimationName != null && AnimationLookup.get(selectedAnimationName) instanceof BrAnimationEntry animation) {
            return animation.bones().keySet().intStream().mapToObj(GlobalBoneIdHandler::get);
        }
        return Stream.empty();
    }

    private static boolean hover(int x, int y, int width, int height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    final class StringsScrollPanel extends UIScrollPanel {
        private static final int ENTRY_HEIGHT = 20;

        private final Consumer<String> onSelect;
        private final Supplier<Stream<String>> source;
        private List<String> entries = List.of();
        private int selectedIndex = -1;

        StringsScrollPanel(int width, int height, int top, int left, int border,
                           Consumer<String> onSelect, Supplier<Stream<String>> source) {
            super(left, top, width, height);
            this.border = border;
            this.onSelect = onSelect;
            this.source = source;
        }

        void refresh() {
            entries = source.get().toList();
            selectedIndex = -1;
            setScrollDistance(scrollDistance);
        }

        @Override
        protected void renderContent(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
            gfx.fill(x, y + (int) scrollDistance, x + width, y + height + (int) scrollDistance, PANEL_BACKGROUND);
            int first = Math.max(0, (int) (scrollDistance / ENTRY_HEIGHT));
            int last = Math.min(entries.size(), first + height / ENTRY_HEIGHT + 2);
            for (int i = first; i < last; i++) {
                int entryY = y + border + i * ENTRY_HEIGHT;
                int entryX = x + border;
                boolean hovered = hover(entryX, entryY, width - border * 2, ENTRY_HEIGHT, mouseX, mouseY);
                if (i == selectedIndex || hovered) {
                    gfx.fill(entryX, entryY, x + width - border, entryY + ENTRY_HEIGHT, i == selectedIndex ? SELECTED_BACKGROUND : HOVER_BACKGROUND);
                }
                gfx.drawText(entries.get(i), entryX + 2, entryY + (ENTRY_HEIGHT - gfx.fontHeight()) / 2, TEXT_COLOR);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0 || !contains(mouseX, mouseY)) {
                return false;
            }

            int index = (int) ((mouseY - y + scrollDistance) / ENTRY_HEIGHT);
            if (index >= 0 && index < entries.size()) {
                selectedIndex = index;
                onSelect.accept(entries.get(index));
                return true;
            }

            return false;
        }

        @Override
        public int getContentHeight() {
            return entries.size() * ENTRY_HEIGHT + border * 2;
        }
    }

    final class ButtonGroup implements UIWidget {
        private final List<String> tabs;
        private final int buttonWidth;
        private final int buttonHeight;
        private final int startX;
        private final int deltaX;
        private final int startY;
        private int selectedTab;

        ButtonGroup(List<String> tabs, int buttonWidth, int buttonHeight, int startX, int deltaX, int startY) {
            this.tabs = List.copyOf(tabs);
            this.buttonWidth = buttonWidth;
            this.buttonHeight = buttonHeight;
            this.startX = startX;
            this.deltaX = deltaX;
            this.startY = startY;
        }

        @Override
        public void render(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
            for (int i = 0; i < tabs.size(); i++) {
                int x = startX + i * deltaX;
                boolean selected = i == selectedTab;
                boolean hovered = hover(x, startY, buttonWidth, buttonHeight, mouseX, mouseY);
                int color = selected ? TAB_SELECTED_BACKGROUND : hovered ? HOVER_BACKGROUND : TAB_BACKGROUND;
                gfx.fill(x, startY, x + buttonWidth, startY + buttonHeight, color);
                gfx.drawCenteredText(tabs.get(i), x + buttonWidth / 2, startY + (buttonHeight - gfx.fontHeight()) / 2, TEXT_COLOR);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != 0) {
                return false;
            }

            for (int i = 0; i < tabs.size(); i++) {
                int x = startX + i * deltaX;
                if (hover(x, startY, buttonWidth, buttonHeight, mouseX, mouseY)) {
                    selectedTab = i;
                    return true;
                }
            }

            return false;
        }

        String selectedTabName() {
            if (tabs.isEmpty()) {
                return "";
            }
            return tabs.get(selectedTab);
        }

        @Override
        public int getWidth() {
            return tabs.isEmpty() ? 0 : (tabs.size() - 1) * deltaX + buttonWidth;
        }

        @Override
        public int getHeight() {
            return buttonHeight;
        }
    }

    final class CurvePanel implements UIWidget {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        CurvePanel(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public void render(UIGraphics gfx, int mouseX, int mouseY, float partialTick) {
            if (selectedAnimationName == null || selectedBoneName == null || channelGroup == null || axisGroup == null) {
                return;
            }
            if (!(AnimationLookup.get(selectedAnimationName) instanceof BrAnimationEntry animation)) {
                return;
            }

            AnimationChanel channel = switch (channelGroup.selectedTabName()) {
                case "position" -> AnimationChanel.POS;
                case "rotation" -> AnimationChanel.ROT;
                case "scale" -> AnimationChanel.SCL;
                default -> throw new IllegalStateException("Unexpected value: " + channelGroup.selectedTabName());
            };
            Axis axis = switch (axisGroup.selectedTabName()) {
                case "x" -> Axis.X;
                case "y" -> Axis.Y;
                case "z" -> Axis.Z;
                default -> throw new IllegalStateException("Unexpected value: " + axisGroup.selectedTabName());
            };

            int centerY = y + height / 2;
            int labelX = x;
            int lineX = x + 20;
            int right = x + width;
            int fontY = centerY - gfx.fontHeight() / 2;
            int yScale = 5;
            int lineOffset = yScale * 10;

            gfx.drawText("0", labelX, fontY, TEXT_COLOR);
            gfx.fillGradient(lineX, centerY - 1, right, centerY + 1, TEXT_COLOR, TEXT_COLOR);
            gfx.drawText("10", labelX, fontY - lineOffset, TEXT_COLOR);
            gfx.fillGradient(lineX, centerY - lineOffset - 1, right, centerY - lineOffset + 1, TEXT_COLOR, TEXT_COLOR);
            gfx.drawText("-10", labelX, fontY + lineOffset, TEXT_COLOR);
            gfx.fillGradient(lineX, centerY + lineOffset - 1, right, centerY + lineOffset + 1, TEXT_COLOR, TEXT_COLOR);

            sample(animation, Map.of(), new MolangScope(), channel, axis).render(gfx, lineX, centerY, 150, yScale);
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }
    }

    public static final class AnimationSampler {
        private AnimationSampler() {}

        public record SampleResult(List<Vector2f> keyframePoints, List<Vector2f> samplePoints) {
            public void render(UIGraphics gfx, int startX, int startY, int xScale, int yScale) {
                List<Vector2f> points = samplePoints.stream()
                        .map(v -> new Vector2f(v).mul(xScale, yScale).add(startX, startY))
                        .toList();

                for (int i = 0; i + 1 < points.size(); i++) {
                    Vector2f curr = points.get(i);
                    Vector2f next = points.get(i + 1);
                    gfx.drawLine(curr.x, curr.y, next.x, next.y, 2, 0xFFFF0000);
                }

                for (Vector2f point : keyframePoints.stream()
                        .map(v -> new Vector2f(v).mul(xScale, yScale).add(startX, startY))
                        .toList()) {
                    gfx.fill(Math.round(point.x - 2), Math.round(point.y - 2), Math.round(point.x + 3), Math.round(point.y + 3), 0xFF0000FF);
                }
            }
        }

        public static Vector2f[] createRectangleFromLine(Vector2f p1, Vector2f p2, float width) {
            Vector2f direction = new Vector2f(p2).sub(p1);
            if (direction.lengthSquared() == 0) {
                return new Vector2f[]{new Vector2f(p1), new Vector2f(p1), new Vector2f(p1), new Vector2f(p1)};
            }

            direction.normalize();
            Vector2f normal = new Vector2f(-direction.y, direction.x).mul(width / 2F);
            return new Vector2f[]{
                    new Vector2f(p1).add(normal),
                    new Vector2f(p2).add(normal),
                    new Vector2f(p2).sub(normal),
                    new Vector2f(p1).sub(normal)
            };
        }

        public static Vector2f[] createSquareFromCenter(Vector2f center, float size) {
            float halfSize = size / 2F;
            return new Vector2f[]{
                    new Vector2f(center.x - halfSize, center.y + halfSize),
                    new Vector2f(center.x + halfSize, center.y + halfSize),
                    new Vector2f(center.x + halfSize, center.y - halfSize),
                    new Vector2f(center.x - halfSize, center.y - halfSize)
            };
        }
    }

    public enum AnimationChanel {
        POS,
        ROT,
        SCL,
    }

    public enum Axis {
        X,
        Y,
        Z
    }

    public AnimationSampler.SampleResult sample(BrAnimationEntry animation, Map<String, String> animations,
                                                MolangScope scope, AnimationChanel animationChanel, Axis axis) {
        if (selectedBoneName == null) {
            return new AnimationSampler.SampleResult(new ArrayList<>(), new ArrayList<>());
        }

        var data = animation.createData();
        List<Vector2f> points = new ArrayList<>();
        int boneId = GlobalBoneIdHandler.get(selectedBoneName);
        AnimationEffects effects = new AnimationEffects();
        for (float tick = 0; tick < animation.animationLength(); tick += 0.01F) {
            ModelRuntimeData infos = new ModelRuntimeData();
            animation.tickAnimation(data, animations, scope, tick, 1, infos, effects, () -> {});
            var boneRenderInfoEntry = infos.getData(boneId);
            var vec3 = switch (animationChanel) {
                case POS -> boneRenderInfoEntry.position.mul(16).mul(-1, 1, 1);
                case ROT -> boneRenderInfoEntry.rotation.div(EyeMath.DEGREES_TO_RADIANS).mul(-1, -1, 1);
                case SCL -> boneRenderInfoEntry.scale;
            };
            points.add(new Vector2f(tick, switch (axis) {
                case X -> vec3.x;
                case Y -> vec3.y;
                case Z -> vec3.z;
            }));
        }

        var bone = animation.bones().get(boneId);
        if (bone == null) {
            return new AnimationSampler.SampleResult(new ArrayList<>(), points);
        }

        return new AnimationSampler.SampleResult(
                (switch (animationChanel) {
                    case POS -> bone.position();
                    case ROT -> bone.rotation();
                    case SCL -> bone.scale();
                }).getData().float2ObjectEntrySet().stream()
                        .flatMap(entry -> entry.getValue().dataPoints().stream()
                                .map(value -> new Vector2f(entry.getFloatKey(), switch (axis) {
                                    case X -> value.eval(scope).x;
                                    case Y -> value.eval(scope).y;
                                    case Z -> value.eval(scope).z;
                                })))
                        .toList(),
                points
        );
    }
}
