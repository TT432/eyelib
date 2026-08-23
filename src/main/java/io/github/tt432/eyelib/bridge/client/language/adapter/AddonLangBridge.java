package io.github.tt432.eyelib.bridge.client.language.adapter;
//? if <26.1 {

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
//? if <1.20.6 {
import net.minecraftforge.event.AddPackFindersEvent;
//?} else {
import net.neoforged.neoforge.event.AddPackFindersEvent;
//?}

/**
 * addon 语言桥：注册 {@link AddonLangPack}（required 客户端资源包，mod 总线
 * AddPackFindersEvent），addon 替换后重载 LanguageManager（{@link #triggerLangReload}）
 * 使新合成的 lang JSON 生效。
 *
 * @author TT432
 */
public final class AddonLangBridge {
    private AddonLangBridge() {
    }

    /** AddPackFindersEvent 监听：把 addon 语言包挂进客户端资源扫描。 */
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        //? if <1.20.6 {
        event.addRepositorySource(consumer -> consumer.accept(Pack.readMetaAndCreate(
                AddonLangPack.PACK_ID,
                Component.literal("eyelib addon lang"),
                true,
                id -> new AddonLangPack(),
                PackType.CLIENT_RESOURCES,
                Pack.Position.TOP,
                PackSource.DEFAULT)));
        //?} else {
        // 1.21 的 ResourcesSupplier 双抽象方法（openPrimary/openFull），不能写 lambda
        Pack.ResourcesSupplier resources = new Pack.ResourcesSupplier() {
            @Override
            public net.minecraft.server.packs.PackResources openPrimary(
                    net.minecraft.server.packs.PackLocationInfo info) {
                return new AddonLangPack();
            }

            @Override
            public net.minecraft.server.packs.PackResources openFull(
                    net.minecraft.server.packs.PackLocationInfo info,
                    Pack.Metadata metadata) {
                return new AddonLangPack();
            }
        };
        event.addRepositorySource(consumer -> consumer.accept(Pack.readMetaAndCreate(
                new net.minecraft.server.packs.PackLocationInfo(
                        AddonLangPack.PACK_ID,
                        Component.literal("eyelib addon lang"),
                        PackSource.DEFAULT,
                        java.util.Optional.empty()),
                resources,
                PackType.CLIENT_RESOURCES,
                new net.minecraft.server.packs.PackSelectionConfig(true, Pack.Position.TOP, true))));
        //?}
    }

    /**
     * addon 资产替换后调用：重载 LanguageManager 使新合成的 lang JSON 生效。
     * onResourceManagerReload 是 public 同步方法（仅解析 1-2 个 JSON，毫秒级，无遮罩），
     * 比全量 reloadResourcePacks（LoadingOverlay + 全部 listener）轻一个量级。
     */
    public static void triggerLangReload() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return; // 单测/headless 环境无客户端实例
        }
        minecraft.execute(() -> minecraft.getLanguageManager()
                .onResourceManagerReload(minecraft.getResourceManager()));
    }
}
//?}
