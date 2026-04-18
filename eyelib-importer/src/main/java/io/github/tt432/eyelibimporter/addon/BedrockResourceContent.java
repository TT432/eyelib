package io.github.tt432.eyelibimporter.addon;

public sealed interface BedrockResourceContent permits BedrockResourceContent.StructuredContent,
        BedrockResourceContent.TextContent, BedrockResourceContent.BinaryContent {
    record StructuredContent(BedrockResourceValue value) implements BedrockResourceContent {
    }

    record TextContent(String text) implements BedrockResourceContent {
    }

    record BinaryContent(byte[] bytes) implements BedrockResourceContent {
        public BinaryContent {
            bytes = bytes.clone();
        }
    }
}
