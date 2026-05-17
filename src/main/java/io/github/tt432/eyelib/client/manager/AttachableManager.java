package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibutil.manager.ManagerReadPort;
import io.github.tt432.eyelibutil.manager.ManagerWritePort;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttachableManager extends Manager<BrClientEntity> {
    public static final AttachableManager INSTANCE = new AttachableManager();

    public static ManagerReadPort<BrClientEntity> readPort() {
        return INSTANCE;
    }

    public static ManagerWritePort<BrClientEntity> writePort() {
        return INSTANCE;
    }
}
