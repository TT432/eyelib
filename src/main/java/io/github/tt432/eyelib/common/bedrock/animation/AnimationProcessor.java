package io.github.tt432.eyelib.common.bedrock.animation;

import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.api.bedrock.AnimatableModel;
import io.github.tt432.eyelib.api.bedrock.model.Bone;
import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationData;
import io.github.tt432.eyelib.common.bedrock.animation.util.BoneAnimationQueue;
import io.github.tt432.eyelib.util.BoneSnapshot;
import io.github.tt432.eyelib.util.math.MathE;
import io.github.tt432.eyelib.util.molang.MolangParser;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;

public class AnimationProcessor<T extends Animatable> {
    public boolean reloadAnimations = false;
    private final List<Bone> modelRendererList = new ObjectArrayList<>();
    private double lastTickValue = -1;
    private final IntSet animatedEntities = new IntOpenHashSet();
    private final AnimatableModel animatedModel;

    public AnimationProcessor(AnimatableModel animatedModel) {
        this.animatedModel = animatedModel;
    }

    public void tickAnimation(Animatable entity, int uniqueID, double seekTime, AnimationEvent<T> event,
                              MolangParser parser, boolean crashWhenCantFindBone) {
        if (seekTime != lastTickValue) {
            animatedEntities.clear();
        } else if (animatedEntities.contains(uniqueID)) { // Entity already animated on this tick
            return;
        }

        lastTickValue = seekTime;
        animatedEntities.add(uniqueID);

        // Each animation has its own collection of animations (called the
        // EntityAnimationManager), which allows for multiple independent animations
        AnimationData manager = entity.getFactory().getOrCreateAnimationData(uniqueID);

        // Store the current value of each bone rotation/position/scale
        updateBoneSnapshots(manager.getBoneSnapshotCollection());

        Map<String, Pair<Bone, BoneSnapshot>> boneSnapshots = manager.getBoneSnapshotCollection();

        for (AnimationController<T> controller : manager.getAnimationControllers().values()) {
            if (reloadAnimations) {
                controller.markNeedsReload();
                controller.getBoneAnimationQueues().clear();
            }

            controller.isJustStarting = manager.isFirstTick;

            // Set current controller to animation test event
            event.setController(controller);

            // Process animations and add new values to the point queues
            controller.process(seekTime, event, modelRendererList, boneSnapshots, parser, crashWhenCantFindBone);

            // Loop through every single bone and lerp each property
            for (BoneAnimationQueue boneAnimation : controller.getBoneAnimationQueues().values()) {
                Bone bone = boneAnimation.bone();
                BoneSnapshot snapshot = boneSnapshots.get(bone.getName()).getRight();
                BoneSnapshot initialSnapshot = bone.getInitialSnapshot();

                initialSnapshot.apply(bone);

                var rotatePoint = boneAnimation.rotate().poll();

                if (rotatePoint != null && rotatePoint.getValue() != null) {
                    bone.setRotation(
                            (float) (bone.getRotationX() - Math.toRadians(rotatePoint.getValue().x)),
                            (float) (bone.getRotationY() - Math.toRadians(rotatePoint.getValue().y)),
                            (float) (bone.getRotationZ() + Math.toRadians(rotatePoint.getValue().z))
                    );
                    snapshot.rotationValueX = bone.getRotationX();
                    snapshot.rotationValueY = bone.getRotationY();
                    snapshot.rotationValueZ = bone.getRotationZ();
                }

                var positionPoint = boneAnimation.position().poll();
                if (positionPoint != null && positionPoint.getValue() != null) {
                    bone.setPosition(
                            (float) (bone.getPositionX() + positionPoint.getValue().x),
                            (float) (bone.getPositionY() + positionPoint.getValue().y),
                            (float) (bone.getPositionZ() + positionPoint.getValue().z)
                    );
                    snapshot.positionOffsetX = bone.getPositionX();
                    snapshot.positionOffsetY = bone.getPositionY();
                    snapshot.positionOffsetZ = bone.getPositionZ();
                }

                var scalePoint = boneAnimation.scale().poll();
                if (scalePoint != null && scalePoint.getValue() != null) {
                    bone.setScale(
                            (float) (bone.getScaleX() * MathE.notZero(scalePoint.getValue().x, 0.00001)),
                            (float) (bone.getScaleY() * MathE.notZero(scalePoint.getValue().y, 0.00001)),
                            (float) (bone.getScaleZ() * MathE.notZero(scalePoint.getValue().z, 0.00001))
                    );
                    snapshot.scaleValueX = bone.getScaleX();
                    snapshot.scaleValueY = bone.getScaleY();
                    snapshot.scaleValueZ = bone.getScaleZ();
                }
            }
        }

        this.reloadAnimations = false;
        manager.isFirstTick = false;
    }

    private void updateBoneSnapshots(Map<String, Pair<Bone, BoneSnapshot>> boneSnapshotCollection) {
        for (Bone bone : modelRendererList) {
            if (!boneSnapshotCollection.containsKey(bone.getName())) {
                boneSnapshotCollection.put(bone.getName(), Pair.of(bone, new BoneSnapshot(bone.getInitialSnapshot())));
            }
        }
    }

    /**
     * Gets a bone by name.
     *
     * @param boneName The bone name
     * @return the bone
     */
    public Bone getBone(String boneName) {
        for (Bone bone : this.modelRendererList) {
            if (bone.getName().equals(boneName))
                return bone;
        }

        return null;
    }

    /**
     * Register model renderer. Each AnimatedModelRenderer (group in blockbench)
     * NEEDS to be registered via this method.
     *
     * @param modelRenderer The model renderer
     */
    public void registerModelRenderer(Bone modelRenderer) {
        modelRenderer.saveInitialSnapshot();
        modelRendererList.add(modelRenderer);
    }

    public void clearModelRendererList() {
        this.modelRendererList.clear();
    }

    public List<Bone> getModelRendererList() {
        return modelRendererList;
    }

    public void preAnimationSetup(Animatable animatable, double seekTime, int instanceId) {
        this.animatedModel.setMolangQueries(animatable, seekTime, instanceId);
    }
}
