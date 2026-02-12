package io.github.tt432.eyelib.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.compute.ModelWithAnimation;
import io.github.tt432.eyelib.compute.UnsafeWithGlBuffer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import lombok.RequiredArgsConstructor;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import static io.github.tt432.eyelib.compute.ParallelAnimatorHelper.putMatrix3f;
import static io.github.tt432.eyelib.compute.ParallelAnimatorHelper.putMatrix4f;

/**
 * @param model bone -> bone对应的计算链条
 * @author TT432
 */
public record GPUParallelModel<B extends Model.Bone<B>>(
        Int2ObjectMap<IntList> model,
        IntList animationBones,
        IntList modelOffsets,
        List<B> boneList,
        long buffer,
        int bufferSize
) {
    public static final MemoryUtil.MemoryAllocator ALLOCATOR = MemoryUtil.getAllocator(false);

    public static <B extends Model.Bone<B>> GPUParallelModel<B> from(Model<B> model) {
        Int2ObjectMap<IntList> nModel = new Int2ObjectOpenHashMap<>();
        for (var bone : model.allBones().int2ObjectEntrySet()) {
            IntArrayList nBones = new IntArrayList();
            int boneId = bone.getIntKey();
            fillParent(model, boneId, bone.getValue(), nBones);
            nModel.put(boneId, new IntArrayList(nBones.reversed()));
        }

        IntList animationBones = new IntArrayList();
        IntList modelOffsets = new IntArrayList();
        int modelOffset = 0;
        List<B> boneList = new ArrayList<>();
        for (Int2ObjectMap.Entry<IntList> intListEntry : nModel.int2ObjectEntrySet()) {
            IntList bones = intListEntry.getValue();
            for (int i = 0; i < bones.size(); i++) {
                boneList.add(model.allBones().get(bones.getInt(i)));
                modelOffset++;
            }

            modelOffsets.add(modelOffset);
            animationBones.add(intListEntry.getIntKey());
        }

        var bufferSize = ANIMATION_ENTRY_BYTE_SIZE * boneList.size();
        return new GPUParallelModel<>(nModel, animationBones, modelOffsets, boneList, ALLOCATOR.malloc(bufferSize), bufferSize);
    }

    private static <B extends Model.Bone<B>> void fillParent(Model<B> model, int boneId, B bone, IntList nBones) {
        nBones.add(boneId);
        if (bone.parent() != -1) {
            fillParent(model, bone.parent(), model.allBones().get(bone.parent()), nBones);
        }
    }

    public static List<Int2ObjectMap<PoseStack.Pose>> getAnimationBones(List<IntList> animationBones, List<PoseStack.Pose> poses) {
        List<Int2ObjectMap<PoseStack.Pose>> result = new ArrayList<>(animationBones.size());
        int poseIndex = 0;

        for (IntList boneIds : animationBones) {
            Int2ObjectMap<PoseStack.Pose> map = new Int2ObjectOpenHashMap<>(boneIds.size());

            for (int i = 0; i < boneIds.size(); i++) {
                if (poseIndex < poses.size()) {
                    map.put(boneIds.getInt(i), poses.get(poseIndex++));
                }
            }

            result.add(map);
        }

        return result;
    }

    public record AnimationInfo(
            int parallelNumGroups,
            List<IntList> animationBones
    ) {
    }

    private record OrderedModelWithAnimation<B extends Model.Bone<B>>(
            int order,
            ModelWithAnimation<B> modelWithAnimation
    ) {
    }

    private record OrderedGPUParallelModel<B extends Model.Bone<B>>(
            int order,
            GPUParallelModel<B> model
    ) {
    }

    @RequiredArgsConstructor
    private static class UnsafeWithGlBufferReserveBuilder {
        private final UnsafeWithGlBuffer buffer;
        private final List<Action> actions = new ArrayList<>();
        private int bytes;

        interface Action {
            int bytes();

            void doAction(long ptr);
        }

        private record MemCopy(
                long src,
                int bytes
        ) implements Action {
            @Override
            public void doAction(long ptr) {
                MemoryUtil.memCopy(src, ptr, bytes);
            }
        }

        private record PutPose(
                PoseStack.Pose pose,
                int count
        ) implements Action {
            @Override
            public int bytes() {
                return count * POSE_ENTRY_BYTE_SIZE;
            }

            @Override
            public void doAction(long ptr) {
                for (int i = 0; i < count; i++) {
                    putMatrix4f(ptr, pose.pose());
                    putMatrix3f(ptr, pose.normal());
                    ptr += POSE_ENTRY_BYTE_SIZE;
                }
            }
        }

        void memCopy(long src, int bytes) {
            this.bytes += bytes;
            actions.add(new MemCopy(src, bytes));
        }

        void putPose(PoseStack.Pose pose, int count) {
            this.bytes += count * POSE_ENTRY_BYTE_SIZE;
            actions.add(new PutPose(pose, count));
        }

        void doAll() {
            long startPtr = buffer.reserve(bytes);
            for (Action action : actions) {
                action.doAction(startPtr);
                startPtr += action.bytes();
            }
        }
    }

    private record StreamResult(
            IntList offsets,
            List<IntList> animationBones,
            UnsafeWithGlBufferReserveBuilder builder,
            UnsafeWithGlBufferReserveBuilder poseBuilder
    ) {
    }

    private static <T> T cast(Object obj) {
        return (T) obj;
    }

    public static AnimationInfo put(List<ModelWithAnimation<?>> modelWithAnimations, List<PoseStack.Pose> posesList,
                                    UnsafeWithGlBuffer animationOffsets, UnsafeWithGlBuffer animation, UnsafeWithGlBuffer entityBasePose) {
        var streamResult = IntStream.range(0, modelWithAnimations.size())
                .mapToObj(i -> new OrderedModelWithAnimation<>(i, modelWithAnimations.get(i)))
                .parallel()
                .map(orderedModelWithAnimation -> {
                    var modelWithAnimation = orderedModelWithAnimation.modelWithAnimation();
                    var gpuModel = Eyelib.getModelManager().gpuModel.get(modelWithAnimation.model().name());
                    var bones = gpuModel.boneList();

                    for (int i = 0; i < bones.size(); i++) {
                        put(gpuModel.buffer + (long) i * ANIMATION_ENTRY_BYTE_SIZE, cast(bones.get(i)), modelWithAnimation.infos());
                    }

                    return new OrderedGPUParallelModel<>(orderedModelWithAnimation.order, gpuModel);
                })
                .toList()
                .stream()
                .sorted(Comparator.comparingInt(OrderedGPUParallelModel::order))
                .collect(
                        () -> new StreamResult(new IntArrayList(), new ArrayList<>(), new UnsafeWithGlBufferReserveBuilder(animation), new UnsafeWithGlBufferReserveBuilder(entityBasePose)),
                        (result, orderedModel) -> {
                            var model = orderedModel.model();
                            var offsets = result.offsets;
                            if (!offsets.isEmpty()) {
                                int last = offsets.getInt(offsets.size() - 1);
                                model.modelOffsets.forEach(offset -> offsets.add(offset + last));
                            } else {
                                offsets.addAll(model.modelOffsets);
                            }

                            result.builder.memCopy(model.buffer, model.bufferSize);

                            result.animationBones.add(model.animationBones);

                            result.poseBuilder.putPose(posesList.get(orderedModel.order()), model.boneList.size());
                        },
                        (r1, r2) -> {
                            // do nothing
                        }
                );

        streamResult.builder.doAll();
        streamResult.poseBuilder.doAll();

        var offsets = streamResult.offsets;
        MemoryUtil.memPutInt(animationOffsets.reserve(4), 0);
        offsets.forEach(i -> MemoryUtil.memPutInt(animationOffsets.reserve(4), i));
        return new AnimationInfo(offsets.size(), streamResult.animationBones);
    }

    public static final int ANIMATION_ENTRY_BYTE_SIZE = 4 * 4 * 4;
    public static final int POSE_ENTRY_BYTE_SIZE = (3 + 4) * 4 * 4;

    private static <B extends Model.Bone<B>> void put(long buffer, B bone, ModelRuntimeData<B> infos) {
        Vector3fc pivot = infos.pivot(bone);
        Vector3fc position = infos.position(bone);
        Vector3fc rotation = infos.rotation(bone);
        Vector3fc scale = infos.scale(bone);

        MemoryUtil.memPutFloat(buffer, pivot.x());
        MemoryUtil.memPutFloat(buffer += 4, pivot.y());
        MemoryUtil.memPutFloat(buffer += 4, pivot.z());
        MemoryUtil.memPutFloat(buffer += 4, 0);
        MemoryUtil.memPutFloat(buffer += 4, position.x());
        MemoryUtil.memPutFloat(buffer += 4, position.y());
        MemoryUtil.memPutFloat(buffer += 4, position.z());
        MemoryUtil.memPutFloat(buffer += 4, 0);
        MemoryUtil.memPutFloat(buffer += 4, rotation.x());
        MemoryUtil.memPutFloat(buffer += 4, rotation.y());
        MemoryUtil.memPutFloat(buffer += 4, rotation.z());
        MemoryUtil.memPutFloat(buffer += 4, 0);
        MemoryUtil.memPutFloat(buffer += 4, scale.x());
        MemoryUtil.memPutFloat(buffer += 4, scale.y());
        MemoryUtil.memPutFloat(buffer += 4, scale.z());
        MemoryUtil.memPutFloat(buffer += 4, 0);
    }
}
