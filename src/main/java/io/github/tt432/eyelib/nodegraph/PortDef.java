package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * 端口定义（节点类型侧）。
 *
 * @param id          端口 id（节点内唯一、稳定，wire 以其引用）
 * @param direction   方向
 * @param type        端口类型
 * @param defaultValue 未连接输入的内联默认值（仅 IN 有意义；空 = 无默认，未连接即错误）
 * @param multi       是否多连接（OUT 默认 true；SLOT 类入口为 true）
 */
public record PortDef(
        String id,
        PortDirection direction,
        PortType type,
        Optional<JsonElement> defaultValue,
        boolean multi
) {
    public static final Codec<PortDef> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("id").forGetter(PortDef::id),
            PortDirection.CODEC.fieldOf("direction").forGetter(PortDef::direction),
            PortType.CODEC.fieldOf("type").forGetter(PortDef::type),
            GraphJson.ELEMENT_CODEC.optionalFieldOf("default").forGetter(PortDef::defaultValue),
            Codec.BOOL.optionalFieldOf("multi", false).forGetter(PortDef::multi)
    ).apply(ins, PortDef::new));

    public static PortDef in(String id, PortType type) {
        return new PortDef(id, PortDirection.IN, type, Optional.empty(), false);
    }

    public static PortDef in(String id, PortType type, JsonElement defaultValue) {
        return new PortDef(id, PortDirection.IN, type, Optional.of(defaultValue), false);
    }

    public static PortDef inMulti(String id, PortType type) {
        return new PortDef(id, PortDirection.IN, type, Optional.empty(), true);
    }

    public static PortDef out(String id, PortType type) {
        return new PortDef(id, PortDirection.OUT, type, Optional.empty(), true);
    }

    public static PortDef outSingle(String id, PortType type) {
        return new PortDef(id, PortDirection.OUT, type, Optional.empty(), false);
    }
}
