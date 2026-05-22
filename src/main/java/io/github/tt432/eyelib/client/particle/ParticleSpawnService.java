package io.github.tt432.eyelib.client.particle;

import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.ClientTickHandler;
import io.github.tt432.eyelibattachment.dataattach.mc.DataAttachmentHelper;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibparticle.api.ParticleSpawnApi;
import io.github.tt432.eyelibparticle.api.ParticleSpawnRequest;
import io.github.tt432.eyelibparticle.client.ParticleEmitterPoseInitializer;
import io.github.tt432.eyelibparticle.client.ParticleRenderManager;
import io.github.tt432.eyelibparticle.client.ParticleSpawnRuntimeAdapter;
import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;
import io.github.tt432.eyelibparticle.network.SpawnParticlePacket;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;
import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleEmitter;
import io.github.tt432.eyelibparticle.runtime.bedrock.ParticleRuntimeEnvironment;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Transitional root runtime adapter for {@link ParticleSpawnApi}。
 * 将粒子专属的生成/运行时行为委托给 {@link ParticleSpawnRuntimeAdapter}。
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public final class ParticleSpawnService {
    private static final ParticleSpawnRuntimeAdapter ADAPTER = new ParticleSpawnRuntimeAdapter(
            ParticleDefinitionRegistry.store(),
            ParticleRenderManager.INSTANCE,
            ParticleSpawnService::currentEnvironment,
            ParticleSpawnService::currentParentScope
    );

    public static ParticleSpawnApi api() {
        return ADAPTER;
    }

    public static void spawnFromPacket(SpawnParticlePacket packet) {
        api().spawn(new ParticleSpawnRequest(packet.spawnId(), packet.particleId(), packet.position()));
    }

    public static @Nullable BedrockParticleEmitter spawnEmitter(
            String spawnId,
            ParticleDefinition definition,
            @Nullable MolangScope parentScope,
            Level level,
            Vector3f position
    ) {
        return ADAPTER.spawnEmitter(
                spawnId,
                definition,
                Optional.ofNullable(parentScope),
                new MinecraftParticleRuntimeEnvironment(level),
                position
        );
    }

    public static void removeEmitter(String removeId) {
        api().remove(removeId);
    }

    public static void initPose(BedrockParticleEmitter emitter, @Nullable Matrix4f locatorMatrix, @Nullable Entity attachedEntity) {
        ParticleEmitterPoseInitializer.initPose(emitter, locatorMatrix, attachedEntity);
    }

    private static Optional<ParticleRuntimeEnvironment> currentEnvironment() {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) {
            return Optional.empty();
        }
        return Optional.of(new MinecraftParticleRuntimeEnvironment(Minecraft.getInstance().level));
    }

    private static Optional<MolangScope> currentParentScope() {
        if (Minecraft.getInstance().player == null) {
            return Optional.empty();
        }
        RenderData<?> data = DataAttachmentHelper.getOrCreate(EyelibAttachableData.RENDER_DATA.get(), Minecraft.getInstance().player);
        return Optional.ofNullable(data.getScope());
    }

    private record MinecraftParticleRuntimeEnvironment(Level level) implements ParticleRuntimeEnvironment {
        @Override
        public int ticks() {
            return ClientTickHandler.getTick();
        }

        @Override
        public float partialTick() {
            return Minecraft.getInstance().timer.partialTick;
        }

        @Override
        public Optional<EmitterParticleComponent.EmitterAccess.Bounds> entityBounds() {
            Entity entity = Minecraft.getInstance().player;
            if (entity == null) {
                return Optional.empty();
            }
            AABB bounds = entity.getBoundingBox();
            return Optional.of(new EmitterParticleComponent.EmitterAccess.Bounds(
                    new Vector3f((float) bounds.getCenter().x, (float) bounds.getCenter().y, (float) bounds.getCenter().z),
                    new Vector3f((float) bounds.getXsize() / 2F, (float) bounds.getYsize() / 2F, (float) bounds.getZsize() / 2F)
            ));
        }

        @Override
        public Optional<String> blockAtPosition(Vector3f position) {
            return Optional.of(BuiltInRegistries.BLOCK.getKey(level.getBlockState(BlockPos.containing(
                    position.x,
                    position.y,
                    position.z
            )).getBlock()).toString());
        }
    }
}
