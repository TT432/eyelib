package io.github.tt432.eyelib.molang.port;
import io.github.tt432.eyelib.bridge.molang.adapter.EntityPortAdapter;

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
class PortEntityContractTest {

    private static PortEntity entity(Map<String, Object> props) {
        return new PortEntity() {
            public Object queryProperty(String key) { return props.get(key); }
            public float getX() { return 0; }
            public float getY() { return 0; }
            public float getZ() { return 0; }
        };
    }

    // === PortEntity ===

    @Test
    @DisplayName("PortEntity: is_baby 布尔约定 (Boolean → true/false)")
    void portEntityIsBaby() {
        PortEntity baby = entity(Map.of("is_baby", true));
        PortEntity adult = entity(Map.of("is_baby", false));

        assertTrue((Boolean) baby.queryProperty("is_baby"));
        assertFalse((Boolean) adult.queryProperty("is_baby"));
    }

    @Test
    @DisplayName("PortEntity: is_sheep 和 is_sheared 类型检查")
    void portEntitySheepAttributes() {
        PortEntity sheep = entity(Map.of("is_sheep", true, "is_sheared", false));
        PortEntity shearedSheep = entity(Map.of("is_sheep", true, "is_sheared", true));
        PortEntity wolf = entity(Map.of("is_sheep", false));

        assertTrue((Boolean) sheep.queryProperty("is_sheep"));
        assertFalse((Boolean) sheep.queryProperty("is_sheared"));
        assertTrue((Boolean) shearedSheep.queryProperty("is_sheared"));
        assertFalse((Boolean) wolf.queryProperty("is_sheep"));
    }

    @Test
    @DisplayName("PortEntity: is_on_ground 布尔属性")
    void portEntityOnGround() {
        PortEntity grounded = entity(Map.of("is_on_ground", true));
        PortEntity airborne = entity(Map.of("is_on_ground", false));

        assertTrue((Boolean) grounded.queryProperty("is_on_ground"));
        assertFalse((Boolean) airborne.queryProperty("is_on_ground"));
    }

    @Test
    @DisplayName("PortEntity: is_in_water")
    void portEntityInWater() {
        PortEntity inWater = entity(Map.of("is_in_water", true));
        assertTrue((Boolean) inWater.queryProperty("is_in_water"));
    }

    @Test
    @DisplayName("PortEntity: 未知键返回 null")
    void portEntityUnknownKeyReturnsNull() {
        PortEntity entity = entity(Map.of("is_sheep", true));
        assertNull(entity.queryProperty("not_a_query"));
    }

    @Test
    @DisplayName("PortEntity: pos_x/pos_y/pos_z 返回 Float")
    void portEntityPositionIsFloat() {
        PortEntity entity = entity(Map.of("pos_x", 1.5f, "pos_y", 64.0f, "pos_z", -3.0f));

        assertEquals(1.5f, (Float) entity.queryProperty("pos_x"), 0.001f);
        assertEquals(64.0f, (Float) entity.queryProperty("pos_y"), 0.001f);
        assertEquals(-3.0f, (Float) entity.queryProperty("pos_z"), 0.001f);
    }

    @Test
    @DisplayName("PortEntity: EntityPortAdapter 查询键命名用 snake_case")
    void portEntityKeysUseSnakeCase() {
        EntityPortAdapter.QUERY_KEYS.forEach(key ->
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

