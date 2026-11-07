package io.github.tt432.eyelib.molang;

/**
 * 由宿主游戏按实体状态解析的 Bedrock 引擎变量（{@code variable.*}）。
 *
 * <p>Bedrock 的部分 <b>entity variable</b> 不是资源包脚本写入的，而是引擎内置：
 * 典型例子是羊驼的 {@code variable.DecorTextureIndex}（官方
 * {@code controller.render.llama} 用它选择装饰层；值为 0 表示无装饰，1..16 = 地毯颜色 id + 1）。
 * 这类变量在 Java 侧必须由宿主按 JE 实体状态推导，资源包只需按官方写法引用。
 *
 * <p>解析入口是 {@code EntityRenderPorts.RenderSystemPort#engineVariable}；application 层
 * 通过统一绑定把值挂到 {@link MolangScope}（见 {@code EngineEntityVariables}），
 * 宿主实现里再处理版本差异的实体类型/装备槽。
 *
 * @author TT432
 */
public enum EngineEntityVariable {
    /** 羊驼装饰层索引：0 = 无，1..16 = 地毯颜色 id + 1（Bedrock 引擎内置变量）。 */
    DECOR_TEXTURE_INDEX("variable.DecorTextureIndex");

    private final String molangName;

    EngineEntityVariable(String molangName) {
        this.molangName = molangName;
    }

    /** 资源包中引用的规范写法（Molang 标识符大小写不敏感，此处保留官方拼写）。 */
    public String molangName() {
        return molangName;
    }
}
