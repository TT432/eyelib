package io.github.tt432.eyelib.client.gui.preview;

import io.github.tt432.eyelib.importer.model.bbmodel.Texture;
import io.github.tt432.eyelib.model.Model;
/**
 * @author TT432
 */
public record ModelPreviewAsset(
        Model model,
        Texture atlasTexture
) {
}
