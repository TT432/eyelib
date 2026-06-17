package io.github.tt432.eyelib.material.material;

import io.github.tt432.eyelib.material.gl.BlendFactor;
import io.github.tt432.eyelib.material.gl.DepthFunc;
import io.github.tt432.eyelib.material.gl.GLStates;
import io.github.tt432.eyelib.material.gl.stencil.Face;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Bedrock 材质继承归并后的运行时语义输入。
 *
 * @author TT432
 */
public record ResolvedBrMaterial(
        String name,
        List<String> inheritanceChain,
        Optional<String> vertexShader,
        Optional<String> fragmentShader,
        Set<String> defines,
        Set<GLStates> states,
        List<BrSamplerState> samplerStates,
        Optional<DepthFunc> depthFunc,
        BlendState blend,
        StencilState stencil,
        List<BrMaterialEntry> variants
) {
    public boolean hasDefine(String define) {
        return defines.contains(define);
    }

    public boolean hasState(GLStates state) {
        return states.contains(state);
    }

    public boolean hasShaders() {
        return vertexShader.isPresent() && fragmentShader.isPresent();
    }

    public record BlendState(
            BlendFactor blendSrc,
            BlendFactor blendDst,
            BlendFactor alphaSrc,
            BlendFactor alphaDst
    ) {
        public static final BlendState DEFAULT = new BlendState(
                BlendFactor.SourceAlpha,
                BlendFactor.OneMinusSrcAlpha,
                BlendFactor.One,
                BlendFactor.OneMinusSrcAlpha
        );

        public boolean isDefaultTranslucent() {
            return blendSrc == BlendFactor.SourceAlpha
                    && blendDst == BlendFactor.OneMinusSrcAlpha
                    && alphaSrc == BlendFactor.One
                    && alphaDst == BlendFactor.OneMinusSrcAlpha;
        }

        public boolean isAdditive() {
            return blendDst == BlendFactor.One
                    || blendSrc == BlendFactor.One && blendDst == BlendFactor.OneMinusSrcAlpha;
        }
    }

    public record StencilState(
            int stencilRef,
            int stencilRefOverride,
            int stencilReadMask,
            int stencilWriteMask,
            Face frontFace,
            Face backFace
    ) {
        public static final StencilState DEFAULT = new StencilState(
                0,
                0,
                0xFF,
                0xFF,
                Face.DEFAULT_FRONT,
                Face.DEFAULT_BACK
        );
    }
}
