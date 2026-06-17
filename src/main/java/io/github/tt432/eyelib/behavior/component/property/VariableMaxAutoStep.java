package io.github.tt432.eyelib.behavior.component.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.behavior.component.Component;

/**
 * minecraft:variable_max_auto_step
 *
 * @param base_value           默认 0.6f
 * @param controlled_value     默认 1.0f
 * @param jump_prevented_value 默认 0.6f
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public record VariableMaxAutoStep(
        float base_value,
        float controlled_value,
        float jump_prevented_value
) implements Component {
    public static final Codec<VariableMaxAutoStep> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.FLOAT.optionalFieldOf("base_value", 0.6f).forGetter(VariableMaxAutoStep::base_value),
            Codec.FLOAT.optionalFieldOf("controlled_value", 1.0f).forGetter(VariableMaxAutoStep::controlled_value),
            Codec.FLOAT.optionalFieldOf("jump_prevented_value", 0.6f).forGetter(VariableMaxAutoStep::jump_prevented_value)
    ).apply(ins, VariableMaxAutoStep::new));

    @Override
    public String id() {
        return "variable_max_auto_step";
    }
}
