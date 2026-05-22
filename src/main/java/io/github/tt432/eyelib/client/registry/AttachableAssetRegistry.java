package io.github.tt432.eyelib.client.registry;

import io.github.tt432.eyelib.client.manager.AttachableManager;
import io.github.tt432.eyelibimporter.entity.BrClientEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.LinkedHashMap;


/** @author TT432 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public final class AttachableAssetRegistry {
    public static void publishAttachable(BrClientEntity attachable) {
        AttachableManager.writePort().put(attachable.identifier(), attachable);
    }

    public static void replaceAttachables(Iterable<BrClientEntity> attachables) {
        LinkedHashMap<String, BrClientEntity> flattened = new LinkedHashMap<>();
        attachables.forEach(attachable -> flattened.put(attachable.identifier(), attachable));
        AttachableManager.writePort().replaceAll(flattened);
    }
}
