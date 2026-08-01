package io.github.tt432.eyelib.molang.type;

/**
 * 实体引用包装：箭头访问（{@code ->}）左侧查询（如 {@code query.target}）的返回值类型。
 * <p>
 * 携带宿主实体对象（domain 侧通常为 {@link io.github.tt432.eyelib.molang.port.PortEntity}），
 * 由 {@link io.github.tt432.eyelib.molang.port.ArrowHostInstaller} 翻译后注入宿主上下文；
 * 作为 Molang 值求值时始终为 0/false/空串（实体引用本身没有数值语义）。
 *
 * @author TT432
 */
public record MolangEntityRef(
        Object entity
) implements MolangObject {
    @Override
    public float asFloat() {
        return 0;
    }

    @Override
    public boolean asBoolean() {
        return false;
    }

    @Override
    public String asString() {
        return "";
    }

    @Override
    public boolean isNumber() {
        return false;
    }
}
