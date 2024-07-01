package io.github.tt432.eyelib.client.particle.bedrock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

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
            Map<String, Object> components
    ) {
        public static final Codec<ParticleEffect> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Description.CODEC.fieldOf("description").forGetter(o -> o.description),
                Codec.unboundedMap(Codec.STRING, Curve.CODEC).optionalFieldOf("curves", Map.of())
                        .forGetter(o -> o.curves),
                Events.CODEC.optionalFieldOf("events", new Events()).forGetter(o -> o.events),
                Codec.dispatchedMap(
                        Codec.STRING,
                        id -> {
                            ParticleComponentManager.ParticleComponentInfo particleComponentInfo =
                                    ParticleComponentManager.all.byName.get(ResourceLocation.parse(id));
                            if (particleComponentInfo == null) return Codec.unit(new Object());
                            return particleComponentInfo.codec();
                        }
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
                    String texture
            ) {
                public static final Codec<BasicRenderParameters> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                        Codec.STRING.fieldOf("material").forGetter(o -> o.material),
                        Codec.STRING.fieldOf("texture").forGetter(o -> o.texture)
                ).apply(ins, BasicRenderParameters::new));
            }
        }

        public record Curve(
                Type type,
                TreeMap<Float, MolangValue> nodes,
                TreeMap<Float, ChainNode> chainNodes,
                MolangValue input,
                MolangValue horizontal_range
        ) {
            public static final Codec<Curve> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                    Type.CODEC.optionalFieldOf("type", Type.LINEAR).forGetter(o -> o.type),
                    MolangValue.CODEC.listOf().xmap(list -> {
                                TreeMap<Float, MolangValue> r = new TreeMap<>(Comparator.comparingDouble(k -> k));
                                for (int i = 0; i < list.size(); i++) {
                                    r.put(i / (list.size() - 1F), list.get(i));
                                }
                                return r;
                            }, tmap -> tmap.values().stream().toList())
                            .optionalFieldOf("nodes", new TreeMap<>()).forGetter(o -> o.nodes),
                    EyelibCodec.treeMap(
                            Codec.STRING.xmap(Float::parseFloat, String::valueOf),
                            ChainNode.CODEC,
                            Comparator.comparingDouble(k -> k)
                    ).optionalFieldOf("chainNodes", new TreeMap<>()).forGetter(o -> o.chainNodes),
                    MolangValue.CODEC.optionalFieldOf("input", MolangValue.ZERO).forGetter(o -> o.input),
                    MolangValue.CODEC.optionalFieldOf("horizontal_range", MolangValue.ONE)
                            .forGetter(o -> o.horizontal_range)
            ).apply(ins, Curve::new));

            public record ChainNode(
                    float leftValue,
                    float rightValue,
                    float leftSlope,
                    float rightSlope
            ) {
                public static final Codec<ChainNode> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                        EyelibCodec.withAlternative(
                                Codec.FLOAT.fieldOf("left_value"),
                                Codec.FLOAT.fieldOf("value")
                        ).forGetter(o -> o.leftValue),
                        EyelibCodec.withAlternative(
                                Codec.FLOAT.fieldOf("right_value"),
                                Codec.FLOAT.fieldOf("value")
                        ).forGetter(o -> o.rightValue),
                        EyelibCodec.withAlternative(
                                Codec.FLOAT.fieldOf("left_slope"),
                                Codec.FLOAT.fieldOf("slope")
                        ).forGetter(o -> o.leftSlope),
                        EyelibCodec.withAlternative(
                                Codec.FLOAT.fieldOf("right_slope"),
                                Codec.FLOAT.fieldOf("slope")
                        ).forGetter(o -> o.rightSlope)
                ).apply(ins, ChainNode::new));
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
                float inputValue = input.eval(scope);
                float horizontalRange = horizontal_range.eval(scope);
                float normalizedInput = inputValue / horizontalRange;

                return switch (type) {
                    case LINEAR -> calculateLinear(normalizedInput, scope);
                    case BEZIER -> calculateBezier(normalizedInput, scope);
                    case BEZIER_CHAIN -> calculateBezierChain(normalizedInput);
                    case CATMULL_ROM -> calculateCatmullRom(normalizedInput, scope);
                };
            }

            private float calculateLinear(float input, MolangScope scope) {
                var before = nodes.floorEntry(input);
                var after = nodes.higherEntry(input);

                if (before == null) return after.getValue().eval(scope);
                else if (after == null) return before.getValue().eval(scope);
                else return EyeMath.lerp(before.getValue().eval(scope),
                            after.getValue().eval(scope),
                            (input - before.getKey()) / (after.getKey() - before.getKey()));
            }

            private float calculateBezier(float input, MolangScope scope) {
                if (nodes.size() != 4) return 0;
                FloatList floats = new FloatArrayList();

                nodes.forEach((k, v) -> floats.add(v.eval(scope)));

                return bezier(input, floats.getFloat(0), floats.getFloat(1),
                        floats.getFloat(2), floats.getFloat(3));
            }

            private float calculateBezierChain(float input) {
                if (chainNodes.isEmpty()) return 0;

                Map.Entry<Float, ChainNode> lowerEntry = chainNodes.floorEntry(input);
                Map.Entry<Float, ChainNode> higherEntry = chainNodes.ceilingEntry(input);

                if (lowerEntry == null || higherEntry == null || lowerEntry.equals(higherEntry)) {
                    return lowerEntry != null ? lowerEntry.getValue().leftValue : 0;
                }

                ChainNode lowerNode = lowerEntry.getValue();
                ChainNode higherNode = higherEntry.getValue();

                float t = (input - lowerEntry.getKey()) / (higherEntry.getKey() - lowerEntry.getKey());
                return bezierChain(t, lowerNode.leftValue, higherNode.rightValue, lowerNode.leftSlope, higherNode.rightSlope);
            }

            private float calculateCatmullRom(float input, MolangScope scope) {
                var before = nodes.floorEntry(input);
                var after = nodes.higherEntry(input);

                if (before == null) return after.getValue().eval(scope);
                else if (after == null) return before.getValue().eval(scope);
                else return Curves.catmullRom(input,
                            Objects.requireNonNullElse(nodes.lowerEntry(before.getKey()), before).getValue().eval(scope),
                            before.getValue().eval(scope),
                            after.getValue().eval(scope),
                            Objects.requireNonNullElse(nodes.higherEntry(after.getKey()), after).getValue().eval(scope));
            }

            private float bezier(float t, float p0, float p1, float p2, float p3) {
                float u = 1 - t;
                return (u * u * u * p0) + (3 * u * u * t * p1) + (3 * u * t * t * p2) + (t * t * t * p3);
            }

            private float bezierChain(float t, float p0, float p1, float m0, float m1) {
                float u = 1 - t;
                return (u * u * u * p0) + (3 * u * u * t * m0) + (3 * u * t * t * m1) + (t * t * t * p1);
            }
        }

        public record Events(
                //TODO
        ) {
            public static final Codec<Events> CODEC = Codec.unit(new Events());
        }
    }
}
