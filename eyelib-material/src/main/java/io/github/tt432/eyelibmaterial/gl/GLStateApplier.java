package io.github.tt432.eyelibmaterial.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.tt432.eyelibmaterial.material.BrMaterialEntry;
import org.jspecify.annotations.NullMarked;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.*;

/**
 * 按正确的Bedrock渲染管线顺序应用{@link BrMaterialEntry}中的所有GL状态，并提供{@link #reset()}方法恢复默认GL状态。
 * 所有公开方法都断言在渲染线程上执行。
 *
 * @author TT432
 */
@NullMarked
public final class GLStateApplier {

    private GLStateApplier() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 按Bedrock渲染管线顺序应用给定材质中的GL状态：深度函数、颜色掩码、裁剪、混合、深度写入、深度测试、模板、Alpha到覆盖、线框。
     *
     * @param material  要应用状态的材质条目
     * @param materials 完整材质映射（用于继承解析）
     */
    public static void apply(BrMaterialEntry material, Map<String, BrMaterialEntry> materials) {
        RenderSystem.assertOnRenderThread();

        List<GLStates> states = material.states().toList(material, materials);
        EnumSet<GLStates> stateSet = EnumSet.noneOf(GLStates.class);
        stateSet.addAll(states);

        // 1. Depth function
        material.depthFunc().ifPresent(df -> GL11.glDepthFunc(df.value));

        // 2. Color mask (mutually exclusive)
        if (stateSet.contains(GLStates.DisableColorWrite)) {
            GLStates.DisableColorWrite.enable(material, materials);
        } else if (stateSet.contains(GLStates.DisableAlphaWrite)) {
            GLStates.DisableAlphaWrite.enable(material, materials);
        } else if (stateSet.contains(GLStates.DisableRgbWrite)) {
            GLStates.DisableRgbWrite.enable(material, materials);
        }

        // 3. Culling (mutually exclusive)
        if (stateSet.contains(GLStates.DisableCulling)) {
            GLStates.DisableCulling.enable(material, materials);
        } else if (stateSet.contains(GLStates.InvertCulling)) {
            GLStates.InvertCulling.enable(material, materials);
        }

        // 4. Blending
        if (stateSet.contains(GLStates.Blending)) {
            GLStates.Blending.enable(material, materials);
        }

        // 5. Depth write — always explicitly set
        GL11.glDepthMask(!stateSet.contains(GLStates.DisableDepthWrite));

        // 6. Depth test — always explicitly set
        if (stateSet.contains(GLStates.DisableDepthTest)) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        } else {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }

        // 7. Stencil
        if (stateSet.contains(GLStates.EnableStencilTest)) {
            GLStates.EnableStencilTest.enable(material, materials);
        }
        if (stateSet.contains(GLStates.StencilWrite)) {
            GLStates.StencilWrite.enable(material, materials);
        }

        // 8. Alpha to coverage
        if (stateSet.contains(GLStates.EnableAlphaToCoverage)) {
            GLStates.EnableAlphaToCoverage.enable(material, materials);
        }

        // 9. Wireframe
        if (stateSet.contains(GLStates.Wireframe)) {
            GLStates.Wireframe.enable(material, materials);
        }
    }

    public static void reset() {
        RenderSystem.assertOnRenderThread();
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glColorMask(true, true, true, true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL30.GL_SAMPLE_ALPHA_TO_COVERAGE);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
    }
}