package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.util.manager.ManagerEventPublishBridge;
import io.github.tt432.eyelib.util.registry.Registry;
import io.github.tt432.eyelib.util.registry.RegistryChange;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

/**
 * 客户端实体定义注册表与按 id 解析入口。
 * <p>
 * 解析规则（顺序固定）：精确 id → BE 实体别名（{@code villager → villager_v2}）；
 * 资源定位符重载额外先尝试裸路径（{@code minecraft:doki_x → doki_x}）。
 * 所有解析都收敛在这里——绑定方（{@code bindById}）只需保存调用方的**原始** id，
 * 不关心查找宽容度。
 *
 * @author TT432
 */
public final class ClientEntityManager {
    public static final Registry<BrClientEntity> INSTANCE =
            new Registry<>("ClientEntityManager", ManagerEventPublishBridge.publisher());

    /**
     * BE 重命名实体的 JE 类型别名：BE 村与掠夺更新把 villager/zombie_villager 重命名为 *_v2，
     * 资源包（如 A&S）只注册新名；JE 1.20.1 实体类型仍是旧名，查表时回退别名。
     */
    private static final Map<String, String> JE_TO_BE_ENTITY_ALIAS = Map.of(
            "minecraft:villager", "minecraft:villager_v2",
            "minecraft:zombie_villager", "minecraft:zombie_villager_v2"
    );

    private ClientEntityManager() {
    }

    /** 按实体类型 id 解析（精确 → JE 别名）。 */
    public static @Nullable BrClientEntity byId(String entityId) {
        BrClientEntity direct = INSTANCE.get(entityId);
        if (direct != null) {
            return direct;
        }
        String alias = JE_TO_BE_ENTITY_ALIAS.get(entityId);
        return alias == null ? null : INSTANCE.get(alias);
    }

    /** 按「全名 → 裸路径 → 别名」解析（{@code ns:path} 与 {@code path} 两种写法都接受）。 */
    public static @Nullable BrClientEntity byLocation(String fullId, String path) {
        BrClientEntity exact = INSTANCE.get(fullId);
        if (exact != null) {
            return exact;
        }
        BrClientEntity bare = INSTANCE.get(path);
        return bare != null ? bare : byId(fullId);
    }

    /** 订阅注册表变更（类型化、可注销）；代际比较仍是正确性机制，事件用于通知/清理。 */
    public static AutoCloseable addListener(Consumer<RegistryChange<BrClientEntity>> listener) {
        return INSTANCE.addListener(listener);
    }

    /** 注册表代际（单调递增），供绑定方判断是否需要重新解析。 */
    public static long generation() {
        return INSTANCE.generation();
    }
}
