package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelibutil.manager.ManagerReadPort;
import io.github.tt432.eyelibutil.manager.ManagerWritePort;

public class ClientEntityManager extends Manager<BrClientEntity> {
    public static final ClientEntityManager INSTANCE = new ClientEntityManager();

    public static ManagerReadPort<BrClientEntity> readPort() {
        return INSTANCE;
    }

    public static ManagerWritePort<BrClientEntity> writePort() {
        return INSTANCE;
    }
}
