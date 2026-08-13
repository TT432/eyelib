package io.github.tt432.eyelib.behavior;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.TestCodecUtil;
import io.github.tt432.eyelib.behavior.component.property.Attack;
import io.github.tt432.eyelib.behavior.component.property.EntitySensor;
import io.github.tt432.eyelib.behavior.component.property.EnvironmentSensor;
import io.github.tt432.eyelib.behavior.component.property.Equipment;
import io.github.tt432.eyelib.behavior.component.property.Equippable;
import io.github.tt432.eyelib.behavior.component.property.Shooter;
import io.github.tt432.eyelib.behavior.component.property.SpawnEntity;
import io.github.tt432.eyelib.behavior.component.property.SpellEffects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * mcpack 兼容性回归测试：输入采用 Actions-and-Stuff mcpack 实际 JSON 形态
 * （见 docs/concepts/behavior-component-pitfalls.md「Bedrock 组件字段简写与可选性」节），
 * oracle 为 Mojang Creator 文档 EntityComponents 参考。
 *
 * @author TT432
 */
class McpackComponentCodecCompatTest {

    // ─── minecraft:spawn_entity ─────────────────
    // 文档：entities "Can be a single object or an array of objects"
    // （minecraftComponent_spawn_entity.md）

    @Test
    @DisplayName("spawn_entity: entities 单对象简写（mcpack armadillo 形态）")
    void spawnEntitySingleObjectShorthand() {
        // latest.log:86-88，armadillo 掉落 scute，仅给 spawn_item 不给 spawn_entity
        var json = """
                {"entities":{"spawn_item":"armadillo_scute","min_wait_time":300,"spawn_sound":"mob.armadillo.scute_drop","max_wait_time":600}}
                """;
        var parsed = TestCodecUtil.unwrap(SpawnEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
        assertEquals(1, parsed.entities().size());
        var entry = parsed.entities().get(0);
        assertEquals("", entry.spawn_entity()); // 文档：留空则生成 spawn_item 指定的物品
        assertEquals("armadillo_scute", entry.spawn_item());
        assertEquals(300, entry.min_wait_time());
        assertEquals(600, entry.max_wait_time());
        assertEquals("mob.armadillo.scute_drop", entry.spawn_sound());
    }

    @Test
    @DisplayName("spawn_entity: 单对象 + filters 对象（mcpack chicken 形态）")
    void spawnEntitySingleObjectWithFilters() {
        // latest.log:1746-1750，filters 为内联 JSON 对象
        var json = """
                {"entities":{"spawn_item":"egg","spawn_sound":"plop","min_wait_time":300,"filters":{"value":0,"test":"rider_count","subject":"self","operator":"=="},"max_wait_time":600}}
                """;
        var parsed = TestCodecUtil.unwrap(SpawnEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
        assertEquals(1, parsed.entities().size());
        var entry = parsed.entities().get(0);
        assertEquals("egg", entry.spawn_item());
        assertEquals("rider_count", entry.filters().get("test").getAsString());
    }

    @Test
    @DisplayName("spawn_entity: 数组形态保持兼容")
    void spawnEntityArrayForm() {
        var json = """
                {"entities":[{"spawn_entity":"minecraft:cow","spawn_item":"egg"},{"spawn_entity":"minecraft:pig","spawn_item":"egg"}]}
                """;
        var parsed = TestCodecUtil.unwrap(SpawnEntity.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
        assertEquals(2, parsed.entities().size());
        assertEquals("minecraft:cow", parsed.entities().get(0).spawn_entity());
        assertEquals("minecraft:pig", parsed.entities().get(1).spawn_entity());
    }

    // ─── minecraft:environment_sensor ───────────
    // 文档：triggers "Can be an array of trigger objects or a single trigger object"
    // （minecraftComponent_environment_sensor.md，Cave Spider 官方示例即单对象）

    @Test
    @DisplayName("environment_sensor: triggers 单对象简写（mcpack spider 形态）")
    void environmentSensorSingleTriggerShorthand() {
        // latest.log:1465-1467
        var json = """
                {"triggers":{"filters":{"operator":"<","value":0.49,"test":"is_brightness"},"event":"minecraft:become_hostile"}}
                """;
        var parsed = TestCodecUtil.unwrap(EnvironmentSensor.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
        assertEquals(1, parsed.triggers().size());
        var trigger = parsed.triggers().get(0);
        assertEquals("minecraft:become_hostile", trigger.event());
        assertEquals("self", trigger.target());
        assertEquals("is_brightness", trigger.filters().get("test").getAsString());
    }

    @Test
    @DisplayName("environment_sensor: 数组形态保持兼容")
    void environmentSensorArrayForm() {
        var json = """
                {"triggers":[{"event":"navigation_on_land"},{"event":"start_dryingout","target":"self"}]}
                """;
        var parsed = TestCodecUtil.unwrap(EnvironmentSensor.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
        assertEquals(2, parsed.triggers().size());
        assertEquals("navigation_on_land", parsed.triggers().get(0).event());
        assertEquals("start_dryingout", parsed.triggers().get(1).event());
    }

    // ─── minecraft:equippable ───────────────────
    // 文档：slots 条目 interact_text Default "*not set*"（可选）
    // （minecraftComponent_equippable.md）

    @Test
    @DisplayName("equippable: 鞍具槽省略 interact_text（mcpack camel 形态）")
    void equippableSlotWithoutInteractText() {
        // latest.log:1326-1328
        var json = """
                {"slots":[{"slot":0,"item":"saddle","on_equip":{"event":"minecraft:camel_saddled"},"on_unequip":{"event":"minecraft:camel_unsaddled"},"accepted_items":["saddle"]}]}
                """;
        var parsed = TestCodecUtil.unwrap(Equippable.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
        assertEquals(1, parsed.slots().size());
        var slot = parsed.slots().get(0);
        assertEquals(0, slot.slot());
        assertEquals("", slot.interact_text());
        assertEquals(List.of("saddle"), slot.accepted_items());
    }

    // ─── minecraft:shooter ──────────────────────
    // 文档：现行 schema 无 pots 字段（旧版遗留），全部属性可选
    // （minecraftComponent_shooter.md，Blaze 示例仅 {"def": ...}）

    @Test
    @DisplayName("shooter: 省略 pots 默认空表（mcpack blaze 形态）")
    void shooterWithoutPots() {
        // latest.log:1151-1154
        var json = """
                {"def":"minecraft:small_fireball"}
                """;
        var parsed = TestCodecUtil.unwrap(Shooter.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
        assertEquals("minecraft:small_fireball", parsed.def());
        assertTrue(parsed.pots().isEmpty());
    }

    // ─── minecraft:spell_effects ────────────────
    // 文档：key 为 add_effects / remove_effects，remove_effects 为单个 String；
    // 空组件 {} 合法（Player clear_raid_omen_spell_effect 示例）
    // （minecraftComponent_spell_effects.md）

    @Test
    @DisplayName("spell_effects: Bedrock 原生 key add_effects/remove_effects（mcpack 形态）")
    void spellEffectsBedrockNativeKeys() {
        // latest.log:895-897
        var json = """
                {"remove_effects":"wither","add_effects":[{"effect":"wither","duration":40,"display_on_screen_animation":true}]}
                """;
        var parsed = TestCodecUtil.unwrap(SpellEffects.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
        assertEquals(1, parsed.add_spell_effects().size());
        var entry = parsed.add_spell_effects().get(0);
        assertEquals("wither", entry.effect());
        assertEquals(40.0f, entry.duration(), 0.001f);
        assertEquals(0, entry.amplifier()); // mcpack 省略，默认 0
        assertEquals(List.of("wither"), parsed.remove_spell_effects());
    }

    @Test
    @DisplayName("spell_effects: 空组件 {} 合法（文档 Player 示例）")
    void spellEffectsEmptyComponent() {
        var parsed = TestCodecUtil.unwrap(SpellEffects.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")));
        assertTrue(parsed.add_spell_effects().isEmpty());
        assertTrue(parsed.remove_spell_effects().isEmpty());
    }

    // ─── minecraft:equipment ────────────────────
    // 文档：slot_drop_chance Default "*not set*"（可选）
    // （minecraftComponent_equipment.md）

    @Test
    @DisplayName("equipment: 仅给 table 省略 slot_drop_chance（mcpack skeleton 形态）")
    void equipmentWithoutSlotDropChance() {
        // debug.log:1276
        var json = """
                {"table":"loot_tables/entities/skeleton_gear.json"}
                """;
        var parsed = TestCodecUtil.unwrap(Equipment.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
        assertEquals("loot_tables/entities/skeleton_gear.json", parsed.table());
        assertTrue(parsed.slot_drop_chance().isEmpty());
    }

    // ─── minecraft:entity_sensor ────────────────
    // 文档：顶级属性仅 find_players_only / relative_range / subsensors，
    // 无顶级 event；subsensor range 为 [horizontal, vertical] 数组（默认 [10, 10]）
    // （minecraftComponent_entity_sensor.md，Parrot 官方示例）

    @Test
    @DisplayName("entity_sensor: 含 subsensors 时顶级 event 省略（mcpack 形态）")
    void entitySensorSubsensorsWithoutTopLevelEvent() {
        // latest.log:120（裁减至首个 subsensor，保留 range 数组与 event_filters 对象形态）
        var json = """
                {"subsensors":[{"range":[7.0,2.0],"minimum_count":0,"event_filters":{"any_of":[{"subject":"other","value":"undead","test":"is_family"}]},"event":"minecraft:no_threat_detected","cooldown":0.2,"maximum_count":0}]}
                """;
        var parsed = TestCodecUtil.unwrap(EntitySensor.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
        assertEquals("", parsed.event());
        assertEquals(1, parsed.subsensors().size());
        var sub = parsed.subsensors().get(0);
        assertEquals("minecraft:no_threat_detected", sub.event());
        assertEquals(7.0f, sub.range(), 0.001f); // [horizontal, vertical] 取水平距离
        assertTrue(sub.event_filters().has("any_of"));
    }

    @Test
    @DisplayName("entity_sensor: 顶级 event + 标量 range 形态保持兼容")
    void entitySensorTopLevelEventForm() {
        var json = """
                {"event":"minecraft:on_riding_player","range":10.0,"require_all":false}
                """;
        var parsed = TestCodecUtil.unwrap(EntitySensor.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
        assertEquals("minecraft:on_riding_player", parsed.event());
        assertTrue(parsed.subsensors().isEmpty());
    }

    // ─── minecraft:attack ───────────────────────
    // 文档：damage "Can be a number, an array [min, max], or an object with
    // range_min and range_max properties"（minecraftComponent_attack.md）

    @Test
    @DisplayName("attack: damage 支持 [min,max] 范围数组（mcpack 形态）")
    void attackDamageRangeArray() {
        // latest.log:2885
        var json = """
                {"damage":[3,9]}
                """;
        var parsed = TestCodecUtil.unwrap(Attack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)));
        assertEquals("[3, 9]", parsed.damage());
        assertEquals(0f, parsed.effect_duration(), 0.001f);
        assertEquals("", parsed.effect_name());
    }

    @Test
    @DisplayName("attack: 数字与字符串 damage 形态保持兼容")
    void attackDamageNumberAndStringForms() {
        var intForm = TestCodecUtil.unwrap(Attack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"damage\":7}")));
        assertEquals("7", intForm.damage());
        var stringForm = TestCodecUtil.unwrap(Attack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"damage\":\"query.attack_damage\"}")));
        assertEquals("query.attack_damage", stringForm.damage());
    }
}
