package io.github.tt432.eyelib.bridge.adapter;

/**
 * Forge/NeoForge 环境信息翻译。
 *
 * @author TT432
 */
public final class ForgeEnvironment {
    private ForgeEnvironment() {}


    public static boolean isProduction() {
        //? if <1.20.6 {
        return net.minecraftforge.fml.loading.FMLLoader.isProduction();
        //?} elif <26.1 {
        return net.neoforged.fml.loading.FMLLoader.isProduction();
        //?} else {
        return false;
        //?}
    }
}

