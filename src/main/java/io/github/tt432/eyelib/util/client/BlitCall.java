package io.github.tt432.eyelib.util.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * An immutable render command describing a textured quad (blit) and how to draw it.
 * <p>
 * Use {@link Builder} to construct instances. This class does not depend on GuiGraphics
 * and can be rendered with a provided {@link PoseStack}.
 */
public record BlitCall(
        int texture,
        Supplier<ShaderInstance> shaderSupplier,
        VertexFormat vertexFormat,
        boolean blend,
        int x0,
        int x1,
        int y0,
        int y1,
        int z,
        float u0,
        float u1,
        float v0,
        float v1,
        float r,
        float g,
        float b,
        float a
) {

    /**
     * Executes the blit using the given pose stack.
     *
     * @param poseStack the transform stack to use
     * @throws NullPointerException if poseStack is null
     */
    public void render(PoseStack poseStack) {
        Objects.requireNonNull(poseStack, "poseStack");
        RenderSystem.setShaderTexture(0, this.texture);
        if (this.blend) {
            RenderSystem.enableBlend();
        }
        RenderSystem.setShader(shaderSupplier);
        Matrix4f matrix4f = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS,
                vertexFormat
        );
        if (vertexFormat.contains(VertexFormatElement.COLOR)) {
            buffer.addVertex(matrix4f, (float) this.x0, (float) this.y0, (float) this.z)
                    .setUv(this.u0, this.v0)
                    .setColor(this.r, this.g, this.b, this.a);
            buffer.addVertex(matrix4f, (float) this.x0, (float) this.y1, (float) this.z)
                    .setUv(this.u0, this.v1)
                    .setColor(this.r, this.g, this.b, this.a);
            buffer.addVertex(matrix4f, (float) this.x1, (float) this.y1, (float) this.z)
                    .setUv(this.u1, this.v1)
                    .setColor(this.r, this.g, this.b, this.a);
            buffer.addVertex(matrix4f, (float) this.x1, (float) this.y0, (float) this.z)
                    .setUv(this.u1, this.v0)
                    .setColor(this.r, this.g, this.b, this.a);
        } else {
            buffer.addVertex(matrix4f, (float) this.x0, (float) this.y0, (float) this.z).setUv(this.u0, this.v0);
            buffer.addVertex(matrix4f, (float) this.x0, (float) this.y1, (float) this.z).setUv(this.u0, this.v1);
            buffer.addVertex(matrix4f, (float) this.x1, (float) this.y1, (float) this.z).setUv(this.u1, this.v1);
            buffer.addVertex(matrix4f, (float) this.x1, (float) this.y0, (float) this.z).setUv(this.u1, this.v0);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        if (blend) {
            RenderSystem.disableBlend();
        }
    }

    /**
     * Creates a new builder targeting the given texture.
     *
     * @param texture the texture resource to bind
     * @return builder preconfigured with the texture
     */
    public static Builder builder(ResourceLocation texture) {
        return new Builder(Objects.requireNonNull(texture, "texture"));
    }

    public static final class Builder {
        private final int texture;
        private int x0;
        private int x1;
        private int y0;
        private int y1;
        private int z = 0;
        private Supplier<ShaderInstance> shaderSupplier = GameRenderer::getPositionTexShader;
        private VertexFormat vertexFormat = DefaultVertexFormat.POSITION_TEX_COLOR;
        private boolean blend = false;
        private float u0;
        private float u1;
        private float v0;
        private float v1;
        private boolean destSet = false;
        private boolean uvSet = false;
        private float r = 1.0f;
        private float g = 1.0f;
        private float b = 1.0f;
        private float a = 1.0f;

        /**
         * Creates a builder bound to the specified texture.
         *
         * @param texture the texture resource location
         */
        public Builder(ResourceLocation texture) {
            this(Minecraft.getInstance().getTextureManager().getTexture(texture).getId());
        }

        public Builder(int texture) {
            this.texture = texture;
        }

        /**
         * Sets the destination rectangle using position and size.
         *
         * @param x      left x
         * @param y      top y
         * @param width  width of the quad
         * @param height height of the quad
         * @return this builder
         * @throws IllegalArgumentException if width or height is zero
         */
        public Builder dest(int x, int y, int width, int height) {
            if (width == 0 || height == 0) {
                throw new IllegalArgumentException("width/height must be non-zero");
            }
            this.x0 = x;
            this.y0 = y;
            this.x1 = x + width;
            this.y1 = y + height;
            this.destSet = true;
            return this;
        }

        /**
         * Sets the destination rectangle using explicit corners.
         *
         * @param x0 left x
         * @param y0 top y
         * @param x1 right x
         * @param y1 bottom y
         * @return this builder
         */
        public Builder coords(int x0, int y0, int x1, int y1) {
            if (x0 == x1 || y0 == y1) {
                throw new IllegalArgumentException("coords must define a non-empty rectangle");
            }
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            this.destSet = true;
            return this;
        }

        /**
         * Sets the depth (z) value.
         *
         * @param z depth
         * @return this builder
         */
        public Builder depth(int z) {
            this.z = z;
            return this;
        }

        /**
         * Sets source UVs directly.
         *
         * @param u0 left U
         * @param v0 top V
         * @param u1 right U
         * @param v1 bottom V
         * @return this builder
         */
        public Builder srcUV(float u0, float v0, float u1, float v1) {
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
            this.uvSet = true;
            return this;
        }

        /**
         * Computes and sets source UVs from pixel-space region and texture size.
         *
         * @param u         region start u (pixels)
         * @param v         region start v (pixels)
         * @param uWidth    region width (pixels)
         * @param vHeight   region height (pixels)
         * @param texWidth  texture width (pixels)
         * @param texHeight texture height (pixels)
         * @return this builder
         */
        public Builder srcFromPixels(int u, int v, int uWidth, int vHeight, int texWidth, int texHeight) {
            if (texWidth <= 0 || texHeight <= 0) {
                throw new IllegalArgumentException("texture size must be positive");
            }
            this.u0 = (u + 0.0f) / (float) texWidth;
            this.u1 = (u + (float) uWidth) / (float) texWidth;
            this.v0 = (v + 0.0f) / (float) texHeight;
            this.v1 = (v + (float) vHeight) / (float) texHeight;
            this.uvSet = true;
            return this;
        }

        /**
         * Sets UVs to cover the entire sprite.
         *
         * @param sprite atlas sprite
         * @return this builder
         */
        public Builder spriteFull(TextureAtlasSprite sprite) {
            Objects.requireNonNull(sprite, "sprite");
            this.u0 = sprite.getU0();
            this.u1 = sprite.getU1();
            this.v0 = sprite.getV0();
            this.v1 = sprite.getV1();
            this.uvSet = true;
            return this;
        }

        /**
         * Sets UVs for a sub-region of the sprite given sprite's logical size.
         *
         * @param sprite       atlas sprite
         * @param spriteWidth  sprite logical width
         * @param spriteHeight sprite logical height
         * @param regionU      region left in pixels relative to sprite
         * @param regionV      region top in pixels relative to sprite
         * @param regionWidth  region width in pixels
         * @param regionHeight region height in pixels
         * @return this builder
         */
        public Builder spriteRegion(
                TextureAtlasSprite sprite,
                int spriteWidth,
                int spriteHeight,
                int regionU,
                int regionV,
                int regionWidth,
                int regionHeight
        ) {
            Objects.requireNonNull(sprite, "sprite");
            if (spriteWidth <= 0 || spriteHeight <= 0) {
                throw new IllegalArgumentException("sprite size must be positive");
            }
            this.u0 = sprite.getU((float) regionU / (float) spriteWidth);
            this.u1 = sprite.getU((float) (regionU + regionWidth) / (float) spriteWidth);
            this.v0 = sprite.getV((float) regionV / (float) spriteHeight);
            this.v1 = sprite.getV((float) (regionV + regionHeight) / (float) spriteHeight);
            this.uvSet = true;
            return this;
        }

        /**
         * Applies a color tint with alpha.
         *
         * @param r red in [0,1]
         * @param g green in [0,1]
         * @param b blue in [0,1]
         * @param a alpha in [0,1]
         * @return this builder
         */
        public Builder tint(float r, float g, float b, float a) {
            validateUnit(r, "r");
            validateUnit(g, "g");
            validateUnit(b, "b");
            validateUnit(a, "a");

            shaderSupplier = GameRenderer::getPositionTexColorShader;
            vertexFormat = DefaultVertexFormat.POSITION_TEX;
            blend = true;


            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            return this;
        }

        /**
         * Builds the immutable {@link BlitCall}.
         *
         * @return the render call
         * @throws IllegalStateException if destination or UVs are not set
         */
        public BlitCall build() {
            if (!this.destSet) {
                throw new IllegalStateException("destination rectangle not set");
            }
            if (!this.uvSet) {
                throw new IllegalStateException("UVs not set");
            }
            return new BlitCall(
                    texture,
                    shaderSupplier,
                    vertexFormat,
                    blend,
                    x0, x1, y0, y1, z,
                    u0, u1, v0, v1,
                    r, g, b, a
            );
        }

        private static void validateUnit(float v, String name) {
            if (Float.isNaN(v) || v < 0.0f || v > 1.0f) {
                throw new IllegalArgumentException(name + " must be in [0,1], got " + v);
            }
        }
    }
}

