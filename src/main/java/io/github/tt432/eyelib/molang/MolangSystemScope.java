package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.client.animation.system.AnimationControllerSystem;

import java.util.function.Supplier;

/**
 * @author TT432
 */
public enum MolangSystemScope {
    ANIMATIONS(AnimationControllerSystem::getScope),
    NONE(() -> null),
    ;

    final Supplier<MolangScope> scopeSupplier;

    MolangSystemScope(Supplier<MolangScope> scopeSupplier) {
        this.scopeSupplier = scopeSupplier;
    }

    public MolangScope getScope() {
        return scopeSupplier.get();
    }
}
