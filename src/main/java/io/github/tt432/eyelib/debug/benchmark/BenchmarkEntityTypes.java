package io.github.tt432.eyelib.debug.benchmark;

import net.minecraft.core.registries.BuiltInRegistries;
//? if <26.1 {
import net.minecraft.resources.ResourceLocation;
//?} else {
import net.minecraft.resources.Identifier;
//?}
import net.minecraft.world.entity.EntityType;

/** Cross-version entity registry lookup used by benchmark workloads. */
final class BenchmarkEntityTypes {
    private BenchmarkEntityTypes() {
    }

    static EntityType<?> resolve(String id) {
        //? if <26.1 {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(key)) {
            throw new IllegalArgumentException("Unknown entity type: " + id);
        }
        return BuiltInRegistries.ENTITY_TYPE.get(key);
        //?} else {
        Identifier key = Identifier.tryParse(id);
        if (key == null) {
            throw new IllegalArgumentException("Invalid entity type: " + id);
        }
        return BuiltInRegistries.ENTITY_TYPE.get(key)
                .map(holder -> holder.value())
                .orElseThrow(() -> new IllegalArgumentException("Unknown entity type: " + id));
        //?}
    }
}
