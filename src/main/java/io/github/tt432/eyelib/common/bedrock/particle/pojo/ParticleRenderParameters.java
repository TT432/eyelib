package io.github.tt432.eyelib.common.bedrock.particle.pojo;

/**
 *
 * @author DustW
 */
public class ParticleRenderParameters {
    /**
     * Minecraft material to use for emitter
     * -----------------------------
     * Materials
     * There are several material options available that determine how particles handle transparency and color blending.
     * -----------------------------
     * Name	Description
     * particles_alpha	Pixels with an alpha of 0 will be fully transparent, colored pixels will always be opaque.
     * particles_blend	Enables color blending and transparency in colored pixels, uses a normal blend mode.
     * particles_add	Enables color blending and transparency in colored pixels, uses an additive blend mode.
     */
    String material;
    /**
     * Minecraft texture to use for emitter
     */
    String texture;
}
