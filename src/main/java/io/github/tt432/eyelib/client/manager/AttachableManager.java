package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;


/** @author TT432 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public final class AttachableManager extends Manager<BrClientEntity> {
    public static final AttachableManager INSTANCE = new AttachableManager();
}
