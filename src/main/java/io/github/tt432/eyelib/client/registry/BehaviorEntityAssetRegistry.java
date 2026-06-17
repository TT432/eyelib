package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.BehaviorEntityManager;
import io.github.tt432.eyelib.common.behavior.BehaviorEntityRegistry;
import io.github.tt432.eyelib.common.behavior.BehaviorPackPublication;
import io.github.tt432.eyelib.importer.addon.BrBehaviorEntityFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 旧客户端行为实体发布入口；运行时权威注册表在 common 行为包边界。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public final class BehaviorEntityAssetRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(BehaviorEntityAssetRegistry.class);

    public static void replaceBehaviorEntities(Map<String, BrBehaviorEntityFile> behaviorEntities) {
        BehaviorPackPublication.replaceBehaviorEntities(behaviorEntities, LOGGER);
        BehaviorEntityManager.INSTANCE.replaceAll(BehaviorEntityRegistry.all());
    }
}
