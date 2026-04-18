package io.github.tt432.eyelibimporter.addon;

public record BedrockUnmanagedResource(
        BedrockResourceFamily family,
        String relativePath,
        BedrockResourceContent content,
        BedrockUnmanagedReason reason
) {
}
