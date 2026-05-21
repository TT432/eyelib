package io.github.tt432.eyelibimporter.addon;

/** @author TT432 */
@org.jspecify.annotations.NullMarked
public record BedrockBinaryAsset(
        String extension,
        byte[] bytes
) {
    public BedrockBinaryAsset {
        bytes = bytes.clone();
    }
}
