package io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.lifetime;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.particle.runtime.bedrock.component.emitter.EmitterParticleComponent;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** @author TT432 */
public record EmitterLifetimeEvents(
        List<String> creationEvent,
        List<String> expirationEvent,
        Timeline timeline,
        TravelDistanceEvents travelDistanceEvents,
        LoopingTravelDistanceEvents loopingTravelDistanceEvents
) implements EmitterParticleComponent {
    public static final Codec<EmitterLifetimeEvents> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.listOf().optionalFieldOf("creation_event", List.of()).forGetter(EmitterLifetimeEvents::creationEvent),
            Codec.STRING.listOf().optionalFieldOf("expiration_event", List.of()).forGetter(EmitterLifetimeEvents::expirationEvent),
            Timeline.CODEC.optionalFieldOf("timeline", Timeline.EMPTY).forGetter(EmitterLifetimeEvents::timeline),
            TravelDistanceEvents.CODEC.optionalFieldOf("travel_distance_events", TravelDistanceEvents.EMPTY)
                    .forGetter(EmitterLifetimeEvents::travelDistanceEvents),
            LoopingTravelDistanceEvents.CODEC.optionalFieldOf("looping_travel_distance_events", LoopingTravelDistanceEvents.EMPTY)
                    .forGetter(EmitterLifetimeEvents::loopingTravelDistanceEvents)
    ).apply(ins, EmitterLifetimeEvents::new));

    public record Timeline(TreeMap<Float, String> time) {
        public static final Timeline EMPTY = new Timeline(new TreeMap<>());
        public static final Codec<Timeline> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                timeEventCodec().fieldOf("time").forGetter(Timeline::time)
        ).apply(ins, Timeline::new));
    }

    public record TravelDistanceEvents(TreeMap<Float, String> distance) {
        public static final TravelDistanceEvents EMPTY = new TravelDistanceEvents(new TreeMap<>());
        public static final Codec<TravelDistanceEvents> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                timeEventCodec().fieldOf("distance").forGetter(TravelDistanceEvents::distance)
        ).apply(ins, TravelDistanceEvents::new));
    }

    public record LoopingTravelDistanceEvents(List<EventEntry> events) {
        public static final LoopingTravelDistanceEvents EMPTY = new LoopingTravelDistanceEvents(List.of());
        public static final Codec<LoopingTravelDistanceEvents> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                EventEntry.CODEC.listOf().fieldOf("events").forGetter(LoopingTravelDistanceEvents::events)
        ).apply(ins, LoopingTravelDistanceEvents::new));

        public record EventEntry(float distance, List<String> effects) {
            public static final Codec<EventEntry> CODEC = RecordCodecBuilder.create(ins -> ins.group(
                    Codec.FLOAT.fieldOf("distance").forGetter(EventEntry::distance),
                    Codec.STRING.listOf().fieldOf("effects").forGetter(EventEntry::effects)
            ).apply(ins, EventEntry::new));
        }
    }

    private static Codec<TreeMap<Float, String>> timeEventCodec() {
        return Codec.either(Codec.STRING.listOf(), Codec.STRING.xmap(List::of, list -> list.get(0)))
                .xmap(either -> either.map(left -> left, right -> right), Either::left)
                .xmap(values -> values.stream()
                                .map(value -> value.split(":"))
                                .map(parts -> Map.entry(Float.parseFloat(parts[0]), parts[1]))
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        (first, second) -> second,
                                        () -> new TreeMap<>(Comparator.comparingDouble(key -> key))
                                )),
                        map -> map.entrySet().stream()
                                .map(entry -> entry.getKey() + ":" + entry.getValue())
                                .toList());
    }
}