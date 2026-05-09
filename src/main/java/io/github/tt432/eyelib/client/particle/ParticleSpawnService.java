package io.github.tt432.eyelib.client.particle;

import io.github.tt432.eyelib.capability.EyelibAttachableData;
import io.github.tt432.eyelib.capability.RenderData;
import io.github.tt432.eyelib.client.ClientTickHandler;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticle;
import io.github.tt432.eyelib.client.particle.bedrock.BrParticleEmitter;
import io.github.tt432.eyelib.mc.impl.data_attach.DataAttachmentHelper;
import io.github.tt432.eyelib.mc.impl.network.packet.SpawnParticlePacket;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibparticle.api.ParticleSpawnApi;
import io.github.tt432.eyelibparticle.api.ParticleSpawnRequest;
import io.github.tt432.eyelibparticle.client.ParticleRenderManager;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinitionAdapter;
import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleEmitter;
import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleRuntime;
import io.github.tt432.eyelibparticle.runtime.bedrock.ParticleRuntimeEnvironment;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.mojang.serialization.JsonOps;

import java.util.Optional;

/**
 * Transitional root runtime adapter for {@link ParticleSpawnApi}.
 * <p>
 * Packet adaptation delegates through the particle-module request API while Minecraft/capability/render-manager work stays
 * in root. Remove this facade after packet/runtime callers bind directly to particle API adapters/services.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticleSpawnService {
    private static final ParticleSpawnApi API = new RootParticleSpawnApi();

    public static ParticleSpawnApi api() {
        return API;
    }

    public static void spawnFromPacket(SpawnParticlePacket packet) {
        api().spawn(new ParticleSpawnRequest(packet.spawnId(), packet.particleId(), packet.position()));
    }

    public static void spawnEmitter(String spawnId, BrParticleEmitter emitter) {
        spawnEmitter(spawnId, emitter.getParticle(), emitter.molangScope, emitter.getLevel(), emitter.getPosition());
    }

    public static @Nullable BedrockParticleEmitter spawnEmitter(
            String spawnId,
            BrParticle particle,
            @Nullable MolangScope parentScope,
            Level level,
            Vector3f position
    ) {
        BedrockParticleEmitter emitter = createEmitter(particle, parentScope, level, position);
        if (emitter != null) {
            ParticleRenderManager.INSTANCE.spawnEmitter(spawnId, emitter);
        }
        return emitter;
    }

    public static void removeEmitter(String removeId) {
        api().remove(removeId);
    }

    public static void initPose(BedrockParticleEmitter emitter, @Nullable Matrix4f locatorMatrix, @Nullable Entity attachedEntity) {
        emitter.baseRotation().identity();
        Matrix4f matrix = new Matrix4f()
                .translate(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().toVector3f(), new Matrix4f());
        if (locatorMatrix != null) {
            matrix.mul(locatorMatrix);
        }

        if (emitter.space().position() || emitter.position().equals(0, 0, 0)) {
            if (locatorMatrix != null) {
                matrix.transformPosition(emitter.position().zero());
            } else if (attachedEntity != null) {
                emitter.position().set(attachedEntity.getX(), attachedEntity.getY(), attachedEntity.getZ());
            }
        }

        if (emitter.space().position() && emitter.space().rotation()) {
            if (locatorMatrix != null) {
                emitter.baseRotation().set(matrix);
            } else if (attachedEntity != null) {
                emitter.baseRotation().rotateZYX(new Vector3f(
                        (float) Math.toRadians(attachedEntity.getXRot()),
                        (float) Math.toRadians(attachedEntity.getYRot()),
                        0
                ));
            }
        }
    }

    private static @Nullable BedrockParticleEmitter createEmitter(
            BrParticle particle,
            @Nullable MolangScope parentScope,
            Level level,
            Vector3f position
    ) {
        Optional<ParticleDefinition> definition = toModuleDefinition(particle);
        if (definition.isEmpty()) {
            return null;
        }
        return new BedrockParticleRuntime(
                definition.orElseThrow(),
                new MinecraftParticleRuntimeEnvironment(level),
                ParticleRenderManager.INSTANCE::spawnParticle
        ).createEmitter(Optional.ofNullable(parentScope), position);
    }

    private static Optional<ParticleDefinition> toModuleDefinition(BrParticle particle) {
        return BrParticle.CODEC.encodeStart(JsonOps.INSTANCE, particle)
                .flatMap(json -> io.github.tt432.eyelibimporter.particle.BrParticle.CODEC.parse(JsonOps.INSTANCE, json))
                .flatMap(ParticleDefinitionAdapter::fromSchema)
                .result();
    }

    private static final class RootParticleSpawnApi implements ParticleSpawnApi {
        @Override
        public void spawn(ParticleSpawnRequest request) {
            BrParticle particle = ParticleLookup.get(request.particleId());
            if (particle == null || Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) {
                return;
            }

            RenderData<?> data = DataAttachmentHelper.getOrCreate(EyelibAttachableData.RENDER_DATA.get(), Minecraft.getInstance().player);
            BedrockParticleEmitter emitter = createEmitter(
                    particle,
                    data.getScope(),
                    Minecraft.getInstance().level,
                    request.position()
            );
            if (emitter == null) {
                return;
            }
            ParticleRenderManager.INSTANCE.spawnEmitter(
                    request.spawnId(),
                    emitter
            );

        }

        @Override
        public void remove(String spawnId) {
            ParticleRenderManager.INSTANCE.removeEmitter(spawnId);
        }
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
