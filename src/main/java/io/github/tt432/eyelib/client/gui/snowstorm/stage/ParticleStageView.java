package io.github.tt432.eyelib.client.gui.snowstorm.stage;
//? if >=1.20.1 {
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextElement;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import io.github.tt432.eyelib.client.gui.snowstorm.SnowstormTheme;
import io.github.tt432.eyelib.snowstorm.editor.EditorOptions;
import io.github.tt432.eyelib.snowstorm.editor.EditorRuntime;
import io.github.tt432.eyelib.snowstorm.editor.SnowstormValidator;
import io.github.tt432.eyelib.wintersky.Emitter;
import io.github.tt432.eyelib.wintersky.Scene;
import io.github.tt432.eyelib.wintersky.molang.JsSemantics;
import io.github.tt432.eyelib.wintersky.three.Object3D;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? if <26.1 {
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
//? if <1.20.6 {
import com.mojang.blaze3d.vertex.PoseStack;
//?} else {
import org.joml.Matrix4fStack;
//?}
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import io.github.tt432.eyelib.bridge.particle.WinterskyParticlePort;
import io.github.tt432.eyelib.util.PortResourceLocation;
import io.github.tt432.eyelib.wintersky.Config;
import io.github.tt432.eyelib.wintersky.Particle;
import io.github.tt432.eyelib.wintersky.three.BufferAttribute;
import io.github.tt432.eyelib.wintersky.three.Matrix4;
import io.github.tt432.eyelib.wintersky.three.ShaderMaterial;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.InputStream;
//?} else {
import com.lowdragmc.lowdraglib2.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.IGUIContext;
//?}
import java.util.ArrayList;

/**
 * Snowstorm 3D 预览舞台（ADR-0036 D4/U2；Preview.vue as-is）：
 * OrbitCamera 轨道相机（左拖旋转/滚轮缩放/右拖平移）、GridHelper 64×64、
 * CustomAxesHelper(1)、minecraft_block 参考方块、wintersky 粒子渲染、
 * 32ms tick 门、FPS/粒子数/警告计数、时间戳浮层、页脚 {@link StageFooterBar}。
 *
 * <p>渲染路径（R2 实证约束）：</p>
 * <ul>
 *   <li>GUI/LDLib2 上下文 bufferSource 批渲染零像素——唯一实证路径是
 *       Tesselator 立即模式 + {@code BufferUploader.drawWithShader}
 *       （{@code GuiGraphics.innerBlit} 同款，同 NodeAssetPreview.renderModel 先例）；</li>
 *   <li>禁止 enableScissor（画布坐标系下剪错区域）——内容溢出舞台矩形容忍；</li>
 *   <li>透视投影自管：RenderSystem 投影矩阵压栈/恢复，透视矩阵折叠
 *       「NDC → 舞台矩形」映射（等效把相机视口对准舞台区域）；</li>
 *   <li>26.1 GUI 渲染路径未迁移：舞台降级为占位文字（事件/纹理加载同守卫）。</li>
 * </ul>
 *
 * <p>相机替身（Scene.updateFacingRotation）：OrbitCamera 已是 three 约定，
 * position/quaternion 直接喂入（wu7 的 q_proxy=q_mc*rotY(π) 是 MC 相机转换，此处无需）。</p>
 */
public final class ParticleStageView extends UIElement {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticleStageView.class);

    /** Preview.vue animate()：timestamp - last_frame_time > 32 才 tick。 */
    private static final int TICK_GATE_MS = 32;
    /** Preview.vue mounted()：fps 1s / particles 200ms / warnings 500ms 采样周期。 */
    private static final int FPS_PERIOD_MS = 1000;
    private static final int PARTICLES_PERIOD_MS = 200;
    private static final int WARNINGS_PERIOD_MS = 500;

    private final OrbitCamera camera = new OrbitCamera();
    /** Scene.updateFacingRotation 的相机替身（three 约定直接喂）。 */
    private final Object3D cameraProxy = new Object3D();
    /** emitter.js initParticles(View) 的 View.scene 等价物（wintersky 场景图根）。 */
    private final Object3D viewSceneRoot = new Object3D();
    private final TextElement timestampOverlay;
    private final StageFooterBar footer;

    // Preview.vue stats / View.frames_this_second
    private int framesThisSecond;
    private int fps;
    private int particleCount;
    private int warningCount;
    private long lastFrameTime = System.currentTimeMillis();
    private long lastFpsTime = System.currentTimeMillis();
    private long lastParticleTime;
    private long lastWarningTime;

    public ParticleStageView() {
        layout(layout -> layout.flexDirection(FlexDirection.COLUMN));

        StageCanvas canvas = new StageCanvas();
        canvas.layout(layout -> layout.widthPercent(100).flex(1));

        // #overlay_timestamp（canvas 左上角）
        timestampOverlay = new TextElement();
        timestampOverlay.setText(Component.literal("0:0"));
        timestampOverlay.textStyle(style -> style.fontSize(9).textColor(SnowstormTheme.TEXT_GRAYED));
        timestampOverlay.layout(layout -> layout
                .positionType(TaffyPosition.ABSOLUTE)
                .left(4).top(2));
        canvas.addChild(timestampOverlay);

        footer = new StageFooterBar(this);
        footer.layout(layout -> layout.widthPercent(100));

        addChildren(canvas, footer);

        // emitter.js initParticles(View)：View.scene.add(Scene.space)
        EditorRuntime.initParticles(viewSceneRoot);
    }

    public int fps() {
        return fps;
    }

    public int particleCount() {
        return particleCount;
    }

    public int warningCount() {
        return warningCount;
    }

    public OrbitCamera camera() {
        return camera;
    }

    /** Preview.vue keypress 空格：Ctrl+空格=暂停切换，空格=重新开始。返回 true 表示已消费。 */
    public boolean onSpaceKey(boolean ctrl) {
        if (ctrl) {
            PlaybackController.togglePause();
        } else {
            PlaybackController.startAnimation();
        }
        return true;
    }

    /**
     * Screen 关闭清理（不变量 I4：发射器停止，无泄漏到世界渲染）。
     * Main 集成接线：SnowstormEditorScreen.onClose → stageView.dispose()。
     */
    public void dispose() {
        PlaybackController.stop();
        for (Emitter emitter : new ArrayList<>(EditorRuntime.Scene.emitters)) {
            emitter.stop(true);
        }
    }

    /** Preview.vue computed timestamp：{@code ${floor(time)}:${floor((time%1)*10)}}，time=Emitter.age。 */
    private static String timestamp() {
        double time = EditorRuntime.Emitter.age;
        int fractions = (int) Math.floor((time % 1) * 10);
        return (int) Math.floor(time) + ":" + fractions;
    }

    //? if <26.1 {
    // ==================================================================
    // 渲染（<26.1 实证路径）
    // ==================================================================

    /** gizmo_colors（Preview.vue as-is）。 */
    private static final int COLOR_AXIS_R = 0xFD3043;
    private static final int COLOR_AXIS_G = 0x26EC45;
    private static final int COLOR_AXIS_B = 0x2D5EE8;
    private static final int COLOR_GRID = 0x3D4954;

    /** minecraft_block.png（classpath /snowstorm/assets/，非 assets/ 命名空间 → 手动注册 DynamicTexture 接缝）。 */
    private static final ResourceLocation BLOCK_TEXTURE_ID =
            ResourceLocation.tryParse("eyelib:snowstorm_stage/minecraft_block");
    private static boolean blockTextureReady;

    private static final Object DRAG_ROTATE = new Object();
    private static final Object DRAG_PAN = new Object();

    private final Matrix4 viewMatrixTmp = new Matrix4();

    /** Preview.vue animate() 渲染帧主体。 */
    private void drawStage(GUIContext guiContext, float x, float y, float w, float h) {
        // 先把已排队的 GUI 纹理 flush 落盘（NodeAssetPreview 先例：否则 3D 顶点被后 flush 的内容覆盖）
        guiContext.graphics.flush();
        if (w < 2 || h < 2) {
            return;
        }

        long now = System.currentTimeMillis();
        // 32ms tick 门（as-is）
        if (now - lastFrameTime > TICK_GATE_MS) {
            lastFrameTime = now;
            if (!EditorRuntime.Emitter.paused) {
                EditorRuntime.Emitter.tick();
            }
        }

        // controls.update() + Scene.updateFacingRotation(camera)
        cameraProxy.position.set(camera.position.x, camera.position.y, camera.position.z);
        cameraProxy.quaternion.set(camera.quaternion.getX(), camera.quaternion.getY(),
                camera.quaternion.getZ(), camera.quaternion.getW());
        Scene scene = EditorRuntime.Scene;
        scene.updateFacingRotation(cameraProxy);
        scene.space.updateMatrixWorld(true);

        // 统计采样（as-is 周期）
        framesThisSecond++;
        if (now - lastFpsTime >= FPS_PERIOD_MS) {
            fps = framesThisSecond;
            framesThisSecond = 0;
            lastFpsTime = now;
        }
        if (now - lastParticleTime >= PARTICLES_PERIOD_MS) {
            particleCount = EditorRuntime.Emitter.particles.size();
            lastParticleTime = now;
        }
        // JS 另有 hasFocus/canvas.offsetParent 守卫——Screen 打开即聚焦且舞台可见，归一为恒真（偏离已记录）
        if (now - lastWarningTime >= WARNINGS_PERIOD_MS) {
            warningCount = SnowstormValidator.validate().size();
            lastWarningTime = now;
        }
        footer.refreshStats();
        timestampOverlay.setText(Component.literal(timestamp()));

        render3D(scene, x, y, w, h);
    }

    /** 透视投影自管压栈/恢复 + 立即模式绘制（renderModel 先例扩展）。 */
    private void render3D(Scene scene, float x, float y, float w, float h) {
        Minecraft mc = Minecraft.getInstance();
        float guiW = (float) mc.getWindow().getGuiScaledWidth();
        float guiH = (float) mc.getWindow().getGuiScaledHeight();
        if (guiW < 1 || guiH < 1) {
            return;
        }

        // resizeCanvas()：camera.aspect = width/height
        Matrix4f perspective = new Matrix4f().perspective(
                (float) Math.toRadians(OrbitCamera.FOV), w / h,
                (float) OrbitCamera.NEAR, (float) OrbitCamera.FAR);
        // NDC → 舞台矩形映射（GUI 像素空间 y 向下 → sy 取负翻转）
        float sx = w / guiW;
        float sy = -h / guiH;
        float tx = 2f * (x + w / 2f) / guiW - 1f;
        float ty = 1f - 2f * (y + h / 2f) / guiH;
        Matrix4f projection = new Matrix4f().translate(tx, ty, 0).scale(sx, sy, 1).mul(perspective);

        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        //? if <1.20.6 {
        PoseStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushPose();
        modelViewStack.setIdentity();
        modelViewStack.last().pose().set(toJoml(camera.viewMatrix(viewMatrixTmp).elements, new Matrix4f()));
        //?} else {
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.identity();
        modelViewStack.mul(toJoml(camera.viewMatrix(viewMatrixTmp).elements, new Matrix4f()));
        //?}
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(projection, VertexSorting.ORTHOGRAPHIC_Z);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        try {
            if (JsSemantics.truthy(EditorOptions.OptionValues.get("axis_helper_visible"))) {
                drawAxes();
            }
            if (JsSemantics.truthy(EditorOptions.OptionValues.get("grid_visible"))) {
                drawGrid();
            }
            if (JsSemantics.truthy(EditorOptions.OptionValues.get("minecraft_block_visible"))) {
                drawBlock();
            }
            drawParticles(scene);
        } catch (Exception e) {
            // 舞台渲染失败不崩编辑器（NodeAssetPreview 同款处理）
            LOGGER.warn("[snowstorm] stage render failed", e);
        } finally {
            RenderSystem.setProjectionMatrix(oldProjection, VertexSorting.ORTHOGRAPHIC_Z);
            //? if <1.20.6 {
            modelViewStack.popPose();
            //?} else {
            modelViewStack.popMatrix();
            //?}
            RenderSystem.applyModelViewMatrix();
            RenderSystem.disableDepthTest();
        }
    }

    /** GridHelper(64, 64, grid, grid)，position.y -= 0.0005（as-is）。 */
    private static void drawGrid() {
        float y = -0.0005f;
        int r = (COLOR_GRID >> 16) & 0xFF;
        int g = (COLOR_GRID >> 8) & 0xFF;
        int b = COLOR_GRID & 0xFF;
        BufferBuilder builder = beginLines(DefaultVertexFormat.POSITION_COLOR);
        for (int i = -32; i <= 32; i++) {
            colorVertex(builder, i, y, -32, r, g, b, 255);
            colorVertex(builder, i, y, 32, r, g, b, 255);
            colorVertex(builder, -32, y, i, r, g, b, 255);
            colorVertex(builder, 32, y, i, r, g, b, 255);
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();
        drawNow(builder);
        RenderSystem.enableCull();
    }

    /** CustomAxesHelper(1)：原点 → (1,0,0)/(0,1,0)/(0,0,1)，vertexColors（as-is）。 */
    private static void drawAxes() {
        BufferBuilder builder = beginLines(DefaultVertexFormat.POSITION_COLOR);
        axisLine(builder, 1, 0, 0, COLOR_AXIS_R);
        axisLine(builder, 0, 1, 0, COLOR_AXIS_G);
        axisLine(builder, 0, 0, 1, COLOR_AXIS_B);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull();
        drawNow(builder);
        RenderSystem.enableCull();
    }

    private static void axisLine(BufferBuilder builder, float x, float y, float z, int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        colorVertex(builder, 0, 0, 0, r, g, b, 255);
        colorVertex(builder, x, y, z, r, g, b, 255);
    }

    // ==================================================================
    // 立即模式缓冲管道（1.20.1 getBuilder/begin/end ↔ 1.21+ Tesselator.begin/buildOrThrow，
    // TwoSideModelBakeInfo.drawGuiPreview 同款版本分界）
    // ==================================================================

    private static BufferBuilder beginQuads(VertexFormat format) {
        //? if <1.20.6 {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, format);
        return builder;
        //?} else {
        return Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, format);
        //?}
    }

    private static BufferBuilder beginLines(VertexFormat format) {
        //? if <1.20.6 {
        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.LINES, format);
        return builder;
        //?} else {
        return Tesselator.getInstance().begin(VertexFormat.Mode.LINES, format);
        //?}
    }

    /** 以当前 shader 立即绘制（1.20.1 Tesselator.end 内部即 drawWithShader）。 */
    private static void drawNow(BufferBuilder builder) {
        //? if <1.20.6 {
        Tesselator.getInstance().end();
        //?} else {
        BufferUploader.drawWithShader(builder.buildOrThrow());
        //?}
    }

    /** RenderType 复合状态 + 立即绘制（setupRenderState → drawWithShader → clearRenderState）。 */
    private static void drawWithRenderType(BufferBuilder builder, RenderType renderType) {
        //? if <1.20.6 {
        renderType.setupRenderState();
        BufferUploader.drawWithShader(builder.end());
        renderType.clearRenderState();
        //?} else {
        // 1.21 RenderType.draw(MeshData) 内部同款 setup/draw/clear
        renderType.draw(builder.buildOrThrow());
        //?}
    }

    /** 纯色顶点（POSITION_COLOR；1.20.1 float 色 ↔ 1.21+ int 色，BedrockParticleRenderer 同款分界）。 */
    private static void colorVertex(BufferBuilder builder, float x, float y, float z,
                                    int r, int g, int b, int a) {
        //? if <1.20.6 {
        builder.vertex(x, y, z).color(r / 255f, g / 255f, b / 255f, a / 255f).endVertex();
        //?} else {
        builder.addVertex(x, y, z).setColor(r, g, b, a);
        //?}
    }

    /**
     * minecraft_block 参考方块（Preview.vue BoxGeometry 逐面明暗 as-is：
     * East/West 0.64、Up 1、Down 0.5、North/South 0.8；UV 0.002 内缩防渗色；
     * position.set(0,-0.51,0)；纹理 NearestFilter）。
     */
    private static void drawBlock() {
        ensureBlockTexture();
        if (BLOCK_TEXTURE_ID == null) {
            return;
        }
        BufferBuilder builder = beginQuads(cubeFormat());
        float s = 0.5f;
        float cy = -0.51f;
        // East / West（shade 0.64，uv [0, 0.5, 0.5, 0.0]）
        quad(builder, s, s + cy, -s, s, s + cy, s, s, -s + cy, -s, s, -s + cy, s, 0.64f, 0, 0.5, 0.5, 0.0);
        quad(builder, -s, s + cy, s, -s, s + cy, -s, -s, -s + cy, s, -s, -s + cy, -s, 0.64f, 0, 0.5, 0.5, 0.0);
        // Up（shade 1，uv [0, 0.5, 0.5, 1]）
        quad(builder, -s, s + cy, s, s, s + cy, s, -s, s + cy, -s, s, s + cy, -s, 1f, 0, 0.5, 0.5, 1);
        // Down（shade 0.5，uv [0.5, 0.5, 1, 0.0]）
        quad(builder, -s, -s + cy, -s, s, -s + cy, -s, -s, -s + cy, s, s, -s + cy, s, 0.5f, 0.5, 0.5, 1, 0.0);
        // North / South（shade 0.8，uv [0, 0.5, 0.5, 0.0]）
        quad(builder, s, s + cy, s, -s, s + cy, s, s, -s + cy, s, -s, -s + cy, s, 0.8f, 0, 0.5, 0.5, 0.0);
        quad(builder, -s, s + cy, -s, s, s + cy, -s, -s, -s + cy, -s, s, -s + cy, -s, 0.8f, 0, 0.5, 0.5, 0.0);
        //? if <1.20.6 {
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        //?} else {
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        //?}
        RenderSystem.setShaderTexture(0, BLOCK_TEXTURE_ID);
        RenderSystem.disableCull();
        drawNow(builder);
        RenderSystem.enableCull();
    }

    /** 单面 quad：顶点 TL,TR,BL,BR + 逐面 shade；UV 0.002 内缩 + three flipY（v'=1-v）。 */
    private static void quad(BufferBuilder builder,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             float shade, double u0, double v0, double u1, double v1) {
        // JS setupFace：uv[i] = Math.lerp(uv[i], uv[(i+2)%4], 0.002)
        double iu0 = u0 + (u1 - u0) * 0.002;
        double iv0 = v0 + (v1 - v0) * 0.002;
        double iu1 = u1 + (u0 - u1) * 0.002;
        double iv1 = v1 + (v0 - v1) * 0.002;
        float fu0 = (float) iu0, fu1 = (float) iu1;
        float fv0 = 1f - (float) iv0, fv1 = 1f - (float) iv1;
        colorUvVertex(builder, x1, y1, z1, shade, fu0, fv0);
        colorUvVertex(builder, x2, y2, z2, shade, fu1, fv0);
        colorUvVertex(builder, x3, y3, z3, shade, fu0, fv1);
        colorUvVertex(builder, x4, y4, z4, shade, fu1, fv1);
    }

    /** 方块顶点格式：1.20.1 POSITION_COLOR_TEX ↔ 1.21+ POSITION_TEX_COLOR（元素序换名）。 */
    private static VertexFormat cubeFormat() {
        //? if <1.20.6 {
        return DefaultVertexFormat.POSITION_COLOR_TEX;
        //?} else {
        return DefaultVertexFormat.POSITION_TEX_COLOR;
        //?}
    }

    /** 带色纹理顶点（shade 逐面明暗；1.20.1 float 色 ↔ 1.21+ int 色）。 */
    private static void colorUvVertex(BufferBuilder builder, float x, float y, float z,
                                      float shade, float u, float v) {
        //? if <1.20.6 {
        builder.vertex(x, y, z).color(shade, shade, shade, 1f).uv(u, v).endVertex();
        //?} else {
        int shadeInt = Math.min(255, Math.max(0, Math.round(shade * 255f)));
        builder.addVertex(x, y, z).setUv(u, v).setColor(shadeInt, shadeInt, shadeInt, 255);
        //?}
    }

    /**
     * 粒子渲染：顶点生成复用 wintersky Particle 的 quad/clr/uv（WinterskyParticleRenderer 同款
     * 取值），提交走立即模式 drawWithShader + WinterskyRenderTypes 4 材质
     * （setupRenderState → drawWithShader → clearRenderState）。
     */
    private static void drawParticles(Scene scene) {
        for (Emitter emitter : new ArrayList<>(scene.emitters)) {
            if (!emitter.initialized || emitter.particles.isEmpty()) {
                continue;
            }
            ShaderMaterial material = emitter.material;
            if (material == null) {
                continue;
            }
            PortResourceLocation texture = resolveTexture(emitter);
            if (texture == null) {
                continue;
            }
            RenderType renderType = (RenderType) WinterskyParticlePort.renderType(
                    emitter.config.particle_appearance_material, texture);
            BufferBuilder builder = beginQuads(DefaultVertexFormat.NEW_ENTITY);
            for (Particle particle : new ArrayList<>(emitter.particles)) {
                emitParticle(builder, particle);
            }
            // quad 正反面同位：关 cull 双面绘制安全（blend/add 在 JS 本就 DoubleSide；
            // opaque/alpha 同位置同深度重复写结果一致），规避投影 y 翻转的绕序问题
            RenderSystem.disableCull();
            drawWithRenderType(builder, renderType);
            RenderSystem.enableCull();
        }
    }

    /** WinterskyParticleRenderer.renderParticle 同款取值（matrixWorld / clr / uv flipY / 顶点序 1,0,2,3）。 */
    private static void emitParticle(BufferBuilder builder, Particle particle) {
        BufferAttribute position = particle.geometry.getAttribute("position");
        BufferAttribute uv = particle.geometry.getAttribute("uv");
        BufferAttribute clr = particle.geometry.getAttribute("clr");
        if (position == null || uv == null || clr == null) {
            return;
        }
        Matrix4f world = toJoml(particle.mesh.matrixWorld.elements, new Matrix4f());
        Matrix3f normalMat = new Matrix3f().set(world).invert().transpose();
        Vector3f normal = new Vector3f(0, 0, 1).mul(normalMat);
        int light = LightTexture.FULL_BRIGHT;

        // PlaneGeometry 顶点序 TL,TR,BL,BR → 周界序 (1,0,2,3)（生产同款，防 bowtie）
        final int[] order = {1, 0, 2, 3};
        for (int i = 0; i < 4; i++) {
            int vi = order[i];
            Vector3f p = new Vector3f(
                    position.array.get(vi * 3),
                    position.array.get(vi * 3 + 1),
                    position.array.get(vi * 3 + 2)).mulPosition(world);
            entityVertex(builder, p,
                    packChannel(clr.array.get(vi * 4)),
                    packChannel(clr.array.get(vi * 4 + 1)),
                    packChannel(clr.array.get(vi * 4 + 2)),
                    packChannel(clr.array.get(vi * 4 + 3)),
                    uv.array.get(vi * 2), 1.0F - uv.array.get(vi * 2 + 1),
                    light, normal);
        }
    }

    /** NEW_ENTITY 顶点（1.20.1 float 色 + uv2/overlayCoords ↔ 1.21+ int 色 + setLight/setOverlay）。 */
    private static void entityVertex(BufferBuilder builder, Vector3f p, int r, int g, int b, int a,
                                     float u, float v, int light, Vector3f normal) {
        //? if <1.20.6 {
        builder.vertex(p.x, p.y, p.z)
                .color(r / 255f, g / 255f, b / 255f, a / 255f)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal.x, normal.y, normal.z)
                .endVertex();
        //?} else {
        builder.addVertex(p.x, p.y, p.z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(normal.x, normal.y, normal.z);
        //?}
    }

    private static int packChannel(float channel) {
        if (Float.isNaN(channel)) {
            return 0;
        }
        return Math.min(255, Math.max(0, Math.round(channel * 255.0F)));
    }

    /** wintersky Matrix4（double[16] 列主序）→ JOML（WinterskyParticleRenderer 同款）。 */
    private static Matrix4f toJoml(double[] e, Matrix4f out) {
        return out.set(
                (float) e[0], (float) e[1], (float) e[2], (float) e[3],
                (float) e[4], (float) e[5], (float) e[6], (float) e[7],
                (float) e[8], (float) e[9], (float) e[10], (float) e[11],
                (float) e[12], (float) e[13], (float) e[14], (float) e[15]);
    }

    /** WinterskyParticleRenderer.resolveTexture 同款（fetchTexture 钩子结果 / MISSING_TEX 兜底）。 */
    private static PortResourceLocation resolveTexture(Emitter emitter) {
        var image = emitter.config.texture.image;
        String src = image != null ? image.getSrc() : null;
        if (src == null || src.isEmpty()) {
            src = Config.MISSING_TEX;
        }
        if (src.indexOf(':') >= 0) {
            return PortResourceLocation.parse(src);
        }
        return PortResourceLocation.of("eyelib", src);
    }

    /** minecraft_block.png：classpath 资源 → DynamicTexture（NearestFilter as-is），幂等。 */
    private static void ensureBlockTexture() {
        if (blockTextureReady) {
            return;
        }
        blockTextureReady = true;
        if (BLOCK_TEXTURE_ID == null) {
            return;
        }
        try (InputStream in = ParticleStageView.class.getResourceAsStream("/snowstorm/assets/minecraft_block.png")) {
            if (in == null) {
                LOGGER.warn("[snowstorm] minecraft_block.png not found on classpath");
                return;
            }
            DynamicTexture texture = new DynamicTexture(NativeImage.read(in));
            Minecraft.getInstance().getTextureManager().register(BLOCK_TEXTURE_ID, texture);
            RenderSystem.bindTexture(texture.getId());
            int glTexture2D = 0x0DE1; // GL_TEXTURE_2D
            GlStateManager._texParameter(glTexture2D, 0x2800 /* GL_TEXTURE_MAG_FILTER */, 0x2600 /* GL_NEAREST */);
            GlStateManager._texParameter(glTexture2D, 0x2801 /* GL_TEXTURE_MIN_FILTER */, 0x2600);
        } catch (Exception e) {
            LOGGER.warn("[snowstorm] failed to load minecraft_block.png", e);
        }
    }

    // ==================================================================
    // 相机交互（EvmRefPreviewElement 同款事件体系，<26.1）
    // ==================================================================

    private void onMouseDown(UIEvent event) {
        if (!isHover()) {
            return;
        }
        if (event.button == 0 || event.button == 1) {
            startDrag(event.button == 0 ? DRAG_ROTATE : DRAG_PAN, null);
            event.stopPropagation();
        }
    }

    private void onDragUpdate(UIEvent event) {
        if (event.target != event.currentElement || event.dragHandler == null) {
            return;
        }
        // 屏幕增量 → 元素局部增量（JS OrbitControls 用元素 clientHeight 归一）
        var delta = ((StageCanvas) event.currentElement).getLocalMouseNormal(event.deltaX, event.deltaY);
        if (event.dragHandler.getDraggingObject() == DRAG_ROTATE) {
            camera.rotate(delta.x, delta.y, ((StageCanvas) event.currentElement).getSizeHeight());
        } else if (event.dragHandler.getDraggingObject() == DRAG_PAN) {
            camera.pan(delta.x, delta.y, ((StageCanvas) event.currentElement).getSizeHeight());
        }
    }

    private void onMouseWheel(UIEvent event) {
        if (event.target != event.currentElement) {
            return;
        }
        // JS handleMouseWheel：deltaY<0（滚上）dollyIn；MC 滚上 event.deltaY=+1 → 取负对齐
        camera.dolly(-event.deltaY);
        event.stopPropagation();
    }

    /** Preview.vue window keypress（焦点在输入框时不生效——LDLib 按键只派发到聚焦元素，语义等价）。 */
    private void onKeyDown(UIEvent event) {
        if (event.keyCode == 32 /* GLFW_KEY_SPACE；JS e.which === 32 */) {
            onSpaceKey((event.modifiers & 0x2 /* GLFW_MOD_CONTROL */) != 0);
            event.stopPropagation();
        }
    }
    //?}

    /** 舞台画布元素：自定义 3D 绘制宿主（26.1 降级占位文字）。 */
    private final class StageCanvas extends UIElement {
        StageCanvas() {
            style(style -> style.backgroundTexture(new ColorRectTexture(SnowstormTheme.BACKGROUND)));
            //? if <26.1 {
            addEventListener(UIEvents.MOUSE_DOWN, ParticleStageView.this::onMouseDown);
            addEventListener(UIEvents.MOUSE_WHEEL, ParticleStageView.this::onMouseWheel);
            addEventListener(UIEvents.DRAG_SOURCE_UPDATE, ParticleStageView.this::onDragUpdate);
            addEventListener(UIEvents.KEY_DOWN, ParticleStageView.this::onKeyDown);
            //?}
        }

        //? if <26.1 {
        @Override
        public void drawBackgroundAdditional(GUIContext guiContext) {
            drawStage(guiContext, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        }
        //?} else {
        private static final TextTexture DEGRADED =
                new TextTexture("Preview Stage\n(26.1 暂不支持 3D 舞台渲染)", 0xFF939AA3);

        @Override
        protected void drawBackgroundAdditional(IGUIContext context) {
            context.drawTexture(DEGRADED, getPositionX(), getPositionY(), getSizeWidth(), getSizeHeight());
        }
        //?}
    }
}
//?}
