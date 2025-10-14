package io.github.tt432.eyelib.client.compat.ar;

import io.github.tt432.eyelib.client.model.bake.BakedModel;
import io.github.tt432.eyelib.client.render.RenderParams;
import net.neoforged.fml.loading.LoadingModList;

public class ARCompat {
	public static final boolean AR_INSTALLED = LoadingModList.get().getModFileById("acceleratedrendering") != null;

	public static boolean renderWithAR(BakedModel.BakedBone bakedBone, RenderParams params) {
		return ARCompatImpl.renderWithAR(bakedBone, params);
	}
}
