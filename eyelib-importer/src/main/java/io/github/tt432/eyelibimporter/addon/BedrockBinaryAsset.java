package io.github.tt432.eyelibimporter.addon;

public record BedrockBinaryAsset(
        String extension,
        byte[] bytes
) {
    public BedrockBinaryAsset {
        bytes = bytes.clone();
    }
}
