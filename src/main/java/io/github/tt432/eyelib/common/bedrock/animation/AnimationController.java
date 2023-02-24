/*
 * Copyright (c) 2020.
 * Author: Bernie G. (Gecko)
 */

package io.github.tt432.eyelib.common.bedrock.animation;

import io.github.tt432.eyelib.api.bedrock.AnimatableModel;
import io.github.tt432.eyelib.api.bedrock.animation.Animatable;
import io.github.tt432.eyelib.api.bedrock.animation.LoopType;
import io.github.tt432.eyelib.api.bedrock.animation.PlayState;
import io.github.tt432.eyelib.api.bedrock.model.Bone;
import io.github.tt432.eyelib.api.sound.SoundPlayer;
import io.github.tt432.eyelib.common.bedrock.animation.builder.AnimationBuilder;
import io.github.tt432.eyelib.common.bedrock.animation.manager.AnimationData;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.BoneAnimation;
import io.github.tt432.eyelib.common.bedrock.animation.pojo.SingleAnimation;
import io.github.tt432.eyelib.common.bedrock.animation.util.AnimationPointQueue;
import io.github.tt432.eyelib.common.bedrock.animation.util.AnimationState;
import io.github.tt432.eyelib.common.bedrock.animation.util.BoneAnimationQueue;
import io.github.tt432.eyelib.molang.MolangParser;
import io.github.tt432.eyelib.util.BoneSnapshot;
import io.github.tt432.eyelib.util.math.easing.EasingType;
import it.unimi.dsi.fastutil.doubles.Double2DoubleFunction;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The type Animation controller.
 *
 * @param <T> the type parameter
 */
@Slf4j
public class AnimationController<T extends Animatable> {
    static List<ModelFetcher<?>> modelFetchers = new ObjectArrayList<>();
    /**
     * The Entity.
     */
    protected T animatable;
    /**
     * The animation predicate, is tested in every process call (i.e. every frame)
     */
    protected IAnimationPredicate<T> animationPredicate;

    SoundControl soundControl = new SoundControl();
    ParticleControl particleControl = new ParticleControl();

    /**
     * The name of the animation controller
     */
    private final String name;

    protected AnimationState animationState = AnimationState.STOPPED;

    /**
     * How long it takes to transition between animations
     */
    public double transitionLengthTicks;

    public boolean isJustStarting = false;

    public static void addModelFetcher(ModelFetcher<?> fetcher) {
        modelFetchers.add(fetcher);
    }

    public static void removeModelFetcher(ModelFetcher<?> fetcher) {
        Objects.requireNonNull(fetcher);
        modelFetchers.remove(fetcher);
    }

    /**
     * An AnimationPredicate is run every render frame for ever AnimationController.
     * The "test" method is where you should change animations, stop animations,
     * restart, etc.
     */
    @FunctionalInterface
    public interface IAnimationPredicate<P extends Animatable> {
        /**
         * An AnimationPredicate is run every render frame for ever AnimationController.
         * The "test" method is where you should change animations, stop animations,
         * restart, etc.
         *
         * @return CONTINUE if the animation should continue, STOP if it should stop.
         */
        PlayState test(AnimationEvent<P> event);
    }

    private final HashMap<String, BoneAnimationQueue> boneAnimationQueues = new HashMap<>();
    public double tickOffset;
    protected Queue<SingleAnimation> animationQueue = new LinkedList<>();
    protected SingleAnimation currentAnimation;
    protected AnimationBuilder currentAnimationBuilder = new AnimationBuilder();
    protected boolean shouldResetTick = false;
    private final HashMap<String, BoneSnapshot> boneSnapshots = new HashMap<>();
    private boolean justStopped = false;
    protected boolean justStartedTransition = false;
    public Double2DoubleFunction customEasingMethod;
    protected boolean needsAnimationReload = false;
    public double animationSpeed = 1D;

    /**
     * This method sets the current animation with an animation builder. You can run
     * this method every frame, if you pass in the same animation builder every
     * time, it won't restart. Additionally, it smoothly transitions between
     * animation states.
     */
    public void setAnimation(AnimationBuilder builder) {
        AnimatableModel<T> model = getModel(this.animatable);
        if (model != null) {
            if (builder == null || builder.getRawAnimationList().isEmpty()) {
                this.animationState = AnimationState.STOPPED;
            } else if (!builder.getRawAnimationList().equals(this.currentAnimationBuilder.getRawAnimationList())
                    || this.needsAnimationReload) {
                AtomicBoolean encounteredError = new AtomicBoolean(false);
                // Convert the list of animation names to the actual list, keeping track of the
                // loop boolean along the way
                LinkedList<SingleAnimation> animations = builder.getRawAnimationList().stream().map(rawAnimation -> {
                    SingleAnimation animation = model.getAnimation(rawAnimation.animationName, animatable);

                    if (animation == null) {
                        log.error("Could not load animation: {}. Is it missing?", rawAnimation.animationName);
                        encounteredError.set(true);
                    }

                    if (animation != null && rawAnimation.loopType != null)
                        animation.setLoop(rawAnimation.loopType);

                    return animation;
                }).collect(Collectors.toCollection(LinkedList::new));

                if (encounteredError.get())
                    return;

                this.animationQueue = animations;
                this.currentAnimationBuilder = builder;
                this.shouldResetTick = true; // Reset the adjusted tick to 0 on next animation process call
                this.animationState = AnimationState.TRANSITIONING;
                this.justStartedTransition = true;
                this.needsAnimationReload = false;
            }
        }
    }

    /**
     * By default Geckolib uses the easing types of every keyframe. If you want to
     * override that for an entire AnimationController, change this value.
     */
    public EasingType easingType = EasingType.NONE;

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
    public AnimationController(T animatable, String name, float transitionLengthTicks,
                               IAnimationPredicate<T> animationPredicate) {
        this.animatable = animatable;
        this.name = name;
        this.transitionLengthTicks = transitionLengthTicks;
        this.animationPredicate = animationPredicate;
        this.tickOffset = 0.0d;
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
     * @param easingtype            The method of easing to use. The other
     *                              constructor defaults to no easing.
     */
    public AnimationController(T animatable, String name, float transitionLengthTicks, EasingType easingtype,
                               IAnimationPredicate<T> animationPredicate) {
        this.animatable = animatable;
        this.name = name;
        this.transitionLengthTicks = transitionLengthTicks;
        this.easingType = easingtype;
        this.animationPredicate = animationPredicate;
        this.tickOffset = 0.0d;
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
     * @param customEasingMethod    If you want to use an easing method that's not
     *                              included in the EasingType enum, pass your
     *                              method into here. The parameter that's passed in
     *                              will be a number between 0 and 1. Return a
     *                              number also within 0 and 1. Take a look at
     */
    public AnimationController(T animatable, String name, float transitionLengthTicks,
                               Double2DoubleFunction customEasingMethod, IAnimationPredicate<T> animationPredicate) {
        this.animatable = animatable;
        this.name = name;
        this.transitionLengthTicks = transitionLengthTicks;
        this.customEasingMethod = customEasingMethod;
        this.easingType = EasingType.CUSTOM;
        this.animationPredicate = animationPredicate;
        this.tickOffset = 0.0d;
    }

    /**
     * Gets the controller's name.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the current animation. Can be null
     *
     * @return the current animation
     */

    public SingleAnimation getCurrentAnimation() {
        return this.currentAnimation;
    }

    /**
     * Returns the current state of this animation controller.
     */
    public AnimationState getAnimationState() {
        return this.animationState;
    }

    /**
     * Gets the current animation's bone animation queues.
     *
     * @return the bone animation queues
     */
    public Map<String, BoneAnimationQueue> getBoneAnimationQueues() {
        return this.boneAnimationQueues;
    }

    /**
     * This method is called every frame in order to populate the animation point
     * queues, and process animation state logic.
     *
     * @param tick                   The current tick + partial tick
     * @param event                  The animation test event
     * @param modelRendererList      The list of all AnimatedModelRender's
     */
    public void process(final double tick, AnimationEvent<T> event, List<Bone> modelRendererList, MolangParser parser,
                        boolean crashWhenCantFindBone) {
        SoundPlayer player = getSoundPlayer(event);

        parser.setValue("query.life_time", () -> tick / 20);

        if (this.currentAnimation != null) {
            AnimatableModel<T> model = getModel(this.animatable);

            if (model != null) {
                SingleAnimation animation = model.getAnimation(currentAnimation.getAnimationName(), this.animatable);

                if (animation != null) {
                    LoopType loop = this.currentAnimation.getLoop();
                    this.currentAnimation = animation;
                    this.currentAnimation.setLoop(loop);
                }
            }
        }

        createInitialQueues(modelRendererList);

        double adjustedTick = adjustTick(tick);

        // Transition period has ended, reset the tick and set the animation to running
        if (animationState == AnimationState.TRANSITIONING && adjustedTick >= this.transitionLengthTicks) {
            this.shouldResetTick = true;
            this.animationState = AnimationState.RUNNING;
            adjustedTick = adjustTick(tick);
        }

        assert adjustedTick >= 0 : "GeckoLib: Tick was less than zero";

        // This tests the animation predicate
        PlayState playState = this.testAnimationPredicate(event);

        if (playState == PlayState.STOP || (this.currentAnimation == null && this.animationQueue.isEmpty())) {
            // The animation should transition to the model's initial state
            this.animationState = AnimationState.STOPPED;
            this.justStopped = true;

            soundControl.stop(player);
            particleControl.stop();

            return;
        }

        if (this.justStartedTransition && (this.shouldResetTick || this.justStopped)) {
            this.justStopped = false;
            adjustedTick = adjustTick(tick);
        } else if (this.currentAnimation == null) {
            this.shouldResetTick = true;
            this.animationState = AnimationState.TRANSITIONING;
            this.justStartedTransition = true;
            this.needsAnimationReload = false;
            adjustedTick = adjustTick(tick);
        } else if (this.animationState != AnimationState.TRANSITIONING) {
            this.animationState = AnimationState.RUNNING;
        }

        // Handle transitioning to a different animation (or just starting one)
        if (this.animationState == AnimationState.TRANSITIONING) {
            // Just started transitioning, so set the current animation to the first one
            if (adjustedTick == 0 || this.isJustStarting) {
                this.justStartedTransition = false;
                nextAnimation(player);
            }

            if (this.currentAnimation != null) {
                setAnimTime(parser, 0);

                processCurrentAnimation(adjustedTick, tick, parser, crashWhenCantFindBone, event);
            }
        } else if (getAnimationState() == AnimationState.RUNNING) {
            // Actually run the animation
            processCurrentAnimation(adjustedTick, tick, parser, crashWhenCantFindBone, event);
        }
    }

    private void setAnimTime(MolangParser parser, final double tick) {
        parser.setValue("query.anim_time", () -> tick / 20);
    }

    private AnimatableModel<T> getModel(T animatable) {
        for (ModelFetcher<?> modelFetcher : modelFetchers) {
            AnimatableModel<T> model = (AnimatableModel<T>) modelFetcher.apply(animatable);

            if (model != null)
                return model;
        }

        log.error("Could not find suitable model for animatable of type {}. Did you register a Model Fetcher?%n",
                animatable.getClass());

        return null;
    }

    protected PlayState testAnimationPredicate(AnimationEvent<T> event) {
        return this.animationPredicate.test(event);
    }

    private SoundPlayer getSoundPlayer(AnimationEvent<T> event) {
       return event.getAnimatable() instanceof SoundPlayer sp ? sp : null;
    }

    private void processCurrentAnimation(double tick, double actualTick, MolangParser parser,
                                         boolean crashWhenCantFindBone, AnimationEvent<T> event) {
        SoundPlayer player = getSoundPlayer(event);

        assert currentAnimation != null;
        // Animation has ended
        if (tick >= this.currentAnimation.getAnimationLength()) {
            // If the current animation is set to loop, keep it as the current animation and
            // just start over
            if (!this.currentAnimation.getLoop().isRepeatingAfterEnd()) {
                // Pull the next animation from the queue
                SingleAnimation peek = this.animationQueue.peek();

                if (peek == null) {
                    // No more animations left, stop the animation controller
                    this.animationState = AnimationState.STOPPED;
                    soundControl.stop(player);
                    particleControl.stop();

                    return;
                } else {
                    // Otherwise, set the state to transitioning and start transitioning to the next
                    // animation next frame
                    this.animationState = AnimationState.TRANSITIONING;
                    this.shouldResetTick = true;

                    nextAnimation(player);
                }
            } else {
                // Reset the adjusted tick so the next animation starts at tick 0
                this.shouldResetTick = true;
                tick = adjustTick(actualTick);
            }
        }

        setAnimTime(parser, tick);

        // Loop through every boneanimation in the current animation and process the values
        processBoneAnimation(currentAnimation.getBones(), tick, crashWhenCantFindBone);

        soundControl.processSoundEffect(player, tick);
        Entity entity = MolangParser.getCurrentDataSource().get(Entity.class);
        if (entity != null)
            particleControl.process(entity, tick);

        if (this.transitionLengthTicks == 0 && shouldResetTick && this.animationState == AnimationState.TRANSITIONING)
            nextAnimation(player);
    }

    private void processBoneAnimation(@Nullable Map<String, BoneAnimation> boneAnimations, double tick, boolean crashWhenCantFindBone) {
        if (boneAnimations != null) {
            for (var entry : boneAnimations.entrySet()) {
                var boneName = entry.getKey();
                BoneAnimation boneAnim = entry.getValue();

                BoneAnimationQueue boneAnimationQueue = boneAnimationQueues.get(boneName);

                if (boneAnimationQueue == null) {
                    if (crashWhenCantFindBone)
                        throw new RuntimeException("Could not find bone: " + boneName);

                    continue;
                }

                AnimationData data = MolangParser.getCurrentDataSource().getData();

                if (data != null)
                    data.putExtraData("anim.current_bone", boneName);

                boneAnimationQueue.rotate().push(new AnimationPointQueue.LerpInfo(boneAnim.lerpRotation(tick)));
                boneAnimationQueue.position().push(new AnimationPointQueue.LerpInfo(boneAnim.lerpPosition(tick)));
                boneAnimationQueue.scale().push(new AnimationPointQueue.LerpInfo(boneAnim.lerpScale(tick)));

                if (data != null)
                    data.removeExtraData("anim.current_bone");
            }
        }
    }

    private void nextAnimation(@Nullable SoundPlayer player) {
        if (currentAnimation != null) {
            if (player != null) {
                soundControl.stop(player);
            }

            particleControl.stop();
        }

        currentAnimation = animationQueue.poll();

        if (player != null) {
            soundControl.init(currentAnimation, player);
        }

        particleControl.init(currentAnimation);
    }

    // Helper method to populate all the initial animation point queues
    private void createInitialQueues(List<Bone> modelRendererList) {
        this.boneAnimationQueues.clear();

        for (Bone modelRenderer : modelRendererList) {
            this.boneAnimationQueues.put(modelRenderer.getName(), new BoneAnimationQueue(modelRenderer));
        }
    }

    // Used to reset the "tick" everytime a new animation starts, a transition
    // starts, or something else of importance happens
    protected double adjustTick(double tick) {
        if (this.shouldResetTick) {
            if (getAnimationState() == AnimationState.TRANSITIONING) {
                this.tickOffset = tick;
            } else if (getAnimationState() == AnimationState.RUNNING) {
                this.tickOffset = tick;
            }

            this.shouldResetTick = false;

            return 0;
        } else {
            // assert tick - this.tickOffset >= 0;
            return this.animationSpeed * Math.max(tick - this.tickOffset, 0.0D);
        }
    }

    public void markNeedsReload() {
        this.needsAnimationReload = true;
    }

    public void clearAnimationCache() {
        this.currentAnimationBuilder = new AnimationBuilder();
    }

    public double getAnimationSpeed() {
        return this.animationSpeed;
    }

    public void setAnimationSpeed(double animationSpeed) {
        this.animationSpeed = animationSpeed;
    }

    @FunctionalInterface
    public interface ModelFetcher<T extends Animatable> extends Function<Animatable, AnimatableModel<T>> {
    }
}