package io.github.tt432.eyelib.client.manager;

import io.github.tt432.eyelibmaterial.material.BrMaterialEntry;
import io.github.tt432.eyelibutil.manager.ManagerReadPort;
import io.github.tt432.eyelibutil.manager.ManagerWritePort;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
/** @author TT432 */
public class MaterialManager extends Manager<BrMaterialEntry> {
    public static final MaterialManager INSTANCE = new MaterialManager();

    public static ManagerReadPort<BrMaterialEntry> readPort() {
        return INSTANCE;
    }

    public static ManagerWritePort<BrMaterialEntry> writePort() {
        return INSTANCE;
    }
}