package io.github.tt432.eyelibimporter.addon;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** @author TT432 */
@NullMarked
public record BedrockAddonWarning(
        BedrockAddonWarningSeverity severity,
        BedrockAddonWarningCode code,
        String packSource,
        @Nullable String relativePath,
        String message
) {
}
