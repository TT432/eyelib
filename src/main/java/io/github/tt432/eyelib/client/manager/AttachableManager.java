package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
/** @author TT432 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AttachableManager extends Manager<BrClientEntity> {
    public static final AttachableManager INSTANCE = new AttachableManager();
}
