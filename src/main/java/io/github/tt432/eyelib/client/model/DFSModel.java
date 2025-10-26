package io.github.tt432.eyelib.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.client.model.transformer.ModelTransformer;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitContext;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitor;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.AllArgsConstructor;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * @author TT432
 */
public record DFSModel(
        List<Frame<?>> frames
) {
    public <D extends ModelRuntimeData<? extends Model.Bone, ?, D>> void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, D infos, StateMachine stateMachine) {
        for (int i = 0; i < frames.size(); i++) {
            frames.get(i).visit(params, context, visitor, cast(infos), stateMachine);
        }
    }

    public static class StateMachine {
        public boolean render;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    public static DFSModel create(Model model) {
        List<Frame<?>> frames = new ArrayList<>();
        DFSModel result = new DFSModel(frames);

        new ModelVisitor() {
            @Override
            public <D extends ModelRuntimeData<Model.Bone, ?, D>> void visitPreModel(RenderParams params, ModelVisitContext context, D infos, Model model) {
                frames.add(new PreModelFrame<>(model));
            }

            @Override
            public <D extends ModelRuntimeData<Model.Bone, ?, D>> void visitPostModel(RenderParams params, ModelVisitContext context, D infos, Model model) {
                frames.add(new PostModelFrame<>(model));
            }

            @Override
            public <D extends ModelRuntimeData<Model.Bone, ?, D>> void visitPreBone(RenderParams renderParams, ModelVisitContext context, Model.Bone group, D data, GroupLocator groupLocator, ModelTransformer<Model.Bone, D> transformer) {
                frames.add(new PreBoneFrame<>(group, groupLocator, transformer));
            }

            @Override
            public <D extends ModelRuntimeData<Model.Bone, ?, D>> void visitPostBone(RenderParams renderParams, ModelVisitContext context, Model.Bone group, D data, GroupLocator groupLocator, ModelTransformer<Model.Bone, D> transformer) {
                frames.add(new PostBoneFrame<>(group, groupLocator, transformer));
            }

            @Override
            public void visitCube(RenderParams renderParams, ModelVisitContext context, Model.Cube cube) {
                frames.add(new CubeFrame<>(cube));
            }

            @Override
            public <R extends ModelRuntimeData<Model.Bone, ?, R>> void visitLocator(RenderParams renderParams, ModelVisitContext context, Model.Bone bone, LocatorEntry locator, R data, ModelTransformer<Model.Bone, R> transformer) {
                frames.add(new LocatorFrame<>(bone, locator, cast(transformer)));
            }
        }.visitModel(new RenderParams(
                null, new PoseStack.Pose(new Matrix4f(), new Matrix3f()), new PoseStack(),
                null, null, false, null, 0,
                OverlayTexture.NO_OVERLAY, new Object2BooleanOpenHashMap<>()
        ), new ModelVisitContext(), cast(model.data()), model);

        return result;
    }

    public interface Frame<D extends ModelRuntimeData<Model.Bone, ?, D>> {
        void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, D infos, StateMachine stateMachine);
    }

    @AllArgsConstructor
    public static final class PreModelFrame<D extends ModelRuntimeData<Model.Bone, ?, D>> implements Frame<D> {
        private final Model model;

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, D infos, StateMachine stateMachine) {
            visitor.visitPreModel(params, context, infos, model);
        }
    }

    @AllArgsConstructor
    public static final class PostModelFrame<D extends ModelRuntimeData<Model.Bone, ?, D>> implements Frame<D> {
        private final Model model;

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, D infos, StateMachine stateMachine) {
            visitor.visitPostModel(params, context, infos, model);
        }
    }

    @AllArgsConstructor
    public static final class PreBoneFrame<D extends ModelRuntimeData<Model.Bone, ?, D>> implements Frame<D> {
        private final Model.Bone group;
        private final GroupLocator groupLocator;
        private final ModelTransformer<Model.Bone, D> transformer;

        private static final Object2ObjectOpenHashMap<String, Object2BooleanOpenHashMap<String>> partVisibilityCache = new Object2ObjectOpenHashMap<>();

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, D infos, StateMachine stateMachine) {
            visitor.visitPreBone(params, context, group, infos, groupLocator, transformer);

            AtomicBoolean render = new AtomicBoolean(false);
            params.partVisibility().forEach((k, v) ->
                    render.set(partVisibilityCache.computeIfAbsent(k, __ -> new Object2BooleanOpenHashMap<>())
                            .computeIfAbsent(group.name(), ___ -> !Pattern.compile(k.replace("*", ".*")).matcher(group.name()).matches() || v)));
            stateMachine.render = render.get();
        }
    }

    @AllArgsConstructor
    public static final class PostBoneFrame<D extends ModelRuntimeData<Model.Bone, ?, D>> implements Frame<D> {
        private final Model.Bone group;
        private final GroupLocator groupLocator;
        private final ModelTransformer<Model.Bone, D> transformer;

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, D infos, StateMachine stateMachine) {
            stateMachine.render = false;
            visitor.visitPostBone(params, context, group, infos, groupLocator, transformer);
        }
    }

    @AllArgsConstructor
    public static final class LocatorFrame<D extends ModelRuntimeData<Model.Bone, ?, D>> implements Frame<D> {
        private final Model.Bone group;
        private final LocatorEntry locator;
        private final ModelTransformer<Model.Bone, D> transformer;

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, D infos, StateMachine stateMachine) {
            visitor.visitLocator(params, context, group, locator, infos, transformer);
        }
    }

    @AllArgsConstructor
    public static final class CubeFrame<D extends ModelRuntimeData<Model.Bone, ?, D>> implements Frame<D> {
        private final Model.Cube cube;

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, D infos, StateMachine stateMachine) {
            if (stateMachine.render) {
                visitor.visitCube(params, context, cube);
            }
        }
    }
}
