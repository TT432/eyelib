package io.github.tt432.eyelib.bridge.client.compat.ar;

//? if <1.20.6 {
import net.minecraftforge.fml.loading.LoadingModList;
//?} else {
import net.neoforged.fml.loading.LoadingModList;
//?}
/**
 * 加速渲染（AR）模组加载检测。实际渲染逻辑在 {@link io.github.tt432.eyelib.client.compat.ar.ARCompatImpl}。
 *
 * @author TT432
 */
public final class ARCompat {
    public static final boolean AR_INSTALLED = LoadingModList.get().getModFileById("acceleratedrendering") != null;

    private ARCompat() {}
}
