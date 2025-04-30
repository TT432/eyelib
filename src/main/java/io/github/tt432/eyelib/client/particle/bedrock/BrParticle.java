package io.github.tt432.eyelib.client.particle.bedrock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.chin.codec.ChinExtraCodecs;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponentManager;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.util.codec.EyelibCodec;
import io.github.tt432.eyelib.util.math.Curves;
import io.github.tt432.eyelib.util.math.EyeMath;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;

import java.util.*;

/**
 * @author TT432
 */
public record BrParticle(
        String formatVersion,
        ParticleEffect particleEffect
) {
    public static final Codec<BrParticle> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("format_version").forGetter(o -> o.formatVersion),
            ParticleEffect.CODEC.fieldOf("particle_effect").forGetter(o -> o.particleEffect)
    ).apply(ins, BrParticle::new));

    public record ParticleEffect(
            Description description,
            Map<String, Curve> curves,
            Events events,
            Map<ResourceLocation, ParticleComponent> components
    ) {
        @SuppressWarnings("unchecked")
        public <T extends ParticleComponent> Optional<T> getComponent(ResourceLocation location) {
            return (Optional<T>) Optional.ofNullable(components.get(location));
        }

        public static final Codec<ParticleEffect> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Description.CODEC.fieldOf("description").forGetter(o -> o.description),
                Codec.unboundedMap(Codec.STRING, Curve.CODEC).optionalFieldOf("curves", Map.of())
                        .forGetter(o -> o.curves),
                Events.CODEC.optionalFieldOf("events", new Events()).forGetter(o -> o.events),
                Codec.dispatchedMap(
                        ResourceLocation.CODEC,
                        ParticleComponentManager::codec
                ).optionalFieldOf("components", Map.of()).forGetter(o -> o.components)
        ).apply(ins, ParticleEffect::new));

        public record Description(
                String identifier,
                BasicRenderParameters basicRenderParameters
        ) {
            public static final Codec<Description> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                    Codec.STRING.fieldOf("identifier").forGetter(o -> o.identifier),
                    BasicRenderParameters.CODEC.fieldOf("basic_render_parameters")
                            .forGetter(o -> o.basicRenderParameters)
            ).apply(ins, Description::new));

            public record BasicRenderParameters(
                    String material,
                    ResourceLocation texture
            ) {
                public static final Codec<BasicRenderParameters> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                        Codec.STRING.fieldOf("material").forGetter(o -> o.material),
                        ResourceLocation.CODEC.fieldOf("texture").forGetter(o -> o.texture)
                ).apply(ins, BasicRenderParameters::new));
            }
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
                MolangValue horizontal_range
        ) {
            public static final Codec<Curve> CODEC = RecordCodecBuilder.create(ins -> {
                Comparator<Float> comparator = Comparator.comparingDouble(k -> k);
                return ins.group(
                        Type.CODEC.optionalFieldOf("type", Type.LINEAR).forGetter(o -> o.type),
                        Codec.withAlternative(
                                        ChinExtraCodecs.treeMap(EyelibCodec.STR_FLOAT_CODEC, ChainNode.CODEC, comparator).xmap(
                                                a -> new CurveNodes(Optional.empty(), Optional.of(a)),
                                                nodes -> nodes.chainNodes().orElse(new TreeMap<>())
                                        ),
                                        MolangValue.CODEC.listOf().xmap(list -> {
                                            TreeMap<Float, MolangValue> r = new TreeMap<>(Comparator.comparingDouble(k -> k));
                                            for (int i = 0; i < list.size(); i++) {
                                                r.put(i / (list.size() - 1F), list.get(i));
                                            }
                                            return r;
                                        }, tmap -> new ArrayList<>(tmap.values())).xmap(
                                                a -> new CurveNodes(Optional.of(a), Optional.empty()),
                                                nodes -> nodes.nodes().orElse(new TreeMap<>())
                                        ))
                                .optionalFieldOf("nodes", new CurveNodes(Optional.empty(), Optional.empty()))
                                .forGetter(o -> o.nodes),
                        MolangValue.CODEC.optionalFieldOf("input", MolangValue.ZERO).forGetter(o -> o.input),
                        MolangValue.CODEC.optionalFieldOf("horizontal_range", new MolangValue("v.particle_lifetime"))
                                .forGetter(o -> o.horizontal_range)
                ).apply(ins, Curve::new);
            });

            public record ChainNode(
                    float leftValue,
                    float rightValue,
                    float leftSlope,
                    float rightSlope
            ) {
                public static final Codec<ChainNode> CODEC = RecordCodecBuilder.create(ins -> {
                    final MapCodec<Float> primary = Codec.FLOAT.fieldOf("right_slope");
                    final MapCodec<? extends Float> alternative = Codec.FLOAT.fieldOf("slope");
                    final MapCodec<Float> primary1 = Codec.FLOAT.fieldOf("left_slope");
                    final MapCodec<? extends Float> alternative1 = Codec.FLOAT.fieldOf("slope");
                    final MapCodec<Float> primary2 = Codec.FLOAT.fieldOf("right_value");
                    final MapCodec<? extends Float> alternative2 = Codec.FLOAT.fieldOf("value");
                    final MapCodec<Float> primary3 = Codec.FLOAT.fieldOf("left_value");
                    final MapCodec<? extends Float> alternative3 = Codec.FLOAT.fieldOf("value");
                    return ins.group(
                            ChinExtraCodecs.withAlternative(primary3, alternative3).forGetter(o -> o.leftValue),
                            ChinExtraCodecs.withAlternative(primary2, alternative2).forGetter(o -> o.rightValue),
                            ChinExtraCodecs.withAlternative(primary1, alternative1).forGetter(o -> o.leftSlope),
                            ChinExtraCodecs.withAlternative(primary, alternative).forGetter(o -> o.rightSlope)
                    ).apply(ins, ChainNode::new);
                });
            }

            public enum Type implements StringRepresentable {
                LINEAR,
                BEZIER,
                BEZIER_CHAIN,
                CATMULL_ROM;
                public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

                @Override
                @NotNull
                public String getSerializedName() {
                    return name().toLowerCase();
                }
            }

            public float calculate(MolangScope scope) {
                float time = input.eval(scope) / horizontal_range.eval(scope);

                return switch (type) {
                    case LINEAR -> calculateLinear(time, scope);
                    case BEZIER -> calculateBezier(time, scope);
                    case BEZIER_CHAIN -> calculateBezierChain(time);
                    case CATMULL_ROM -> calculateCatmullRom(time, scope);
                };
            }

            private float calculateLinear(float input, MolangScope scope) {
                return nodes.nodes.map(nodes -> {
                    var before = nodes.floorEntry(input);
                    var after = nodes.higherEntry(input);

                    if (before == null) return after.getValue().eval(scope);
                    else if (after == null) return before.getValue().eval(scope);
                    else return EyeMath.lerp(before.getValue().eval(scope),
                                after.getValue().eval(scope),
                                (input - before.getKey()) / (after.getKey() - before.getKey()));
                }).orElse(0F);
            }

            private float calculateBezier(float input, MolangScope scope) {
                return nodes.nodes.map(nodes -> {
                    if (nodes.size() != 4) return 0F;
                    FloatList floats = new FloatArrayList();

                    nodes.forEach((k, v) -> floats.add(v.eval(scope)));

                    return bezier(input, floats.getFloat(0), floats.getFloat(1),
                            floats.getFloat(2), floats.getFloat(3));
                }).orElse(0F);
            }

            private float calculateBezierChain(float input) {
                return nodes.chainNodes.map(chainNodes -> {
                    if (chainNodes.isEmpty()) return 0F;

                    Map.Entry<Float, ChainNode> lowerEntry = chainNodes.floorEntry(input);
                    Map.Entry<Float, ChainNode> higherEntry = chainNodes.ceilingEntry(input);

                    if (lowerEntry == null || higherEntry == null || lowerEntry.equals(higherEntry)) {
                        return lowerEntry != null ? lowerEntry.getValue().leftValue : 0;
                    }

                    ChainNode lowerNode = lowerEntry.getValue();
                    ChainNode higherNode = higherEntry.getValue();

                    float t = (input - lowerEntry.getKey()) / (higherEntry.getKey() - lowerEntry.getKey());
                    return bezierChain(t, lowerNode.leftValue, higherNode.rightValue, lowerNode.leftSlope, higherNode.rightSlope);
                }).orElse(0F);
            }

            private float calculateCatmullRom(float input, MolangScope scope) {
                return nodes.nodes.map(nodes -> {
                    List<Vector2f> catmullromArray = new ArrayList<>();
                    nodes.forEach((k, v) -> catmullromArray.add(new Vector2f(k, v.eval(scope))));

                    float c = nodes.size() - 3;
                    float u = (1 + input * c) / (c + 2);
                    return Curves.lerpSplineCurve(catmullromArray, u);
                }).orElse(0F);
            }

            private float bezier(float t, float p0, float p1, float p2, float p3) {
                float u = 1 - t;
                return (u * u * u * p0) + (3 * u * u * t * p1) + (3 * u * t * t * p2) + (t * t * t * p3);
            }

            private float bezierChain(float t, float p0, float p1, float m0, float m1) {
                // 根据贝塞尔曲线公式计算中间点
                float h00 = (2 * t * t * t) - (3 * t * t) + 1;
                float h10 = t * t * t - (2 * t * t) + t;
                float h01 = (-2 * t * t * t) + (3 * t * t);
                float h11 = t * t * t - t * t;

                // 计算最终结果
                return h00 * p0 + h10 * m0 + h01 * p1 + h11 * m1;
            }
        }

        public record Events(
                //TODO
        ) {
            public static final Codec<Events> CODEC = Codec.unit(new Events());
        }
    }
}
