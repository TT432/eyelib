package io.github.tt432.eyelib.debug.benchmark;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import io.github.tt432.clientsmoke.runtime.EntitySceneRenderer;
import io.github.tt432.eyelib.bridge.client.render.adapter.RenderLivingEventAdapter;
import io.github.tt432.eyelib.client.render.EntityRenderOrchestrator;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
//? if >=26.1
import net.minecraft.world.entity.EntitySpawnReason;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Repeated controlled entity rendering into a private fixed-size framebuffer. */
final class FboBenchmarkWorkload implements BenchmarkWorkload {
    private static final int BACKGROUND_ARGB = 0xFF202028;

    private final List<Entity> entities = new ArrayList<>();
    private @Nullable RenderTarget target;
    private @Nullable BenchmarkScenario scenario;

    @Override
    public void prepare(Minecraft minecraft, BenchmarkScenario scenario) {
        if (minecraft.level == null) {
            throw new IllegalStateException("Client level is not ready");
        }
        this.scenario = scenario;
        //? if modern {
        target = new TextureTarget(null, scenario.fboSize(), scenario.fboSize(), true);
        //?} else {
        target = new TextureTarget(scenario.fboSize(), scenario.fboSize(), true, Minecraft.ON_OSX);
        //?}

        int entityIndex = 0;
        for (BenchmarkScenario.EntitySpec spec : scenario.entitySpecs()) {
            EntityType<?> entityType = BenchmarkEntityTypes.resolve(spec.id());
            for (int i = 0; i < spec.count(); i++) {
                //? if <26.1 {
                Entity entity = entityType.create(minecraft.level);
                //?} else {
                Entity entity = entityType.create(minecraft.level, EntitySpawnReason.COMMAND);
                //?}
                if (entity == null) {
                    throw new IllegalStateException("Failed to create client entity " + spec.id() + " at index " + entityIndex);
                }
                if (!(entity instanceof LivingEntity)) {
                    throw new IllegalArgumentException("FBO benchmark requires living entities: " + spec.id());
                }
                entity.setPos(0.0, 0.0, 0.0);
                entity.tickCount = 20 + entityIndex;
                EntityRenderOrchestrator.prepareDetachedEntity(entity);
                entities.add(entity);
                entityIndex++;
            }
        }
    }

    @Override
    public boolean isReady(Minecraft minecraft) {
        BenchmarkScenario currentScenario = scenario;
        return target != null && currentScenario != null && entities.size() == currentScenario.entityCount();
    }

    @Override
    public void maintain(Minecraft minecraft) {
        for (Entity entity : entities) {
            entity.tickCount++;
        }
    }

    @Override
    public void render(Minecraft minecraft) {
        if (target == null || scenario == null) {
            throw new IllegalStateException("FBO workload is not prepared");
        }

        int columns = Math.max(1, (int) Math.ceil(Math.sqrt(entities.size())));
        int rows = Math.max(1, (entities.size() + columns - 1) / columns);
        float cellWidth = target.width / (float) columns;
        float cellHeight = target.height / (float) rows;
        float scale = Math.max(16.0F, Math.min(cellWidth, cellHeight) * 0.72F);

        EntitySceneRenderer.beginScene(minecraft, target, BACKGROUND_ARGB);
        //? if <26.1
        io.github.tt432.eyelib.bridge.client.render.skinning.adapter.LegacySkinningManager.openBatchWindow();
        try {
            for (int i = 0; i < entities.size(); i++) {
                int column = i % columns;
                int row = i / columns;
                float centerX = (column + 0.5F) * cellWidth;
                float centerY = (row + 0.78F) * cellHeight;
                float yaw = (i * 37.0F) % 360.0F;
                Entity entity = entities.get(i);
                RenderLivingEventAdapter.sceneEntityOverride = entity instanceof LivingEntity living ? living : null;
                EntitySceneRenderer.renderEntityAt(minecraft, entity, centerX, centerY, scale, yaw);
            }
        } finally {
            RenderLivingEventAdapter.sceneEntityOverride = null;
            //? if <26.1
            io.github.tt432.eyelib.bridge.client.render.skinning.adapter.LegacySkinningManager.drainBatch();
            EntitySceneRenderer.endScene(minecraft);
            //? if <26.1
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    @Override
    public int actualEntityCount(Minecraft minecraft) {
        return entities.size();
    }

    @Override
    public void close() {
        entities.clear();
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
        scenario = null;
    }

}
