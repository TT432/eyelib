package io.github.tt432.eyelib.bridge.client.compat.ldlib;

//? if <1.20.6 {
import net.minecraftforge.fml.loading.LoadingModList;
//?} else {
import net.neoforged.fml.loading.LoadingModList;
//?}

/**
 * LDLib2 可选前置检测端口（规格 docs/specs/nodegraph-visual-molang.md D7）。
 *
 * <p>全版本检测 {@code ldlib2}（ldlib1 已于 2026-08-07 退役，见 docs/decisions/0028）。
 * 与 {@code ARCompat} 同模式：版本特定 loader API 集中在 bridge，application 只读布尔。
 *
 * @author TT432
 */
public interface LdlibCompat {
    /** 节点图编辑器所需的可选前置是否已安装。 */
    static boolean isLdlibLoaded() {
        return LoadingModList.get().getModFileById("ldlib2") != null;
    }
}
