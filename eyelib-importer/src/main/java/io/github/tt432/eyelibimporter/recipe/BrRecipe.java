package io.github.tt432.eyelibimporter.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Bedrock 配方数据结构，支持 7 种配方类型的序列化。
 * @author TT432 */
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

    record RecipeDescription(String identifier) {
        public static final Codec<RecipeDescription> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("identifier").forGetter(RecipeDescription::identifier)
        ).apply(ins, RecipeDescription::new));
    }

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

    record RecipeResult(String item, int count, int data) {
        private static final Codec<RecipeResult> OBJ = RecordCodecBuilder.create(ins -> ins.group(
                Codec.STRING.fieldOf("item").forGetter(RecipeResult::item),
                Codec.INT.optionalFieldOf("count", 1).forGetter(RecipeResult::count),
                Codec.INT.optionalFieldOf("data", 0).forGetter(RecipeResult::data)
        ).apply(ins, RecipeResult::new));

        static final Codec<RecipeResult> CODEC = Codec.either(
                Codec.STRING.xmap(s -> new RecipeResult(s, 1, 0), RecipeResult::item),
                OBJ
        ).xmap(
                either -> either.map(Function.identity(), Function.identity()),
                result -> result.count == 1 && result.data == 0
                        ? Either.left(result) : Either.right(result)
        );
    }

    record Shaped(
            RecipeDescription description,
            List<String> tags,
            List<String> pattern,
            Map<String, RecipeIngredient> key,
            RecipeResult result
    ) implements BrRecipe {
        @Override
        public String typeKey() {
            return "minecraft:recipe_shaped";
        }

        static final Codec<Shaped> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                RecipeDescription.CODEC.fieldOf("description").forGetter(Shaped::description),
                Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(Shaped::tags),
                Codec.STRING.listOf().fieldOf("pattern").forGetter(Shaped::pattern),
                Codec.unboundedMap(Codec.STRING, RecipeIngredient.CODEC).fieldOf("key").forGetter(Shaped::key),
                RecipeResult.CODEC.fieldOf("result").forGetter(Shaped::result)
        ).apply(ins, Shaped::new));
    }

    record Shapeless(
            RecipeDescription description,
            List<String> tags,
            List<RecipeIngredient> ingredients,
            RecipeResult result
    ) implements BrRecipe {
        @Override
        public String typeKey() {
            return "minecraft:recipe_shapeless";
        }

        static final Codec<Shapeless> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                RecipeDescription.CODEC.fieldOf("description").forGetter(Shapeless::description),
                Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(Shapeless::tags),
                RecipeIngredient.CODEC.listOf().fieldOf("ingredients").forGetter(Shapeless::ingredients),
                RecipeResult.CODEC.fieldOf("result").forGetter(Shapeless::result)
        ).apply(ins, Shapeless::new));
    }

    record Furnace(
            RecipeDescription description,
            List<String> tags,
            RecipeIngredient input,
            RecipeResult output
    ) implements BrRecipe {
        @Override
        public String typeKey() {
            return "minecraft:recipe_furnace";
        }

        static final Codec<Furnace> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                RecipeDescription.CODEC.fieldOf("description").forGetter(Furnace::description),
                Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(Furnace::tags),
                RecipeIngredient.CODEC.fieldOf("input").forGetter(Furnace::input),
                RecipeResult.CODEC.fieldOf("output").forGetter(Furnace::output)
        ).apply(ins, Furnace::new));
    }

    record BrewingMix(
            RecipeDescription description,
            RecipeIngredient input,
            RecipeIngredient reagent,
            RecipeResult output
    ) implements BrRecipe {
        @Override
        public String typeKey() {
            return "minecraft:recipe_brewing_mix";
        }

        static final Codec<BrewingMix> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                RecipeDescription.CODEC.fieldOf("description").forGetter(BrewingMix::description),
                RecipeIngredient.CODEC.fieldOf("input").forGetter(BrewingMix::input),
                RecipeIngredient.CODEC.fieldOf("reagent").forGetter(BrewingMix::reagent),
                RecipeResult.CODEC.fieldOf("output").forGetter(BrewingMix::output)
        ).apply(ins, BrewingMix::new));
    }

    record BrewingContainer(
            RecipeDescription description,
            RecipeIngredient input,
            RecipeIngredient reagent,
            RecipeResult output
    ) implements BrRecipe {
        @Override
        public String typeKey() {
            return "minecraft:recipe_brewing_container";
        }

        static final Codec<BrewingContainer> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                RecipeDescription.CODEC.fieldOf("description").forGetter(BrewingContainer::description),
                RecipeIngredient.CODEC.fieldOf("input").forGetter(BrewingContainer::input),
                RecipeIngredient.CODEC.fieldOf("reagent").forGetter(BrewingContainer::reagent),
                RecipeResult.CODEC.fieldOf("output").forGetter(BrewingContainer::output)
        ).apply(ins, BrewingContainer::new));
    }

    record SmithingTransform(
            RecipeDescription description,
            RecipeIngredient template,
            RecipeIngredient base,
            RecipeIngredient addition,
            RecipeResult result
    ) implements BrRecipe {
        @Override
        public String typeKey() {
            return "minecraft:recipe_smithing_transform";
        }

        static final Codec<SmithingTransform> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                RecipeDescription.CODEC.fieldOf("description").forGetter(SmithingTransform::description),
                RecipeIngredient.CODEC.fieldOf("template").forGetter(SmithingTransform::template),
                RecipeIngredient.CODEC.fieldOf("base").forGetter(SmithingTransform::base),
                RecipeIngredient.CODEC.fieldOf("addition").forGetter(SmithingTransform::addition),
                RecipeResult.CODEC.fieldOf("result").forGetter(SmithingTransform::result)
        ).apply(ins, SmithingTransform::new));
    }

    record SmithingTrim(
            RecipeDescription description,
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
                RecipeIngredient.CODEC.fieldOf("template").forGetter(SmithingTrim::template),
                RecipeIngredient.CODEC.fieldOf("base").forGetter(SmithingTrim::base),
                RecipeIngredient.CODEC.fieldOf("addition").forGetter(SmithingTrim::addition)
        ).apply(ins, SmithingTrim::new));
    }
}
