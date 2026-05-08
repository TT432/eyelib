package io.github.tt432.eyelibmaterialsmoke;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.JsonOps;
import io.github.tt432.clientsmoke.runtime.ClientSmokeVisualHooks;
import io.github.tt432.clientsmokeannotation.ClientSmoke;
import io.github.tt432.eyelibmaterial.gl.GLStates;
import io.github.tt432.eyelibmaterial.material.BrMaterial;
import io.github.tt432.eyelibmaterial.material.BrMaterialEntry;
import io.github.tt432.eyelibmaterial.render.RenderTypeResolver;
import io.github.tt432.eyelibmaterial.shader.ShaderManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Runtime smoke test for the eyelib-material client pipeline.
 *
 * <p>This module is intentionally separate from the generic client smoke
 * framework. The framework owns discovery/reporting/screenshots; this target
 * module owns material-specific fixtures and visual assertions.</p>
 */
@ClientSmoke(
        description = "Validates eyelib-material multi-material visual pipeline in a live client",
        priority = 10,
        modId = "eyelibmaterial"
)
public class MaterialPipelineSmoke {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialPipelineSmoke.class);
    private static final ResourceLocation TEST_TEXTURE = new ResourceLocation("minecraft", "textures/block/stone.png");

    // @formatter:off
    private static final String MATERIAL_FIXTURE = """
        {
          "materials": {
            "solid": {
              "defines": [],
              "samplerStates": [],
              "states": [],
              "variants": []
            },
            "material_red": {
              "vertexShader": "eyelibmaterialsmoke:shaders/smoke.vert",
              "fragmentShader": "eyelibmaterialsmoke:shaders/smoke.frag",
              "defines": ["MATERIAL_RED", "X_CENTER -0.55"],
              "samplerStates": [
                {"samplerIndex": 0, "textureFilter": "Point", "textureWrap": "Clamp"}
              ],
              "states": [],
              "variants": [
                {
                  "blue_base": {
                    "vertexShader": "eyelibmaterialsmoke:shaders/smoke.vert",
                    "fragmentShader": "eyelibmaterialsmoke:shaders/smoke.frag",
                    "defines": ["MATERIAL_BLUE", "X_CENTER 0.55"],
                    "samplerStates": [],
                    "states": [],
                    "variants": []
                  }
                }
              ]
            },
            "material_green:material_red": {
              "vertexShader": "eyelibmaterialsmoke:shaders/smoke.vert",
              "fragmentShader": "eyelibmaterialsmoke:shaders/smoke.frag",
              "defines": ["MATERIAL_GREEN", "X_CENTER 0.0"],
              "+states": ["DisableCulling"],
              "variants": []
            },
            "material_yellow_overlay": {
              "vertexShader": "eyelibmaterialsmoke:shaders/smoke.vert",
              "fragmentShader": "eyelibmaterialsmoke:shaders/smoke.frag",
              "defines": ["MATERIAL_YELLOW", "MATERIAL_ALPHA_HALF", "X_CENTER 0.55"],
              "samplerStates": [],
              "states": ["Blending"],
              "blendSrc": "SourceAlpha",
              "blendDst": "OneMinusSrcAlpha",
              "variants": []
            }
          }
        }
        """;
    // @formatter:on

    public MaterialPipelineSmoke() {
        long start = System.currentTimeMillis();
        LOGGER.info("[MaterialSmoke] Pipeline smoke starting");

        BrMaterial material = parseFixture();
        Map<String, BrMaterialEntry> entries = material.materials();
        BrMaterialEntry solid = requireEntry(entries, "solid");
        BrMaterialEntry red = requireEntry(entries, "material_red");
        BrMaterialEntry green = requireEntry(entries, "material_green:material_red");
        BrMaterialEntry yellowOverlay = requireEntry(entries, "material_yellow_overlay");

        Map<String, BrMaterialEntry> resolutionMap = buildResolutionMap(entries);
        BrMaterialEntry blueBase = verifyRepresentativeDataShape(entries, red, green, resolutionMap);
        verifyShaderResourcesLoad();
        MaterialPrograms programs = verifyRuntimeRenderPath(solid, red, green, blueBase, yellowOverlay);
        ClientSmokeVisualHooks.set(
                mc -> renderMaterialScene(programs, resolutionMap),
                MaterialPipelineSmoke::verifyMaterialPixels
        );

        long elapsed = System.currentTimeMillis() - start;
        LOGGER.info("[MaterialSmoke] Pipeline setup passed; visual assertion will run during screenshot capture ({}ms)", elapsed);
    }

    private static BrMaterial parseFixture() {
        var json = JsonParser.parseString(MATERIAL_FIXTURE);
        return BrMaterial.CODEC
                .parse(JsonOps.INSTANCE, json)
                .getOrThrow(false, e -> new AssertionError("[MaterialSmoke] fixture parse failed: " + e));
    }

    private static BrMaterialEntry requireEntry(Map<String, BrMaterialEntry> entries, String key) {
        BrMaterialEntry entry = entries.get(key);
        if (entry == null) {
            throw new AssertionError("[MaterialSmoke] missing fixture entry: " + key);
        }
        return entry;
    }

    private static BrMaterialEntry verifyRepresentativeDataShape(
            Map<String, BrMaterialEntry> entries,
            BrMaterialEntry red,
            BrMaterialEntry green,
            Map<String, BrMaterialEntry> resolutionMap
    ) {
        if (entries.size() != 4) {
            throw new AssertionError("[MaterialSmoke] fixture should contain exactly 4 entries, got " + entries.size());
        }
        Optional<BrMaterialEntry> blueBase = red.getVariant("blue_base");
        if (blueBase.isEmpty()) {
            throw new AssertionError("[MaterialSmoke] representative blue_base variant lookup failed");
        }

        List<GLStates> resolvedStates = green.states().toList(green, resolutionMap);
        if (!resolvedStates.contains(GLStates.DisableCulling)) {
            throw new AssertionError("[MaterialSmoke] child material did not resolve additive DisableCulling state: " + resolvedStates);
        }
        LOGGER.info("[MaterialSmoke] Fixture parsed and representative inheritance/variant path resolved");
        return blueBase.get();
    }

    private static void verifyShaderResourcesLoad() {
        String vert = ShaderManager.loadFromResource("assets/eyelibmaterialsmoke/shaders/smoke.vert");
        String frag = ShaderManager.loadFromResource("assets/eyelibmaterialsmoke/shaders/smoke.frag");
        if (vert.isBlank() || frag.isBlank()) {
            throw new AssertionError("[MaterialSmoke] smoke shader resources must not be blank");
        }
        LOGGER.info("[MaterialSmoke] Shader resources loaded from runtime classpath");
    }

    private static MaterialPrograms verifyRuntimeRenderPath(
            BrMaterialEntry solid,
            BrMaterialEntry red,
            BrMaterialEntry green,
            BrMaterialEntry blueBase,
            BrMaterialEntry yellowOverlay
    ) {
        RenderType resolved = RenderTypeResolver.resolve(new ResourceLocation("minecraft", "solid")).factory().apply(TEST_TEXTURE);
        RenderType fallback = solid.getRenderType(TEST_TEXTURE);
        RenderType redType = red.getRenderType(TEST_TEXTURE);
        RenderType greenType = green.getRenderType(TEST_TEXTURE);
        RenderType blueType = blueBase.getRenderType(TEST_TEXTURE);
        RenderType yellowType = yellowOverlay.getRenderType(TEST_TEXTURE);

        if (resolved == null || fallback == null || redType == null || greenType == null || blueType == null || yellowType == null) {
            throw new AssertionError("[MaterialSmoke] RenderType creation returned null");
        }

        MaterialPrograms programs = new MaterialPrograms(
                new DrawMaterial(red, red.getCompiledShaderProgram()),
                new DrawMaterial(green, green.getCompiledShaderProgram()),
                new DrawMaterial(blueBase, blueBase.getCompiledShaderProgram()),
                new DrawMaterial(yellowOverlay, yellowOverlay.getCompiledShaderProgram())
        );

        if (programs.red().program() <= 0 || programs.green().program() <= 0
                || programs.blueBase().program() <= 0 || programs.yellowOverlay().program() <= 0) {
            throw new AssertionError("[MaterialSmoke] one or more shader programs were not compiled: " + programs);
        }
        if (programs.red().program() == programs.green().program()
                || programs.red().program() == programs.blueBase().program()
                || programs.blueBase().program() == programs.yellowOverlay().program()) {
            throw new AssertionError("[MaterialSmoke] define-specific materials should compile distinct programs: " + programs);
        }

        LOGGER.info("[MaterialSmoke] RenderType paths and define-specific programs verified: red={}, green={}, blue={}, yellow={}",
                programs.red().program(), programs.green().program(),
                programs.blueBase().program(), programs.yellowOverlay().program());
        return programs;
    }

    private static void renderMaterialScene(MaterialPrograms programs, Map<String, BrMaterialEntry> resolutionMap) {
        RenderSystem.assertOnRenderThread();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        try {
            draw(programs.red(), resolutionMap);
            draw(programs.green(), resolutionMap);
            draw(programs.blueBase(), resolutionMap);
            draw(programs.yellowOverlay(), resolutionMap);
        } finally {
            ARBShaderObjects.glUseProgramObjectARB(0);
            GL30.glBindVertexArray(0);
            GL30.glDeleteVertexArrays(vao);
        }

        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    private static void draw(DrawMaterial material, Map<String, BrMaterialEntry> resolutionMap) {
        if (material.entry().states().toList(material.entry(), resolutionMap).contains(GLStates.Blending)) {
            RenderSystem.enableBlend();
            material.entry().blend().apply(material.entry(), resolutionMap);
        } else {
            RenderSystem.disableBlend();
        }
        ARBShaderObjects.glUseProgramObjectARB(material.program());
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    private static void verifyMaterialPixels(NativeImage image) {
        assertRegion(image, -0.55F, MaterialPipelineSmoke::isRed, "red material");
        assertRegion(image, 0.0F, MaterialPipelineSmoke::isGreen, "green inherited material");
        assertRegion(image, 0.55F, MaterialPipelineSmoke::isGrey, "blue material with yellow alpha overlay");
    }

    private static void assertRegion(NativeImage image, float ndcX, PixelPredicate predicate, String label) {
        int centerX = Math.round((ndcX + 1.0F) * 0.5F * image.getWidth());
        int centerY = image.getHeight() / 2;
        int matchingPixels = 0;
        int sampledPixels = 0;

        for (int y = centerY - 12; y <= centerY + 12; y++) {
            for (int x = centerX - 12; x <= centerX + 12; x++) {
                sampledPixels++;
                if (predicate.matches(image.getPixelRGBA(x, y))) {
                    matchingPixels++;
                }
            }
        }

        if (matchingPixels < sampledPixels * 8 / 10) {
            throw new AssertionError("[MaterialSmoke] " + label + " region did not match expected pixels: "
                    + matchingPixels + "/" + sampledPixels + " matched");
        }
        LOGGER.info("[MaterialSmoke] {} verified in framebuffer: {}/{} pixels matched", label, matchingPixels, sampledPixels);
    }

    private static boolean isRed(int rgba) {
        int r = rgba & 0xFF;
        int g = (rgba >> 8) & 0xFF;
        int b = (rgba >> 16) & 0xFF;
        return r >= 220 && g <= 50 && b <= 50;
    }

    private static boolean isGreen(int rgba) {
        int r = rgba & 0xFF;
        int g = (rgba >> 8) & 0xFF;
        int b = (rgba >> 16) & 0xFF;
        return r <= 50 && g >= 220 && b <= 50;
    }

    private static boolean isGrey(int rgba) {
        int r = rgba & 0xFF;
        int g = (rgba >> 8) & 0xFF;
        int b = (rgba >> 16) & 0xFF;
        return r >= 90 && r <= 180 && g >= 90 && g <= 180 && b >= 90 && b <= 180;
    }

    private static Map<String, BrMaterialEntry> buildResolutionMap(Map<String, BrMaterialEntry> entries) {
        Map<String, BrMaterialEntry> resolutionMap = new HashMap<>(entries);
        for (BrMaterialEntry entry : entries.values()) {
            resolutionMap.put(entry.name(), entry);
        }
        return resolutionMap;
    }

    private record DrawMaterial(BrMaterialEntry entry, int program) {}

    private record MaterialPrograms(
            DrawMaterial red,
            DrawMaterial green,
            DrawMaterial blueBase,
            DrawMaterial yellowOverlay
    ) {}

    @FunctionalInterface
    private interface PixelPredicate {
        boolean matches(int rgba);
    }
}
