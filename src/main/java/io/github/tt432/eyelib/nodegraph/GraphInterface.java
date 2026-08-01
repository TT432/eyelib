package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;

/**
 * 子图接口：命名输入参数 + 单一输出（表达式子图，见规格 §2/D5）。
 *
 * @param inputs 输入参数（调用点按名映射到实参端口）
 * @param output 输出（子图结果值）
 */
public record GraphInterface(
        List<Param> inputs,
        Param output
) {
    public static final Codec<GraphInterface> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Param.CODEC.listOf().optionalFieldOf("inputs", List.of()).forGetter(GraphInterface::inputs),
            Param.CODEC.fieldOf("output").forGetter(GraphInterface::output)
    ).apply(ins, GraphInterface::new));

    /**
     * 接口参数。
     *
     * @param name         参数名
     * @param type         类型
     * @param defaultValue 输入参数缺省值（仅 inputs 有意义）
     */
    public record Param(String name, PortType type, Optional<JsonElement> defaultValue) {
        public static final Codec<Param> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("name").forGetter(Param::name),
                PortType.CODEC.fieldOf("type").forGetter(Param::type),
                GraphJson.ELEMENT_CODEC.optionalFieldOf("default").forGetter(Param::defaultValue)
        ).apply(ins, Param::new));

        public static Param of(String name, PortType type) {
            return new Param(name, type, Optional.empty());
        }
    }
}
