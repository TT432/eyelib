package io.github.tt432.eyelib.importer.particle;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.importer.addon.BedrockResourceValue;
import io.github.tt432.eyelib.importer.util.ImporterCodecUtil;
import io.github.tt432.eyelib.molang.MolangValue;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Bedrock 粒子效果的数据结构。
 * @author TT432 */
@org.jspecify.annotations.NullMarked
public record BrParticle(
        String formatVersion,
        ParticleEffect particleEffect
) {
    private static final Codec<JsonElement> JSON_ELEMENT_CODEC = ImporterCodecUtil.JSON_ELEMENT_CODEC;

    public static final Codec<BrParticle> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("format_version").forGetter(BrParticle::formatVersion),
            ParticleEffect.CODEC.fieldOf("particle_effect").forGetter(BrParticle::particleEffect)
    ).apply(instance, BrParticle::new));

    public record ParticleEffect(
            Description description,
            Map<String, Curve> curves,
            Events events,
            Map<String, BedrockResourceValue> components
    ) {
        public static final Codec<ParticleEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Description.CODEC.fieldOf("description").forGetter(ParticleEffect::description),
                Codec.unboundedMap(Codec.STRING, Curve.CODEC).optionalFieldOf("curves", Map.of()).forGetter(ParticleEffect::curves),
                Events.CODEC.optionalFieldOf("events", new Events()).forGetter(ParticleEffect::events),
                Codec.unboundedMap(Codec.STRING, JSON_ELEMENT_CODEC.xmap(BedrockResourceValue::fromJsonElement, value -> {
                    throw new UnsupportedOperationException("Particle component encoding is not supported");
                })).optionalFieldOf("components", Map.of()).forGetter(ParticleEffect::components)
        ).apply(instance, ParticleEffect::new));

        public Optional<BillboardFlipbook> billboardFlipbook() {
            BedrockResourceValue appearance = Optional.ofNullable(components.get("minecraft:particle_appearance_billboard"))
                    .orElse(components.get("particle_appearance_billboard"));
            if (!(appearance instanceof BedrockResourceValue.ObjectValue appearanceObject)) {
                return Optional.empty();
            }

            BedrockResourceValue uvValue = appearanceObject.values().get("uv");
            if (!(uvValue instanceof BedrockResourceValue.ObjectValue uvObject)) {
                return Optional.empty();
            }

            BedrockResourceValue flipbookValue = uvObject.values().get("flipbook");
            if (!(flipbookValue instanceof BedrockResourceValue.ObjectValue flipbookObject)) {
                return Optional.empty();
            }

            Optional<VectorExpression2> baseUV = getVectorExpression2(flipbookObject, "base_UV");
            Optional<VectorExpression2> sizeUV = getVectorExpression2(flipbookObject, "size_UV");
            Optional<VectorExpression2> stepUV = getVectorExpression2(flipbookObject, "step_UV");
            Optional<String> maxFrame = getScalarExpression(flipbookObject, "max_frame");
            if (baseUV.isEmpty() || sizeUV.isEmpty() || stepUV.isEmpty() || maxFrame.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new BillboardFlipbook(
                    getInt(uvObject, "texture_width").orElse(1),
                    getInt(uvObject, "texture_height").orElse(1),
                    baseUV.orElseThrow(),
                    sizeUV.orElseThrow(),
                    stepUV.orElseThrow(),
                    getScalarExpression(flipbookObject, "frames_per_second").orElse("0"),
                    maxFrame.orElseThrow(),
                    getBoolean(flipbookObject, "stretch_to_lifetime").orElse(false),
                    getBoolean(flipbookObject, "loop").orElse(false)
            ));
        }

        private static Optional<VectorExpression2> getVectorExpression2(BedrockResourceValue.ObjectValue objectValue, String key) {
            BedrockResourceValue value = objectValue.values().get(key);
            if (!(value instanceof BedrockResourceValue.ArrayValue arrayValue) || arrayValue.values().size() != 2) {
                return Optional.empty();
            }

            Optional<String> x = toScalarExpression(arrayValue.values().get(0));
            Optional<String> y = toScalarExpression(arrayValue.values().get(1));
            if (x.isEmpty() || y.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new VectorExpression2(x.orElseThrow(), y.orElseThrow()));
        }

        private static Optional<String> getScalarExpression(BedrockResourceValue.ObjectValue objectValue, String key) {
            return Optional.ofNullable(objectValue.values().get(key)).flatMap(ParticleEffect::toScalarExpression);
        }

        private static Optional<Integer> getInt(BedrockResourceValue.ObjectValue objectValue, String key) {
            return getScalarExpression(objectValue, key).flatMap(value -> {
                try {
                    return Optional.of(new BigDecimal(value).intValueExact());
                } catch (ArithmeticException | NumberFormatException ignored) {
                    return Optional.empty();
                }
            });
        }

        private static Optional<Boolean> getBoolean(BedrockResourceValue.ObjectValue objectValue, String key) {
            BedrockResourceValue value = objectValue.values().get(key);
            if (value instanceof BedrockResourceValue.BooleanValue booleanValue) {
                return Optional.of(booleanValue.value());
            }
            if (value instanceof BedrockResourceValue.NumberValue numberValue) {
                return Optional.of(numberValue.value().compareTo(BigDecimal.ZERO) != 0);
            }
            if (value instanceof BedrockResourceValue.StringValue stringValue) {
                if ("true".equalsIgnoreCase(stringValue.value())) {
                    return Optional.of(true);
                }
                if ("false".equalsIgnoreCase(stringValue.value())) {
                    return Optional.of(false);
                }
            }
            return Optional.empty();
        }

        private static Optional<String> toScalarExpression(BedrockResourceValue value) {
            if (value instanceof BedrockResourceValue.NumberValue numberValue) {
                return Optional.of(numberValue.value().stripTrailingZeros().toPlainString());
            }
            if (value instanceof BedrockResourceValue.StringValue stringValue) {
                return Optional.of(stringValue.value());
            }
            if (value instanceof BedrockResourceValue.BooleanValue booleanValue) {
                return Optional.of(booleanValue.value() ? "1" : "0");
            }
            return Optional.empty();
        }
    }

    public record VectorExpression2(
            String x,
            String y
    ) {
    }

    public record BillboardFlipbook(
            int textureWidth,
            int textureHeight,
            VectorExpression2 baseUV,
            VectorExpression2 sizeUV,
            VectorExpression2 stepUV,
            String framesPerSecond,
            String maxFrame,
            boolean stretchToLifetime,
            boolean loop
    ) {
    }

    public record Description(
            String identifier,
            BasicRenderParameters basicRenderParameters
    ) {
        public static final Codec<Description> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("identifier").forGetter(Description::identifier),
                BasicRenderParameters.CODEC.fieldOf("basic_render_parameters").forGetter(Description::basicRenderParameters)
        ).apply(instance, Description::new));
    }

    public record BasicRenderParameters(
            String material,
            String texture
    ) {
        public static final Codec<BasicRenderParameters> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("material").forGetter(BasicRenderParameters::material),
                Codec.STRING.fieldOf("texture").forGetter(BasicRenderParameters::texture)
        ).apply(instance, BasicRenderParameters::new));
    }

    public record CurveNodes(
            Optional<TreeMap<Float, MolangValue>> nodes,
            Optional<TreeMap<Float, Curve.ChainNode>> chainNodes
    ) {
    }

    public record Curve(
            Type type,
            CurveNodes nodes,
            MolangValue input,
            MolangValue horizontalRange
    ) {
        private static final Codec<TreeMap<Float, ChainNode>> CHAIN_NODE_MAP_CODEC = Codec.unboundedMap(Codec.STRING, ChainNode.CODEC).xmap(
                Curve::toSortedChainNodeMap,
                Curve::fromSortedChainNodeMap
        );
        private static final Codec<CurveNodes> CURVE_NODES_CODEC = Codec.either(CHAIN_NODE_MAP_CODEC, MolangValue.CODEC.listOf().xmap(
                Curve::toNormalizedNodeMap,
                treeMap -> new ArrayList<>(treeMap.values())
        )).xmap(
                either -> either.map(
                        chainNodes -> new CurveNodes(Optional.empty(), Optional.of(chainNodes)),
                        nodes -> new CurveNodes(Optional.of(nodes), Optional.empty())
                ),
                nodes -> nodes.chainNodes().<com.mojang.datafixers.util.Either<TreeMap<Float, ChainNode>, TreeMap<Float, MolangValue>>>map(com.mojang.datafixers.util.Either::left)
                        .orElseGet(() -> com.mojang.datafixers.util.Either.right(nodes.nodes().orElse(new TreeMap<>(Comparator.comparingDouble(value -> value)))))
        );

        public static final Codec<Curve> CODEC = RecordCodecBuilder.create(instance -> {
            return instance.group(
                    Type.CODEC.optionalFieldOf("type", Type.LINEAR).forGetter(Curve::type),
                    CURVE_NODES_CODEC.optionalFieldOf("nodes", new CurveNodes(Optional.empty(), Optional.empty()))
                            .forGetter(Curve::nodes),
                    MolangValue.CODEC.optionalFieldOf("input", MolangValue.ZERO).forGetter(Curve::input),
                    MolangValue.CODEC.optionalFieldOf("horizontal_range", new MolangValue("v.particle_lifetime"))
                            .forGetter(Curve::horizontalRange)
            ).apply(instance, Curve::new);
        });

        public record ChainNode(
                float leftValue,
                float rightValue,
                float leftSlope,
                float rightSlope
        ) {
            public static final Codec<ChainNode> CODEC = JSON_ELEMENT_CODEC.comapFlatMap(ChainNode::decode, ChainNode::encode);

            private static DataResult<ChainNode> decode(JsonElement jsonElement) {
                if (!jsonElement.isJsonObject()) {
                    return DataResult.error(() -> "Chain node must be a JSON object");
                }
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                try {
                    Float value = readOptionalFloat(jsonObject, "value");
                    Float slope = readOptionalFloat(jsonObject, "slope");
                    return DataResult.success(new ChainNode(
                            firstNonNull(readOptionalFloat(jsonObject, "left_value"), value, "left_value/value"),
                            firstNonNull(readOptionalFloat(jsonObject, "right_value"), value, "right_value/value"),
                            firstNonNull(readOptionalFloat(jsonObject, "left_slope"), slope, "left_slope/slope"),
                            firstNonNull(readOptionalFloat(jsonObject, "right_slope"), slope, "right_slope/slope")
                    ));
                } catch (RuntimeException exception) {
                    return DataResult.error(() -> exception.getMessage());
                }
            }

            private static JsonElement encode(ChainNode chainNode) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("left_value", chainNode.leftValue);
                jsonObject.addProperty("right_value", chainNode.rightValue);
                jsonObject.addProperty("left_slope", chainNode.leftSlope);
                jsonObject.addProperty("right_slope", chainNode.rightSlope);
                return jsonObject;
            }

            private static @Nullable Float readOptionalFloat(JsonObject jsonObject, String key) {
                return jsonObject.has(key) ? jsonObject.get(key).getAsFloat() : null;
            }
        }

        private static float firstNonNull(@Nullable Float primary, @Nullable Float alternative, String label) {
            if (primary != null) {
                return primary;
            }
            if (alternative != null) {
                return alternative;
            }
            throw new IllegalArgumentException("Missing required curve value: " + label);
        }

        private static TreeMap<Float, MolangValue> toSortedMolangMap(Map<String, MolangValue> values) {
            TreeMap<Float, MolangValue> result = new TreeMap<>(Comparator.comparingDouble(value -> value));
            values.forEach((key, value) -> result.put(Float.parseFloat(key), value));
            return result;
        }

        private static Map<String, MolangValue> fromSortedMolangMap(TreeMap<Float, MolangValue> values) {
            LinkedHashMap<String, MolangValue> result = new LinkedHashMap<>();
            values.forEach((key, value) -> result.put(Float.toString(key), value));
            return result;
        }

        private static TreeMap<Float, ChainNode> toSortedChainNodeMap(Map<String, ChainNode> values) {
            TreeMap<Float, ChainNode> result = new TreeMap<>(Comparator.comparingDouble(value -> value));
            values.forEach((key, value) -> result.put(Float.parseFloat(key), value));
            return result;
        }

        private static Map<String, ChainNode> fromSortedChainNodeMap(TreeMap<Float, ChainNode> values) {
            LinkedHashMap<String, ChainNode> result = new LinkedHashMap<>();
            values.forEach((key, value) -> result.put(Float.toString(key), value));
            return result;
        }

        private static TreeMap<Float, MolangValue> toNormalizedNodeMap(java.util.List<MolangValue> list) {
            TreeMap<Float, MolangValue> result = new TreeMap<>(Comparator.comparingDouble(value -> value));
            for (int i = 0; i < list.size(); i++) {
                result.put(i / (list.size() - 1F), list.get(i));
            }
            return result;
        }

        public enum Type {
            LINEAR,
            BEZIER,
            BEZIER_CHAIN,
            CATMULL_ROM;

            public static final Codec<Type> CODEC = Codec.STRING.comapFlatMap(Type::fromSerializedName, Type::getSerializedName);

            private static DataResult<Type> fromSerializedName(String name) {
                for (Type value : values()) {
                    if (value.getSerializedName().equals(name)) {
                        return DataResult.success(value);
                    }
                }
                return DataResult.error(() -> "Unknown particle curve type: " + name);
            }

            public String getSerializedName() {
                return name().toLowerCase();
            }
        }
    }

    public record Events(Map<String, BedrockResourceValue> values) {
        public Events() {
            this(Map.of());
        }

        public Events {
            values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
        }

        public static final Codec<Events> CODEC = Codec.unboundedMap(
                Codec.STRING,
                JSON_ELEMENT_CODEC.xmap(BedrockResourceValue::fromJsonElement, value -> {
                    throw new UnsupportedOperationException("Particle event encoding is not supported");
                })
        ).xmap(Events::new, Events::values);
    }
}