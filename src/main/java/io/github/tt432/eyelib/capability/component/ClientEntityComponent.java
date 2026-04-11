package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import io.github.tt432.eyelib.client.entity.ClientEntityRuntimeData;
import io.github.tt432.eyelibimporter.model.Model;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author TT432
 */
public class ClientEntityComponent {
    @Nullable
    private BrClientEntity clientEntity;
    private final ClientEntityRuntimeData runtimeData = new ClientEntityRuntimeData();
    private long clientEntityVersion;
    private long appliedClientEntityVersion;

    public void setClientEntity(@Nullable BrClientEntity clientEntity) {
        this.clientEntity = clientEntity;
        if (runtimeData.sync(clientEntity)) {
            clientEntityVersion++;
        }
    }

    public boolean consumeChanged() {
        if (appliedClientEntityVersion == clientEntityVersion) {
            return false;
        }

        appliedClientEntityVersion = clientEntityVersion;
        return true;
    }

    @Nullable
    public BrClientEntity getClientEntity() {
        return clientEntity;
    }

    public Collection<Model> getModels() {
        return runtimeData.models();
    }
}
