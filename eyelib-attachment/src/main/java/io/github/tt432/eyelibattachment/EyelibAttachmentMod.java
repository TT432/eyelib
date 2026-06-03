package io.github.tt432.eyelibattachment;

import io.github.tt432.eyelibattachment.dataattach.mc.DataAttachmentTypeRegistry;
import io.github.tt432.eyelibattachment.network.*;
import io.github.tt432.eyelibnetwork.EyelibNetworkTransport;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * 附加数据模块的主模组类。
 *
 * @author TT432
 */
@Mod(EyelibAttachmentMod.MOD_ID)
public class EyelibAttachmentMod {
    public static final String MOD_ID = "eyelibattachment";

    public EyelibAttachmentMod() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        DataAttachmentTypeRegistry.DATA_ATTACHMENTS.register(bus);

        registerNetworkPackets();
    }

    private static void registerNetworkPackets() {
        EyelibNetworkTransport.registerServerPacket(
                UpdateDestroyInfoPacket.class,
                UpdateDestroyInfoPacket.STREAM_CODEC::encode,
                UpdateDestroyInfoPacket.STREAM_CODEC::decode,
                DataAttachmentSyncRuntime::handleDestroyInfoUpdate
        );

        EyelibNetworkTransport.registerClientPacket(
                ExtraEntityUpdateDataPacket.class,
                ExtraEntityUpdateDataPacket.STREAM_CODEC::encode,
                ExtraEntityUpdateDataPacket.STREAM_CODEC::decode,
                DataAttachmentSyncRuntime::applyExtraEntityUpdateData
        );

        EyelibNetworkTransport.registerClientPacket(
                UniDataUpdatePacket.class,
                UniDataUpdatePacket.STREAM_CODEC::encode,
                UniDataUpdatePacket.STREAM_CODEC::decode,
                DataAttachmentSyncRuntime::applyUpdate
        );

        EyelibNetworkTransport.registerClientPacket(
                ExtraEntityDataPacket.class,
                ExtraEntityDataPacket.STREAM_CODEC::encode,
                ExtraEntityDataPacket.STREAM_CODEC::decode,
                DataAttachmentSyncRuntime::applyExtraEntityData
        );

        EyelibNetworkTransport.registerClientPacket(
                DataAttachmentUpdatePacket.class,
                DataAttachmentUpdatePacket.STREAM_CODEC::encode,
                DataAttachmentUpdatePacket.STREAM_CODEC::decode,
                DataAttachmentSyncRuntime::applyUpdate
        );

        EyelibNetworkTransport.registerClientPacket(
                DataAttachmentSyncPacket.class,
                DataAttachmentSyncPacket.STREAM_CODEC::encode,
                DataAttachmentSyncPacket.STREAM_CODEC::decode,
                DataAttachmentSyncRuntime::applySync
        );
    }
}