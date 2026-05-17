package io.github.tt432.eyelibanimation;
import io.github.tt432.eyelibparticle.client.ParticleRenderManager;
import io.github.tt432.eyelibparticle.client.ParticleSpawnRuntimeAdapter;
import io.github.tt432.eyelibparticle.loading.ParticleDefinitionRegistry;
import io.github.tt432.eyelibparticle.runtime.ParticleDefinition;
import io.github.tt432.eyelibparticle.runtime.bedrock.BedrockParticleEmitter;
import io.github.tt432.eyelibparticle.runtime.bedrock.ParticleRuntimeEnvironment;
import io.github.tt432.eyelibparticle.runtime.bedrock.component.emitter.EmitterParticleComponent;
import lombok.AccessLevel; import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft; import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries; import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level; import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import java.util.Optional;
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnimationParticleSpawner {
    private static final ParticleSpawnRuntimeAdapter ADAPTER = new ParticleSpawnRuntimeAdapter(ParticleDefinitionRegistry.store(), ParticleRenderManager.INSTANCE, AnimationParticleSpawner::makeEnvironment, Optional::empty);
    public static BedrockParticleEmitter spawn(String spawnId, ParticleDefinition definition, Vector3f position) { var env = makeEnvironment().orElse(null); if (env == null) return null; return ADAPTER.spawnEmitter(spawnId, definition, Optional.empty(), env, position); }
    public static void remove(String spawnId) { ParticleRenderManager.INSTANCE.removeEmitter(spawnId); }
    private static Optional<ParticleRuntimeEnvironment> makeEnvironment() { Level level = Minecraft.getInstance().level; if (level == null) return Optional.empty(); return Optional.of(new Environment(level)); }
    private record Environment(Level level) implements ParticleRuntimeEnvironment {
        @Override public int ticks() { var lvl = Minecraft.getInstance().level; return lvl != null ? (int) lvl.getGameTime() : 0; }
        @Override public float partialTick() { return Minecraft.getInstance().getFrameTime(); }
        @Override public Optional<EmitterParticleComponent.EmitterAccess.Bounds> entityBounds() { Entity entity = Minecraft.getInstance().player; if (entity == null) return Optional.empty(); AABB bounds = entity.getBoundingBox(); return Optional.of(new EmitterParticleComponent.EmitterAccess.Bounds(new Vector3f((float) bounds.getCenter().x, (float) bounds.getCenter().y, (float) bounds.getCenter().z), new Vector3f((float) bounds.getXsize() / 2F, (float) bounds.getYsize() / 2F, (float) bounds.getZsize() / 2F))); }
        @Override public Optional<String> blockAtPosition(Vector3f position) { return Optional.of(BuiltInRegistries.BLOCK.getKey(level.getBlockState(BlockPos.containing(position.x, position.y, position.z)).getBlock()).toString()); }
    }
}
