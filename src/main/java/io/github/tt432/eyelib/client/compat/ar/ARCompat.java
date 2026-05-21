package io.github.tt432.eyelib.client.compat.ar;

import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.bake.BakedModel;
import net.minecraftforge.fml.loading.LoadingModList;
import org.jspecify.annotations.NullMarked;

/** @author TT432 */
@NullMarked
public class ARCompat {
    public static final boolean AR_INSTALLED = LoadingModList.get().getModFileById("acceleratedrendering") != null;

    public static boolean renderWithAR(BakedModel.BakedBone bakedBone, RenderParams params) {
        return ARCompatImpl.renderWithAR(bakedBone, params);
    }
}
