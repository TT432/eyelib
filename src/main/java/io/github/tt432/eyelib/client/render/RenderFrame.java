package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.animation.ModelPoseTransforms;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.bridge.client.render.RenderSink;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.Model;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 一帧渲染的上下文，交给挂载声明使用（G3）。
 * <p>
 * 生命周期：只在挂载阶段内有效，库不跨帧持有。位姿矩阵（{@link #entityPose()} / {@link #rootPose()}）
 * 是调用方位姿栈顶的副本，矩阵空间与 {@link RenderResult} 同源同值——
 * 与 {@link ModelPoseTransforms} 的入参一致（**不含**模型 Y 180° 翻转，工具自己施加）。
 * <p>
 * 为什么暴露这么多：下游过去必须在库外重新拼 locator 表、重算根位姿、自己 tick 动画、
 * 自造「有 rightitem 骨骼就跳过 locator 手持物」这类内部规则。把它们收敛到本上下文后，
 * 下游只需要声明「挂在哪个锚点、画什么」。
 *
 * @author TT432
 */
public final class RenderFrame {

    private final RenderHost host;
    private final RenderData<?> renderData;
    private final @Nullable BrClientEntity clientEntity;
    private final List<Model> models;
    private final @Nullable Model primaryModel;
    private final ModelRuntimeData tickedInfos;
    private final float partialTick;
    private final int light;
    private final int overlay;
    private final RenderSink sink;
    private final Matrix4f entityPose;
    private final Matrix4f rootPose;
    private final boolean renderable;
    private Set<EquipmentSlot> handledSlots = Set.of();

    RenderFrame(RenderHost host, RenderData<?> renderData, @Nullable BrClientEntity clientEntity,
                List<Model> models, @Nullable Model primaryModel, @Nullable ModelRuntimeData tickedInfos,
                float partialTick, int light, int overlay, RenderSink sink,
                Matrix4f entityPose, Matrix4f rootPose, boolean renderable) {
        this.host = host;
        this.renderData = renderData;
        this.clientEntity = clientEntity;
        this.models = models;
        this.primaryModel = primaryModel;
        this.tickedInfos = tickedInfos != null ? tickedInfos : ModelRuntimeData.EMPTY;
        this.partialTick = partialTick;
        this.light = light;
        this.overlay = overlay;
        this.sink = sink;
        this.entityPose = entityPose;
        this.rootPose = rootPose;
        this.renderable = renderable;
    }

    public RenderHost host() {
        return host;
    }

    /** 宿主的实体面；非实体宿主为 {@code null}（挂载声明在无实体宿主上通常直接返回空列表）。 */
    public @Nullable Entity hostEntity() {
        return host.entity();
    }

    public RenderData<?> renderData() {
        return renderData;
    }

    /** 本帧生效的客户端实体定义（已按注册表代际解析/归一化）。 */
    public @Nullable BrClientEntity clientEntity() {
        return clientEntity;
    }

    /** 本帧参与渲染的模型（按组件顺序）。 */
    public List<Model> models() {
        return models;
    }

    /** 主模型：骨骼最多的已就绪组件（挂载点解析基准）。 */
    public @Nullable Model primaryModel() {
        return primaryModel;
    }

    /** 本帧动画采样结果；永不为 {@code null}（无动画时为 {@code ModelRuntimeData.EMPTY}）。 */
    public ModelRuntimeData tickedInfos() {
        return tickedInfos;
    }

    public float partialTick() {
        return partialTick;
    }

    public int light() {
        return light;
    }

    public int overlay() {
        return overlay;
    }

    /** 本次渲染的 sink：声明绘制必须走它才能落在同一批次/延迟提交通道内。 */
    public RenderSink sink() {
        return sink;
    }

    /**
     * 底部 vanilla 渲染目标请经 {@link #sink()} 取：{@code RenderSink} 在自己的版本分支里暴露
     * {@code multiBufferSource()}（{@code <26.1}）或 {@code submitNodeCollector()}（{@code >=26.1}）。
     * 库内不直接读它——版本差异必须收敛在 bridge（ADR-0016 §6）。
     */

    /** 宿主位姿（未施加脚本缩放/幼体缩放/睡眠位移/身体朝向）。 */
    public Matrix4f entityPose() {
        return new Matrix4f(entityPose);
    }

    /** 模型根位姿（已施加脚本缩放/幼体缩放/睡眠位移/身体朝向）。 */
    public Matrix4f rootPose() {
        return new Matrix4f(rootPose);
    }

    /** 本帧是否会有模型几何被提交（{@code skipIfUnbaked} 主动跳过时仍可能为 true）。 */
    public boolean hasRenderableComponent() {
        return renderable;
    }

    /** 本帧已实际绘制装备的槽位（真实 attachable + 内置代理）；外部声明据此避让，避免双画。 */
    public Set<EquipmentSlot> handledSlots() {
        return handledSlots;
    }

    void setHandledSlots(Set<EquipmentSlot> handledSlots) {
        this.handledSlots = Set.copyOf(handledSlots);
    }

    /** 具名 locator 的位姿；跨模型按 {@link #models()} 顺序取首个命中。 */
    public Optional<Matrix4f> locator(String name) {
        for (Model model : models) {
            var pose = ModelPoseTransforms.resolveLocatorPose(model, tickedInfos, name, rootPose);
            if (pose.isPresent()) {
                return pose;
            }
        }
        return Optional.empty();
    }

    /** 具名骨骼的累积位姿（基岩版骨骼帧，含模型 Y 翻转）。 */
    public Optional<Matrix4f> bonePose(String boneName) {
        int boneId = GlobalBoneIdHandler.get(boneName);
        for (Model model : models) {
            var pose = ModelPoseTransforms.resolveBonePose(model, tickedInfos, boneId, rootPose);
            if (pose.isPresent()) {
                return pose;
            }
        }
        return Optional.empty();
    }

    /** 全部模型的 locator 名（去重，按模型/骨骼顺序）；由 {@link AttachmentStage} 按模型缓存。 */
    public List<String> locatorNames() {
        return AttachmentStage.locatorNames(models);
    }

    /** 由位姿矩阵构造一个新的 {@link PoseStack}（拷贝语义，可安全 push/pop）。 */
    public PoseStack newPose(Matrix4f frame) {
        return RenderPorts.get().renderSystemPort().createPoseStackFromMatrix(frame);
    }
}
