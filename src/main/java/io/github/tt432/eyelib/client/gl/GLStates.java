package io.github.tt432.eyelib.client.gl;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.client.material.BrMaterialEntry;
import net.minecraft.util.StringRepresentable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author TT432
 */
public enum GLStates implements StringRepresentable {
    EnableAlphaToCoverage(
            (material, materials) -> {
                GL11.glEnable(GL30.GL_SAMPLE_ALPHA_TO_COVERAGE);
            },
            (material, materials) -> {
                GL11.glDisable(GL30.GL_SAMPLE_ALPHA_TO_COVERAGE);
            }),
    Wireframe(
            (material, materials) -> {
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
            },
            (material, materials) -> {
                GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
            }),
    Blending(
            (material, materials) -> {
                GL11.glEnable(GL11.GL_BLEND);
                material.blend().apply(material, materials);
            },
            (material, materials) -> {
                GL11.glDisable(GL11.GL_BLEND);
            }),
    DisableColorWrite(
            (material, materials) -> {
                GL11.glColorMask(false, false, false, false);
            },
            (material, materials) -> {
                GL11.glColorMask(true, true, true, true);
            }),
    DisableAlphaWrite(
            (material, materials) -> {
                GL11.glColorMask(true, true, true, false);
            },
            (material, materials) -> {
                GL11.glColorMask(true, true, true, true);
            }),
    DisableRgbWrite(
            (material, materials) -> {
                GL11.glColorMask(false, false, false, true);
            },
            (material, materials) -> {
                GL11.glColorMask(true, true, true, true);
            }),
    DisableDepthTest(
            (material, materials) -> {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
            },
            (material, materials) -> {
                GL11.glEnable(GL11.GL_DEPTH_TEST);
            }),
    DisableDepthWrite(
            (material, materials) -> {
                GL11.glDepthMask(false);
            },
            (material, materials) -> {
                GL11.glDepthMask(true);
            }),
    DisableCulling(
            (material, materials) -> {
                GL11.glDisable(GL11.GL_CULL_FACE);
            },
            (material, materials) -> {
                GL11.glEnable(GL11.GL_CULL_FACE);
            }),
    InvertCulling(
            (material, materials) -> {
                GL11.glEnable(GL11.GL_CULL_FACE);
                GL11.glCullFace(GL11.GL_FRONT);
            },
            (material, materials) -> {
                GL11.glEnable(GL11.GL_CULL_FACE);
                GL11.glCullFace(GL11.GL_BACK);
            }),
    StencilWrite(
            (material, materials) -> {
                GL11.glStencilMask(0xFF);
            },
            (material, materials) -> {
                GL11.glStencilMask(0x00);
            }),
    EnableStencilTest(
            (material, materials) -> {
                GL11.glEnable(GL11.GL_STENCIL_TEST);

                material.stencil().apply(material, materials);
            },
            (material, materials) -> {
                GL11.glDisable(GL11.GL_STENCIL_TEST);
            });

    public static final Codec<GLStates> CODEC = StringRepresentable.fromEnum(GLStates::values);
    private final BiConsumer<BrMaterialEntry, Map<String, BrMaterialEntry>> enableAction;
    private final BiConsumer<BrMaterialEntry, Map<String, BrMaterialEntry>> disableAction;

    GLStates(BiConsumer<BrMaterialEntry, Map<String, BrMaterialEntry>> enableAction, BiConsumer<BrMaterialEntry, Map<String, BrMaterialEntry>> disableAction) {
        this.enableAction = enableAction;
        this.disableAction = disableAction;
    }

    public void enable(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
        enableAction.accept(material, materials);
    }

    public void disable(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
        disableAction.accept(material, materials);
    }

    @Override
    public String getSerializedName() {
        return name();
    }
}
