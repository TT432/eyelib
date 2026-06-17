package io.github.tt432.eyelibmaterial.material;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import org.jspecify.annotations.NullMarked;

import java.util.function.Supplier;

/**
 * Bedrock .material.bin shader pass → JE RenderType shader 映射。
 *
 * <p>直接映射表见
 * assets/eyelib/eyelib/materials/shader_mapping.json，
 * 此枚举是其 Java 代理。
 *
 * @author TT432
 */
@NullMarked
public enum BrShaderMapping {

    OPAQUE("entity_solid"),
    ENTITY("entity_solid"),
    ALPHA_TEST("entity_cutout"),
    ALPHA_TEST_COLOR_MASK("entity_cutout"),
    ALPHA_TEST_COLOR_MASK_GLINT("entity_cutout"),
    ALPHA_TEST_COLOR_MASK_MULTIPLICATIVE_TINT("entity_cutout"),
    ALPHA_TEST_EMISSIVE("entity_translucent_emissive"),
    ALPHA_TEST_EMISSIVE_ONLY("entity_translucent_emissive"),
    ALPHA_TEST_GLINT("entity_cutout"),
    ALPHA_TEST_MASKED_OVERWRITE("entity_cutout"),
    ALPHA_TEST_MULTI_COLOR("entity_cutout"),
    TRANSPARENT("entity_translucent"),
    EMISSIVE("entity_solid"),
    EMISSIVE_ONLY("entity_solid"),
    GLINT("entity_solid"),
    GLINT_COLOR("entity_solid"),
    COLOR_MASK("entity_solid"),
    MULTI_COLOR("entity_solid"),
    MASKED_MULTITEXTURE("entity_cutout"),
    MULTIPLICATIVE_TINT_COLOR("entity_solid"),
    TINTED_ALPHA_TEST_ENABLED("entity_cutout");

    private final String jeShaderName;

    BrShaderMapping(String jeShaderName) {
        this.jeShaderName = jeShaderName;
    }

    public String jeShaderName() {
        return jeShaderName;
    }

    /** 返回对应的 MC ShaderInstance 供应器，用于 ShaderStateShard */
    public Supplier<ShaderInstance> shader() {
        return switch (jeShaderName) {
            case "entity_solid" ->
                    () -> Minecraft.getInstance().gameRenderer.getRendertypeEntitySolidShader();
            case "entity_cutout" ->
                    () -> Minecraft.getInstance().gameRenderer.getRendertypeEntityCutoutShader();
            case "entity_translucent" ->
                    () -> Minecraft.getInstance().gameRenderer.getRendertypeEntityTranslucentShader();
            case "entity_translucent_emissive" ->
                    () -> Minecraft.getInstance().gameRenderer.getRendertypeEntityTranslucentEmissiveShader();
            default ->
                    () -> Minecraft.getInstance().gameRenderer.getRendertypeEntitySolidShader();
        };
    }

    /** 同 shader()，但禁用面剔除 */
    public Supplier<ShaderInstance> shaderNoCull() {
        if ("entity_cutout".equals(jeShaderName)) {
            return () -> Minecraft.getInstance().gameRenderer.getRendertypeEntityCutoutNoCullShader();
        }
        return shader();
    }
}
