package io.github.tt432.eyelib.client.particle.bedrock.component.emitter.lifetime;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.client.particle.bedrock.component.ComponentTarget;
import io.github.tt432.eyelib.client.particle.bedrock.component.ParticleComponent;
import io.github.tt432.eyelib.util.Collectors;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author TT432
 */
@ParticleComponent(value = "emitter_lifetime_events", target = ComponentTarget.EMITTER)
public record EmitterLifetimeEvents(
        List<String> creationEvent,
        List<String> expirationEvent,
        Timeline timeline,
        TravelDistanceEvents travelDistanceEvents,
        LoopingTravelDistanceEvents loopingTravelDistanceEvents
) {
    public static final Codec<EmitterLifetimeEvents> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.listOf().optionalFieldOf("creation_event", List.of()).forGetter(o -> o.creationEvent),
            Codec.STRING.listOf().optionalFieldOf("expiration_event", List.of()).forGetter(o -> o.expirationEvent),
            Timeline.CODEC.optionalFieldOf("timeline", Timeline.EMPTY).forGetter(o -> o.timeline),
            TravelDistanceEvents.CODEC
                    .optionalFieldOf("travel_distance_events", TravelDistanceEvents.EMPTY)
                    .forGetter(o -> o.travelDistanceEvents),
            LoopingTravelDistanceEvents.CODEC
                    .optionalFieldOf("looping_travel_distance_events", LoopingTravelDistanceEvents.EMPTY)
                    .forGetter(o -> o.loopingTravelDistanceEvents)
    ).apply(ins, EmitterLifetimeEvents::new));

    /**
     * 一系列时间，例如 0.0 或 1.0，用于触发事件
     * 这些事件在发射器每次循环时触发
     * "time" 表示时间，例如其中一行可能是：
     * "0.4": "event"
     */
    public record Timeline(
            TreeMap<Float, String> time
    ) {
        public static final Timeline EMPTY = new Timeline(new TreeMap<>());

        public static final Codec<Timeline> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.either(Codec.STRING.listOf(), Codec.STRING.xmap(List::of, List::getFirst))
                        .xmap(Either::unwrap, Either::left)
                        .xmap(sl -> sl.stream().map(s -> s.split(":"))
                                        .map(s -> Map.entry(Float.parseFloat(s[0]), s[1]))
                                        .collect(Collectors.toTreeMap(Comparator.comparingDouble(k -> k),
                                                Map.Entry::getKey, Map.Entry::getValue)),
                                map -> map.entrySet().stream()
                                        .map(e -> e.getKey() + ":" + e.getValue())
                                        .toList())
                        .fieldOf("time").forGetter(o -> o.time)
        ).apply(ins, Timeline::new));
    }

    /**
     * 一系列的距离，例如 0.0 或 1.0，会触发事件
     * 当发射器移动了指定的输入距离时，这些事件就会被触发
     * 例如，一行代码可能是：
     * "0.4": "event"
     */
    public record TravelDistanceEvents(
            TreeMap<Float, String> distance
    ) {
        public static final TravelDistanceEvents EMPTY = new TravelDistanceEvents(new TreeMap<>());

        public static final Codec<TravelDistanceEvents> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                Codec.either(Codec.STRING.listOf(), Codec.STRING.xmap(List::of, List::getFirst))
                        .xmap(Either::unwrap, Either::left)
                        .xmap(sl -> sl.stream().map(s -> s.split(":"))
                                        .map(s -> Map.entry(Float.parseFloat(s[0]), s[1]))
                                        .collect(Collectors.toTreeMap(Comparator.comparingDouble(k -> k),
                                                Map.Entry::getKey, Map.Entry::getValue)),
                                map -> map.entrySet().stream()
                                        .map(e -> e.getKey() + ":" + e.getValue())
                                        .toList())
                        .fieldOf("distance").forGetter(o -> o.distance)
        ).apply(ins, TravelDistanceEvents::new));
    }

    /**
     * 一系列按固定间隔发生的事件
     * 这些事件在发射器从上次触发位置移动指定输入距离时触发。
     * 这些事件的格式示例如下：
     * {
     * "distance": 1.0,
     * "effects": [ "effect_one" ]
     * },
     * {
     * "distance": 2.0,
     * "effects": [ "effect_two" ]
     * }
     * 注意，“effect_one”和“effect_two”必须是particle_effect中定义的事件
     */
    public record LoopingTravelDistanceEvents(
            List<EventEntry> events
    ) {
        public static final LoopingTravelDistanceEvents EMPTY = new LoopingTravelDistanceEvents(List.of());

        public static final Codec<LoopingTravelDistanceEvents> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                EventEntry.CODEC.listOf().fieldOf("events").forGetter(o -> o.events)
        ).apply(ins, LoopingTravelDistanceEvents::new));

        public record EventEntry(
                float distance,
                List<String> effects
        ) {
            public static final Codec<EventEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                    Codec.FLOAT.fieldOf("distance").forGetter(o -> o.distance),
                    Codec.STRING.listOf().fieldOf("effects").forGetter(o -> o.effects)
            ).apply(ins, EventEntry::new));
        }
    }
}
