package io.github.tt432.eyelib.model;

import io.github.tt432.eyelib.importer.model.bbmodel.Texture;

/**
 * @author TT432
 */
public record ModelPreviewAsset(
        Model model,
        Texture atlasTexture
) {
}
