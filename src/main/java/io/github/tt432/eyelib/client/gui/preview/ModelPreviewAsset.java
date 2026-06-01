package io.github.tt432.eyelib.client.gui.preview;

import io.github.tt432.eyelibimporter.model.bbmodel.Texture;
import io.github.tt432.eyelibmodel.Model;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NullMarked
public record ModelPreviewAsset(
        Model model,
        Texture atlasTexture
) {
}
