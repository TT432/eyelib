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
 * @param suggestionKey 资产候选键（纯元数据；client 运行时从注册表供给候选，见规格 §4.1）
 */
public record NodeOptionDef(
        String id,
        OptionType type,
        JsonElement defaultValue,
        Optional<List<String>> choices,
        Optional<String> suggestionKey
) {
    public static final Codec<NodeOptionDef> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("id").forGetter(NodeOptionDef::id),
            OptionType.CODEC.fieldOf("type").forGetter(NodeOptionDef::type),
            GraphJson.ELEMENT_CODEC.fieldOf("default").forGetter(NodeOptionDef::defaultValue),
            Codec.STRING.listOf().optionalFieldOf("choices").forGetter(NodeOptionDef::choices),
            Codec.STRING.optionalFieldOf("suggestion").forGetter(NodeOptionDef::suggestionKey)
    ).apply(ins, NodeOptionDef::new));

    public static NodeOptionDef of(String id, OptionType type, JsonElement defaultValue) {
        return new NodeOptionDef(id, type, defaultValue, Optional.empty(), Optional.empty());
    }

    /** 带资产候选键的字符串选项（编辑器渲染为可输入下拉，不锁死自由输入）。 */
    public static NodeOptionDef asset(String id, String defaultValue, String suggestionKey) {
        return new NodeOptionDef(id, OptionType.STRING, new JsonPrimitive(defaultValue),
                Optional.empty(), Optional.of(suggestionKey));
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
        return new NodeOptionDef(id, OptionType.ENUM, new JsonPrimitive(defaultValue),
                Optional.of(choices), Optional.empty());
    }

    /** 颜色选项（#AARRGGBB 十六进制字符串；编辑器渲染为取色器）。 */
    public static NodeOptionDef color(String id, String defaultValue) {
        return of(id, OptionType.COLOR, new JsonPrimitive(defaultValue));
    }

    /** 字面值列表选项（JSON 数组；编辑器渲染为节点内列表编辑器——变长 call 参数用）。 */
    public static NodeOptionDef list(String id) {
        return of(id, OptionType.LIST, new com.google.gson.JsonArray());
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
        IDENTIFIER,
        /** 颜色（#AARRGGBB 十六进制字符串）。 */
        COLOR,
        /** 字面值列表（JSON 数组；变长参数等节点内列表编辑）。 */
        LIST;

        public static final Codec<OptionType> CODEC = PortStringRepresentable.fromEnum(OptionType::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
