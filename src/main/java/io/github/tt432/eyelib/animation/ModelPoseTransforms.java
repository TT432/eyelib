package io.github.tt432.eyelib.animation;

import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.locator.LocatorEntry;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 模型渲染与 locator 解析共享的骨骼位姿变换。
 *
 * @author TT432
 */
public final class ModelPoseTransforms {
    private static final float MODEL_Y_ROTATION = (float) Math.PI;

    private ModelPoseTransforms() {
    }

    public static void applyBone(Matrix4f pose, Model.Bone bone, ModelRuntimeData data) {
        applyBone(pose, bone, data,
                (rotation, scale) -> pose.scale(scale.x(), scale.y(), scale.z()));
    }

    public static void applyBone(
            Matrix4f pose,
            Model.Bone bone,
            ModelRuntimeData data,
            BonePostRotationApplier postRotation
    ) {
        pose.translate(data.position(bone));

        var pivot = bone.pivot();
        pose.translate(pivot);

        var rotation = data.rotation(bone);
        pose.rotateZYX(rotation.z(), rotation.y(), rotation.x());

        var scale = data.scale(bone);
        postRotation.apply(rotation, scale);
        pose.translate(-pivot.x(), -pivot.y(), -pivot.z());
    }

    @FunctionalInterface
    public interface BonePostRotationApplier {
        void apply(Vector3fc rotation, Vector3fc scale);
    }

    /**
     * 解析单个骨骼的累积位姿：模型根位姿 → 骨骼链（与模型渲染同一条变换链，含模型 Y 翻转）。
     * 挂载物（手持物/盔甲）用它取得「基岩版骨骼帧」，骨骼不存在时返回空。
     */
    public static Optional<Matrix4f> resolveBonePose(
            Model model,
            ModelRuntimeData data,
            int boneId,
            Matrix4fc rootPose
    ) {
        Model.Bone bone = model.allBones().get(boneId);
        if (bone == null) {
            return Optional.empty();
        }

        // 由叶到根收集链，再正向施加；parent 缺省/自引用视为根
        List<Model.Bone> chain = new java.util.ArrayList<>(6);
        Model.Bone current = bone;
        while (current != null) {
            chain.add(current);
            int parentId = current.parent();
            current = parentId < 0 || parentId == current.id() ? null : model.allBones().get(parentId);
        }

        Matrix4f pose = new Matrix4f(rootPose).rotateY(MODEL_Y_ROTATION);
        for (int i = chain.size() - 1; i >= 0; i--) {
            applyBone(pose, chain.get(i), data);
        }
        return Optional.of(pose);
    }

    public static Optional<Matrix4f> resolveLocatorPose(
            Model model,
            ModelRuntimeData data,
            String locatorName,
            Matrix4fc rootPose
    ) {
        Matrix4f modelPose = new Matrix4f(rootPose).rotateY(MODEL_Y_ROTATION);
        for (Model.Bone bone : model.toplevelBones().values()) {
            @Nullable Matrix4f resolved = resolveBoneLocator(bone, data, locatorName, modelPose);
            if (resolved != null) {
                return Optional.of(resolved);
            }
        }
        return Optional.empty();
    }

    private static @Nullable Matrix4f resolveBoneLocator(
            Model.Bone bone,
            ModelRuntimeData data,
            String locatorName,
            Matrix4fc parentPose
    ) {
        Matrix4f bonePose = new Matrix4f(parentPose);
        applyBone(bonePose, bone, data);

        List<LocatorEntry> locators = bone.locator().cubes();
        if (locators != null) {
            for (LocatorEntry locator : locators) {
                if (locatorName.equals(locator.name())) {
                    return new Matrix4f(bonePose)
                            .translate(locator.offset())
                            .rotateZYX(locator.rotation().z(), locator.rotation().y(), locator.rotation().x());
                }
            }
        }

        for (Model.Bone child : bone.children().values()) {
            @Nullable Matrix4f resolved = resolveBoneLocator(child, data, locatorName, bonePose);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }
}
