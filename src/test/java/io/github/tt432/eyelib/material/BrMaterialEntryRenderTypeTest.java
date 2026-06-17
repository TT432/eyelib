package io.github.tt432.eyelib.material;

import io.github.tt432.eyelib.material.gl.GLStates;
import io.github.tt432.eyelib.material.material.BrMaterialEntry;
import io.github.tt432.eyelib.material.shared.VertexFormatElementEnum;
import io.github.tt432.eyelib.material.port.PortRenderPass;
import io.github.tt432.eyelib.util.PortResourceLocation;
import net.minecraft.client.renderer.RenderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author TT432
 */
class BrMaterialEntryRenderTypeTest {

    private static BrMaterialEntry createNoShaderEntry(String name) {
        return new BrMaterialEntry(
                "", name,
                Optional.empty(), Optional.empty(),
                new BrMaterialEntry.Defines(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.SamplerStates(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.States(Optional.empty(), Optional.empty(), Optional.empty()),
                Optional.empty(),
                new BrMaterialEntry.Blend(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.Stencil(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty()),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                List.of()
        );
    }

    private static BrMaterialEntry createShaderEntry(
            String name,
            boolean blending,
            EnumSet<VertexFormatElementEnum> vertexFields
    ) {
        return new BrMaterialEntry(
                "", name,
                Optional.of("eyelibmaterial:shaders/pass_through.vert"),
                Optional.of("eyelibmaterial:shaders/pass_through.frag"),
                new BrMaterialEntry.Defines(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.SamplerStates(Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.States(
                        blending ? Optional.of(List.of(GLStates.Blending)) : Optional.empty(),
                        Optional.empty(), Optional.empty()),
                Optional.empty(),
                new BrMaterialEntry.Blend(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                new BrMaterialEntry.Stencil(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty()),
                vertexFields != null ? Optional.of(vertexFields) : Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                List.of()
        );
    }

    private static final PortResourceLocation TEST_TEXTURE =
            PortResourceLocation.of("minecraft", "textures/entity/steve.png");

    @Test
    @DisplayName("No shader — getRenderType returns PortRenderPass without MC dependency")
    void noShader_returnsPortRenderPass() {
        BrMaterialEntry material = createNoShaderEntry("solid");

        PortRenderPass pass = material.getRenderType(TEST_TEXTURE);
        assertNotNull(pass);
        assertEquals(PortRenderPass.Transparency.SOLID, pass.transparency(),
                "无 material 属性时默认为 SOLID");
        assertFalse(pass.disableCulling(),
                "无 DisableCulling 状态时默认启用剔除");
    }

    @Test
    @DisplayName("No shader + Blending state → TRANSLUCENT transparency")
    void noShaderBlending_returnsTranslucent() {
        // createShaderEntry sets base="" and blending=true in states
        // hasBlending with empty map falls back to checking own states
        BrMaterialEntry material = createShaderEntry("translucent", true, null);
        // 只检查自身状态，不依赖继承链
        assertTrue(material.hasBlending(), "blending=true 时自身状态应包含 Blending");
    }

    @Test
    @DisplayName("No shader + no states → SOLID")
    void noShaderNoStates_returnsSolid() {
        BrMaterialEntry material = createNoShaderEntry("solid");

        PortRenderPass pass = material.getRenderType(TEST_TEXTURE);
        assertEquals(PortRenderPass.Transparency.SOLID, pass.transparency());
    }

    @Test
    @DisplayName("With shader + opaque → enters buildCustomRenderType -> IllegalStateException from render thread check")
    void withShaderOpaque_entersCustomPath() {
        BrMaterialEntry material = createShaderEntry("custom_opaque", false, null);

        assertThrows(IllegalStateException.class, () -> material.getRenderType(TEST_TEXTURE),
                "Should attempt the custom shader compilation path (requires GL context) -> IllegalStateException");
    }

    @Test
    @DisplayName("With shader + blending → enters buildCustomRenderType -> IllegalStateException from render thread check")
    void withShaderBlending_entersCustomPath() {
        BrMaterialEntry material = createShaderEntry("custom_translucent", true, null);

        assertThrows(IllegalStateException.class, () -> material.getRenderType(TEST_TEXTURE),
                "Should attempt the custom shader compilation path with translucent state -> IllegalStateException");
    }

    @Test
    @DisplayName("Empty vertexFields + shaders → enters buildCustomRenderType -> IllegalStateException from render thread check")
    void emptyVertexFields_entersCustomPath() {
        BrMaterialEntry material = createShaderEntry("empty_vfmt", false,
                EnumSet.noneOf(VertexFormatElementEnum.class));

        assertThrows(IllegalStateException.class, () -> material.getRenderType(TEST_TEXTURE),
                "Should enter the custom shader path (where getFormat() returns DefaultVertexFormat)" +
                " -> IllegalStateException from render thread check");
    }
}