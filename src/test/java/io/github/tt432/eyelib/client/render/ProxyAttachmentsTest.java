package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.model.locator.GroupLocator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 代理 attachable 的挂载目标解析：模型缺少挂载骨骼时**必须**解析失败（渲染静默跳过），
 * 否则「僵尸村民手持物不显示」这类缺陷无从发现。
 *
 * @author TT432
 */
class ProxyAttachmentsTest {

    private static int id(String name) {
        return GlobalBoneIdHandler.get(name);
    }

    private static Model modelOf(String... boneNames) {
        Int2ObjectMap<Model.Bone> all = new Int2ObjectOpenHashMap<>();
        for (String name : boneNames) {
            int boneId = id(name);
            all.put(boneId, Model.Bone.of(boneId, -1,
                    new Vector3f(0), new Vector3f(0), new Vector3f(0), new Vector3f(1),
                    null, new Int2ObjectOpenHashMap<>(), List.of(),
                    new GroupLocator(new Int2ObjectOpenHashMap<>(), List.of())));
        }
        return Model.of("proxy-test", all);
    }

    private static ProxyAttachable heldItemMain() {
        return new ProxyAttachable("eyelib:held_item_main", net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                ProxyAttachable.Kind.HELD_ITEM,
                List.of(List.of("rightitem", "righthand", "righthandlocator")));
    }

    private static String auditLine(Model model, String identifier) {
        return ProxyAttachments.audit(model).stream()
                .filter(line -> line.startsWith(identifier + " "))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void resolvesBoneWhenItemBoneExists() {
        Model model = modelOf("rightarm", "rightitem");
        assertEquals(id("rightitem"), heldItemMain().resolveBindTarget(model));
        assertTrue(auditLine(model, "eyelib:held_item_main").contains("->rightitem"));
    }

    @Test
    void fallsBackAlongCandidateChain() {
        Model model = modelOf("rightarm", "righthand");
        assertEquals(id("righthand"), heldItemMain().resolveBindTarget(model));

        Model locatorOnly = modelOf("rightarm", "rightHandLocator");
        assertEquals(id("righthandlocator"), heldItemMain().resolveBindTarget(locatorOnly));
    }

    @Test
    void reportsMissingWhenModelHasNoHandBone() {
        Model model = modelOf("rightarm", "rightforearm", "body", "head");
        assertEquals(-1, heldItemMain().resolveBindTarget(model),
                "只有肩/肘骨骼时不能兜底：手臂 pivot 在关节点上，物品会被摆到关节");
        assertTrue(auditLine(model, "eyelib:held_item_main").contains("MISSING"));
    }

    @Test
    void auditReportsEachDeclaredTarget() {
        Model model = modelOf("head", "body", "rightleg", "leftleg");
        String chest = auditLine(model, "eyelib:armor_chest");
        assertTrue(chest.contains("->body"), chest);
        assertTrue(chest.contains("->head"), chest);
        assertTrue(auditLine(model, "eyelib:armor_legs").contains("->body"));

        // 无 body 的模型（phantom / bee / warden / llama…）胸甲与护腿都没有挂载目标
        Model noBody = modelOf("head", "rightleg");
        assertTrue(auditLine(noBody, "eyelib:armor_chest").contains("MISSING"));
        assertTrue(auditLine(noBody, "eyelib:armor_legs").contains("MISSING"));
        assertTrue(auditLine(noBody, "eyelib:armor_feet").contains("->rightleg"));
    }
}
