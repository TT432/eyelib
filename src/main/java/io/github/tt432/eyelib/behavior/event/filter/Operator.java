package io.github.tt432.eyelibbehavior.event.filter;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelibutil.PortStringRepresentable;

/**
 * 比较操作符枚举，定义过滤器中的比较逻辑。
 *
 * @author TT432
 */
public enum Operator implements PortStringRepresentable {
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

    public static final Codec<Operator> CODEC = PortStringRepresentable.fromEnum(Operator::values);
    final String realString;

    Operator(String realString) {
        this.realString = realString;
    }

    @Override
    public String getSerializedName() {
        return realString;
    }
}