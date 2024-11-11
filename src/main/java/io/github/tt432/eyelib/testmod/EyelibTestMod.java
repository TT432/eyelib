package io.github.tt432.eyelib.testmod;

import com.mojang.datafixers.DSL;
import io.github.tt432.eyelib.Eyelib;
import io.github.tt432.eyelib.testmod.blocks.TestBenchBlockEntity;
import io.github.tt432.eyelib.testmod.blocks.TestBenchBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EyelibTestMod {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Eyelib.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Eyelib.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Eyelib.MOD_ID);

    public static final DeferredBlock<TestBenchBlock> TEST_BENCH_BLOCK = BLOCKS.register("test_bench", TestBenchBlock::new);
    public static final DeferredItem<BlockItem> TEST_BENCH_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(TEST_BENCH_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TestBenchBlockEntity>> TEST_BENCH_BLOCK_ENTITY_TYPE = BLOCK_ENTITY_TYPES.register("test_bench", () -> BlockEntityType.Builder.of(TestBenchBlockEntity::new, TEST_BENCH_BLOCK.get()).build(DSL.remainderType()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
