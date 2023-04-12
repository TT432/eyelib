package io.github.tt432.eyelib.common.bedrock.animation;

import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.api.bedrock.model.Bone;
import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationData;
import io.github.tt432.eyelib.common.bedrock.animation.util.BoneAnimationQueue;
import io.github.tt432.eyelib.util.BoneSnapshot;
import io.github.tt432.eyelib.util.math.MathE;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;

import java.util.List;

public class AnimationProcessor<T extends Animatable> {
    public boolean reloadAnimations = false;
    @Getter
    private final List<Bone> modelRendererList = new ObjectArrayList<>();
    private double lastTickValue = -1;
    private final IntSet animatedEntities = new IntOpenHashSet();

    public void tickAnimation(Animatable entity, int uniqueID, double seekTime, AnimationEvent<T> event) {
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

        modelRendererList.forEach(bone -> {
            bone.saveInitialSnapshot();
            BoneSnapshot initialSnapshot = bone.getInitialSnapshot();
            initialSnapshot.apply(bone);
        });

        for (AnimationController<T> controller : manager.getAnimationControllers().values()) {
            if (reloadAnimations) {
                controller.markNeedsReload();
                controller.getBoneAnimationQueues().clear();
            }

            // Set current controller to animation test event
            event.setController(controller);

            // Process animations and add new values to the point queues
            controller.process(seekTime, event, modelRendererList);

            // Loop through every single bone and lerp each property
            for (BoneAnimationQueue boneAnimation : controller.getBoneAnimationQueues().values()) {
                Bone bone = boneAnimation.bone();
                boneAnimation.rotate().forEach(li -> {
                    if (li != null && li.getValue() != null) {
                        bone.setRotation(
                                (float) (bone.getRotationX() - Math.toRadians(li.getValue().x)),
                                (float) (bone.getRotationY() - Math.toRadians(li.getValue().y)),
                                (float) (bone.getRotationZ() + Math.toRadians(li.getValue().z))
                        );
                    }
                });
                boneAnimation.rotate().clear();

                boneAnimation.position().forEach(li -> {
                    if (li != null && li.getValue() != null) {
                        bone.setPosition(
                                (float) (bone.getPositionX() + li.getValue().x),
                                (float) (bone.getPositionY() + li.getValue().y),
                                (float) (bone.getPositionZ() + li.getValue().z)
                        );
                    }
                });
                boneAnimation.position().clear();

                boneAnimation.scale().forEach(li -> {
                    if (li != null && li.getValue() != null) {
                        bone.setScale(
                                (float) (bone.getScaleX() * MathE.notZero(li.getValue().x, 0.00001)),
                                (float) (bone.getScaleY() * MathE.notZero(li.getValue().y, 0.00001)),
                                (float) (bone.getScaleZ() * MathE.notZero(li.getValue().z, 0.00001))
                        );
                    }
                });
                boneAnimation.scale().clear();
            }
        }

        this.reloadAnimations = false;
        manager.setFirstTick(false);
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
        modelRendererList.add(modelRenderer);
    }

    public void clearModelRendererList() {
        this.modelRendererList.clear();
    }
}
