package io.github.tt432.eyelib.bridge.adapter;

import io.github.tt432.eyelib.bridge.EnvironmentPort;

/**
 * Forge/NeoForge 环境信息翻译。
 *
 * @author TT432
 */
public final class ForgeEnvironment {
    private ForgeEnvironment() {
    }

    /**
     * 精确环境状态。
     * <p>
     * 26.1 节点的 loader 不再提供 {@code isProduction()}（该节点的 remap/运行期模型与 21.1 线不同），
     * 因此返回 {@code UNKNOWN} 而不是像旧的 {@code isProduction()} 那样直接 {@code false}——
     * 「false」会被误读为「开发环境」并被用作 fail-fast 判据。
     */
    public static EnvironmentPort.ProductionState productionState() {
        //? if <1.20.6 {
        return net.minecraftforge.fml.loading.FMLLoader.isProduction()
                ? EnvironmentPort.ProductionState.PRODUCTION
                : EnvironmentPort.ProductionState.DEVELOPMENT;
        //?} elif <26.1 {
        return net.neoforged.fml.loading.FMLLoader.isProduction()
                ? EnvironmentPort.ProductionState.PRODUCTION
                : EnvironmentPort.ProductionState.DEVELOPMENT;
        //?} else {
        return EnvironmentPort.ProductionState.UNKNOWN;
        //?}
    }

    /** 是否为生产环境；{@code UNKNOWN} 按「非生产」处理（保持旧调用点行为不变）。 */
    public static boolean isProduction() {
        return productionState() == EnvironmentPort.ProductionState.PRODUCTION;
    }
}
