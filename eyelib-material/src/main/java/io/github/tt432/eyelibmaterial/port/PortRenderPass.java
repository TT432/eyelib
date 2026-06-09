package io.github.tt432.eyelibmaterial.port;

import org.jspecify.annotations.NullMarked;

/**
 * 描述渲染 Pass 的语义需求，替代 MC 的 RenderType 体系。
 *
 * @author TT432
 */
@NullMarked
public interface PortRenderPass {

    /** 渲染 pass 的半透明类型 */
    Transparency transparency();

    /** 是否禁用背面剔除 */
    boolean disableCulling();

    /**
     * 创建一个 PortRenderPass 实例。
     *
     * @param transparency  半透明类型
     * @param disableCulling 是否禁用背面剔除
     * @return PortRenderPass 实例
     */
    static PortRenderPass of(Transparency transparency, boolean disableCulling) {
        return new PortRenderPass() {
            @Override
            public Transparency transparency() {
                return transparency;
            }

            @Override
            public boolean disableCulling() {
                return disableCulling;
            }

            @Override
            public String toString() {
                return "PortRenderPass[" + transparency + ", cull=" + !disableCulling + "]";
            }
        };
    }

    enum Transparency {
        SOLID,
        ALPHA_TEST,
        TRANSLUCENT,
        ADDITIVE
    }
}
