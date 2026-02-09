package io.github.tt432.eyelib.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.model.bedrock.BrBone;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import io.github.tt432.eyelib.client.render.bone.BoneRenderInfos;
import io.github.tt432.eyelib.compute.ParallelAnimatorHelper;
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

/**
 * @param model bone -> bone对应的计算链条
 * @author TT432
 */
public record GPUParallelModel(
        Int2ObjectMap<IntList> model,
        IntList animationBones,
        IntList modelOffsets,
        List<Model.Bone> boneList,
        long buffer,
        int bufferSize
) {
    public static final MemoryUtil.MemoryAllocator ALLOCATOR = MemoryUtil.getAllocator(false);

    public static GPUParallelModel from(Model model) {
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
        List<Model.Bone> boneList = new ArrayList<>();
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
        return new GPUParallelModel(nModel, animationBones, modelOffsets, boneList, ALLOCATOR.malloc(bufferSize), bufferSize);
    }

    private static void fillParent(Model model, int boneId, Model.Bone bone, IntList nBones) {
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

    private record OrderedModelWithAnimation(
            int order,
            ParallelAnimatorHelper.ModelWithAnimation modelWithAnimation
    ) {
    }

    private record OrderedGPUParallelModel(
            int order,
            GPUParallelModel model
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

        void memCopy(long src, int bytes) {
            this.bytes += bytes;
            actions.add(new MemCopy(src, bytes));
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
            UnsafeWithGlBufferReserveBuilder builder
    ) {
    }

    public static AnimationInfo put(List<ParallelAnimatorHelper.ModelWithAnimation> modelWithAnimations,
                                    UnsafeWithGlBuffer animationOffsets, UnsafeWithGlBuffer animation) {
        var streamResult = IntStream.range(0, modelWithAnimations.size())
                .mapToObj(i -> new OrderedModelWithAnimation(i, modelWithAnimations.get(i)))
                .parallel()
                .map(orderedModelWithAnimation -> {
                    ParallelAnimatorHelper.ModelWithAnimation modelWithAnimation = orderedModelWithAnimation.modelWithAnimation();
                    GPUParallelModel gpuModel = Eyelib.getModelManager().gpuModel.get(modelWithAnimation.model().name());
                    List<Model.Bone> bones = gpuModel.boneList();

                    for (int i = 0; i < bones.size(); i++) {
                        put(gpuModel.buffer + (long) i * ANIMATION_ENTRY_BYTE_SIZE, bones.get(i), modelWithAnimation.infos());
                    }

                    return new OrderedGPUParallelModel(orderedModelWithAnimation.order, gpuModel);
                })
                .toList()
                .stream()
                .sorted(Comparator.comparingInt(OrderedGPUParallelModel::order))
                .map(OrderedGPUParallelModel::model)
                .collect(
                        () -> new StreamResult(new IntArrayList(), new ArrayList<>(), new UnsafeWithGlBufferReserveBuilder(animation)),
                        (result, model) -> {
                            var offsets = result.offsets;
                            if (!offsets.isEmpty()) {
                                int last = offsets.getInt(offsets.size() - 1);
                                model.modelOffsets.forEach(offset -> offsets.add(offset + last));
                            } else {
                                offsets.addAll(model.modelOffsets);
                            }

                            result.builder.memCopy(model.buffer, model.bufferSize);

                            result.animationBones.add(model.animationBones);
                        },
                        (r1, r2) -> {
                            // do nothing
                        }
                );

        streamResult.builder.doAll();

        var offsets = streamResult.offsets;
        MemoryUtil.memPutInt(animationOffsets.reserve(4), 0);
        offsets.forEach(i -> MemoryUtil.memPutInt(animationOffsets.reserve(4), i));
        return new AnimationInfo(offsets.size(), streamResult.animationBones);
    }

    public static final int ANIMATION_ENTRY_BYTE_SIZE = 4 * 4 * 4;

    public static void put(long buffer, Model.Bone bone, BoneRenderInfos infos) {
        ModelTransformer<BrBone, BoneRenderInfos> transformer = infos.transformer();
        Vector3fc pivot = transformer.pivot((BrBone) bone, infos);
        Vector3fc position = transformer.position((BrBone) bone, infos);
        Vector3fc rotation = transformer.rotation((BrBone) bone, infos);
        Vector3fc scale = transformer.scale((BrBone) bone, infos);

        MemoryUtil.memPutFloat(buffer, pivot.x());
        MemoryUtil.memPutFloat(buffer, pivot.y());
        MemoryUtil.memPutFloat(buffer, pivot.z());
        MemoryUtil.memPutFloat(buffer, 0);
        MemoryUtil.memPutFloat(buffer, position.x());
        MemoryUtil.memPutFloat(buffer, position.y());
        MemoryUtil.memPutFloat(buffer, position.z());
        MemoryUtil.memPutFloat(buffer, 0);
        MemoryUtil.memPutFloat(buffer, rotation.x());
        MemoryUtil.memPutFloat(buffer, rotation.y());
        MemoryUtil.memPutFloat(buffer, rotation.z());
        MemoryUtil.memPutFloat(buffer, 0);
        MemoryUtil.memPutFloat(buffer, scale.x());
        MemoryUtil.memPutFloat(buffer, scale.y());
        MemoryUtil.memPutFloat(buffer, scale.z());
        MemoryUtil.memPutFloat(buffer, 0);
    }
}
