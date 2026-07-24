package io.github.tt432.eyelib.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.visitor.ModelVisitor;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.ModelVisitContext;
import io.github.tt432.eyelib.model.locator.GroupLocator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OPT-R1（render-scout-report）裁决的可执行证据：DFSModel 的帧序列由基类递归
 * {@code ModelVisitor.visitBone} 生成，PreBone/PostBone 严格嵌套配对，
 * 因此播放时子骨骼 PreBoneFrame pushPose 拿到的栈顶仍含父骨骼变换。
 *
 * <p>测试骨骼树（position 均为 bind pose 偏移，pivot/rotation 为 0，scale 为 1）：
 * <pre>
 *   A(id=1001, pos=(1,0,0), 1 cube)        D(id=1004, pos=(0,0,3), 1 cube)   ← 两个顶层骨骼
 *   └─ B(id=1002, pos=(0,2,0), 1 cube)
 *      └─ C(id=1003, pos=(0,0,5), 0 cube)
 * </pre>
 * 模型根部有 {@code rotateY(180°)}，故探针点期望值为 R180·(累积平移)。
 */
class DFSModelTest {
    private static final int A = 1001;
    private static final int B = 1002;
    private static final int C = 1003;
    private static final int D = 1004;

    private static final float DELTA = 1e-4f;

    private static Model.Bone bone(int id, int parent, Vector3fc position, int cubeCount) {
        List<Model.Cube> cubes = new ArrayList<>();
        for (int i = 0; i < cubeCount; i++) {
            cubes.add(new Model.Cube(List.of()));
        }
        return Model.Bone.of(id, parent,
                new Vector3f(0), new Vector3f(0), position, new Vector3f(1),
                null, new Int2ObjectOpenHashMap<>(), cubes,
                new GroupLocator(new Int2ObjectOpenHashMap<>(), List.of()));
    }

    private static Model treeModel() {
        Int2ObjectMap<Model.Bone> all = new Int2ObjectOpenHashMap<>();
        all.put(A, bone(A, -1, new Vector3f(1, 0, 0), 1));
        all.put(B, bone(B, A, new Vector3f(0, 2, 0), 1));
        all.put(C, bone(C, B, new Vector3f(0, 0, 5), 0));
        all.put(D, bone(D, -1, new Vector3f(0, 0, 3), 1));
        return Model.of("dfs-test", all);
    }

    /** 只记录帧序列，不调 super（不触碰 PoseStack）。 */
    private static final class TraceVisitor extends ModelVisitor {
        final List<String> trace = new ArrayList<>();

        @Override
        public void visitPreModel(RenderParams params, ModelVisitContext context, ModelRuntimeData infos, Model model) {
            trace.add("PreModel");
        }

        @Override
        public void visitPostModel(RenderParams params, ModelVisitContext context, ModelRuntimeData infos, Model model) {
            trace.add("PostModel");
        }

        @Override
        public void visitPreBone(RenderParams renderParams, ModelVisitContext context, Model.Bone bone, ModelRuntimeData data) {
            trace.add("PreBone:" + bone.id());
        }

        @Override
        public void visitPostBone(RenderParams renderParams, ModelVisitContext context, Model.Bone bone, ModelRuntimeData data) {
            trace.add("PostBone:" + bone.id());
        }

        @Override
        public void visitCube(RenderParams renderParams, ModelVisitContext context, Model.Cube cube) {
            trace.add("Cube");
        }
    }

    /**
     * 调 super 走真实 pushPose/popPose + applyBoneTranslate，记录每个 cube 处
     * 所属骨骼、当时的 pose 矩阵副本与 push/pop 计数器深度。
     */
    private static final class PoseRecordingVisitor extends ModelVisitor {
        final List<Integer> cubeBone = new ArrayList<>();
        final List<Matrix4f> cubePose = new ArrayList<>();
        final List<Integer> cubeDepth = new ArrayList<>();
        final Int2ObjectMap<Matrix4f> poseByBone = new Int2ObjectOpenHashMap<>();
        final it.unimi.dsi.fastutil.ints.Int2IntMap depthByBone = new it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap();
        int depth;
        private int currentBone = -1;

        @Override
        public void visitPreModel(RenderParams params, ModelVisitContext context, ModelRuntimeData infos, Model model) {
            super.visitPreModel(params, context, infos, model);
            depth++;
        }

        @Override
        public void visitPostModel(RenderParams params, ModelVisitContext context, ModelRuntimeData infos, Model model) {
            super.visitPostModel(params, context, infos, model);
            depth--;
        }

        @Override
        public void visitPreBone(RenderParams renderParams, ModelVisitContext context, Model.Bone bone, ModelRuntimeData data) {
            super.visitPreBone(renderParams, context, bone, data);
            depth++;
            currentBone = bone.id();
        }

        @Override
        public void visitPostBone(RenderParams renderParams, ModelVisitContext context, Model.Bone bone, ModelRuntimeData data) {
            super.visitPostBone(renderParams, context, bone, data);
            depth--;
            currentBone = -1;
        }

        @Override
        public void visitCube(RenderParams renderParams, ModelVisitContext context, Model.Cube cube) {
            cubeBone.add(currentBone);
            Matrix4f pose = new Matrix4f(renderParams.poseStack().last().pose());
            cubePose.add(pose);
            cubeDepth.add(depth);
            poseByBone.put(currentBone, pose);
            depthByBone.put(currentBone, depth);
        }
    }

    private static Vector3f probe(Matrix4f pose) {
        return pose.transformPosition(new Vector3f(0, 0, 0));
    }

    private static void assertProbe(Vector3f actual, float x, float y, float z) {
        assertEquals(x, actual.x, DELTA, "probe.x");
        assertEquals(y, actual.y, DELTA, "probe.y");
        assertEquals(z, actual.z, DELTA, "probe.z");
    }

    @Test
    void frameSequenceIsStrictlyNestedPrePostPairs() {
        DFSModel dfsModel = DFSModel.create(treeModel());
        TraceVisitor visitor = new TraceVisitor();
        dfsModel.visit(RenderParams.noRender(), new ModelVisitContext(), visitor,
                new ModelRuntimeData(), new DFSModel.StateMachine());

        List<String> trace = visitor.trace;
        assertEquals("PreModel", trace.get(0));
        assertEquals("PostModel", trace.get(trace.size() - 1));

        // 括号配对模拟：每个 PostBone 必须闭合最近未闭合的 PreBone，且 id 相同。
        // 侦察报告声称的 "Pre(parent) → Post(parent) → Pre(child)" 序列在此必然失败。
        Deque<Integer> open = new ArrayDeque<>();
        int subtreeStart = -1;
        int subtreeEnd = -1;
        for (int i = 0; i < trace.size(); i++) {
            String event = trace.get(i);
            if (event.startsWith("PreBone:")) {
                int id = Integer.parseInt(event.substring("PreBone:".length()));
                open.push(id);
                if (id == A) {
                    subtreeStart = i;
                }
            } else if (event.startsWith("PostBone:")) {
                int id = Integer.parseInt(event.substring("PostBone:".length()));
                assertFalse(open.isEmpty(), "PostBone:" + id + " 没有可配对的 PreBone");
                assertEquals(id, open.pop(), "PostBone:" + id + " 闭合了错误的骨骼（嵌套被打断）");
                if (id == A) {
                    subtreeEnd = i;
                }
            }
        }
        assertTrue(open.isEmpty(), "存在未闭合的 PreBone");

        // A 的子树区间必须与递归遍历的期望序列完全一致（B、C 嵌套在 A 内部）。
        assertTrue(subtreeStart >= 0 && subtreeEnd > subtreeStart, "未找到 A 的子树区间");
        assertEquals(
                List.of("PreBone:" + A, "Cube",
                        "PreBone:" + B, "Cube",
                        "PreBone:" + C, "PostBone:" + C,
                        "PostBone:" + B, "PostBone:" + A),
                trace.subList(subtreeStart, subtreeEnd + 1));
    }

    @Test
    void childBoneInheritsParentTransformAndStackDepthBalances() {
        DFSModel dfsModel = DFSModel.create(treeModel());
        PoseRecordingVisitor visitor = new PoseRecordingVisitor();
        dfsModel.visit(RenderParams.noRender(), new ModelVisitContext(), visitor,
                new ModelRuntimeData(), new DFSModel.StateMachine());

        // push/pop 完全平衡：遍历结束后计数器归零。
        assertEquals(0, visitor.depth, "visit 结束后 push/pop 不平衡");
        // 三个带 cube 的骨骼都被访问（顶层骨骼迭代顺序不定，按 id 断言）。
        assertEquals(3, visitor.cubeBone.size());
        assertEquals(java.util.Set.of(A, B, D), new java.util.HashSet<>(visitor.cubeBone));

        // 深度：model 推 1 层；A、D 的 cube 在深度 2，B 的 cube 在深度 3（A 未弹栈）。
        assertEquals(2, visitor.depthByBone.get(A));
        assertEquals(3, visitor.depthByBone.get(B));
        assertEquals(2, visitor.depthByBone.get(D));

        // 数值证据（模型根部 R180 = rotateY(π)，(x,y,z) → (−x,y,−z)）：
        // A 的 cube：R180·T(1,0,0) → (−1,0,0)
        assertProbe(probe(visitor.poseByBone.get(A)), -1f, 0f, 0f);
        // B 的 cube：R180·T(1,0,0)·T(0,2,0) → (−1,2,0) —— 含父骨骼 A 的平移。
        // 若侦察报告成立（栈顶丢失父变换），此处会是 R180·T(0,2,0) → (0,2,0)。
        assertProbe(probe(visitor.poseByBone.get(B)), -1f, 2f, 0f);
        // 兄弟顶层骨骼 D 不受 A 子树影响：R180·T(0,0,3) → (0,0,−3)
        assertProbe(probe(visitor.poseByBone.get(D)), 0f, 0f, -3f);
    }

    @Test
    void secondVisitWithCachedBonesKeepsInheritedTransform() {
        // applyBoneTranslate 第二次访问同一 context 时走 "bones" 缓存的 replaceLast 分支，
        // 缓存姿势必须仍包含父骨骼变换。
        DFSModel dfsModel = DFSModel.create(treeModel());
        ModelVisitContext context = new ModelVisitContext();
        ModelRuntimeData data = new ModelRuntimeData();

        PoseRecordingVisitor first = new PoseRecordingVisitor();
        dfsModel.visit(RenderParams.noRender(), context, first, data, new DFSModel.StateMachine());

        PoseRecordingVisitor second = new PoseRecordingVisitor();
        dfsModel.visit(RenderParams.noRender(), context, second, data, new DFSModel.StateMachine());

        assertEquals(first.cubeBone, second.cubeBone);
        assertEquals(first.cubePose.size(), second.cubePose.size());
        assertEquals(first.poseByBone.keySet(), second.poseByBone.keySet());
        for (int boneId : first.poseByBone.keySet()) {
            Vector3f expected = probe(first.poseByBone.get(boneId));
            assertProbe(probe(second.poseByBone.get(boneId)), expected.x, expected.y, expected.z);
        }
        assertEquals(0, second.depth);
    }
}
