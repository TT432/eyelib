package io.github.tt432.eyelib.client.render;

import io.github.tt432.eyelib.molang.EngineEntityVariable;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderPorts;
import io.github.tt432.eyelib.molang.MolangScope;
import net.minecraft.world.entity.Entity;

/**
 * Bedrock 引擎变量（{@link EngineEntityVariable}）到 {@link MolangScope} 的统一绑定。
 *
 * <p>Bedrock 的部分 {@code variable.*} 由引擎按实体状态提供（官方文档称之为 entity variables），
 * 资源包不写、只读。Java 侧由宿主通过
 * {@link io.github.tt432.eyelib.bridge.client.adapter.EntityRenderPorts.RenderSystemPort#engineVariable}
 * 解析；本类负责把它们挂到实体 scope 上：
 *
 * <ul>
 *   <li>只绑定宿主声明适用的变量（端口返回 {@link Float#NaN} 表示不适用）；</li>
 *   <li>已存在同名变量时<b>不覆盖</b>——资源包 {@code scripts.initialize}/{@code animate}
 *       或动画关键帧写过的值优先（Bedrock 里变量就是实体上的可写存储）；</li>
 *   <li>绑定为惰性供应商，读时取当前值（如羊驼装备地毯变化后下一帧即生效），
 *       不做逐帧 {@code set} 写入。</li>
 * </ul>
 *
 * <p>变量名用官方规范写法；Molang 标识符大小写不敏感（{@link MolangScope} 在存取时统一规范化）。
 *
 * @author TT432
 */
final class EngineEntityVariables {
    private EngineEntityVariables() {
    }

    static void bind(Entity entity, MolangScope scope) {
        var port = RenderPorts.get().renderSystemPort();
        for (EngineEntityVariable variable : EngineEntityVariable.values()) {
            String name = variable.molangName();
            if (scope.contains(name)) {
                continue;
            }
            if (Float.isNaN(port.engineVariable(entity, variable))) {
                continue;
            }
            scope.set(name, () -> port.engineVariable(entity, variable));
        }
    }
}
