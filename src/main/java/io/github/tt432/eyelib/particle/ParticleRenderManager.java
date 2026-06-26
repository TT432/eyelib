package io.github.tt432.eyelib.particle;

import io.github.tt432.eyelib.particle.runtime.bedrock.BedrockParticleEmitter;
import io.github.tt432.eyelib.particle.runtime.bedrock.BedrockParticleInstance;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Module-owned client render manager for Bedrock particle emitters and particles.
 * 单例由 bridge 层 {@code ParticleRuntimeBridge} 持有并注入 {@link ParticleClientRuntimeServices}。
 */
/** @author TT432 */
public final class ParticleRenderManager {
    private final Object2ObjectMap<String, BedrockParticleEmitter> emitters = new Object2ObjectOpenHashMap<>();
    private final ObjectList<BedrockParticleInstance> particles = new ObjectArrayList<>();
    private final ParticleClientRuntimeServices runtimeServices;

    private final Predicate<Map.Entry<String, BedrockParticleEmitter>> removeEmitters = entry -> entry.getValue().removed();
    private final Consumer<BedrockParticleEmitter> renderEmitters = BedrockParticleEmitter::onRenderFrame;
    private final Predicate<BedrockParticleInstance> removeParticles = BedrockParticleInstance::removed;
    private final Consumer<BedrockParticleInstance> renderParticles = BedrockParticleInstance::onRenderFrame;

    public ParticleRenderManager(ParticleClientRuntimeServices runtimeServices) {
        this.runtimeServices = Objects.requireNonNull(runtimeServices, "runtimeServices");
    }

    public int getEmitterCount() {
        return emitters.size();
    }

    public int getParticleCount() {
        return particles.size();
    }

    public void spawnEmitter(String id, BedrockParticleEmitter emitter) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(emitter, "emitter");
        runtimeServices.submit(() -> {
            if (emitters.containsKey(id)) {
                return;
            }
            emitters.put(id, emitter);
        });
    }

    public void removeEmitter(String id) {
        Objects.requireNonNull(id, "id");
        runtimeServices.submit(() -> emitters.remove(id));
    }

    public void spawnParticle(BedrockParticleInstance particle) {
        Objects.requireNonNull(particle, "particle");
        runtimeServices.submit(() -> particles.add(particle));
    }

    public void onRenderTickStart() {
        emitters.object2ObjectEntrySet().removeIf(removeEmitters);
        emitters.values().forEach(renderEmitters);
        particles.removeIf(removeParticles);
        particles.forEach(renderParticles);
    }

    public void onClientTickStart() {
        emitters.values().forEach(BedrockParticleEmitter::onTick);
    }

    public void renderAfterEntities(ParticleRenderer renderer) {
        Objects.requireNonNull(renderer, "renderer");
        particles.forEach(renderer::render);
    }

    public void clear() {
        emitters.clear();
        particles.clear();
    }

    @FunctionalInterface
    public interface ParticleRenderer {
        void render(BedrockParticleInstance particle);
    }
}