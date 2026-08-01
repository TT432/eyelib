package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;

/**
 * 图中的一个节点实例。
 *
 * @param uid       实例 uid（图内唯一，稳定字符串）
 * @param type      节点类型 id（见 {@code NodeTypes}）
 * @param x         画布坐标 x
 * @param y         画布坐标 y
 * @param options   选项值（键 = 选项 id；未设的取类型默认）
 * @param constants 未连接输入端口的内联值（键 = 端口 id；优先级高于端口默认）
 */
public record NodeInstance(
        String uid,
        String type,
        float x,
        float y,
        Map<String, JsonElement> options,
        Map<String, JsonElement> constants
) {
    public static final Codec<NodeInstance> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("uid").forGetter(NodeInstance::uid),
            Codec.STRING.fieldOf("type").forGetter(NodeInstance::type),
            Codec.FLOAT.optionalFieldOf("x", 0f).forGetter(NodeInstance::x),
            Codec.FLOAT.optionalFieldOf("y", 0f).forGetter(NodeInstance::y),
            Codec.unboundedMap(Codec.STRING, GraphJson.ELEMENT_CODEC)
                    .optionalFieldOf("options", Map.of()).forGetter(NodeInstance::options),
            Codec.unboundedMap(Codec.STRING, GraphJson.ELEMENT_CODEC)
                    .optionalFieldOf("constants", Map.of()).forGetter(NodeInstance::constants)
    ).apply(ins, NodeInstance::new));

    public static NodeInstance of(String uid, String type, float x, float y) {
        return new NodeInstance(uid, type, x, y, Map.of(), Map.of());
    }

    /** 取实例自身的选项值（不含类型默认）。 */
    public Optional<JsonElement> optionRaw(String id) {
        return Optional.ofNullable(options.get(id));
    }

    /** 取 int 选项（实例值优先，否则给默认）。 */
    public int optionInt(String id, int defaultValue) {
        JsonElement own = options.get(id);
        if (own instanceof JsonPrimitive primitive && primitive.isNumber()) {
            return primitive.getAsInt();
        }
        return defaultValue;
    }

    /** 取 string 选项（实例值优先，否则给默认）。 */
    public String optionString(String id, String defaultValue) {
        JsonElement own = options.get(id);
        if (own instanceof JsonPrimitive primitive && primitive.isString()) {
            return primitive.getAsString();
        }
        return defaultValue;
    }

    /** 取选项值；缺省回落到类型默认值。 */
    public Optional<JsonElement> option(String id, NodeType nodeType) {
        JsonElement own = options.get(id);
        if (own != null) return Optional.of(own);
        return nodeType.option(id).map(NodeOptionDef::defaultValue);
    }
}
