package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.PortStringRepresentable;
import java.util.Locale;
import java.util.Optional;

/**
 * 黑板变量声明。
 *
 * @param name         变量名（不带根；molang 根由 scope 决定：{@code variable.<name>} 或
 *                     {@code temp.<name>}）
 * @param type         值类型
 * @param group        分组路径（空 = 未分组；形如 {@code "a/b"}）
 * @param defaultValue 默认值（空 = 0；VARIABLE 作用域且非空时导出为 initialize 初始化语句）
 * @param scope        molang 作用域（规格 nodegraph-variable-table §2.1）；缺省 VARIABLE
 */
public record VariableDecl(
        String name,
        PortType type,
        Optional<String> group,
        Optional<JsonElement> defaultValue,
        Scope scope
) {
    /** 兼容构造：缺省 VARIABLE（v6 文件无 scope 字段的语义）。 */
    public VariableDecl(String name, PortType type, Optional<String> group,
                        Optional<JsonElement> defaultValue) {
        this(name, type, group, defaultValue, Scope.VARIABLE);
    }

    public static final Codec<VariableDecl> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("name").forGetter(VariableDecl::name),
            PortType.CODEC.fieldOf("type").forGetter(VariableDecl::type),
            Codec.STRING.optionalFieldOf("group").forGetter(VariableDecl::group),
            GraphJson.ELEMENT_CODEC.optionalFieldOf("default").forGetter(VariableDecl::defaultValue),
            Scope.CODEC.optionalFieldOf("scope", Scope.VARIABLE).forGetter(VariableDecl::scope)
    ).apply(ins, VariableDecl::new));

    public static VariableDecl of(String name, PortType type) {
        return new VariableDecl(name, type, Optional.empty(), Optional.empty(), Scope.VARIABLE);
    }

    /**
     * molang 作用域：VARIABLE = 实体级 {@code variable.*}（跨图跨文件，默认值可导出初始化）；
     * TEMP = 求值级 {@code temp.*}（仅当次求值内有效，默认值不导出）。
     */
    public enum Scope implements PortStringRepresentable {
        VARIABLE,
        TEMP;

        public static final Codec<Scope> CODEC = PortStringRepresentable.fromEnum(Scope::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
