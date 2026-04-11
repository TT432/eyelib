package io.github.tt432.eyelib.common.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ParticleCommandRuntime {
    private ParticleCommandRuntime() {
    }

    public static List<String> suggestEffectIds(
            String remaining,
            Iterable<String> candidateIds,
            Predicate<String> isValidId
    ) {
        String normalizedRemaining = remaining == null ? "" : remaining.toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();

        for (String candidateId : candidateIds) {
            String normalizedCandidate = candidateId.toLowerCase(Locale.ROOT);
            if (!normalizedCandidate.startsWith(normalizedRemaining)) {
                continue;
            }
            if (!isValidId.test(candidateId)) {
                continue;
            }
            suggestions.add(candidateId);
        }

        return suggestions;
    }

    public static SpawnParticleRequest buildSpawnParticleRequest(
            String particleId,
            double x,
            double y,
            double z,
            Supplier<String> spawnIdSupplier
    ) {
        return new SpawnParticleRequest(
                spawnIdSupplier.get(),
                particleId,
                (float) x,
                (float) y,
                (float) z
        );
    }

    public static String spawnSuccessMessage(SpawnParticleRequest request) {
        return "已生成粒子: " + request.particleId() + " @ " + request.x() + ", " + request.y() + ", " + request.z();
    }

    public record SpawnParticleRequest(
            String spawnId,
            String particleId,
            float x,
            float y,
            float z
    ) {
    }
}
