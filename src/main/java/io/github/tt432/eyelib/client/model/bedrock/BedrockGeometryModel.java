package io.github.tt432.eyelib.client.model.bedrock;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public record BedrockGeometryModel(
        List<Geometry> geometries
) {
    public record Geometry(
            Description description,
            List<Bone> bones
    ) {
    }

    public record Description(
            String identifier,
            int textureWidth,
            int textureHeight,
            float visibleBoundsWidth,
            float visibleBoundsHeight,
            Vector3f visibleBoundsOffset
    ) {
    }

    /**
     * Parsed Bedrock bone locator: name is the JSON key (without _null_ handling — use {@link #nullObject()}).
     */
    public record BoneLocatorEntry(
            String name,
            Vector3f offset,
            @Nullable Vector3f rotation,
            boolean ignoreInheritedScale,
            boolean nullObject
    ) {
    }

    public record TextureMeshDef(
            String texture,
            Vector3f position,
            Vector3f rotation,
            Vector3f localPivot,
            Vector3f scale
    ) {
    }

    public record Bone(
            String name,
            @Nullable String parent,
            Vector3f pivot,
            Vector3f rotation,
            List<Cube> cubes,
            @Nullable String material,
            @Nullable String binding,
            boolean reset,
            boolean mirror,
            List<BoneLocatorEntry> locators,
            List<TextureMeshDef> textureMeshes
    ) {
    }

    public record Cube(
            Vector3f origin,
            Vector3f size,
            @Nullable Vector3f pivot,
            Vector3f rotation,
            float inflate,
            @Nullable Boolean mirror,
            @Nullable Vector2f boxUv,
            Map<String, FaceUv> faceUvs
    ) {
    }

    public record FaceUv(
            Vector2f uv,
            Vector2f uvSize,
            int uvRotation,
            @Nullable String materialInstance
    ) {
    }
}
