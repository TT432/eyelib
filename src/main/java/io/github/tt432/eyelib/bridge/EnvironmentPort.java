package io.github.tt432.eyelib.bridge;

import io.github.tt432.eyelib.bridge.adapter.ForgeEnvironment;

/**
 * 环境 Port：查询 Forge/NeoForge 环境信息，避免 application 直接依赖 ForgeEnvironment。
 *
 * @author TT432
 */
public interface EnvironmentPort {

    /** 三态环境判定：部分节点（26.1）无法区分开发/生产，必须显式区分「未知」而不是猜 false。 */
    enum ProductionState {
        PRODUCTION,
        DEVELOPMENT,
        UNKNOWN
    }

    static boolean isProduction() {
        return ForgeEnvironment.productionState() == ProductionState.PRODUCTION;
    }

    /**
     * 是否为开发环境（只有这一态允许 fail-fast）。
     * <p>
     * 单独提供布尔入口是因为 application 层不得引用 bridge 的具体类型（含枚举）——ADR-0018 I-5。
     * {@code UNKNOWN}（26.1 无法判定）按「非开发」处理。
     */
    static boolean isDevelopment() {
        return ForgeEnvironment.productionState() == ProductionState.DEVELOPMENT;
    }

    /**
     * 精确环境状态。调用方语义必须按三态处理：只有 {@code DEVELOPMENT} 才允许 fail-fast，
     * {@code UNKNOWN} 必须退化为「计数 + 提示」——在无法判定的节点上抛错等于随机崩玩家。
     */
    static ProductionState productionState() {
        return ForgeEnvironment.productionState();
    }
}
