package io.github.tt432.eyelib.bridge.molang;

import io.github.tt432.eyelib.bridge.molang.adapter.ComponentStore;
import io.github.tt432.eyelib.bridge.molang.adapter.MolangEntityContext;

public interface MolangContextPort {
    static ComponentStoreView newComponentStore() {
        return new ComponentStore();
    }

    static MolangEntityContextView newMolangEntityContext(ComponentStoreView store) {
        return new MolangEntityContext((ComponentStore) store);
    }
}
