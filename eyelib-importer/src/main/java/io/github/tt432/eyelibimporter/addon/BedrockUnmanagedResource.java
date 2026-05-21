package io.github.tt432.eyelibimporter.addon;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record BedrockUnmanagedResource(
        BedrockResourceFamily family,
        String relativePath,
        BedrockResourceContent content,
        BedrockUnmanagedReason reason
) {
}
