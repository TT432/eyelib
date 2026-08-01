package io.github.tt432.eyelib.bridge.client.compat.ldlib;

//? if <1.20.6 {
import net.minecraftforge.fml.loading.LoadingModList;
//?} else {
import net.neoforged.fml.loading.LoadingModList;
//?}

/**
 * LDLib / LDLib2 可选前置检测端口（规格 docs/specs/nodegraph-visual-molang.md D7）。
 *
 * <p>1.20.1 检测 {@code ldlib}（LDLib 1.x），1.21.1+ 检测 {@code ldlib2}（LDLib2）。
 * 与 {@code ARCompat} 同模式：版本特定 loader API 集中在 bridge，application 只读布尔。
 *
 * @author TT432
 */
public interface LdlibCompat {
    /** 节点图编辑器所需的可选前置是否已安装。 */
    static boolean isLdlibLoaded() {
        //? if <1.20.6 {
        return LoadingModList.get().getModFileById("ldlib") != null;
        //?} else {
        return LoadingModList.get().getModFileById("ldlib2") != null;
        //?}
    }
}
