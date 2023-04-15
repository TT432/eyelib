/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package io.github.tt432.eyelib.common.bedrock.animation;

import com.mojang.math.Vector3d;
import io.github.tt432.eyelib.api.bedrock.AnimatableModel;
import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.api.bedrock.animation.LoopType;
import io.github.tt432.eyelib.api.bedrock.animation.PlayState;
import io.github.tt432.eyelib.api.bedrock.model.Bone;
import io.github.tt432.eyelib.api.sound.SoundPlayer;
import io.github.tt432.eyelib.common.bedrock.animation.builder.AnimationBuilder;
import io.github.tt432.eyelib.common.bedrock.animation.builder.AnimationEntry;
import io.github.tt432.eyelib.common.bedrock.animation.control.ParticleControl;
import io.github.tt432.eyelib.common.bedrock.animation.control.SoundControl;
import io.github.tt432.eyelib.common.bedrock.animation.control.TimelineControl;
import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationData;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.BoneAnimation;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.SingleAnimation;
import io.github.tt432.eyelib.common.bedrock.animation.util.BoneAnimationQueue;
import io.github.tt432.eyelib.common.bedrock.animation.util.LerpInfo;
import io.github.tt432.eyelib.molang.MolangParser;
import io.github.tt432.eyelib.util.BoneSnapshot;
import io.github.tt432.eyelib.util.math.MathE;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.tt432.eyelib.api.bedrock.animation.ModelFetcherManager.getModel;

/**
 * The type Animation controller.
 *
 * @param <T> the type parameter
 */
@Slf4j
public class AnimationController<T extends Animatable> {
    SoundControl soundControl = new SoundControl();
    ParticleControl particleControl = new ParticleControl();
    TimelineControl timelineControl = new TimelineControl();
    @Getter
    private final String name;
    private double transitionStartTicks;
    /**
     * How long it takes to transition between animations
     */
    private final double transitionLengthTicks;
    private double tickOffset;
    private double currTick;

    private boolean needNextAnimation;
    private boolean needStopAnimation;

    protected T animatable;
    protected AnimationPredicate<T> animationPredicate;
    @Getter
    protected AnimationState animationState = AnimationState.STOPPED;
    @Getter
    private final Map<String, BoneAnimationQueue> boneAnimationQueues = new HashMap<>();
    protected Queue<SingleAnimation> animationQueue = new LinkedList<>();
    @Getter
    protected SingleAnimation preAnimation;
    @Getter
    protected SingleAnimation currentAnimation;
    @NotNull
    protected AnimationBuilder currentAnimationBuilder = new AnimationBuilder();
    protected boolean shouldResetTick = false;
    protected boolean needsAnimationReload = false;
    @Getter
    @Setter
    protected double animationSpeed = 1D;

    @Getter
    Map<String, Function<AnimationController<?>, Boolean>> nextAnimationCallbacks = new HashMap<>();
    @Getter
    Map<String, Function<AnimationController<?>, Boolean>> noLoopAnimationFinishCallbacks = new HashMap<>();

    public enum AnimationState {
        RUNNING,
        TRANSITION_TO_NEXT(true),
        TRANSITION_TO_END(true),
        STOPPED;

        @Getter
        final boolean isTransition;

        AnimationState(boolean isTransition) {
            this.isTransition = isTransition;
        }

        AnimationState() {
            this.isTransition = false;
        }
    }

    /**
     * This method sets the current animation with an animation builder. You can run
     * this method every frame, if you pass in the same animation builder every
     * time, it won't restart. Additionally, it smoothly transitions between
     * animation states.
     */
    public void setAnimation(AnimationBuilder builder) {
        if (builder == null) {
            needStopAnimation = true;
            return;
        }

        AnimatableModel<T> model = getModel(this.animatable);

        if (model == null) return;

        List<AnimationEntry> rawAnimList = builder.getRawAnimationList();

        if (rawAnimList.isEmpty()) {
            this.animationState = AnimationState.STOPPED;
        }

        if (!rawAnimList.equals(this.currentAnimationBuilder.getRawAnimationList()) || this.needsAnimationReload) {
            AtomicBoolean error = new AtomicBoolean(false);
            // Convert the list of animation names to the actual list, keeping track of the
            // loop boolean along the way
            LinkedList<SingleAnimation> animations = rawAnimList.stream().map(rawAnimation -> {
                SingleAnimation animation = model.getAnimation(rawAnimation.animationName, animatable, null);

                if (animation == null) {
                    log.error("Could not load animation: {}. Is it missing?", rawAnimation.animationName);
                    error.set(true);
                    return null;
                }

                if (rawAnimation.loopType != null)
                    animation.setLoop(rawAnimation.loopType);

                return animation;
            }).collect(Collectors.toCollection(LinkedList::new));

            if (error.get())
                return;

            this.animationQueue = animations;
            this.currentAnimationBuilder = builder;
            this.needNextAnimation = true;
            this.needsAnimationReload = false;
        }
    }

    /**
     * Instantiates a new Animation controller. Each animation controller can run
     * one animation at a time. You can have several animation controllers for each
     * entity, i.e. one animation to control the entity's size, one to control
     * movement, attacks, etc.
     *
     * @param animatable            The entity
     * @param name                  Name of the animation controller
     *                              (move_controller, size_controller,
     *                              attack_controller, etc.)
     * @param transitionLengthTicks How long it takes to transition between
     *                              animations (IN TICKS!!)
     */
    public AnimationController(T animatable, String name, float transitionLengthTicks, AnimationPredicate<T> predicate) {
        this.animatable = animatable;
        this.name = name;
        this.transitionLengthTicks = transitionLengthTicks;
        this.animationPredicate = predicate;
    }

    public void process(double tick, AnimationEvent<T> event, List<Bone> modelRendererList) {
        MolangParser.getInstance().setValue("query.life_time", () -> tick / 20);
        updateCurrentAnimation();
        initQueues(modelRendererList);

        double adjustedTick = transToRunning(adjustTick(tick), tick);

        PlayState test = this.animationPredicate.test(event);
        SoundPlayer currPlayer = getSoundPlayer(event);

        adjustedTick = runningTransToEnd(test, currPlayer, tick, adjustedTick);

        if (needNextAnimation) {
            needNextAnimation = false;
            nextAnimation(currPlayer, tick);
            adjustedTick = adjustTick(tick);
        }

        if (animationState != AnimationState.STOPPED)
            processCurrentAnimation(adjustedTick, tick, currPlayer);
    }

    double runningTransToEnd(PlayState playState, SoundPlayer currPlayer, double tick, double adjustedTick) {
        if (animationState == AnimationState.RUNNING &&
                (playState == PlayState.STOP || needStopAnimation ||
                        (this.currentAnimation == null && this.animationQueue.isEmpty()))) {
            stopAnimation(currPlayer, adjustedTick);
            adjustedTick = adjustTick(tick);
        }

        needStopAnimation = false;

        return adjustedTick;
    }

    private void updateCurrentAnimation() {
        if (this.currentAnimation == null) return;

        AnimatableModel<T> model = getModel(this.animatable);

        if (model == null) return;

        SingleAnimation animation = model.getAnimation(currentAnimation.getAnimationName(), this.animatable, currentAnimation);

        if (animation != null && animation != currentAnimation) {
            LoopType loop = this.currentAnimation.getLoop();
            this.currentAnimation = animation;
            this.currentAnimation.setLoop(loop);
        }
    }

    private void stopAnimation(SoundPlayer currPlayer, double tick) {
        animationQueue.clear();
        currentAnimationBuilder = new AnimationBuilder();

        stopControls(currPlayer);

        if (animationState != AnimationState.TRANSITION_TO_NEXT) {
            nextAnimation(currPlayer, tick);
            setTransitioning(tick, false);
            this.needsAnimationReload = false;
        }
    }

    double transToRunning(double adjustedTick, double tick) {
        // Transition period has ended, reset the tick and set the animation to running
        if (animationState == AnimationState.TRANSITION_TO_NEXT && adjustedTick >= this.transitionLengthTicks) {
            this.shouldResetTick = true;
            this.animationState = AnimationState.RUNNING;
            return adjustTick(tick);
        }

        return adjustedTick;
    }

    void setTransitioning(double tick, boolean next) {
        this.animationState = next ? AnimationState.TRANSITION_TO_NEXT : AnimationState.TRANSITION_TO_END;
        this.shouldResetTick = true;
        this.transitionStartTicks = tick;
    }

    private void setAnimTime(final double tick) {
        MolangParser.getInstance().setValue("query.anim_time", () -> tick / 20);
    }

    private SoundPlayer getSoundPlayer(AnimationEvent<T> event) {
        return event.getAnimatable() instanceof SoundPlayer sp ? sp : null;
    }

    /**
     * (tick, actualTick, player) -> tick
     */
    private double processFinishCurrentAnimation(double tick, double actualTick, SoundPlayer player) {
        // Animation has ended
        if (currentAnimation != null && tick >= this.currentAnimation.getAnimationLength()) {
            // If the current animation is set to loop, keep it as the current animation and
            // just start over
            if (this.currentAnimation.getLoop().isLooping()) {
                // Reset the adjusted tick so the next animation starts at tick 0
                this.shouldResetTick = true;
                return adjustTick(actualTick);
            } else {
                // Pull the next animation from the queue
                SingleAnimation peek = this.animationQueue.peek();

                if (peek == null) {
                    stopAnimation(player, tick);
                    processCallbacks(noLoopAnimationFinishCallbacks);

                    return adjustTick(tick);
                } else {
                    // Otherwise, set the state to transitioning and start transitioning to the next
                    // animation next frame
                    setTransitioning(tick, true);

                    nextAnimation(player, tick);
                }
            }
        }

        return tick;
    }

    private void processCurrentAnimation(double tick, double actualTick, SoundPlayer player) {
        tick = processFinishCurrentAnimation(tick, actualTick, player);

        setAnimTime(tick);
        currTick = tick;

        // Loop through every boneanimation in the current animation and process the values
        if (animationState.isTransition) {
            processTransitioning(tick);
        } else {
            processBoneAnimation(currentAnimation.getBones(), tick);
        }

        AnimatableModel<T> model = getModel(this.animatable);

        if (model != null) {
            model.codeBoneAnimation(this, tick);
        }

        processControls(player, tick);
    }

    private void processBoneAnimation(@Nullable Map<String, BoneAnimation> boneAnimations, double tick) {
        if (boneAnimations != null && animationState == AnimationState.RUNNING) {
            for (var entry : boneAnimations.entrySet()) {
                var boneName = entry.getKey();
                BoneAnimation boneAnim = entry.getValue();

                BoneAnimationQueue boneAnimationQueue = boneAnimationQueues.get(boneName);

                if (boneAnimationQueue == null) {
                    continue;
                }

                AnimationData data = MolangParser.getCurrentDataSource().getData();

                data.putExtraData("anim.current_bone", boneName);

                boneAnimationQueue.rotate().push(new LerpInfo(boneAnim.lerpRotation(tick)));
                boneAnimationQueue.position().push(new LerpInfo(boneAnim.lerpPosition(tick)));
                boneAnimationQueue.scale().push(new LerpInfo(boneAnim.lerpScale(tick)));

                data.removeExtraData("anim.current_bone");
            }
        }
    }

    private void processTransitioning(double tick) {
        AnimationData data = MolangParser.getCurrentDataSource().getData();
        AnimatableModel<T> model = getModel(animatable);

        if (model != null) {
            for (Bone bone : model.getAnimationProcessor().getModelRendererList()) {
                String boneName = bone.getName();
                data.putExtraData("anim.current_bone", boneName);
                processBoneAnimationTransitioning(boneName, tick);
                data.removeExtraData("anim.current_bone");
            }
        }
    }

    void processBoneAnimationTransitioning(String boneName, double tick) {
        double weight = MathE.getWeight(0, transitionLengthTicks, tick);

        Vector3d[] lerpPoints0 = getLerpPoints(preAnimation, transitionStartTicks, boneName);

        Vector3d lerpR0 = lerpPoints0[0];
        Vector3d lerpS0 = lerpPoints0[1];
        Vector3d lerpP0 = lerpPoints0[2];

        Vector3d[] lerpPoints = getLerpPoints(currentAnimation, 0, boneName);

        Vector3d lerpR = lerpPoints[0];
        Vector3d lerpS = lerpPoints[1];
        Vector3d lerpP = lerpPoints[2];

        if (lerpR != null && lerpR0 != null) {
            boneAnimationQueues.get(boneName).rotate().push(new LerpInfo(
                    new Vector3d(
                            MathE.lerp(lerpR0.x, lerpR.x, weight),
                            MathE.lerp(lerpR0.y, lerpR.y, weight),
                            MathE.lerp(lerpR0.z, lerpR.z, weight))));
        }

        if (lerpS != null && lerpS0 != null) {
            boneAnimationQueues.get(boneName).scale().push(new LerpInfo(
                    new Vector3d(
                            MathE.lerp(lerpS0.x, lerpS.x, weight),
                            MathE.lerp(lerpS0.y, lerpS.y, weight),
                            MathE.lerp(lerpS0.z, lerpS.z, weight))));
        }

        if (lerpP != null && lerpP0 != null) {
            boneAnimationQueues.get(boneName).position().push(new LerpInfo(
                    new Vector3d(
                            MathE.lerp(lerpP0.x, lerpP.x, weight),
                            MathE.lerp(lerpP0.y, lerpP.y, weight),
                            MathE.lerp(lerpP0.z, lerpP.z, weight))));
        }
    }

    private Vector3d[] getLerpPoints(@Nullable SingleAnimation anim, double tick, String boneName) {
        Vector3d lerpR0 = null;
        Vector3d lerpS0 = null;
        Vector3d lerpP0 = null;

        if (anim != null && anim.getBones() != null) {
            BoneAnimation other = anim.getBones().get(boneName);

            if (other != null) {
                lerpR0 = other.lerpRotation(tick);
                lerpS0 = other.lerpScale(tick);
                lerpP0 = other.lerpPosition(tick);
            }
        }

        AnimatableModel<T> model = getModel(animatable);

        if (model != null && lerpR0 == null && lerpS0 == null && lerpP0 == null) {
            BoneSnapshot init = model.getBone(boneName).getInitialSnapshot();

            lerpR0 = init.r();
            lerpS0 = init.s();
            lerpP0 = init.p();
        }

        return new Vector3d[]{lerpR0, lerpS0, lerpP0};
    }

    private void nextAnimation(@Nullable SoundPlayer player, double tick) {
        stopControls(player);

        preAnimation = currentAnimation;
        currentAnimation = animationQueue.poll();

        if (currentAnimation != null) {
            setTransitioning(tick, true);
            initControls(player);
        }

        processCallbacks(nextAnimationCallbacks);
    }

    private void processCallbacks(Map<String, Function<AnimationController<?>, Boolean>> callbacks) {
        callbacks.entrySet().removeIf(entry -> entry.getValue().apply(this));
    }

    private void initControls(SoundPlayer player) {
        soundControl.init(currentAnimation, player);
        particleControl.init(currentAnimation);
        timelineControl.init(currentAnimation);
    }

    private void processControls(SoundPlayer player, double tick) {
        soundControl.processSoundEffect(player, tick);
        particleControl.process(tick);
        timelineControl.process(tick);
    }

    private void stopControls(SoundPlayer player) {
        soundControl.stop(player, currTick);
        particleControl.stop(currTick);
        timelineControl.stop(currTick);
    }

    // Helper method to populate all the initial animation point queues
    private void initQueues(List<Bone> modelRendererList) {
        this.boneAnimationQueues.clear();

        for (Bone modelRenderer : modelRendererList) {
            this.boneAnimationQueues.put(modelRenderer.getName(), new BoneAnimationQueue(modelRenderer));
        }
    }

    /**
     * Used to reset the "tick" everytime a new animation starts, a transition starts, or something else of importance happens
     */
    protected double adjustTick(double tick) {
        if (this.shouldResetTick) {
            if (getAnimationState() != AnimationState.STOPPED) {
                this.tickOffset = tick;
            }

            this.shouldResetTick = false;

            return 0;
        } else {
            return this.animationSpeed * Math.max(tick - this.tickOffset, 0.0D);
        }
    }

    public void markNeedsReload() {
        this.needsAnimationReload = true;
    }

    public void clearAnimationCache() {
        this.currentAnimationBuilder = new AnimationBuilder();
    }
}