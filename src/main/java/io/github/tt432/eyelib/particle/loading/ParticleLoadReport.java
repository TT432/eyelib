package io.github.tt432.eyelib.particle.loading;

import java.util.List;
import java.util.Objects;

/**
 * Result of publishing source-keyed particle JSON resources into the module registry.
 */
/** @author TT432 */
public record ParticleLoadReport(
        List<String> processedSourceIds,
        List<String> publishedIdentifiers,
        List<Failure> failures,
        List<String> duplicateIdentifiers
) {
    public ParticleLoadReport {
        processedSourceIds = List.copyOf(Objects.requireNonNull(processedSourceIds, "processedSourceIds"));
        publishedIdentifiers = List.copyOf(Objects.requireNonNull(publishedIdentifiers, "publishedIdentifiers"));
        failures = List.copyOf(Objects.requireNonNull(failures, "failures"));
        duplicateIdentifiers = List.copyOf(Objects.requireNonNull(duplicateIdentifiers, "duplicateIdentifiers"));
    }

    public List<String> failedSourceIds() {
        return failures.stream().map(Failure::sourceId).toList();
    }

    public record Failure(String sourceId, String message) {
        public Failure {
            Objects.requireNonNull(sourceId, "sourceId");
            Objects.requireNonNull(message, "message");
        }
    }
}