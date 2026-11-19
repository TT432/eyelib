package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 装备渲染开关策略：真实 attachable 与代理 attachable 共用同一判定，
 * 语义对齐基岩版 client entity description（enable_attachables / hide_armor /
 * held_item_ignores_lighting / held_item_scale）。
 *
 * @author TT432
 */
class AttachablePolicyTest {

    private static BrClientEntity.Toggles toggles(boolean attachables, boolean hideArmor) {
        return new BrClientEntity.Toggles(attachables, hideArmor, false, MolangValue.ONE);
    }

    @Test
    void unattachedHolderAllowsEverything() {
        // 无 client entity 定义（原版实体 / 未托管类型）→ 不参与这套开关
        assertTrue(AttachablePolicy.allowsAttachables(null));
        assertTrue(AttachablePolicy.allowsArmor(null));
    }

    @Test
    void enableAttachablesGatesBothArmorAndWeapons() {
        BrClientEntity disabled = clientEntity(toggles(false, false));
        assertFalse(AttachablePolicy.allowsAttachables(disabled));
        assertFalse(AttachablePolicy.allows(disabled, true), "盔甲");
        assertFalse(AttachablePolicy.allows(disabled, false), "手持物/头饰");

        BrClientEntity enabled = clientEntity(toggles(true, false));
        assertTrue(AttachablePolicy.allows(enabled, true));
        assertTrue(AttachablePolicy.allows(enabled, false));
    }

    @Test
    void hideArmorOnlySuppressesArmor() {
        BrClientEntity hidden = clientEntity(toggles(true, true));
        assertTrue(AttachablePolicy.allowsAttachables(hidden));
        assertFalse(AttachablePolicy.allowsArmor(hidden));
        assertFalse(AttachablePolicy.allows(hidden, true), "盔甲槽");
        assertTrue(AttachablePolicy.allows(hidden, false), "武器/头饰照常");
    }

    @Test
    void hideArmorCannotOverrideDisabledAttachables() {
        BrClientEntity both = clientEntity(toggles(false, true));
        assertFalse(AttachablePolicy.allowsArmor(both));
    }

    @Test
    void heldItemScaleFallsBackToOneForNonPositiveValues() {
        // Bedrock：held_item_scale 必须解析为 > 0；<=0 / 非有限值一律回落 1.0（不做「缩成 0」的退化）
        assertEquals(1.0F, AttachablePolicy.sanitizeHeldItemScale(0), 1e-6F);
        assertEquals(1.0F, AttachablePolicy.sanitizeHeldItemScale(-2), 1e-6F);
        assertEquals(1.0F, AttachablePolicy.sanitizeHeldItemScale(Double.NaN), 1e-6F);
        assertEquals(1.0F, AttachablePolicy.sanitizeHeldItemScale(Double.POSITIVE_INFINITY), 1e-6F);
        assertEquals(1.5F, AttachablePolicy.sanitizeHeldItemScale(1.5), 1e-6F);
    }

    @Test
    void heldItemScaleEvaluatesMolangAndFallsBackWithoutScope() {
        MolangScope scope = new MolangScope();
        BrClientEntity scaled = clientEntity(new BrClientEntity.Toggles(true, false, false, new MolangValue("2.5")));
        assertEquals(2.5F, AttachablePolicy.heldItemScale(scaled, scope), 1e-6F);

        BrClientEntity defaulted = clientEntity(toggles(true, false));
        assertEquals(1.0F, AttachablePolicy.heldItemScale(defaulted, scope), 1e-6F);
        // 无作用域（未托管实体 / 求值上下文缺失）时必须是 1.0，不能抛异常
        assertEquals(1.0F, AttachablePolicy.heldItemScale(scaled, null), 1e-6F);
        assertEquals(1.0F, AttachablePolicy.heldItemScale(null, scope), 1e-6F);
    }

    private static BrClientEntity clientEntity(BrClientEntity.Toggles toggles) {
        return new BrClientEntity("eyelib:policy_test", java.util.Optional.empty(),
                java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of(),
                java.util.List.of(), java.util.Map.of(), java.util.Map.of(), java.util.List.of(),
                java.util.Map.of(), java.util.Optional.empty(), java.util.Optional.empty(),
                java.util.Map.of(), toggles);
    }
}
