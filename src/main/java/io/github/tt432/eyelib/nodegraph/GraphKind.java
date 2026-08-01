package io.github.tt432.eyelib.nodegraph;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.util.PortStringRepresentable;
import java.util.Locale;

/** 图文档库种类。 */
public enum GraphKind implements PortStringRepresentable {
    /** ClientEntity 制作文档（主图含唯一 entity_root）。 */
    CLIENT_ENTITY,
    /** RenderController 制作文档（主图含唯一 rc_root）。 */
    RENDER_CONTROLLER,
    /** AnimationController 制作文档（主图含唯一 ac_root）。 */
    ANIMATION_CONTROLLER,
    /** 纯子图库（无主图语义，仅供其他库引用——v1 保留，引用机制为库内子图）。 */
    EXPRESSION_LIB;

    public static final Codec<GraphKind> CODEC = PortStringRepresentable.fromEnum(GraphKind::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
