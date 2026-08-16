package io.github.tt432.eyelib.bridge.client.loader.adapter;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 把 resourcepacks/ 下的 .mcpack/.mcaddon 注册为 vanilla 客户端资源包（每文件一个），
 * 使其接受 {@code PackRepository} 原版管理：资源包界面可见、可启用/禁用/排序，
 * 选择持久化在 options.txt。注册为可选包（required=false，Position.TOP），默认未选中。
 * <p>
 * 包内容不经过 vanilla 资源视图（见 {@link BedrockPackResources} 类文档）；
 * 选中集合由 {@link BedrockAddonAutoLoader} 在资源重载时经 ResourceManager 枚举。
 *
 * @author TT432
 */
public final class BedrockAddonPackFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(BedrockAddonPackFinder.class);

    private BedrockAddonPackFinder() {
    }

    /** AddPackFindersEvent 监听（mod 总线，Eyelib 入口注册）。 */
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        event.addRepositorySource(consumer -> discover(consumer));
    }

    private static void discover(java.util.function.Consumer<Pack> consumer) {
        Path resourcepacksDir = Minecraft.getInstance().gameDirectory.toPath().resolve("resourcepacks");
        if (!Files.isDirectory(resourcepacksDir)) {
            return;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(resourcepacksDir, BedrockPackResources::isBedrockAddonFile)) {
            for (Path file : stream) {
                Pack pack = createPack(file);
                if (pack != null) {
                    consumer.accept(pack);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to scan resourcepacks/ for Bedrock addon packs", e);
        }
    }

    private static Pack createPack(Path file) {
        BedrockPackResources resources = BedrockPackResources.of(file);
        //? if <1.20.6 {
        return Pack.readMetaAndCreate(
                BedrockPackResources.packIdOf(file),
                resources.title(),
                false,
                id -> resources,
                PackType.CLIENT_RESOURCES,
                Pack.Position.TOP,
                PackSource.DEFAULT);
        //?} else {
        // >=1.20.6（含 26.1）：PackLocationInfo + 双抽象方法 ResourcesSupplier + PackSelectionConfig；
        // 26.1 仅包名迁移（PackLocationInfo/PackSelectionConfig 在 net.minecraft.server.packs），调用形状相同
        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override
            public net.minecraft.server.packs.PackResources openPrimary(
                    net.minecraft.server.packs.PackLocationInfo info) {
                return resources;
            }

            @Override
            public net.minecraft.server.packs.PackResources openFull(
                    net.minecraft.server.packs.PackLocationInfo info, Pack.Metadata metadata) {
                return resources;
            }
        };
        return Pack.readMetaAndCreate(
                new net.minecraft.server.packs.PackLocationInfo(
                        BedrockPackResources.packIdOf(file),
                        resources.title(), PackSource.DEFAULT, java.util.Optional.empty()),
                supplier,
                PackType.CLIENT_RESOURCES,
                new net.minecraft.server.packs.PackSelectionConfig(false, Pack.Position.TOP, false));
        //?}
    }
}
