package io.github.tt432.eyelib.client.render;

import java.util.Objects;

/**
 * 一条挂载声明（G3）：稳定 id + 锚点 + 绘制实现。
 * <p>
 * {@code identifier} 是**同帧去重键**（例如 {@code eyelib:held_item_main}、
 * {@code motions4mod:jewelry/hat}）：同一帧内同 id 只画第一次，重复声明计入诊断计数。
 * 内置代理与外部声明共用这条规则，因此「多个覆盖层重复绘制」不会以新形态复发
 * （历史缺陷见 ADR-0043 §2.3）。
 *
 * @param identifier 稳定 id（建议 {@code <modid>:<用途>}）
 * @param anchor     锚点解析
 * @param draw       绘制实现
 * @author TT432
 */
public record AttachmentDeclaration(String identifier, AttachmentAnchor anchor, AttachmentDraw draw) {

    public AttachmentDeclaration {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(draw, "draw");
    }
}
