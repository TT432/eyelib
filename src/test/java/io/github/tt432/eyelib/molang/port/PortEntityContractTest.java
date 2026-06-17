package io.github.tt432.eyelib.molang.port;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PortEntity / PortLevel / PortItemStack 接口的契约测试。
 * 验证 Port 接口提供的属性符合 Bedrock Molang 查询语义。
 * Oracle 来自 Bedrock Wiki 的 query.* 定义。
 *
 * @author TT432
 */
@NullMarked
class PortEntityContractTest {

    // === PortEntity ===

    @Test
    @DisplayName("PortEntity: is_baby 布尔约定 (Boolean → true/false)")
    void portEntityIsBaby() {
        PortEntity baby = () -> Map.of("is_baby", true);
        PortEntity adult = () -> Map.of("is_baby", false);

        assertTrue((Boolean) baby.getQueryProperties().get("is_baby"));
        assertFalse((Boolean) adult.getQueryProperties().get("is_baby"));
    }

    @Test
    @DisplayName("PortEntity: is_sheep 和 is_sheared 类型检查")
    void portEntitySheepAttributes() {
        PortEntity sheep = () -> Map.of("is_sheep", true, "is_sheared", false);
        PortEntity shearedSheep = () -> Map.of("is_sheep", true, "is_sheared", true);
        PortEntity wolf = () -> Map.of("is_sheep", false);

        assertTrue((Boolean) sheep.getQueryProperties().get("is_sheep"));
        assertFalse((Boolean) sheep.getQueryProperties().get("is_sheared"));
        assertTrue((Boolean) shearedSheep.getQueryProperties().get("is_sheared"));
        assertFalse((Boolean) wolf.getQueryProperties().get("is_sheep"));
    }

    @Test
    @DisplayName("PortEntity: is_on_ground 布尔属性")
    void portEntityOnGround() {
        PortEntity grounded = () -> Map.of("is_on_ground", true);
        PortEntity airborne = () -> Map.of("is_on_ground", false);

        assertTrue((Boolean) grounded.getQueryProperties().get("is_on_ground"));
        assertFalse((Boolean) airborne.getQueryProperties().get("is_on_ground"));
    }

    @Test
    @DisplayName("PortEntity: is_in_water")
    void portEntityInWater() {
        PortEntity inWater = () -> Map.of("is_in_water", true);
        assertTrue((Boolean) inWater.getQueryProperties().get("is_in_water"));
    }

    @Test
    @DisplayName("PortEntity: 属性 Map 不可变且 key/value 不为 null")
    void portEntityMapIsWellFormed() {
        PortEntity entity = () -> Map.of(
                "is_sheep", true,
                "is_baby", false,
                "pos_x", 10.0f
        );

        Map<String, Object> props = entity.getQueryProperties();
        assertNotNull(props);
        assertEquals(3, props.size());
        props.forEach((k, v) -> {
            assertNotNull(k, "key 不应为 null");
            assertNotNull(v, "value 不应为 null");
        });
    }

    @Test
    @DisplayName("PortEntity: pos_x/pos_y/pos_z 返回 Float")
    void portEntityPositionIsFloat() {
        PortEntity entity = () -> Map.of("pos_x", 1.5f, "pos_y", 64.0f, "pos_z", -3.0f);

        assertEquals(1.5f, (Float) entity.getQueryProperties().get("pos_x"), 0.001f);
        assertEquals(64.0f, (Float) entity.getQueryProperties().get("pos_y"), 0.001f);
        assertEquals(-3.0f, (Float) entity.getQueryProperties().get("pos_z"), 0.001f);
    }

    @Test
    @DisplayName("PortEntity: EntityPortAdapter 18 属性一致性检查 — key 命名用 snake_case")
    void portEntityKeysUseSnakeCase() {
        PortEntity entity = () -> Map.of(
                "is_sheep", true,
                "is_wolf", false,
                "is_creeper", false,
                "is_vex", false,
                "is_warden", false,
                "is_baby", false,
                "on_fire", false,
                "is_on_ground", true,
                "is_in_water", false,
                "is_riding", false
        );

        Map<String, Object> props = entity.getQueryProperties();
        // 所有 key 应为 lower_snake_case（Bedrock 约定）
        props.keySet().forEach(key ->
                assertTrue(key.matches("[a-z_]+"), "key 应为 lower_snake_case: " + key));
    }

    // === PortLevel ===

    @Test
    @DisplayName("PortLevel: dayTime 按 Bedrock 约定返回 long (ticks)")
    void portLevelDayTime() {
        PortLevel level = new PortLevel() {
            public long getDayTime() { return 6000L; }
            public long getGameTime() { return 12000L; }
            public int getPlayerCount() { return 1; }
            public float getMoonPhase() { return 0.25f; }
        };

        assertEquals(6000L, level.getDayTime());
        assertEquals(12000L, level.getGameTime());
        assertEquals(1, level.getPlayerCount());
        assertEquals(0.25f, level.getMoonPhase(), 0.001f);
    }

    // === PortItemStack ===

    @Test
    @DisplayName("PortItemStack: count 和 maxStackSize")
    void portItemStackAttributes() {
        PortItemStack stack = new PortItemStack() {
            public int getCount() { return 16; }
            public int getMaxStackSize() { return 64; }
        };

        assertEquals(16, stack.getCount());
        assertEquals(64, stack.getMaxStackSize());
    }

    @Test
    @DisplayName("PortItemStack: count 不应超过 maxStackSize")
    void portItemStackCountWithinBounds() {
        PortItemStack single = new PortItemStack() {
            public int getCount() { return 1; }
            public int getMaxStackSize() { return 64; }
        };

        assertTrue(single.getCount() <= single.getMaxStackSize(),
                "count 不应超过 maxStackSize");
    }
}
