package io.github.tt432.eyelib.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
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
        ReferenceList<Frame> frames
) {
    public  void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, ModelRuntimeData infos, StateMachine stateMachine) {
        frames.forEach(frame -> frame.visit(params, context, visitor, cast(infos), stateMachine));
    }

    public static class StateMachine {
        public boolean render;
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object o) {
        return (T) o;
    }

    public static  DFSModel create(Model model) {
        ReferenceList<Frame> frames = new ReferenceArrayList<>();

        new ModelVisitor() {
            @Override
            public void visitPreModel(RenderParams params, ModelVisitContext context, ModelRuntimeData infos, Model model) {
                frames.add(new PreModelFrame(model));
            }

            @Override
            public void visitPostModel(RenderParams params, ModelVisitContext context, ModelRuntimeData infos, Model model) {
                frames.add(new PostModelFrame(model));
            }

            @Override
            public void visitPreBone(RenderParams renderParams, ModelVisitContext context, Model.Bone bone, ModelRuntimeData data) {
                frames.add(new PreBoneFrame(bone));
            }

            @Override
            public void visitPostBone(RenderParams renderParams, ModelVisitContext context, Model.Bone group, ModelRuntimeData data) {
                frames.add(new PostBoneFrame(group));
            }

            @Override
            public void visitCube(RenderParams renderParams, ModelVisitContext context, Model.Cube cube) {
                frames.add(new CubeFrame(cube));
            }

            @Override
            public void visitLocator(RenderParams renderParams, ModelVisitContext context, Model.Bone bone, LocatorEntry locator, ModelRuntimeData data) {
                frames.add(new LocatorFrame(bone, locator));
            }
        }.visitModel(new RenderParams(
                null, new PoseStack.Pose(new Matrix4f(), new Matrix3f()), new PoseStack(),
                null, null, false, null, 0,
                OverlayTexture.NO_OVERLAY, new Int2BooleanOpenHashMap()
        ), new ModelVisitContext(), new ModelRuntimeData(), model);

        return new DFSModel(frames);
    }

    public interface Frame {
        void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, ModelRuntimeData infos, StateMachine stateMachine);
    }

    @AllArgsConstructor
    public static final class PreModelFrame implements Frame {
        private final Model model;

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, ModelRuntimeData infos, StateMachine stateMachine) {
            visitor.visitPreModel(params, context, infos, model);
        }
    }

    @AllArgsConstructor
    public static final class PostModelFrame implements Frame {
        private final Model model;

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, ModelRuntimeData infos, StateMachine stateMachine) {
            visitor.visitPostModel(params, context, infos, model);
        }
    }

    @AllArgsConstructor
    public static final class PreBoneFrame implements Frame {
        private final Model.Bone group;

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, ModelRuntimeData infos, StateMachine stateMachine) {
            visitor.visitPreBone(params, context, group, infos);

            stateMachine.render = params.partVisibility().getOrDefault(group.id(), true);
        }
    }

    @AllArgsConstructor
    public static final class PostBoneFrame implements Frame {
        private final Model.Bone group;

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, ModelRuntimeData infos, StateMachine stateMachine) {
            stateMachine.render = false;
            visitor.visitPostBone(params, context, group, infos);
        }
    }

    @AllArgsConstructor
    public static final class LocatorFrame implements Frame {
        private final Model.Bone group;
        private final LocatorEntry locator;

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, ModelRuntimeData infos, StateMachine stateMachine) {
            visitor.visitLocator(params, context, group, locator, infos);
        }
    }

    @AllArgsConstructor
    public static final class CubeFrame implements Frame {
        private final Model.Cube cube;

        @Override
        public void visit(RenderParams params, ModelVisitContext context, ModelVisitor visitor, ModelRuntimeData infos, StateMachine stateMachine) {
            if (stateMachine.render) {
                visitor.visitCube(params, context, cube);
            }
        }
    }
}
