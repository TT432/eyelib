package io.github.tt432.eyelib.nodegraph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 连线：from（输出侧）→ to（输入侧）。
 *
 * @param from 输出端口
 * @param to   输入端口
 */
public record Wire(PortRef from, PortRef to) {
    public static final Codec<Wire> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            PortRef.CODEC.fieldOf("from").forGetter(Wire::from),
            PortRef.CODEC.fieldOf("to").forGetter(Wire::to)
    ).apply(ins, Wire::new));
}
