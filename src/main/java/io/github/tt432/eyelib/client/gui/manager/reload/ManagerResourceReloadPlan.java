package io.github.tt432.eyelib.client.gui.manager.reload;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ManagerResourceReloadPlan {
    public enum ReloadTarget {
        ANIMATION_JSON,
        ANIMATION_CONTROLLER_JSON,
        RENDER_CONTROLLER_JSON,
        ENTITY_JSON,
        PARTICLE_JSON,
        MODEL_JSON,
        MODEL_BBMODEL,
        TEXTURE_PNG,
        UNSUPPORTED
    }

    public static ReloadTarget classifySingleFile(String relativePath) {
        String normalizedRelativePath = normalize(relativePath);

        if (normalizedRelativePath.endsWith(".json")) {
            if (normalizedRelativePath.startsWith("animations/")) {
                return ReloadTarget.ANIMATION_JSON;
            }
            if (normalizedRelativePath.startsWith("animation_controllers/")) {
                return ReloadTarget.ANIMATION_CONTROLLER_JSON;
            }
            if (normalizedRelativePath.startsWith("render_controllers/")) {
                return ReloadTarget.RENDER_CONTROLLER_JSON;
            }
            if (normalizedRelativePath.startsWith("entity/")) {
                return ReloadTarget.ENTITY_JSON;
            }
            if (normalizedRelativePath.startsWith("particles/")) {
                return ReloadTarget.PARTICLE_JSON;
            }
            if (normalizedRelativePath.startsWith("models/")) {
                return ReloadTarget.MODEL_JSON;
            }
            return ReloadTarget.UNSUPPORTED;
        }

        if (normalizedRelativePath.startsWith("models/") && normalizedRelativePath.endsWith(".bbmodel")) {
            return ReloadTarget.MODEL_BBMODEL;
        }

        if (normalizedRelativePath.startsWith("textures/") && normalizedRelativePath.endsWith(".png")) {
            return ReloadTarget.TEXTURE_PNG;
        }

        return ReloadTarget.UNSUPPORTED;
    }

    public static ReloadTarget classifySingleFile(Path basePath, Path file) {
        return classifySingleFile(basePath.relativize(file).toString());
    }

    public static String toTextureKey(Path basePath, Path textureFile) {
        return normalize(basePath.relativize(textureFile).toString()).toLowerCase(Locale.ROOT);
    }

    private static String normalize(String relativePath) {
        return relativePath.replace("\\", "/");
    }
}
