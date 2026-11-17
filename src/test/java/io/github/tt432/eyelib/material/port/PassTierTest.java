package io.github.tt432.eyelib.material.port;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link PassTier} 与 {@link PortRenderPass#tier()} 的档位映射。
 *
 * @author TT432
 */
class PassTierTest {

    @Test
    @DisplayName("SOLID/ALPHA_TEST 映射 OPAQUE，TRANSLUCENT/TRANSLUCENT_EMISSIVE/ADDITIVE 映射 BLEND")
    void transparencyMapping() {
        assertSame(PassTier.OPAQUE, PassTier.of(PortRenderPass.of(PortRenderPass.Transparency.SOLID, false)));
        assertSame(PassTier.OPAQUE, PassTier.of(PortRenderPass.of(PortRenderPass.Transparency.ALPHA_TEST, true)));
        assertSame(PassTier.BLEND, PassTier.of(PortRenderPass.of(PortRenderPass.Transparency.TRANSLUCENT, false)));
        assertSame(PassTier.BLEND, PassTier.of(PortRenderPass.of(PortRenderPass.Transparency.TRANSLUCENT_EMISSIVE, true)));
        assertSame(PassTier.BLEND, PassTier.of(PortRenderPass.of(PortRenderPass.Transparency.ADDITIVE, false)));
    }

    @Test
    @DisplayName("Transparency 的每个成员都必须有确定档位（新增成员时此断言失败，提示显式决定）")
    void everyTransparencyMapped() {
        Set<PortRenderPass.Transparency> opaque = EnumSet.of(
                PortRenderPass.Transparency.SOLID, PortRenderPass.Transparency.ALPHA_TEST);
        Set<PortRenderPass.Transparency> blend = EnumSet.of(
                PortRenderPass.Transparency.TRANSLUCENT,
                PortRenderPass.Transparency.TRANSLUCENT_EMISSIVE,
                PortRenderPass.Transparency.ADDITIVE);
        assertEquals(PortRenderPass.Transparency.values().length, opaque.size() + blend.size(),
                "Transparency 新增成员后必须同时更新 PassTier 与档位表");
        for (PortRenderPass.Transparency transparency : PortRenderPass.Transparency.values()) {
            PassTier expected = opaque.contains(transparency) ? PassTier.OPAQUE : PassTier.BLEND;
            assertSame(expected, PortRenderPass.of(transparency, true).tier(), transparency.name());
            assertSame(expected, PortRenderPass.of(transparency, false).tier(), transparency.name());
        }
    }

    @Test
    @DisplayName("of(null) 按不透明处理（与旧 passOrder 语义一致）")
    void nullPassIsOpaque() {
        assertSame(PassTier.OPAQUE, PassTier.of(null));
    }

    @Test
    @DisplayName("档位顺序：OPAQUE 先于 BLEND")
    void order() {
        assertEquals(0, PassTier.OPAQUE.order());
        assertEquals(1, PassTier.BLEND.order());
    }
}
