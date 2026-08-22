package io.github.tt432.eyelib.material.port;

import java.util.Objects;

/**
 * 描述渲染 Pass 的语义需求，替代 MC 的 RenderType 体系。
 *
 * @author TT432
 */
public interface PortRenderPass {

    /** 渲染 pass 的半透明类型 */
    Transparency transparency();

    /** 是否禁用背面剔除 */
    boolean disableCulling();

    /**
     * 获取 PortRenderPass 实例。10 种 (transparency × culling) 组合为共享规范实例，
     * 消除渲染热路径每帧的匿名类分配；实例间按值相等。
     *
     * @param transparency  半透明类型
     * @param disableCulling 是否禁用背面剔除
     * @return PortRenderPass 实例
     */
    static PortRenderPass of(Transparency transparency, boolean disableCulling) {
        return Cache.PASSES[transparency.ordinal()][disableCulling ? 1 : 0];
    }

    /** 规范实例缓存（接口常量须赶在首次 of() 前完成初始化，用 holder 类保证）。 */
    final class Cache {
        private static final PortRenderPass[][] PASSES;

        static {
            Transparency[] values = Transparency.values();
            PASSES = new PortRenderPass[values.length][2];
            for (Transparency transparency : values) {
                PASSES[transparency.ordinal()][0] = create(transparency, false);
                PASSES[transparency.ordinal()][1] = create(transparency, true);
            }
        }

        private static PortRenderPass create(Transparency transparency, boolean disableCulling) {
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
                public boolean equals(Object obj) {
                    return obj instanceof PortRenderPass other
                            && other.transparency() == transparency
                            && other.disableCulling() == disableCulling;
                }

                @Override
                public int hashCode() {
                    return Objects.hash(transparency, disableCulling);
                }

                @Override
                public String toString() {
                    return "PortRenderPass[" + transparency + ", cull=" + !disableCulling + "]";
                }
            };
        }

        private Cache() {
        }
    }

    enum Transparency {
        SOLID,
        ALPHA_TEST,
        TRANSLUCENT,
        TRANSLUCENT_EMISSIVE,
        ADDITIVE
    }
}
