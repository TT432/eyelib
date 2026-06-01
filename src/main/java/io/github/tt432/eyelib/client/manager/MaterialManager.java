package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelibmaterial.material.BrMaterialEntry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public class MaterialManager extends Manager<BrMaterialEntry> {
    public static final MaterialManager INSTANCE = new MaterialManager();
}