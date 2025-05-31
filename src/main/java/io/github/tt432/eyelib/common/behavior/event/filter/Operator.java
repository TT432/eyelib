package io.github.tt432.eyelib.common.behavior.event.filter;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * @author TT432
 */
public enum Operator implements StringRepresentable {
    NEQ("!="),
    EQ("=="),
    LESS("<"),
    GREATER(">"),
    LESS_EQUAL("<="),
    GREATER_EQUAL(">="),
    NEQ2("<>"),
    EQ2("="),
    EQUALS("equals"),
    NOT("not");

    public static final Codec<Operator> CODEC = StringRepresentable.fromEnum(Operator::values);
    final String realString;

    Operator(String realString) {
        this.realString = realString;
    }

    @Override
    public @NotNull String getSerializedName() {
        return realString;
    }
}
