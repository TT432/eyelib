package io.github.tt432.eyelibimporter.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Bedrock 配方数据结构，支持 7 种配方类型的序列化。
 *
 * @author TT432
 */
@NullMarked
public sealed interface BrRecipe
        permits BrRecipe.Shaped, BrRecipe.Shapeless, BrRecipe.Furnace,
                BrRecipe.BrewingMix, BrRecipe.BrewingContainer,
                BrRecipe.SmithingTransform, BrRecipe.SmithingTrim {

    String typeKey();

    RecipeDescription description();

    Map<String, Codec<? extends BrRecipe>> CODEC_BY_KEY = Map.of(
            "minecraft:recipe_shaped", Shaped.CODEC,
            "minecraft:recipe_shapeless", Shapeless.CODEC,
            "minecraft:recipe_furnace", Furnace.CODEC,
            "minecraft:recipe_brewing_mix", BrewingMix.CODEC,
            "minecraft:recipe_brewing_container", BrewingContainer.CODEC,
            "minecraft:recipe_smithing_transform", SmithingTransform.CODEC,
            "minecraft:recipe_smithing_trim", SmithingTrim.CODEC
    );

    Codec<BrRecipe> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<BrRecipe, T>> decode(DynamicOps<T> ops, T input) {
            var mapResult = ops.getMap(input);
            if (mapResult.error().isPresent()) {
                return DataResult.error(() -> "Not a JSON object: " + mapResult.error().get().message());
            }
            var map = mapResult.result().get();
            for (var entry : CODEC_BY_KEY.entrySet()) {
                var val = map.get(entry.getKey());
                if (val != null) {
                    @SuppressWarnings("unchecked")
                    var codec = (Codec<BrRecipe>) entry.getValue();
                    return codec.decode(ops, val);
                }
            }
            return DataResult.error(() -> "No recipe type key found, expected one of: " + CODEC_BY_KEY.keySet());
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> DataResult<T> encode(BrRecipe input, DynamicOps<T> ops, T prefix) {
            var key = input.typeKey();
            var codec = (Codec<BrRecipe>) CODEC_BY_KEY.get(key);
            if (codec == null) {
                return DataResult.error(() -> "Unknown recipe type: " + key);
            }
            var encoded = codec.encodeStart(ops, input);
            if (encoded.error().isPresent()) {
                return encoded;
            }
            var mapBuilder = ops.mapBuilder();
            mapBuilder.add(ops.createString(key), encoded.result().get());
            return mapBuilder.build(prefix);
        }
    };

    /**
     * 配方描述信息。
     */
    record RecipeDescription(String identifier) {
        public static final Codec<RecipeDescription> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("identifier").forGetter(RecipeDescription::identifier)
        ).apply(ins, RecipeDescription::new));
    }

    /**
     * 配方原料，支持简写（仅字符串）和完整对象两种格式。
     */
    record RecipeIngredient(String item, int data, int count) {
        private static final Codec<RecipeIngredient> OBJ = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("item").forGetter(RecipeIngredient::item),
                Codec.INT.optionalFieldOf("data", 0).forGetter(RecipeIngredient::data),
                Codec.INT.optionalFieldOf("count", 1).forGetter(RecipeIngredient::count)
        ).apply(ins, RecipeIngredient::new));

        static final Codec<RecipeIngredient> CODEC = Codec.either(
                Codec.STRING.xmap(s -> new RecipeIngredient(s, 0, 1), RecipeIngredient::item),
                OBJ
        ).xmap(
                either -> either.map(Function.identity(), Function.identity()),
                ingredient -> ingredient.data == 0 && ingredient.count == 1
                        ? Either.left(ingredient) : Either.right(ingredient)
        );
    }

    /**
     * 配方产物，支持单对象和数组两种格式。
     */
    record RecipeResult(String item, int count, int data) {
        private static final Codec<RecipeResult> OBJ = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("item").forGetter(RecipeResult::item),
                Codec.INT.optionalFieldOf("count", 1).forGetter(RecipeResult::count),
                Codec.INT.optionalFieldOf("data", 0).forGetter(RecipeResult::data)
        ).apply(ins, RecipeResult::new));

        static final Codec<RecipeResult> SINGLE_CODEC = Codec.either(
                Codec.STRING.xmap(s -> new RecipeResult(s, 1, 0), RecipeResult::item),
                OBJ
        ).xmap(
                either -> either.map(Function.identity(), Function.identity()),
                result -> result.count == 1 && result.data == 0
                        ? Either.left(result) : Either.right(result)
        );

        /**
         * 支持单对象和数组两种格式的 Codec。
         * 数组格式用于多输出配方。
         */
        static final Codec<List<RecipeResult>> CODEC = Codec.either(
                SINGLE_CODEC,
                SINGLE_CODEC.listOf()
        ).xmap(
                either -> either.map(List::of, Function.identity()),
                list -> list.size() == 1 ? Either.left(list.get(0)) : Either.right(list)
        );
    }

    /**
     * 有序合成配方。
     */
    record Shaped(
            RecipeDescription description,
            @Nullable String group,
            List<String> tags,
            @Nullable Integer priority,
            boolean assumeSymmetry,
            List<String> pattern,
            Map<String, RecipeIngredient> key,
            List<RecipeResult> result
    ) implements BrRecipe {
        @Override
        public String typeKey() {
            return "minecraft:recipe_shaped";
        }

        static final Codec<Shaped> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                RecipeDescription.CODEC.fieldOf("description").forGetter(Shaped::description),
                Codec.STRING.optionalFieldOf("group")
                        .forGetter(s -> Optional.ofNullable(s.group)),
                Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(Shaped::tags),
                Codec.INT.optionalFieldOf("priority")
                        .forGetter(s -> Optional.ofNullable(s.priority)),
                Codec.BOOL.optionalFieldOf("assume_symmetry", true).forGetter(Shaped::assumeSymmetry),
                Codec.STRING.listOf().fieldOf("pattern").forGetter(Shaped::pattern),
                Codec.unboundedMap(Codec.STRING, RecipeIngredient.CODEC).fieldOf("key").forGetter(Shaped::key),
                RecipeResult.CODEC.fieldOf("result").forGetter(Shaped::result)
        ).apply(ins, (desc, group, tags, priority, assumeSymmetry, pattern, key, result) ->
                new Shaped(desc, group.orElse(null), tags, priority.orElse(null), assumeSymmetry, pattern, key, result)));
    }

    /**
     * 无序合成配方。
     */
    record Shapeless(
            RecipeDescription description,
            @Nullable String group,
            List<String> tags,
            @Nullable Integer priority,
            List<RecipeIngredient> ingredients,
            List<RecipeResult> result
    ) implements BrRecipe {
        @Override
        public String typeKey() {
            return "minecraft:recipe_shapeless";
        }

        static final Codec<Shapeless> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                RecipeDescription.CODEC.fieldOf("description").forGetter(Shapeless::description),
                Codec.STRING.optionalFieldOf("group")
                        .forGetter(s -> Optional.ofNullable(s.group)),
                Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(Shapeless::tags),
                Codec.INT.optionalFieldOf("priority")
                        .forGetter(s -> Optional.ofNullable(s.priority)),
                RecipeIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(Shapeless::ingredients),
                RecipeResult.CODEC.fieldOf("result").forGetter(Shapeless::result)
        ).apply(ins, (desc, group, tags, priority, ingredients, result) ->
                new Shapeless(desc, group.orElse(null), tags, priority.orElse(null), ingredients, result)));
    }

    /**
     * 熔炉烧炼配方。
     */
    record Furnace(
            RecipeDescription description,
            @Nullable String group,
            List<String> tags,
            RecipeIngredient input,
            List<RecipeResult> output
    ) implements BrRecipe {
        @Override
        public String typeKey() {
            return "minecraft:recipe_furnace";
        }

        static final Codec<Furnace> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                RecipeDescription.CODEC.fieldOf("description").forGetter(Furnace::description),
                Codec.STRING.optionalFieldOf("group")
                        .forGetter(f -> Optional.ofNullable(f.group)),
                Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(Furnace::tags),
                RecipeIngredient.CODEC.fieldOf("input").forGetter(Furnace::input),
                RecipeResult.CODEC.fieldOf("output").forGetter(Furnace::output)
        ).apply(ins, (desc, group, tags, input, output) ->
                new Furnace(desc, group.orElse(null), tags, input, output)));
    }

    /**
     * 酿造混合配方。
     */
    record BrewingMix(
            RecipeDescription description,
            @Nullable String group,
            List<String> tags,
            RecipeIngredient input,
            RecipeIngredient reagent,
            List<RecipeResult> output
    ) implements BrRecipe {
        @Override
        public String typeKey() {
            return "minecraft:recipe_brewing_mix";
        }

        static final Codec<BrewingMix> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                RecipeDescription.CODEC.fieldOf("description").forGetter(BrewingMix::description),
                Codec.STRING.optionalFieldOf("group")
                        .forGetter(b -> Optional.ofNullable(b.group)),
                Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(BrewingMix::tags),
                RecipeIngredient.CODEC.fieldOf("input").forGetter(BrewingMix::input),
                RecipeIngredient.CODEC.fieldOf("reagent").forGetter(BrewingMix::reagent),
                RecipeResult.CODEC.fieldOf("output").forGetter(BrewingMix::output)
        ).apply(ins, (desc, group, tags, input, reagent, output) ->
                new BrewingMix(desc, group.orElse(null), tags, input, reagent, output)));
    }

    /**
     * 酿造容器配方。
     */
    record BrewingContainer(
            RecipeDescription description,
            @Nullable String group,
            List<String> tags,
            RecipeIngredient input,
            RecipeIngredient reagent,
            List<RecipeResult> output
    ) implements BrRecipe {
        @Override
        public String typeKey() {
            return "minecraft:recipe_brewing_container";
        }

        static final Codec<BrewingContainer> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                RecipeDescription.CODEC.fieldOf("description").forGetter(BrewingContainer::description),
                Codec.STRING.optionalFieldOf("group")
                        .forGetter(b -> Optional.ofNullable(b.group)),
                Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(BrewingContainer::tags),
                RecipeIngredient.CODEC.fieldOf("input").forGetter(BrewingContainer::input),
                RecipeIngredient.CODEC.fieldOf("reagent").forGetter(BrewingContainer::reagent),
                RecipeResult.CODEC.fieldOf("output").forGetter(BrewingContainer::output)
        ).apply(ins, (desc, group, tags, input, reagent, output) ->
                new BrewingContainer(desc, group.orElse(null), tags, input, reagent, output)));
    }

    /**
     * 锻造台变换配方。
     */
    record SmithingTransform(
            RecipeDescription description,
            @Nullable String group,
            List<String> tags,
            RecipeIngredient template,
            RecipeIngredient base,
            RecipeIngredient addition,
            List<RecipeResult> result
    ) implements BrRecipe {
        @Override
        public String typeKey() {
            return "minecraft:recipe_smithing_transform";
        }

        static final Codec<SmithingTransform> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                RecipeDescription.CODEC.fieldOf("description").forGetter(SmithingTransform::description),
                Codec.STRING.optionalFieldOf("group")
                        .forGetter(s -> Optional.ofNullable(s.group)),
                Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(SmithingTransform::tags),
                RecipeIngredient.CODEC.fieldOf("template").forGetter(SmithingTransform::template),
                RecipeIngredient.CODEC.fieldOf("base").forGetter(SmithingTransform::base),
                RecipeIngredient.CODEC.fieldOf("addition").forGetter(SmithingTransform::addition),
                RecipeResult.CODEC.fieldOf("result").forGetter(SmithingTransform::result)
        ).apply(ins, (desc, group, tags, template, base, addition, result) ->
                new SmithingTransform(desc, group.orElse(null), tags, template, base, addition, result)));
    }

    /**
     * 锻造台纹饰配方（无 result）。
     */
    record SmithingTrim(
            RecipeDescription description,
            @Nullable String group,
            List<String> tags,
            RecipeIngredient template,
            RecipeIngredient base,
            RecipeIngredient addition
    ) implements BrRecipe {
        @Override
        public String typeKey() {
            return "minecraft:recipe_smithing_trim";
        }

        static final Codec<SmithingTrim> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                RecipeDescription.CODEC.fieldOf("description").forGetter(SmithingTrim::description),
                Codec.STRING.optionalFieldOf("group")
                        .forGetter(s -> Optional.ofNullable(s.group)),
                Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(SmithingTrim::tags),
                RecipeIngredient.CODEC.fieldOf("template").forGetter(SmithingTrim::template),
                RecipeIngredient.CODEC.fieldOf("base").forGetter(SmithingTrim::base),
                RecipeIngredient.CODEC.fieldOf("addition").forGetter(SmithingTrim::addition)
        ).apply(ins, (desc, group, tags, template, base, addition) ->
                new SmithingTrim(desc, group.orElse(null), tags, template, base, addition)));
    }
}
