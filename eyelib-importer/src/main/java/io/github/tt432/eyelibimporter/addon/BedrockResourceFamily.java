package io.github.tt432.eyelibimporter.addon;

import java.util.Locale;

public enum BedrockResourceFamily {
    MANIFEST,
    PACK_ICON,
    TEXTURE,
    MATERIAL,
    ANIMATION,
    ANIMATION_CONTROLLER,
    CLIENT_ENTITY,
    ATTACHABLE,
    MODEL,
    RENDER_CONTROLLER,
    PARTICLE,
    SOUND_INDEX,
    SOUND_DEFINITION,
    SOUND_FILE,
    LOCALIZATION,
    BEHAVIOR_ENTITY,
    ITEM,
    BLOCK,
    RECIPE,
    LOOT_TABLE,
    SPAWN_RULE,
    TRADING,
    FEATURE,
    FEATURE_RULE,
    STRUCTURE,
    SCRIPT,
    UI,
    FOG,
    BIOME,
    TEXTURE_METADATA,
    UNKNOWN_JSON,
    UNKNOWN_TEXT,
    UNKNOWN_BINARY;

    public static BedrockResourceFamily classify(String relativePath) {
        String path = relativePath.toLowerCase(Locale.ROOT);
        if (path.equals("manifest.json")) return MANIFEST;
        if (path.equals("pack_icon.png")) return PACK_ICON;
        if (path.startsWith("textures/") && path.endsWith(".png")) return TEXTURE;
        if (path.startsWith("materials/") && path.endsWith(".material")) return MATERIAL;
        if (path.startsWith("animations/")) return ANIMATION;
        if (path.startsWith("animation_controllers/")) return ANIMATION_CONTROLLER;
        if (path.startsWith("entity/")) return CLIENT_ENTITY;
        if (path.startsWith("attachables/")) return ATTACHABLE;
        if (path.startsWith("models/")) return MODEL;
        if (path.startsWith("render_controllers/")) return RENDER_CONTROLLER;
        if (path.startsWith("particles/")) return PARTICLE;
        if (path.equals("sounds.json")) return SOUND_INDEX;
        if (path.startsWith("sounds/")) return path.endsWith("sound_definitions.json") ? SOUND_DEFINITION : SOUND_FILE;
        if (path.startsWith("texts/")) return LOCALIZATION;
        if (path.startsWith("entities/")) return BEHAVIOR_ENTITY;
        if (path.startsWith("items/")) return ITEM;
        if (path.startsWith("blocks/")) return BLOCK;
        if (path.startsWith("recipes/")) return RECIPE;
        if (path.startsWith("loot_tables/")) return LOOT_TABLE;
        if (path.startsWith("spawn_rules/")) return SPAWN_RULE;
        if (path.startsWith("trading/")) return TRADING;
        if (path.startsWith("features/")) return FEATURE;
        if (path.startsWith("feature_rules/")) return FEATURE_RULE;
        if (path.startsWith("structures/")) return STRUCTURE;
        if (path.startsWith("scripts/")) return SCRIPT;
        if (path.startsWith("ui/")) return UI;
        if (path.startsWith("fogs/")) return FOG;
        if (path.startsWith("biomes/")) return BIOME;
        if (path.startsWith("textures/") && path.endsWith(".json")) return TEXTURE_METADATA;
        if (path.endsWith(".json") || path.endsWith(".material") || path.endsWith(".bbmodel")) return UNKNOWN_JSON;
        if (path.endsWith(".lang") || path.endsWith(".txt") || path.endsWith(".md")) return UNKNOWN_TEXT;
        return UNKNOWN_BINARY;
    }
}
