package io.github.tt432.eyelibimporter.addon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelibimporter.util.ImporterCodecUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Bedrock 战利品表数据结构。
 * @author TT432 */
@org.jspecify.annotations.NullMarked
public record BrLootTable(
        List<BrLootTablePool> pools
) {
    public static final Codec<BrLootTable> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            BrLootTablePool.CODEC.listOf().fieldOf("pools").forGetter(BrLootTable::pools)
    ).apply(ins, BrLootTable::new));

    public BrLootTable {
        pools = List.copyOf(pools);
    }

    public static BrLootTable parse(JsonObject root) {
        List<BrLootTablePool> pools = new ArrayList<>();
        JsonArray poolsArray = root.getAsJsonArray("pools");
        for (JsonElement element : poolsArray) {
            pools.add(BrLootTablePool.parse(element.getAsJsonObject()));
        }
        return new BrLootTable(pools);
    }

    /** 战利品表池的 tiers 字段，包含装备/附魔品级范围。
     * @author TT432 */
    @org.jspecify.annotations.NullMarked
    public record BrLootTableTiers(
            List<Integer> initialRange,
            int bonusRounds
    ) {
        public static final Codec<BrLootTableTiers> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.INT.listOf().fieldOf("initial_range").forGetter(BrLootTableTiers::initialRange),
                Codec.INT.optionalFieldOf("bonus_rounds", 0).forGetter(BrLootTableTiers::bonusRounds)
        ).apply(ins, BrLootTableTiers::new));

        public BrLootTableTiers {
            initialRange = List.copyOf(initialRange);
        }

        static BrLootTableTiers parse(JsonObject root) {
            List<Integer> initialRange = new ArrayList<>();
            for (JsonElement e : root.getAsJsonArray("initial_range")) {
                initialRange.add(e.getAsInt());
            }
            int bonusRounds = root.has("bonus_rounds") ? root.get("bonus_rounds").getAsInt() : 0;
            return new BrLootTableTiers(initialRange, bonusRounds);
        }
    }

    /** 战利品表池，封装 rolls、entries 和可选的 conditions/functions/bonus_rolls/tiers。
     * @author TT432 */
    @org.jspecify.annotations.NullMarked
    public record BrLootTablePool(
            Either<Integer, BrLootTableRollsRange> rolls,
            List<BrLootTablePoolEntry> entries,
            List<BedrockResourceValue.ObjectValue> conditions,
            List<BedrockResourceValue.ObjectValue> functions,
            @org.jspecify.annotations.Nullable Integer bonusRolls,
            @org.jspecify.annotations.Nullable BrLootTableTiers tiers
    ) {
        public static final Codec<BrLootTablePool> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.either(Codec.INT, BrLootTableRollsRange.CODEC)
                        .fieldOf("rolls").forGetter(BrLootTablePool::rolls),
                BrLootTablePoolEntry.CODEC.listOf()
                        .fieldOf("entries").forGetter(BrLootTablePool::entries),
                ImporterCodecUtil.OBJECT_VALUE_CODEC.listOf()
                        .optionalFieldOf("conditions", List.of()).forGetter(BrLootTablePool::conditions),
                ImporterCodecUtil.OBJECT_VALUE_CODEC.listOf()
                        .optionalFieldOf("functions", List.of()).forGetter(BrLootTablePool::functions),
                Codec.INT.optionalFieldOf("bonus_rolls")
                        .forGetter(p -> Optional.ofNullable(p.bonusRolls)),
                BrLootTableTiers.CODEC.optionalFieldOf("tiers")
                        .forGetter(p -> Optional.ofNullable(p.tiers))
        ).apply(ins, (rolls, entries, conditions, functions, bonusRolls, tiers) ->
                new BrLootTablePool(rolls, entries, conditions, functions, bonusRolls.orElse(null), tiers.orElse(null))
        ));

        public BrLootTablePool {
            entries = List.copyOf(entries);
            conditions = List.copyOf(conditions);
            functions = List.copyOf(functions);
        }

        static BrLootTablePool parse(JsonObject root) {
            Either<Integer, BrLootTableRollsRange> rolls;
            JsonElement rollsElement = root.get("rolls");
            if (rollsElement.isJsonPrimitive()) {
                rolls = Either.left(rollsElement.getAsInt());
            } else {
                JsonObject rollsObj = rollsElement.getAsJsonObject();
                rolls = Either.right(new BrLootTableRollsRange(
                        rollsObj.get("min").getAsInt(),
                        rollsObj.get("max").getAsInt()
                ));
            }
            List<BrLootTablePoolEntry> entries = new ArrayList<>();
            JsonArray entriesArray = root.getAsJsonArray("entries");
            for (JsonElement element : entriesArray) {
                entries.add(BrLootTablePoolEntry.parse(element.getAsJsonObject()));
            }
            List<BedrockResourceValue.ObjectValue> conditions = parseObjectValueList(root, "conditions");
            List<BedrockResourceValue.ObjectValue> functions = parseObjectValueList(root, "functions");
            Integer bonusRolls = root.has("bonus_rolls") ? root.get("bonus_rolls").getAsInt() : null;
            BrLootTableTiers tiers = root.has("tiers") ? BrLootTableTiers.parse(root.getAsJsonObject("tiers")) : null;
            return new BrLootTablePool(rolls, entries, conditions, functions, bonusRolls, tiers);
        }
    }

    /** rolls 为 {min, max} 范围时的数据载体。
     * @author TT432 */
    @org.jspecify.annotations.NullMarked
    public record BrLootTableRollsRange(
            int min,
            int max
    ) {
        public static final Codec<BrLootTableRollsRange> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.INT.fieldOf("min").forGetter(BrLootTableRollsRange::min),
                Codec.INT.fieldOf("max").forGetter(BrLootTableRollsRange::max)
        ).apply(ins, BrLootTableRollsRange::new));
    }

    /** 战利品表池条目。
     * @author TT432 */
    @org.jspecify.annotations.NullMarked
    public record BrLootTablePoolEntry(
            String type,
            String name,
            int weight,
            int quality,
            List<BedrockResourceValue.ObjectValue> conditions,
            List<BedrockResourceValue.ObjectValue> functions
    ) {
        public static final Codec<BrLootTablePoolEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("type").forGetter(BrLootTablePoolEntry::type),
                Codec.STRING.fieldOf("name").forGetter(BrLootTablePoolEntry::name),
                Codec.INT.optionalFieldOf("weight", 1).forGetter(BrLootTablePoolEntry::weight),
                Codec.INT.optionalFieldOf("quality", 0).forGetter(BrLootTablePoolEntry::quality),
                ImporterCodecUtil.OBJECT_VALUE_CODEC.listOf()
                        .optionalFieldOf("conditions", List.of()).forGetter(BrLootTablePoolEntry::conditions),
                ImporterCodecUtil.OBJECT_VALUE_CODEC.listOf()
                        .optionalFieldOf("functions", List.of()).forGetter(BrLootTablePoolEntry::functions)
        ).apply(ins, BrLootTablePoolEntry::new));

        public BrLootTablePoolEntry {
            conditions = List.copyOf(conditions);
            functions = List.copyOf(functions);
        }

        static BrLootTablePoolEntry parse(JsonObject root) {
            String type = root.get("type").getAsString();
            String name = root.get("name").getAsString();
            int weight = root.has("weight") ? root.get("weight").getAsInt() : 1;
            int quality = root.has("quality") ? root.get("quality").getAsInt() : 0;
            List<BedrockResourceValue.ObjectValue> conditions = parseObjectValueList(root, "conditions");
            List<BedrockResourceValue.ObjectValue> functions = parseObjectValueList(root, "functions");
            return new BrLootTablePoolEntry(type, name, weight, quality, conditions, functions);
        }
    }

    private static List<BedrockResourceValue.ObjectValue> parseObjectValueList(JsonObject root, String key) {
        List<BedrockResourceValue.ObjectValue> result = new ArrayList<>();
        if (root.has(key)) {
            for (JsonElement element : root.getAsJsonArray(key)) {
                result.add((BedrockResourceValue.ObjectValue) BedrockResourceValue.fromJsonElement(element));
            }
        }
        return result;
    }
}
