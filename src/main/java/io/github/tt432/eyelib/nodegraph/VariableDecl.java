package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * 黑板变量声明。
 *
 * @param name         变量名（molang 中 {@code variable.<name>}）
 * @param type         值类型
 * @param group        分组路径（空 = 未分组；形如 {@code "a/b"}）
 * @param defaultValue 默认值（空 = 0）
 */
public record VariableDecl(
        String name,
        PortType type,
        Optional<String> group,
        Optional<JsonElement> defaultValue
) {
    public static final Codec<VariableDecl> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("name").forGetter(VariableDecl::name),
            PortType.CODEC.fieldOf("type").forGetter(VariableDecl::type),
            Codec.STRING.optionalFieldOf("group").forGetter(VariableDecl::group),
            GraphJson.ELEMENT_CODEC.optionalFieldOf("default").forGetter(VariableDecl::defaultValue)
    ).apply(ins, VariableDecl::new));

    public static VariableDecl of(String name, PortType type) {
        return new VariableDecl(name, type, Optional.empty(), Optional.empty());
    }
}
