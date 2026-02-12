package io.github.tt432.eyelib.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.client.model.locator.GroupLocator;
import io.github.tt432.eyelib.client.model.locator.LocatorEntry;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitContext;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitor;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import lombok.AllArgsConstructor;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * @author TT432
 */
public record DFSModel(
        ReferenceList<Frame<?>> frames
) {
    public <B extends Model.Bone<B>> void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, ModelRuntimeData<B> infos, StateMachine stateMachine) {
        frames.forEach(frame -> frame.visit(params, context, visitor, cast(infos), stateMachine));
    }

    public static class StateMachine {
        public boolean render;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    public static <B extends Model.Bone<B>> DFSModel create(Model<B> model) {
        ReferenceList<Frame<?>> frames = new ReferenceArrayList<>();

        new ModelVisitor() {
            @Override
            public <B extends Model.Bone<B>> void visitPreModel(RenderParams params, ModelVisitContext context, ModelRuntimeData<B> infos, Model<B> model) {
                frames.add(new PreModelFrame<>(model));
            }

            @Override
            public <B extends Model.Bone<B>> void visitPostModel(RenderParams params, ModelVisitContext context, ModelRuntimeData<B> infos, Model<B> model) {
                frames.add(new PostModelFrame<>(model));
            }

            @Override
            public <B extends Model.Bone<B>> void visitPreBone(RenderParams renderParams, ModelVisitContext context, B bone, ModelRuntimeData<B> data, GroupLocator groupLocator) {
                frames.add(new PreBoneFrame<>(bone, groupLocator));
            }

            @Override
            public <B extends Model.Bone<B>> void visitPostBone(RenderParams renderParams, ModelVisitContext context, B group, ModelRuntimeData<B> data, GroupLocator groupLocator) {
                frames.add(new PostBoneFrame<>(group, groupLocator));
            }

            @Override
            public void visitCube(RenderParams renderParams, ModelVisitContext context, Model.Cube cube) {
                frames.add(new CubeFrame<>(cube));
            }

            @Override
            public <B extends Model.Bone<B>> void visitLocator(RenderParams renderParams, ModelVisitContext context, B bone, LocatorEntry locator, ModelRuntimeData<B> data) {
                frames.add(new LocatorFrame<>(bone, locator));
            }
        }.visitModel(new RenderParams(
                null, new PoseStack.Pose(new Matrix4f(), new Matrix3f()), new PoseStack(),
                null, null, false, null, 0,
                OverlayTexture.NO_OVERLAY, new Int2BooleanOpenHashMap()
        ), new ModelVisitContext(), model.data(), model);

        return new DFSModel(frames);
    }

    public interface Frame<B extends Model.Bone<B>> {
        void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, ModelRuntimeData<B> infos, StateMachine stateMachine);
    }

    @AllArgsConstructor
    public static final class PreModelFrame<B extends Model.Bone<B>> implements Frame<B> {
        private final Model<B> model;

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, ModelRuntimeData<B> infos, StateMachine stateMachine) {
            visitor.visitPreModel(params, context, infos, model);
        }
    }

    @AllArgsConstructor
    public static final class PostModelFrame<B extends Model.Bone<B>> implements Frame<B> {
        private final Model<B> model;

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, ModelRuntimeData<B> infos, StateMachine stateMachine) {
            visitor.visitPostModel(params, context, infos, model);
        }
    }

    @AllArgsConstructor
    public static final class PreBoneFrame<B extends Model.Bone<B>> implements Frame<B> {
        private final B group;
        private final GroupLocator groupLocator;

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, ModelRuntimeData<B> infos, StateMachine stateMachine) {
            visitor.visitPreBone(params, context, group, infos, groupLocator);

            stateMachine.render = params.partVisibility().getOrDefault(group.id(), true);
        }
    }

    @AllArgsConstructor
    public static final class PostBoneFrame<B extends Model.Bone<B>> implements Frame<B> {
        private final B group;
        private final GroupLocator groupLocator;

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, ModelRuntimeData<B> infos, StateMachine stateMachine) {
            stateMachine.render = false;
            visitor.visitPostBone(params, context, group, infos, groupLocator);
        }
    }

    @AllArgsConstructor
    public static final class LocatorFrame<B extends Model.Bone<B>> implements Frame<B> {
        private final B group;
        private final LocatorEntry locator;

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, ModelRuntimeData<B> infos, StateMachine stateMachine) {
            visitor.visitLocator(params, context, group, locator, infos);
        }
    }

    @AllArgsConstructor
    public static final class CubeFrame<B extends Model.Bone<B>> implements Frame<B> {
        private final Model.Cube cube;

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, ModelRuntimeData<B> infos, StateMachine stateMachine) {
            if (stateMachine.render) {
                visitor.visitCube(params, context, cube);
            }
        }
    }
}
