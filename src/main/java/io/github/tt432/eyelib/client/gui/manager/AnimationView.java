package io.github.tt432.eyelib.client.gui.manager;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.animation.AnimationEffects;
import io.github.tt432.eyelib.client.animation.bedrock.BrAnimationEntry;
import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.client.model.ModelRuntimeData;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.util.math.EyeMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ScrollPanel;
import net.minecraftforge.event.TickEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author TT432
 */
//@Mod.EventBusSubscriber(Dist.CLIENT)
public class AnimationView extends Screen {
    @Nullable
    private String selectedAnimationName = null;
    @Nullable
    private String selectedBoneName = null;
    ButtonGroup animationChanelButtonGroup;
    StringsScrollPanel boneNameScrollPanel;

    protected AnimationView(Component title) {
        super(title);
    }

//    @SubscribeEvent
    public static void onEvent(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START || Minecraft.getInstance().level == null) return;

        if (GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_G) == GLFW.GLFW_PRESS
                && Minecraft.getInstance().screen == null) {
            Minecraft.getInstance().setScreen(new AnimationView(Component.empty()));
        }
    }

    public interface Refreshable {
        void refresh();
    }

    static class ButtonGroup extends AbstractContainerEventHandler implements Renderable, NarratableEntry {
        List<GuiEventListener> children = new ArrayList<>();
        @Nullable
        String selectedButtonName;
        GuiEventListener selectedButton;
        Map<String, List<GuiEventListener>> renderables;

        public ButtonGroup(
                List<String> buttonNames,
                int w,
                int h,
                int startPosX,
                int posXDelta,
                int startPosY,
                Map<String, List<GuiEventListener>> renderables
        ) {
            AtomicInteger posX = new AtomicInteger(startPosX);
            buttonNames.forEach(tabName -> children.add(
                    Button.builder(Component.literal(tabName), button -> {
                                if (selectedButtonName != null) {
                                    if (renderables.get(selectedButtonName) != null) {
                                        for (var o : renderables.get(selectedButtonName)) {
                                            children.remove(o);
                                        }
                                    }
                                    if (selectedButton instanceof AbstractWidget aw) {
                                        aw.active = true;
                                    }
                                }
                                selectedButtonName = tabName;
                                selectedButton = button;
                                button.active = false;
                                if (renderables.get(selectedButtonName) != null) {
                                    children.addAll(renderables.get(selectedButtonName));
                                    for (GuiEventListener listener : renderables.get(selectedButtonName)) {
                                        if (listener instanceof Refreshable refreshable) {
                                            refreshable.refresh();
                                        }
                                    }
                                }
                                if (children instanceof Refreshable refreshable) {
                                    refreshable.refresh();
                                }
                            })
                            .pos(posX.getAndAdd(posXDelta), startPosY)
                            .size(w, h)
                            .build()
            ));
            this.renderables = renderables;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return children;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            for (var child : children) {
                ((Renderable) child).render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public NarrationPriority narrationPriority() {
            return NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(NarrationElementOutput narrationElementOutput) {

        }
    }

    public interface RenderableEventListener extends Renderable, GuiEventListener {
    }

    static class StringsScrollPanel extends ScrollPanel implements Refreshable {
        private final Supplier<Stream<String>> strings;
        private List<Button> buttons;
        private final OnPress onPress;
        private Button lastButton;

        private static final int buttonHeight = 20;

        @Override
        public void refresh() {
            buttons = strings.get().map(s ->
                            Button.builder(Component.literal(s), button -> {
                                        if (lastButton != null) {
                                            lastButton.active = true;
                                        }

                                        lastButton = button;
                                        onPress.onPress(s, button);
                                        button.active = false;
                                    })
                                    .size(98, buttonHeight)
                                    .pos(0, 0)
                                    .build())
                    .toList();
        }

        public interface OnPress {
            void onPress(String name, Button button);
        }

        public StringsScrollPanel(Supplier<Stream<String>> strings,
                                  int width, int height, int top, int left, int border,
                                  OnPress onPress) {
            super(Minecraft.getInstance(), width, height, top, left, border);
            this.strings = strings;
            this.onPress = onPress;
            refresh();
        }

        @Override
        public void updateNarration(NarrationElementOutput narrationElementOutput) {

        }

        @Override
        public NarrationPriority narrationPriority() {
            return NarrationPriority.NONE;
        }

        @Override
        protected int getContentHeight() {
            return Eyelib.getAnimationManager().getAllData().size() * buttonHeight;
        }

        @Override
        protected void drawPanel(GuiGraphics guiGraphics, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY) {
            for (int i = 0; i < buttons.size(); i++) {
                var button = buttons.get(i);
                button.setPosition(left + border, relativeY + (i * buttonHeight));
                button.render(guiGraphics, mouseX, mouseY, Minecraft.getInstance().getPartialTick());
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return super.mouseClicked(mouseX, mouseY, button)
                    || buttons.stream().anyMatch(b -> b.mouseClicked(mouseX, mouseY, button));
        }
    }

    private Stream<String> bones() {
        if (selectedAnimationName != null) {
            if (Eyelib.getAnimationManager().get(selectedAnimationName) instanceof BrAnimationEntry bae) {
                return bae.bones().keySet().intStream().mapToObj(GlobalBoneIdHandler::get);
            }
        }
        return Stream.empty();
    }

    @Override
    protected void init() {
        super.init();

        selectedAnimationName = null;
        selectedBoneName = null;

        int buttonHeight = 20;
        addRenderableWidget(new StringsScrollPanel(() -> Eyelib.getAnimationManager().getAllData().keySet().stream(),
                100, height - 10 - 30, 30, 10, 1, (name, button) -> {
            selectedAnimationName = name;
            boneNameScrollPanel.refresh();
        }));

        int initPosY = 30;
        int interval = 22;

        int rightPanelStartY = 112;

        addRenderableWidget(new ButtonGroup(
                List.of("curve"),
                50, buttonHeight,
                rightPanelStartY, 55,
                initPosY,
                Map.of(
                        "curve", List.of(
                                boneNameScrollPanel = new StringsScrollPanel(this::bones, 100, height - (initPosY + interval) - 10,
                                        initPosY + interval, rightPanelStartY, 1, (name, button) -> selectedBoneName = name),
                                animationChanelButtonGroup = new ButtonGroup(
                                        List.of("position", "rotation", "scale"),
                                        50, buttonHeight,
                                        rightPanelStartY + 100, 55,
                                        initPosY + interval,
                                        Map.of()
                                ),
                                new ButtonGroup(
                                        List.of("x", "y", "z"),
                                        buttonHeight, buttonHeight,
                                        rightPanelStartY + 100, buttonHeight + 5,
                                        initPosY + interval * 2,
                                        Map.of(
                                                "x", List.of(
                                                        curvePanel(initPosY, interval, rightPanelStartY, Axis.X)),
                                                "y", List.of(
                                                        curvePanel(initPosY, interval, rightPanelStartY, Axis.Y)),
                                                "z", List.of(
                                                        curvePanel(initPosY, interval, rightPanelStartY, Axis.Z))
                                        )
                                )
                        )
                )
        ));
    }

    private @NotNull RenderableEventListener curvePanel(int initPosY, int interval, int rightPanelStartY, Axis axis) {
        return new RenderableEventListener() {
            @Override
            public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                if (animationChanelButtonGroup.selectedButtonName == null) return;

                AnimationChanel chanel = switch (animationChanelButtonGroup.selectedButtonName) {
                    case "position" -> AnimationChanel.POS;
                    case "rotation" -> AnimationChanel.ROT;
                    case "scale" -> AnimationChanel.SCL;
                    default ->
                            throw new IllegalStateException("Unexpected value: " + animationChanelButtonGroup.selectedButtonName);
                };

                if (selectedAnimationName != null && selectedBoneName != null && Eyelib.getAnimationManager().get(selectedAnimationName) instanceof BrAnimationEntry bae) {
                    int top = initPosY + interval * 3;
                    int down = height - 10;
                    int left = rightPanelStartY + 100 + 5;
                    int right = width - 10;

                    int fontPos = (top + down) / 2 - 4;
                    int color = 0xFF_FF_FF_FF;
                    guiGraphics.drawString(font, "0", left, fontPos, color);

                    int lineUp = (top + down) / 2 - 1;
                    int lineDown = (top + down) / 2 + 1;
                    int scale = 5;
                    int lineOffset = scale * 10;
                    int lineLeft = left + 20;
                    guiGraphics.fillGradient(lineLeft, lineUp, right, lineDown, color, color);
                    guiGraphics.drawString(font, "10", left, fontPos - lineOffset, color);
                    guiGraphics.fillGradient(lineLeft, lineUp - lineOffset, right, lineDown - lineOffset, color, color);
                    guiGraphics.drawString(font, "-10", left, fontPos + lineOffset, color);
                    guiGraphics.fillGradient(lineLeft, lineUp + lineOffset, right, lineDown + lineOffset, color, color);

                    AnimationSampler.SampleResult sample = sample(bae, Map.of(), new MolangScope(), chanel, axis);
                    sample.render(new PoseStack(), lineLeft, (top + down) / 2, 150, scale);
                }
            }

            @Override
            public void setFocused(boolean focused) {

            }

            @Override
            public boolean isFocused() {
                return false;
            }
        };
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawString(font, "动画视图", 5, 5, 0xFFFFFF);
        guiGraphics.drawString(font, "动画列表", 10, 20, 0xFFFFFF);

        guiGraphics.drawString(font, "当前动画：" + (selectedAnimationName != null ? selectedAnimationName : "空"), 110, 20, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static final class AnimationSampler {
        public record SampleResult(
                List<Vector2f> keyframePoints,
                List<Vector2f> samplePoints
        ) {
            public void render(PoseStack poseStack, int startX, int startY, int xScale, int yScale) {
                MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
                VertexConsumer buffer = bufferSource.getBuffer(RenderType.gui());

                var points = samplePoints.stream().map(v -> new Vector2f(v).mul(xScale, yScale).add(startX, startY)).toList();

                for (int i = 0; i < points.size(); i++) {
                    if (points.size() > i + 1) {
                        var curr = points.get(i);
                        var next = points.get(i + 1);

                        for (Vector2f vector2f : createRectangleFromLine(curr, next, 2)) {
                            buffer.vertex(vector2f.x, vector2f.y, 0)
                                    .color(0xFFFF0000)
                                    .endVertex();
                        }
                    }
                }

                var keyframePoints = this.keyframePoints.stream().map(v -> new Vector2f(v).mul(xScale, yScale).add(startX, startY)).toList();

                for (Vector2f keyframePoint : keyframePoints) {
                    for (Vector2f vector2f : createSquareFromCenter(keyframePoint, 5)) {
                        buffer.vertex(vector2f.x, vector2f.y, 0)
                                .color(0xFF0000FF)
                                .endVertex();
                    }
                }

                bufferSource.endBatch(RenderType.gui());
            }
        }

        /**
         * 根据中线（两个点）和宽度计算矩形的四个顶点。
         *
         * @param p1    中线的起点
         * @param p2    中线的终点
         * @param width 矩形的宽度
         * @return 包含4个顶点的数组 (顺序: 起点左, 起点右, 终点右, 终点左)
         */
        public static Vector2f[] createRectangleFromLine(Vector2f p1, Vector2f p2, float width) {
            // 1. 计算方向向量 (p2 - p1)
            Vector2f direction = new Vector2f(p2).sub(p1);

            // 避免两点重合
            if (direction.lengthSquared() == 0) {
                return new Vector2f[]{
                        new Vector2f(p1), new Vector2f(p1), new Vector2f(p1), new Vector2f(p1)
                };
            }

            direction.normalize();

            // 2. 计算垂直向量 (指向前进方向的左侧)
            // 数学坐标系中 (-y, x) 是逆时针旋转90度，即左侧
            Vector2f normal = new Vector2f(-direction.y, direction.x);

            // 3. 将法向量缩放为宽度的一半
            normal.mul(width / 2.0f);

            // 4. 计算四个角点

            // 左侧点 (Left)
            Vector2f p1Left = new Vector2f(p1).add(normal); // 起点左
            Vector2f p2Left = new Vector2f(p2).add(normal); // 终点左

            // 右侧点 (Right)
            Vector2f p2Right = new Vector2f(p2).sub(normal); // 终点右
            Vector2f p1Right = new Vector2f(p1).sub(normal); // 起点右

            // 5. 按顺时针顺序返回
            return new Vector2f[]{p1Left, p2Left, p2Right, p1Right};
        }

        public static Vector2f[] createSquareFromCenter(Vector2f center, float size) {
            float halfSize = size / 2.0f;

            // 1. 左上 (Top-Left): x减小, y增加
            Vector2f p1 = new Vector2f(center.x - halfSize, center.y + halfSize);

            // 2. 右上 (Top-Right): x增加, y增加
            Vector2f p2 = new Vector2f(center.x + halfSize, center.y + halfSize);

            // 3. 右下 (Bottom-Right): x增加, y减小
            Vector2f p3 = new Vector2f(center.x + halfSize, center.y - halfSize);

            // 4. 左下 (Bottom-Left): x减小, y减小
            Vector2f p4 = new Vector2f(center.x - halfSize, center.y - halfSize);

            return new Vector2f[]{p1, p2, p3, p4};
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

    public AnimationSampler.SampleResult sample(
            BrAnimationEntry animation,
            Map<String, String> animations,
            MolangScope scope,
            AnimationChanel animationChanel,
            Axis axis
    ) {
        if (selectedBoneName == null) return new AnimationSampler.SampleResult(new ArrayList<>(), new ArrayList<>());

        var data = animation.createData();
        List<Vector2f> points = new ArrayList<>();
        int boneId = GlobalBoneIdHandler.get(selectedBoneName);
        AnimationEffects effects = new AnimationEffects();
        for (float tick = 0; tick < animation.animationLength(); tick += 0.01F) {
            ModelRuntimeData infos = new ModelRuntimeData();
            animation.tickAnimation(data, animations, scope, tick, 1, infos, effects, () -> {
            });
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

        return new AnimationSampler.SampleResult(
                (switch (animationChanel) {
                    case POS -> animation.bones().get(boneId).position();
                    case ROT -> animation.bones().get(boneId).rotation();
                    case SCL -> animation.bones().get(boneId).scale();
                }).getData().float2ObjectEntrySet().stream()
                        .flatMap(entry ->
                                entry.getValue().dataPoints().stream()
                                        .map(mv3 -> new Vector2f(
                                                entry.getFloatKey(),
                                                switch (axis) {
                                                    case X -> mv3.eval(scope).x;
                                                    case Y -> mv3.eval(scope).y;
                                                    case Z -> mv3.eval(scope).z;
                                                })))
                        .toList(),
                points
        );
    }
}
