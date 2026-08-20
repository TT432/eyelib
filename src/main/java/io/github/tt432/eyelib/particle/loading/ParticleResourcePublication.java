package io.github.tt432.eyelib.particle.loading;

import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.importer.particle.BrParticle;
import io.github.tt432.eyelib.particle.runtime.ParticleDefinition;
import io.github.tt432.eyelib.particle.runtime.ParticleDefinitionAdapter;
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
    /**
     * 按来源分槽的暂存：sourceKey → 该来源本轮解析出的定义。多个写入方（mod 资源加载器、
     * bedrock addon 桥、GUI 导入）各自替换自己的槽位，互不抹除；flush 时按「最近一次
     * 暂存者靠后、同名 id 后者胜」合并后整体替换注册表。空映射 = 该来源本轮无贡献（卸载语义）。
     */
    private static final Map<Object, Map<String, ParticleDefinition>> STAGED_DEFINITIONS = new LinkedHashMap<>();

    private ParticleResourcePublication() {
    }

    public static ParticleLoadReport replaceFromJsonResources(Object sourceKey, Map<String, JsonElement> resources, Logger logger) {
        Objects.requireNonNull(sourceKey, "sourceKey");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(logger, "logger");

        return replaceFromResources(
                sourceKey,
                resources,
                json -> BrParticle.CODEC.parse(JsonOps.INSTANCE, Objects.requireNonNull(json, "json"))
                        .flatMap(ParticleDefinitionAdapter::fromSchema),
                logger
        );
    }

    public static ParticleLoadReport replaceFromSchemas(Object sourceKey, Map<String, BrParticle> resources, Logger logger) {
        Objects.requireNonNull(sourceKey, "sourceKey");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(logger, "logger");

        return replaceFromResources(
                sourceKey,
                resources,
                schema -> ParticleDefinitionAdapter.fromSchema(Objects.requireNonNull(schema, "schema")),
                logger
        );
    }

    private static <T> ParticleLoadReport replaceFromResources(
            Object sourceKey,
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

        STAGED_DEFINITIONS.remove(sourceKey);
        STAGED_DEFINITIONS.put(sourceKey, definitions);
        LinkedHashMap<String, ParticleDefinition> merged = new LinkedHashMap<>();
        STAGED_DEFINITIONS.values().forEach(merged::putAll);
        ParticleDefinitionRegistry.publisher().replaceParticles(merged.values());
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

    /** 测试钩子：清空全部来源槽位与注册表。 */
    public static void resetStaging() {
        STAGED_DEFINITIONS.clear();
        ParticleDefinitionRegistry.store().clear();
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