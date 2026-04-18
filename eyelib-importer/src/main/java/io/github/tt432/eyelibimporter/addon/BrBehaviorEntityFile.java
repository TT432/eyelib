package io.github.tt432.eyelibimporter.addon;

import com.google.gson.JsonObject;

public record BrBehaviorEntityFile(
        String formatVersion,
        String identifier,
        BedrockResourceValue.ObjectValue description,
        BedrockResourceValue.ObjectValue componentGroups,
        BedrockResourceValue.ObjectValue components,
        BedrockResourceValue.ObjectValue events,
        BedrockResourceValue.ObjectValue extras
) {
    public static BrBehaviorEntityFile parse(JsonObject root) {
        String formatVersion = root.get("format_version").getAsString();
        JsonObject entityRoot = root.getAsJsonObject("minecraft:entity");
        JsonObject description = entityRoot.getAsJsonObject("description");
        String identifier = description.get("identifier").getAsString();
        JsonObject extras = entityRoot.deepCopy();
        extras.remove("description");
        extras.remove("component_groups");
        extras.remove("components");
        extras.remove("events");
        return new BrBehaviorEntityFile(
                formatVersion,
                identifier,
                (BedrockResourceValue.ObjectValue) BedrockResourceValue.fromJsonElement(description),
                getObject(entityRoot, "component_groups"),
                getObject(entityRoot, "components"),
                getObject(entityRoot, "events"),
                (BedrockResourceValue.ObjectValue) BedrockResourceValue.fromJsonElement(extras)
        );
    }

    private static BedrockResourceValue.ObjectValue getObject(JsonObject root, String key) {
        if (!root.has(key)) {
            return null;
        }
        return (BedrockResourceValue.ObjectValue) BedrockResourceValue.fromJsonElement(root.get(key));
    }
}
