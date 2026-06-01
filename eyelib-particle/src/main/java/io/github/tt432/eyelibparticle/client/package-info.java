/**
 * Particle client integration layer.
 * <p>
 * This package is the explicit client integration layer for the particle module.
 * It contains particle spawn/runtime adapters that bridge the pure runtime
 * with Minecraft/Forge client APIs through Dist.CLIENT event delegation.
 * The runtime/** remains root/MC/Forge-clean — no MC/Forge imports leak back
 * into the runtime package.
 * <p>
 * Key components: ParticleSpawnRuntimeAdapter, ParticleRenderManager,
 * BedrockParticleRenderer.
 */
@NullMarked
package io.github.tt432.eyelibparticle.client;

import org.jspecify.annotations.NullMarked;
