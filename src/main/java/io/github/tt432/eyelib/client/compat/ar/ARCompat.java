package io.github.tt432.eyelib.client.compat.ar;

import io.github.tt432.eyelib.client.render.RenderParams;
import io.github.tt432.eyelib.client.render.bake.BakedModel;
import net.minecraftforge.fml.loading.LoadingModList;
/**
 * 加速渲染（AR）兼容性外观类。
 * 检测加速渲染模组是否已安装，并委托实际渲染逻辑。
 *
 * @author TT432
 */
public class ARCompat {
    public static final boolean AR_INSTALLED = LoadingModList.get().getModFileById("acceleratedrendering") != null;

    public static boolean renderWithAR(BakedModel.BakedBone bakedBone, RenderParams params) {
        return ARCompatImpl.renderWithAR(bakedBone, params);
    }
}
