package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.animation.ModelPoseTransforms;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.Model;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * 挂载锚点（G3）：把「挂在哪」与「画什么」分开。
 * <p>
 * 解析为空 = 本帧不绘制——基岩版没有「找不到挂点就退回模型原点」的可用语义，任何兜底位置都是错位。
 * 这与内置装备代理的既有语义一致（{@code ProxyAttachments.audit} 用它逐模型核对数据缺口）。
 *
 * @author TT432
 */
@FunctionalInterface
public interface AttachmentAnchor {

    /** 一个已解析的锚点实例：名称用于诊断/内容选择，位姿是基岩版骨骼帧（含模型 Y 翻转）。 */
    record Resolved(String name, Matrix4f pose) {
    }

    /** 本帧全部锚点实例；空列表表示该声明本帧不绘制。 */
    List<Resolved> resolve(RenderFrame frame);

    /**
     * 骨骼候选链：按序取第一个存在于主模型的骨骼（与内置手持物/头盔代理同一条链语义）。
     * 链内全部缺失 → 空列表。
     */
    static AttachmentAnchor boneChain(List<List<String>> chains) {
        List<List<String>> copy = new ArrayList<>(chains.size());
        for (List<String> chain : chains) {
            copy.add(List.copyOf(chain));
        }
        return frame -> {
            Model model = frame.primaryModel();
            if (model == null) {
                return List.of();
            }
            int boneId = ProxyAttachable.resolveChain(model, firstChain(copy));
            if (boneId < 0) {
                return List.of();
            }
            var pose = ModelPoseTransforms.resolveBonePose(model, frame.tickedInfos(), boneId, frame.rootPose());
            return pose.map(matrix -> List.of(new Resolved(GlobalBoneIdHandler.get(boneId), matrix)))
                       .orElseGet(List::of);
        };
    }

    /** 单骨骼锚点（等价于单元素链）。 */
    static AttachmentAnchor bone(String boneName) {
        return boneChain(List.of(List.of(boneName)));
    }

    /** 具名 locator 锚点；跨模型按顺序取首个命中。 */
    static AttachmentAnchor locator(String locatorName) {
        return frame -> frame.locator(locatorName)
                             .map(pose -> List.of(new Resolved(locatorName, pose)))
                             .orElseGet(List::of);
    }

    /**
     * locator 族锚点：所有名字以 {@code key} 开头的 locator 各出一个锚点实例。
     * <p>
     * 前缀语义与 {@code RenderHelper.renderOnLocator} 一致（{@code name} 或 {@code name + "_t_"…}，
     * 后者是前者的子集）。注意 {@code "pet"} 也会命中 {@code "pet2"}——今天下游的前缀匹配是同一语义，
     * 迁移不改变行为；需要精确区分时请用 {@link #locator(String)}。
     */
    static AttachmentAnchor locatorFamily(String key) {
        return frame -> {
            List<Resolved> resolved = new ArrayList<>();
            for (String name : frame.locatorNames()) {
                if (!name.startsWith(key)) {
                    continue;
                }
                frame.locator(name).ifPresent(pose -> resolved.add(new Resolved(name, pose)));
            }
            return resolved;
        };
    }

    /** 基岩模型空间根位姿（{@code bedrockRootPose}：根位姿绕 Y 180°）。 */
    static AttachmentAnchor modelRoot() {
        return frame -> List.of(new Resolved("", ModelPoseTransforms.bedrockRootPose(frame.rootPose())));
    }

    /**
     * 声明自行解析锚点（内置装备代理专用）。
     * <p>
     * 内置代理（{@code eyelib:held_item_main} 等）的锚点候选链、盔甲逐部件绑定、挂点缺失语义都在
     * 各自的绘制实现里，且需要在绘制时拿到骨骼 pivot 才能摆正物品。这类声明的
     * {@link AttachmentDraw} 会忽略传入的位姿，传入的是模型根位姿，仅供实现取统一入口。
     * 外部声明不应使用它。
     */
    static AttachmentAnchor selfAnchored() {
        return frame -> List.of(new Resolved("", frame.rootPose()));
    }

    private static List<String> firstChain(List<List<String>> chains) {
        for (List<String> chain : chains) {
            if (!chain.isEmpty()) {
                return chain;
            }
        }
        return List.of();
    }
}
