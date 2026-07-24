package io.github.tt432.eyelib.debug.benchmark;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Creates or opens the deterministic flat world used by benchmark runs. */
final class BenchmarkWorldController {
    private static final long WORLD_SEED = 12345L;

    private BenchmarkWorldController() {
    }

    static void openOrCreate(Minecraft minecraft, String worldName) throws Exception {
        if (minecraft.getLevelSource().levelExists(worldName)) {
            //? if legacy {
            minecraft.createWorldOpenFlows().loadLevel(null, worldName);
            //?} else {
            minecraft.createWorldOpenFlows().openWorld(worldName, () -> {
            });
            //?}
            return;
        }

        //? if modern {
        LevelSettings levelSettings = new LevelSettings(
                worldName,
                GameType.CREATIVE,
                new LevelSettings.DifficultySettings(Difficulty.PEACEFUL, false, false),
                true,
                WorldDataConfiguration.DEFAULT
        );
        //?} else {
        LevelSettings levelSettings = new LevelSettings(
                worldName,
                GameType.CREATIVE,
                false,
                Difficulty.PEACEFUL,
                true,
                new net.minecraft.world.level.GameRules(),
                WorldDataConfiguration.DEFAULT
        );
        //?}
        WorldOptions worldOptions = new WorldOptions(WORLD_SEED, false, false);
        minecraft.createWorldOpenFlows().createFreshLevel(
                worldName,
                levelSettings,
                worldOptions,
                //? if modern {
                WorldPresets::createFlatWorldDimensions
                //?} else {
                BenchmarkWorldController::createFlatWorldDimensions
                //?}
                //? if !legacy
                , null
        );
    }

    //? if <26.1 {
    private static WorldDimensions createFlatWorldDimensions(RegistryAccess registry) {
        return registry.registryOrThrow(Registries.WORLD_PRESET)
                .getHolderOrThrow(WorldPresets.FLAT)
                .value()
                .createWorldDimensions();
    }
    //?}
}
