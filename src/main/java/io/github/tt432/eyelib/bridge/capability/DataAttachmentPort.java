package io.github.tt432.eyelib.bridge.capability;

import com.mojang.serialization.Codec;
import io.github.tt432.eyelib.behavior.EntityBehaviorData;
import io.github.tt432.eyelib.behavior.SyncedBehaviorState;
import io.github.tt432.eyelib.util.dataattach.DataAttachmentType;

import java.util.function.Supplier;

/**
 * EyelibAttachableData Port —— 隔离 application 对具体类的直接依赖。
 */
public interface DataAttachmentPort {
    static DataAttachmentType<SyncedBehaviorState> syncedBehaviorState() {
        return EyelibAttachableData.syncedBehaviorState();
    }

    static DataAttachmentType<EntityBehaviorData> entityBehaviorData() {
        return EyelibAttachableData.entityBehaviorData();
    }

    static <T> Supplier<DataAttachmentType<T>> register(String name, Supplier<T> factory, Supplier<Codec<T>> codecSupplier) {
        return EyelibAttachableData.register(name, factory, codecSupplier);
    }
}
