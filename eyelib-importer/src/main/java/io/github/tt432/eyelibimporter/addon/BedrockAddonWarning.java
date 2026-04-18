package io.github.tt432.eyelibimporter.addon;

import org.jspecify.annotations.Nullable;

public record BedrockAddonWarning(
        BedrockAddonWarningSeverity severity,
        BedrockAddonWarningCode code,
        String packSource,
        @Nullable String relativePath,
        String message
) {
}
