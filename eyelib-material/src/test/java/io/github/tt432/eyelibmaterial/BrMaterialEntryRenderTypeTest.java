package io.github.tt432.eyelibmaterial;

import io.github.tt432.eyelibmaterial.gl.GLStates;
import io.github.tt432.eyelibmaterial.material.BrMaterialEntry;
import io.github.tt432.eyelibmaterial.shared.VertexFormatElementEnum;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author TT432
 */
@NullMarked
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

    private static final ResourceLocation TEST_TEXTURE =
            new ResourceLocation("minecraft:textures/entity/steve.png");

    @Test
    @DisplayName("No shader + 'solid' → falls through to RenderTypeResolver (LinkageError from RenderType class init without MC)")
    void noShaderSolid_entersFallbackPath() {
        BrMaterialEntry material = createNoShaderEntry("solid");

        assertThrows(LinkageError.class, () -> material.getRenderType(TEST_TEXTURE),
                "Should fall through to RenderTypeResolver which fails at RenderType class loading -> LinkageError");
    }

    @Test
    @DisplayName("No shader + 'cutout' → falls through to RenderTypeResolver (LinkageError from RenderType class init without MC)")
    void noShaderCutout_entersFallbackPath() {
        BrMaterialEntry material = createNoShaderEntry("cutout");

        assertThrows(LinkageError.class, () -> material.getRenderType(TEST_TEXTURE),
                "Should fall through to RenderTypeResolver which fails at RenderType class loading -> LinkageError");
    }

    @Test
    @DisplayName("No shader + 'translucent' → falls through to RenderTypeResolver (LinkageError from RenderType class init without MC)")
    void noShaderTranslucent_entersFallbackPath() {
        BrMaterialEntry material = createNoShaderEntry("translucent");

        assertThrows(LinkageError.class, () -> material.getRenderType(TEST_TEXTURE),
                "Should fall through to RenderTypeResolver which fails at RenderType class loading -> LinkageError");
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