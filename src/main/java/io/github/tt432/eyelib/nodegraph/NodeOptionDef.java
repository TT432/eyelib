package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.PortStringRepresentable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 节点选项定义（节点类型侧的实例配置项，如二元运算符种类、变量名、常量值）。
 *
 * @param id           选项 id（节点内唯一）
 * @param type         选项值类型
 * @param defaultValue 默认值
 * @param choices      ENUM 类型的可选值
 */
public record NodeOptionDef(
        String id,
        OptionType type,
        JsonElement defaultValue,
        Optional<List<String>> choices
) {
    public static final Codec<NodeOptionDef> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("id").forGetter(NodeOptionDef::id),
            OptionType.CODEC.fieldOf("type").forGetter(NodeOptionDef::type),
            GraphJson.ELEMENT_CODEC.fieldOf("default").forGetter(NodeOptionDef::defaultValue),
            Codec.STRING.listOf().optionalFieldOf("choices").forGetter(NodeOptionDef::choices)
    ).apply(ins, NodeOptionDef::new));

    public static NodeOptionDef of(String id, OptionType type, JsonElement defaultValue) {
        return new NodeOptionDef(id, type, defaultValue, Optional.empty());
    }

    public static NodeOptionDef string(String id, String defaultValue) {
        return of(id, OptionType.STRING, new JsonPrimitive(defaultValue));
    }

    public static NodeOptionDef number(String id, double defaultValue) {
        return of(id, OptionType.FLOAT, new JsonPrimitive(defaultValue));
    }

    public static NodeOptionDef integer(String id, int defaultValue) {
        return of(id, OptionType.INT, new JsonPrimitive(defaultValue));
    }

    public static NodeOptionDef bool(String id, boolean defaultValue) {
        return of(id, OptionType.BOOL, new JsonPrimitive(defaultValue));
    }

    public static NodeOptionDef enumeration(String id, String defaultValue, List<String> choices) {
        return new NodeOptionDef(id, OptionType.ENUM, new JsonPrimitive(defaultValue), Optional.of(choices));
    }

    /** 选项值类型。 */
    public enum OptionType implements PortStringRepresentable {
        STRING,
        /** 多行文本（如原始 JSON 配置）。 */
        TEXT,
        FLOAT,
        INT,
        BOOL,
        ENUM,
        /** 资源标识符（namespace:path 形态，仅语义标注）。 */
        IDENTIFIER;

        public static final Codec<OptionType> CODEC = PortStringRepresentable.fromEnum(OptionType::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
