package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.client.entity.BrClientEntity;
import io.github.tt432.eyelib.client.entity.ClientEntityLookup;
import io.github.tt432.eyelib.event.ManagerEntryChangedEvent;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import org.jetbrains.annotations.Nullable;

/**
 * @author TT432
 */
@Getter
@Setter
public class ClientEntityComponent {
    @Nullable
    private BrClientEntity clientEntity;

    {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ManagerEntryChangedEvent.class, event -> {
            if (event.getManagerName().equals(ClientEntityLookup.managerName())
                    && clientEntity != null
                    && event.getEntryName().equals(clientEntity.identifier())) {
                clientEntity = (BrClientEntity) event.getEntryData();
            }
        });
    }
}
