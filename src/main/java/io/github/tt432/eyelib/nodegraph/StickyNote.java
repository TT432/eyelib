package io.github.tt432.eyelib.nodegraph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 便签（纯文档注释，不参与语义）。
 */
public record StickyNote(
        String uid,
        String text,
        float x,
        float y,
        float width,
        float height,
        String color
) {
    public static final Codec<StickyNote> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("uid").forGetter(StickyNote::uid),
            Codec.STRING.optionalFieldOf("text", "").forGetter(StickyNote::text),
            Codec.FLOAT.optionalFieldOf("x", 0f).forGetter(StickyNote::x),
            Codec.FLOAT.optionalFieldOf("y", 0f).forGetter(StickyNote::y),
            Codec.FLOAT.optionalFieldOf("width", 200f).forGetter(StickyNote::width),
            Codec.FLOAT.optionalFieldOf("height", 100f).forGetter(StickyNote::height),
            Codec.STRING.optionalFieldOf("color", "#FFFF88").forGetter(StickyNote::color)
    ).apply(ins, StickyNote::new));
}
