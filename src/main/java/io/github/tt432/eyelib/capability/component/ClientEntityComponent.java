package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.client.entity.BrClientEntity;
import io.github.tt432.eyelib.event.ManagerEntryChangedEvent;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.List;

/**
 * @author TT432
 */
@Getter
@Setter
public class ClientEntityComponent {
    private BrClientEntity clientEntity;
    public List<ModelComponent> components;

    {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ManagerEntryChangedEvent.class, event -> {
            if (event.getManagerName().equals(Eyelib.getClientEntityLoader().getManagerName())
                    && clientEntity != null
                    && event.getEntryName().equals(clientEntity.identifier())) {
                clientEntity = (BrClientEntity) event.getEntryData();
            }
        });
    }
}
