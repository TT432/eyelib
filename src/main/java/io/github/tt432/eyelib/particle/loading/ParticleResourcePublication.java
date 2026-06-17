package io.github.tt432.eyelibparticle.loading;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelibimporter.particle.BrParticle;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapter;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Parses source-keyed Bedrock particle JSON resources and publishes valid runtime definitions.
 */
/** @author TT432 */
public final class ParticleResourcePublication {
    private ParticleResourcePublication() {
    }

    public static ParticleLoadReport replaceFromJsonResources(Map<String, JsonElement> resources, Logger logger) {
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(logger, "logger");

        return replaceFromResources(
                resources,
                json -> BrParticle.CODEC.parse(JsonOps.INSTANCE, Objects.requireNonNull(json, "json"))
                        .flatMap(ParticleDefinitionAdapter::fromSchema),
                logger
        );
    }

    public static ParticleLoadReport replaceFromSchemas(Map<String, BrParticle> resources, Logger logger) {
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(logger, "logger");

        return replaceFromResources(
                resources,
                schema -> ParticleDefinitionAdapter.fromSchema(Objects.requireNonNull(schema, "schema")),
                logger
        );
    }

    private static <T> ParticleLoadReport replaceFromResources(
            Map<String, T> resources,
            Function<T, DataResult<ParticleDefinition>> parser,
            Logger logger
    ) {

        List<String> processedSourceIds = new ArrayList<>();
        List<ParticleLoadReport.Failure> failures = new ArrayList<>();
        List<String> duplicateIdentifiers = new ArrayList<>();
        LinkedHashMap<String, ParticleDefinition> definitions = new LinkedHashMap<>();

        resources.forEach((sourceId, json) -> {
            String checkedSourceId = Objects.requireNonNull(sourceId, "sourceId");
            processedSourceIds.add(checkedSourceId);

            DataResult<ParticleDefinition> result = parser.apply(json);
            result.result().ifPresentOrElse(definition -> {
                if (definitions.containsKey(definition.identifier())
                        && !duplicateIdentifiers.contains(definition.identifier())) {
                    duplicateIdentifiers.add(definition.identifier());
                }
                definitions.put(definition.identifier(), definition);
            }, () -> recordFailure(checkedSourceId, result, logger, failures));
        });

        ParticleDefinitionRegistry.publisher().replaceParticles(definitions.values());
        return new ParticleLoadReport(
                processedSourceIds,
                List.copyOf(definitions.keySet()),
                failures,
                duplicateIdentifiers
        );
    }

    public static ParticleLoadReport publishFromJsonResource(String sourceId, JsonElement resource, Logger logger) {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(resource, "resource");
        Objects.requireNonNull(logger, "logger");

        List<ParticleLoadReport.Failure> failures = new ArrayList<>();
        DataResult<ParticleDefinition> result = BrParticle.CODEC.parse(JsonOps.INSTANCE, resource)
                .flatMap(ParticleDefinitionAdapter::fromSchema);
        return result.result().map(definition -> {
            ParticleDefinitionRegistry.publisher().publishParticle(definition);
            return new ParticleLoadReport(
                    List.of(sourceId),
                    List.of(definition.identifier()),
                    List.of(),
                    List.of()
            );
        }).orElseGet(() -> {
            recordFailure(sourceId, result, logger, failures);
            return new ParticleLoadReport(List.of(sourceId), List.of(), failures, List.of());
        });
    }

    private static void recordFailure(String sourceId, DataResult<?> result, Logger logger,
            List<ParticleLoadReport.Failure> failures) {
        String message = result.error()
                .map(error -> error.message())
                .orElse("Unknown particle loading failure");
        logger.error("Couldn't parse particle data file {}: {}", sourceId, message);
        failures.add(new ParticleLoadReport.Failure(sourceId, message));
    }
}