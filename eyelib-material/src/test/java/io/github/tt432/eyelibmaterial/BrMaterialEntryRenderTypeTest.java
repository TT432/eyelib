package io.github.tt432.eyelibmaterial;

import io.github.tt432.eyelibmaterial.gl.GLStates;
import io.github.tt432.eyelibmaterial.material.BrMaterialEntry;
import io.github.tt432.eyelibmaterial.shared.VertexFormatElementEnum;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BrMaterialEntry#getRenderType(ResourceLocation)} covering the
 * branching logic and fallback behavior across 6 scenarios.
 * <p>
 * <b>Test environment note:</b> {@link RenderType} class initialisation triggers
 * Minecraft's built-in registries which require a Forge client bootstrap. Since
 * these are JUnit unit tests without a Minecraft runtime, the no-shader paths
 * (scenarios 1-3) throw {@link LinkageError} the moment the code touches
 * {@code RenderType::entitySolid} etc. The shader paths (scenarios 4-6) throw
 * {@link IllegalStateException} from {@code RenderSystem.assertOnRenderThread()}
 * before reaching {@code RenderType.create()}.
 * <p>
 * The <em>different exception types</em> serve as evidence that each scenario
 * entered its intended code branch. Asserting on the exception type is the most
 * meaningful verification that can be performed in a plain JUnit environment.
 * A full RenderType-creation test would require a Forge game-test runner.
 */
class BrMaterialEntryRenderTypeTest {

    // ── helpers ───────────────────────────────────────────────────────────

    /**
     * Creates a minimal {@link BrMaterialEntry} with no shaders and the given name.
     * Used for no-shader fallback tests (scenarios 1-3).
     */
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

    /**
     * Creates a {@link BrMaterialEntry} with shader paths valid in the test
     * resource tree. The shader sources exist at
     * {@code src/test/resources/assets/eyelibmaterial/shaders/pass_through.*},
     * so {@link io.github.tt432.eyelibmaterial.shader.ShaderManager#loadFromResource}
     * succeeds; the actual GL compilation then fails at
     * {@code RenderSystem.assertOnRenderThread()}.
     *
     * @param name         material name
     * @param blending     whether to include the {@link GLStates#Blending} state
     * @param vertexFields optional vertex field set (empty set = explicit empty, null = absent)
     */
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

    // ── 1. No shader + solid → fallback entitySolid ───────────────────────

    @Test
    @DisplayName("No shader + 'solid' → falls through to RenderTypeResolver (LinkageError from RenderType class init without MC)")
    void noShaderSolid_entersFallbackPath() {
        BrMaterialEntry material = createNoShaderEntry("solid");

        assertThrows(LinkageError.class, () -> material.getRenderType(TEST_TEXTURE),
                "Should fall through to RenderTypeResolver which fails at RenderType class loading -> LinkageError");
    }

    // ── 2. No shader + cutout → fallback entityCutout ─────────────────────

    @Test
    @DisplayName("No shader + 'cutout' → falls through to RenderTypeResolver (LinkageError from RenderType class init without MC)")
    void noShaderCutout_entersFallbackPath() {
        BrMaterialEntry material = createNoShaderEntry("cutout");

        assertThrows(LinkageError.class, () -> material.getRenderType(TEST_TEXTURE),
                "Should fall through to RenderTypeResolver which fails at RenderType class loading -> LinkageError");
    }

    // ── 3. No shader + translucent → fallback entityTranslucent ───────────

    @Test
    @DisplayName("No shader + 'translucent' → falls through to RenderTypeResolver (LinkageError from RenderType class init without MC)")
    void noShaderTranslucent_entersFallbackPath() {
        BrMaterialEntry material = createNoShaderEntry("translucent");

        assertThrows(LinkageError.class, () -> material.getRenderType(TEST_TEXTURE),
                "Should fall through to RenderTypeResolver which fails at RenderType class loading -> LinkageError");
    }

    // ── 4. With shader + opaque → custom CompositeRenderType path ─────────

    @Test
    @DisplayName("With shader + opaque → enters buildCustomRenderType -> IllegalStateException from render thread check")
    void withShaderOpaque_entersCustomPath() {
        BrMaterialEntry material = createShaderEntry("custom_opaque", false, null);

        assertThrows(IllegalStateException.class, () -> material.getRenderType(TEST_TEXTURE),
                "Should attempt the custom shader compilation path (requires GL context) -> IllegalStateException");
    }

    // ── 5. With shader + blending → custom translucent RenderType path ────

    @Test
    @DisplayName("With shader + blending → enters buildCustomRenderType -> IllegalStateException from render thread check")
    void withShaderBlending_entersCustomPath() {
        BrMaterialEntry material = createShaderEntry("custom_translucent", true, null);

        assertThrows(IllegalStateException.class, () -> material.getRenderType(TEST_TEXTURE),
                "Should attempt the custom shader compilation path with translucent state -> IllegalStateException");
    }

    // ── 6. Empty vertexFields → DefaultVertexFormat fallback ──────────────

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
