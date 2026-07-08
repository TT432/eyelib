package io.github.tt432.eyelib.bridge;

import io.github.tt432.eyelib.bridge.adapter.ForgeEnvironment;

/**
 * 环境 Port：查询 Forge/NeoForge 环境信息，避免 application 直接依赖 ForgeEnvironment。
 */
public interface EnvironmentPort {
    static boolean isProduction() {
        return ForgeEnvironment.isProduction();
    }
}
