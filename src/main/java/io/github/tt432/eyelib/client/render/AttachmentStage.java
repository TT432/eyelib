package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.capability.component.ModelComponent;
import io.github.tt432.eyelib.client.render.AttachmentAnchor.Resolved;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.locator.GroupLocator;
import io.github.tt432.eyelib.model.locator.LocatorEntry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 挂载阶段（G3）：内置装备代理与外部挂载声明的唯一执行点。
 * <p>
 * 时机固定在**模型几何提交之前**（26.1 的 submit 节点在几何 writer 内已不再接收新内容），
 * 位姿取本帧模型根位姿，因此挂载物与本体同姿、随骨骼动画移动。
 * <p>
 * 三条结构性保证（都是历史缺陷的防护，不要回退）：
 * <ol>
 *   <li><b>每实体每帧一次</b>：嵌套 {@code renderComponents}（饰品/特效/携带宠物）不再触发挂载阶段，
 *       否则宿主装备会被重复画到被挂载模型上（ADR-0043 §2.3 的缺陷形态）。</li>
 *   <li><b>声明级去重</b>：同帧同 {@code identifier} 只画第一次，重复者计入诊断。</li>
 *   <li><b>异常隔离</b>：单个声明源/单条声明抛错只跳过它，不影响本体与其它挂载。</li>
 * </ol>
 *
 * @author TT432
 */
final class AttachmentStage {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentStage.class);

    /** 有 attachable 定义的盔甲槽（人形四槽；BODY 槽狼铠不在范围内）。 */
    private static final EquipmentSlot[] ATTACHABLE_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /**
     * 重入深度：渲染线程单线程使用（{@code ParallelStageExecutor} 只覆盖 setup/tick，不覆盖渲染）。
     * 深度 &gt; 0 表示当前处于某个挂载声明的嵌套绘制内。
     */
    private static int depth = 0;

    private AttachmentStage() {
    }

    /**
     * 构造本帧上下文。{@code entityPose}/{@code rootPose} 由调用方一次性算好并共享给
     * {@link RenderResult}，保证「结果里的根位姿」与「挂载看到的根位姿」是同一个矩阵。
     */
    static RenderFrame createFrame(SimpleRenderAction<?> action, List<ModelComponent> components,
                                   Matrix4f entityPose, Matrix4f rootPose, boolean renderable) {
        List<Model> models = new ArrayList<>(components.size());
        for (ModelComponent component : components) {
            Model model = component.getModel();
            if (model != null) {
                models.add(model);
            }
        }
        RenderData<?> cap = action.renderData();
        BrClientEntity clientEntity = cap.getClientEntityComponent().getClientEntity();
        return new RenderFrame(action.host() == null ? RenderHost.anchor(cap) : action.host(), cap, clientEntity,
                List.copyOf(models), ProxyAttachments.attachmentModel(components),
                action.tickedInfos(), action.partialTick(), action.packedLight(), action.overlay(),
                action.sink(), entityPose, rootPose, renderable);
    }

    /** 执行挂载阶段，返回提交的几何段数。 */
    static int run(SimpleRenderAction<?> action, RenderFrame frame) {
        if (!action.hostBody() || depth > 0) {
            return 0;
        }
        depth++;
        try {
            int draws = 0;
            Set<EquipmentSlot> handled = EnumSet.noneOf(EquipmentSlot.class);
            if (frame.hostEntity() instanceof LivingEntity holder
                    && frame.primaryModel() != null
                    && ProxyAttachments.hasEquipment(holder)) {
                Set<EquipmentSlot> attachableSlots = renderSlotAttachables(action, frame, holder);
                handled.addAll(attachableSlots);
                for (ProxyAttachments.SlotDeclaration slotDeclaration : ProxyAttachments.declarations(action, frame, holder, attachableSlots)) {
                    int emitted = drawDeclaration(frame, slotDeclaration.declaration(), new HashSet<>());
                    if (emitted > 0) {
                        handled.add(slotDeclaration.slot());
                    }
                    draws += emitted;
                }
            }
            frame.setHandledSlots(handled);
            for (AttachmentSource source : AttachmentRegistry.snapshot()) {
                List<AttachmentDeclaration> declarations;
                try {
                    declarations = source.declare(frame);
                } catch (Throwable t) {
                    RenderDiagnostics.recordAttachmentSourceError();
                    LOGGER.error("[Eyelib] 挂载声明源失败，本帧跳过：{}", source.getClass().getName(), t);
                    continue;
                }
                Set<String> seen = new LinkedHashSet<>();
                for (AttachmentDeclaration declaration : declarations) {
                    draws += drawDeclaration(frame, declaration, seen);
                }
            }
            return draws;
        } finally {
            depth--;
        }
    }

    /** 绘制单条声明（先解析锚点，每个锚点实例独立位姿栈）。 */
    private static int drawDeclaration(RenderFrame frame, AttachmentDeclaration declaration, Set<String> seen) {
        if (!seen.add(declaration.identifier())) {
            RenderDiagnostics.recordAttachmentDeclarationDropped();
            return 0;
        }
        List<Resolved> anchors;
        try {
            anchors = declaration.anchor().resolve(frame);
        } catch (Throwable t) {
            RenderDiagnostics.recordAttachmentSourceError();
            LOGGER.error("[Eyelib] 挂载锚点解析失败，本帧跳过：{}", declaration.identifier(), t);
            return 0;
        }
        int draws = 0;
        for (Resolved anchor : anchors) {
            try {
                PoseStack pose = frame.newPose(anchor.pose());
                draws += declaration.draw().draw(frame, pose, anchor.name());
            } catch (Throwable t) {
                RenderDiagnostics.recordAttachmentSourceError();
                LOGGER.error("[Eyelib] 挂载绘制失败，已跳过：{} @ {}", declaration.identifier(), anchor.name(), t);
            }
        }
        return draws;
    }

    /**
     * 渲染盔甲槽的 attachable，返回已由 attachable 接管（回退路径需跳过）的槽位集合。
     * attachable 只 tick 不渲染的旧行为在萌化实体上是空缺：原版 {@code HumanoidArmorLayer}
     * 被 cancel 后没有别的层会画它。
     */
    private static Set<EquipmentSlot> renderSlotAttachables(SimpleRenderAction<?> action, RenderFrame frame,
                                                            LivingEntity holder) {
        Model model = frame.primaryModel();
        if (model == null) {
            return Set.of();
        }
        ModelRuntimeData tickedInfos = frame.tickedInfos();
        Matrix4f modelRoot = frame.rootPose();
        Set<EquipmentSlot> handled = null;
        for (EquipmentSlot slot : ATTACHABLE_SLOTS) {
            if (holder.getItemBySlot(slot).isEmpty()) {
                continue;
            }
            var rd = AttachableItemRenderSetup.getOrPrepare(holder, slot, false);
            if (rd == null) {
                continue;
            }
            AttachableItemRenderSetup.renderAttachableBound(rd, holder, model, tickedInfos, modelRoot,
                    action.multiBufferSource(), frame.light(), frame.overlay());
            if (handled == null) {
                handled = EnumSet.noneOf(EquipmentSlot.class);
            }
            handled.add(slot);
        }
        return handled == null ? Set.of() : handled;
    }

    /** locator 名索引缓存（模型不可变，按模型弱引用缓存；与装配物缓存同模式）。 */
    private static final Map<Model, List<String>> LOCATOR_NAMES = new WeakHashMap<>();

    /** 全部模型的 locator 名（去重、按模型/骨骼顺序）。 */
    static List<String> locatorNames(List<Model> models) {
        Set<String> names = new LinkedHashSet<>();
        for (Model model : models) {
            names.addAll(locatorNames(model));
        }
        return List.copyOf(names);
    }

    private static List<String> locatorNames(Model model) {
        synchronized (LOCATOR_NAMES) {
            List<String> cached = LOCATOR_NAMES.get(model);
            if (cached != null) {
                return cached;
            }
        }
        List<String> names = new ArrayList<>();
        for (Model.Bone bone : model.allBones().values()) {
            GroupLocator group = bone.locator();
            if (group == null || group.cubes() == null) {
                continue;
            }
            for (LocatorEntry locator : group.cubes()) {
                names.add(locator.name());
            }
        }
        List<String> result = List.copyOf(names);
        synchronized (LOCATOR_NAMES) {
            LOCATOR_NAMES.put(model, result);
        }
        return result;
    }

    /** 当前是否处于挂载阶段内（嵌套绘制判定，诊断/测试用）。 */
    static boolean inStage() {
        return depth > 0;
    }

    /** 宿主实体是否可能带挂载物（无宿主实体永远为 false）。 */
    static boolean hostMayHaveAttachments(@Nullable RenderFrame frame) {
        return frame != null && frame.hostEntity() instanceof LivingEntity living && ProxyAttachments.hasEquipment(living);
    }
}
