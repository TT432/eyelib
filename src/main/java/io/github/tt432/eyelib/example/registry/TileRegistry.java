package io.github.tt432.eyelib.example.registry;

import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.example.block.tile.FertilizerTileEntity;
import io.github.tt432.eyelib.example.block.tile.HabitatTileEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class TileRegistry {
    public static final DeferredRegister<BlockEntityType<?>> TILES = DeferredRegister
            .create(ForgeRegistries.BLOCK_ENTITIES, Eyelib.MOD_ID);

    public static final RegistryObject<BlockEntityType<HabitatTileEntity>> HABITAT_TILE = TILES.register("habitattile",
            () -> BlockEntityType.Builder.of(HabitatTileEntity::new, BlockRegistry.HABITAT_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<FertilizerTileEntity>> FERTILIZER = TILES
            .register("fertilizertile", () -> BlockEntityType.Builder
                    .of(FertilizerTileEntity::new, BlockRegistry.FERTILIZER_BLOCK.get()).build(null));
}
