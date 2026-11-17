package io.github.tt432.eyelib.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.tt432.eyelib.animation.ModelRuntimeData;
import io.github.tt432.eyelib.bridge.client.render.RenderSink;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.locator.GroupLocator;
import io.github.tt432.eyelib.model.locator.LocatorEntry;
import io.github.tt432.eyelib.util.PortResourceLocation;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 外部挂载声明 API（G3）与渲染产物（G2）的契约：锚点解析语义、注册表生命周期、跳过原因。
 * <p>
 * 这些断言固定的是**对外承诺**（下游据此判断「库是否已画过某个槽位」「锚点缺失就不画」），
 * 不是实现细节——它们变化时必须按破坏性变更处理。
 *
 * @author TT432
 */
class AttachmentApiTest {

    private static int id(String name) {
        return GlobalBoneIdHandler.get(name);
    }

    private static Model modelOf(List<LocatorEntry> locators, String... boneNames) {
        Int2ObjectMap<Model.Bone> all = new Int2ObjectOpenHashMap<>();
        for (String name : boneNames) {
            int boneId = id(name);
            all.put(boneId, Model.Bone.of(boneId, -1,
                    new Vector3f(0), new Vector3f(0), new Vector3f(0), new Vector3f(1),
                    null, new Int2ObjectOpenHashMap<>(), List.of(),
                    new GroupLocator(new Int2ObjectOpenHashMap<>(), locators)));
        }
        return Model.of("attachment-test", all);
    }

    private static RenderFrame frame(Model model) {
        RenderData<Object> cap = new RenderData<>();
        RenderHost host = RenderHost.anchor(new Object(), 0);
        return new RenderFrame(host, cap, null, List.of(model), model, ModelRuntimeData.EMPTY,
                0F, 0, 0, new NoopSink(), new Matrix4f(), new Matrix4f(), true);
    }

    @Test
    void boneChainTakesFirstExistingBoneAndFailsWhenChainMissing() {
        Model model = modelOf(List.of(), "rightarm", "righthand");
        List<AttachmentAnchor.Resolved> resolved = AttachmentAnchor
                .boneChain(List.of(List.of("rightitem", "righthand", "righthandlocator")))
                .resolve(frame(model));

        assertEquals(1, resolved.size());
        assertEquals("righthand", resolved.get(0).name());

        // 只有肩/肘骨骼时不得兜底：手臂 pivot 在关节上
        Model jointOnly = modelOf(List.of(), "rightarm", "rightforearm");
        assertTrue(AttachmentAnchor.boneChain(List.of(List.of("rightitem", "righthand")))
                                       .resolve(frame(jointOnly)).isEmpty());
    }

    @Test
    void missingLocatorResolvesToEmptyAndLocatorFamilyMatchesPrefix() {
        List<LocatorEntry> locators = List.of(
                new LocatorEntry("pet", new Vector3f(0), new Vector3f(0)),
                new LocatorEntry("pet_t_head", new Vector3f(0), new Vector3f(0)),
                new LocatorEntry("pet2", new Vector3f(0), new Vector3f(0)),
                new LocatorEntry("hand", new Vector3f(0), new Vector3f(0)));
        Model model = modelOf(locators, "body");
        RenderFrame frame = frame(model);

        assertTrue(AttachmentAnchor.locator("missing").resolve(frame).isEmpty());
        assertEquals(1, AttachmentAnchor.locator("pet").resolve(frame).size());
        // 前缀族语义与 RenderHelper.renderOnLocator 一致：pet 命中 pet / pet_t_head / pet2
        assertEquals(List.of("pet", "pet_t_head", "pet2"),
                AttachmentAnchor.locatorFamily("pet").resolve(frame).stream()
                                .map(AttachmentAnchor.Resolved::name).toList());
    }

    @Test
    void modelRootAnchorAppliesBedrockYFlip() {
        Model model = modelOf(List.of(), "body");
        RenderData<Object> cap = new RenderData<>();
        RenderFrame frame = new RenderFrame(RenderHost.anchor(new Object(), 0), cap, null,
                List.of(model), model, ModelRuntimeData.EMPTY, 0F, 0, 0, new NoopSink(),
                new Matrix4f(), new Matrix4f().translate(1F, 2F, 3F), true);

        List<AttachmentAnchor.Resolved> resolved = AttachmentAnchor.modelRoot().resolve(frame);
        assertEquals(1, resolved.size());
        Matrix4f expected = new Matrix4f().translate(1F, 2F, 3F).rotateY((float) Math.PI);
        assertTrue(expected.equals(resolved.get(0).pose(), 1e-6F));
    }

    @Test
    void registryRegistrationIsRemovableAndIdempotent() {
        int before = AttachmentRegistry.size();
        AttachmentRegistry.Registration registration = AttachmentRegistry.register(frame -> List.of());

        assertEquals(before + 1, AttachmentRegistry.size());
        registration.close();
        registration.close();
        assertEquals(before, AttachmentRegistry.size());
    }

    @Test
    void renderResultSkipReasonsCarryNoPosesForEarlyExits() {
        RenderResult missingHost = RenderResult.skipped(RenderSkipReason.MISSING_HOST);
        assertFalse(missingHost.drawn());
        assertNull(missingHost.entityPose());
        assertNull(missingHost.rootPose());
        assertSame(RenderSkipReason.MISSING_HOST, missingHost.skipReason());

        Matrix4f entityPose = new Matrix4f();
        Matrix4f rootPose = new Matrix4f();
        RenderResult unbaked = RenderResult.skipped(RenderSkipReason.NOT_BAKED, entityPose, rootPose);
        assertSame(entityPose, unbaked.entityPose());
        assertSame(rootPose, unbaked.rootPose());
        assertFalse(unbaked.drawn());

        RenderResult drawn = RenderResult.drawn(entityPose, rootPose, 2, 2, 1);
        assertTrue(drawn.drawn());
        assertEquals(2, drawn.submittedPasses());
        assertEquals(1, drawn.attachmentDraws());
    }

    /** 无 GL 参与的 sink 桩：挂载锚点解析只需要 sink 的非空引用。 */
    private static final class NoopSink implements RenderSink {
        @Override
        public void submit(PortRenderPass renderPass, PortResourceLocation texture, PoseStack pose, GeometryWriter writer) {
        }

        @Override
        public void flush() {
        }

        //? if <26.1 {
        @Override
        public net.minecraft.client.renderer.MultiBufferSource multiBufferSource() {
            return null;
        }
        //?} else {
        @Override
        public net.minecraft.client.renderer.SubmitNodeCollector submitNodeCollector() {
            return null;
        }
        //?}
    }
}
