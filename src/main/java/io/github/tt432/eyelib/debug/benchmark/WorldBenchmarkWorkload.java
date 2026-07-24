package io.github.tt432.eyelib.debug.benchmark;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;
//? if >=26.1
import net.minecraft.world.entity.EntitySpawnReason;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Fixed, server-authoritative entities rendered through the real world pipeline. */
final class WorldBenchmarkWorkload implements BenchmarkWorkload {
    private static final String BENCHMARK_TAG = "eyelib_benchmark";
    private static final double CAMERA_X = 0.0;
    private static final double CAMERA_Y = 5.0;
    private static final double CAMERA_Z = 14.0;

    private final List<Entity> serverEntities = new ArrayList<>();
    private volatile boolean serverPrepared;
    private volatile @Nullable Throwable preparationFailure;
    private volatile Set<UUID> spawnedEntityIds = Set.of();
    private @Nullable MinecraftServer server;
    private @Nullable BenchmarkScenario scenario;

    @Override
    public void prepare(Minecraft minecraft, BenchmarkScenario scenario) {
        if (minecraft.level == null || minecraft.player == null) {
            throw new IllegalStateException("Client world is not ready");
        }
        MinecraftServer integratedServer = minecraft.getSingleplayerServer();
        if (integratedServer == null) {
            throw new IllegalStateException("WORLD benchmark requires an integrated server");
        }
        this.server = integratedServer;
        this.scenario = scenario;
        var dimension = minecraft.level.dimension();
        UUID playerId = minecraft.player.getUUID();

        integratedServer.execute(() -> {
            try {
                ServerLevel level = integratedServer.getLevel(dimension);
                if (level == null) {
                    throw new IllegalStateException("Benchmark server level is unavailable");
                }
                var serverPlayer = integratedServer.getPlayerList().getPlayer(playerId);
                if (serverPlayer == null) {
                    throw new IllegalStateException("Benchmark server player is unavailable");
                }
                serverPlayer.setYRot(180.0F);
                serverPlayer.setXRot(12.0F);
                serverPlayer.teleportTo(CAMERA_X, CAMERA_Y, CAMERA_Z);
                var commandSource = integratedServer.createCommandSourceStack();
                integratedServer.setDifficulty(Difficulty.NORMAL, true);
                //? if <26.1 {
                integratedServer.getCommands().getDispatcher().execute("gamerule doMobSpawning false", commandSource);
                integratedServer.getCommands().getDispatcher().execute("gamerule doDaylightCycle false", commandSource);
                //?} else {
                integratedServer.getCommands().getDispatcher().execute("gamerule spawn_mobs false", commandSource);
                integratedServer.getCommands().getDispatcher().execute("gamerule advance_time false", commandSource);
                integratedServer.getCommands().getDispatcher().execute("gamerule advance_weather false", commandSource);
                //?}
                integratedServer.getCommands().getDispatcher().execute("time set noon", commandSource);
                integratedServer.getCommands().getDispatcher().execute("weather clear", commandSource);
                for (Entity existing : level.getAllEntities()) {
                    if (!(existing instanceof Player)) {
                        existing.discard();
                    }
                }
                int columns = Math.max(1, (int) Math.ceil(Math.sqrt(scenario.entityCount())));
                double spacing = 2.25;
                Set<UUID> entityIds = new HashSet<>();
                int entityIndex = 0;
                for (BenchmarkScenario.EntitySpec spec : scenario.entitySpecs()) {
                    EntityType<?> entityType = BenchmarkEntityTypes.resolve(spec.id());
                    for (int i = 0; i < spec.count(); i++) {
                        //? if <26.1 {
                        Entity entity = entityType.create(level);
                        //?} else {
                        Entity entity = entityType.create(level, EntitySpawnReason.COMMAND);
                        //?}
                        if (entity == null) {
                            throw new IllegalStateException("Failed to create server entity " + spec.id()
                                    + " at index " + entityIndex);
                        }
                        int column = entityIndex % columns;
                        int row = entityIndex / columns;
                        double x = (column - (columns - 1) / 2.0) * spacing;
                        double z = -row * spacing;
                        entity.setPos(x, 1.0, z);
                        entity.setNoGravity(true);
                        entity.setInvulnerable(true);
                        entity.setSilent(true);
                        entity.addTag(BENCHMARK_TAG);
                        if (entity instanceof Mob mob) {
                            mob.setNoAi(true);
                        }
                        if (!level.addFreshEntity(entity)) {
                            throw new IllegalStateException("Server rejected benchmark entity " + entityIndex);
                        }
                        serverEntities.add(entity);
                        entityIds.add(entity.getUUID());
                        entityIndex++;
                    }
                }
                spawnedEntityIds = Set.copyOf(entityIds);
                serverPrepared = true;
            } catch (Throwable throwable) {
                preparationFailure = throwable;
            }
        });
    }

    @Override
    public boolean isReady(Minecraft minecraft) {
        checkPreparationFailure();
        BenchmarkScenario currentScenario = scenario;
        return serverPrepared && currentScenario != null
                && actualEntityCount(minecraft) == currentScenario.entityCount();
    }

    @Override
    public void maintain(Minecraft minecraft) {
        checkPreparationFailure();
        if (minecraft.player == null) {
            return;
        }
        minecraft.player.setPos(CAMERA_X, CAMERA_Y, CAMERA_Z);
        minecraft.player.setDeltaMovement(0.0, 0.0, 0.0);
        minecraft.player.setYRot(180.0F);
        minecraft.player.setXRot(12.0F);
    }

    @Override
    public void render(Minecraft minecraft) {
        // Real-world rendering is performed by Minecraft's normal level renderer.
    }

    @Override
    public int actualEntityCount(Minecraft minecraft) {
        if (minecraft.level == null) {
            return 0;
        }
        int count = 0;
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (spawnedEntityIds.contains(entity.getUUID())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void close() {
        MinecraftServer capturedServer = server;
        if (capturedServer != null) {
            CompletableFuture<Void> cleanup = new CompletableFuture<>();
            capturedServer.execute(() -> {
                try {
                    for (Entity entity : serverEntities) {
                        entity.discard();
                    }
                    serverEntities.clear();
                    cleanup.complete(null);
                } catch (Throwable throwable) {
                    cleanup.completeExceptionally(throwable);
                }
            });
            cleanup.orTimeout(10, TimeUnit.SECONDS).join();
        } else {
            serverEntities.clear();
        }
        serverPrepared = false;
        preparationFailure = null;
        spawnedEntityIds = Set.of();
        server = null;
        scenario = null;
    }


    private void checkPreparationFailure() {
        Throwable failure = preparationFailure;
        if (failure != null) {
            throw new IllegalStateException("WORLD workload preparation failed: " + failure, failure);
        }
    }
}
