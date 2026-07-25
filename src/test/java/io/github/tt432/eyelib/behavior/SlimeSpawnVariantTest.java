package io.github.tt432.eyelib.behavior;

import io.github.tt432.eyelib.behavior.component.Variant;
import io.github.tt432.eyelib.behavior.event.logic.LogicNode;
import io.github.tt432.eyelib.common.behavior.BehaviorPackPublication;
import io.github.tt432.eyelib.common.behavior.VanillaBehaviorEntityLoader;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内置 vanilla 行为包史莱姆 spawn 链验证：
 * {@code minecraft:entity_spawned}（randomize → trigger → add component_group）
 * 必须产出 variant ∈ {1, 2, 4}——variant 是史莱姆尺寸层级的唯一真源。
 */
class SlimeSpawnVariantTest {

    @Test
    void slimeSpawnEventAssignsVariantFromVanillaBehaviorPack() {
        // 不存在的目录 → 回退到 classpath 内置 mcpack（data/eyelib/vanilla_behavior_pack.mcpack）
        var files = VanillaBehaviorEntityLoader.load(Path.of("__missing_game_dir__"));
        assertFalse(files.isEmpty(), "vanilla behavior pack must load from classpath mcpack");
        BehaviorPackPublication.mergeBehaviorEntities(files, LoggerFactory.getLogger(SlimeSpawnVariantTest.class));

        BehaviorEntity slime = BehaviorEntityRegistry.get("minecraft:slime");
        assertNotNull(slime, "minecraft:slime must be present in vanilla behavior pack");
        LogicNode spawnEvent = slime.events().get("minecraft:entity_spawned");
        assertNotNull(spawnEvent, "slime must declare minecraft:entity_spawned");

        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 30; i++) {
            EntityBehaviorData data = new EntityBehaviorData(Optional.of(slime), new ArrayList<>());
            spawnEvent.eval(data);
            data.setup();
            Variant variant = data.component(Variant.class);
            assertNotNull(variant, "entity_spawned must assign minecraft:variant");
            assertTrue(Set.of(1, 2, 4).contains(variant.value()),
                    "slime variant must be a size level in {1, 2, 4}, got " + variant.value());
            seen.add(variant.value());
        }
        assertTrue(seen.size() >= 2, "randomize should yield distinct variants across trials, got " + seen);
    }
}
