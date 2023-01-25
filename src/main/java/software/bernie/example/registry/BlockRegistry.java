package software.bernie.example.registry;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import software.bernie.example.block.FertilizerBlock;
import software.bernie.example.block.HabitatBlock;
import io.github.tt432.eyelib.Eyelib;

public class BlockRegistry {
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS,
			Eyelib.MOD_ID);

	public static final RegistryObject<HabitatBlock> HABITAT_BLOCK = BLOCKS.register("habitatblock",
			HabitatBlock::new);
	public static final RegistryObject<FertilizerBlock> FERTILIZER_BLOCK = BLOCKS.register("fertilizerblock",
			FertilizerBlock::new);
}
