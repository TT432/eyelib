package io.github.tt432.eyelib.bridge.client.compat.ldlib;

//? if <1.20.6 {
import net.minecraftforge.fml.loading.LoadingModList;
//?} else {
import net.neoforged.fml.loading.LoadingModList;
//?}

/**
 * LDLib2 加载检测端口（规格 docs/specs/nodegraph-visual-molang.md D7）。
 *
 * <p>全版本检测 {@code ldlib2}（ldlib1 已于 2026-08-07 退役，见 docs/decisions/0028）。
 * 与 {@code ARCompat} 同模式：版本特定 loader API 集中在 bridge，application 只读布尔。
 * LDLib2 自 ADR-0030 起 jarJar 内嵌进产物 jar，正常情况下恒返回 true；
 * 保留检测作为 jarjar 协商被外部覆盖等异常路径的防御。
 *
 * @author TT432
 */
public interface LdlibCompat {
    /** 节点图编辑器运行时组件（LDLib2，内嵌发布）是否已加载。 */
    static boolean isLdlibLoaded() {
        return LoadingModList.get().getModFileById("ldlib2") != null;
    }
}
