/**
 * This is the pure runtime package for the particle module. No Minecraft/Forge client imports
 * are allowed here — this is a client integration boundary.
 * <p>
 * The root legacy client/particle/bedrock/BrParticle compatibility type has been deleted.
 * The allowed particle -> importer dependency for ParticleDefinitionAdapter preserves
 * mapped fields: identifier, format version, basic render material/texture, curves, events, raw components, billboard flipbook summary, and Molang value preservation.
 * <p>
 * Phase 12 owns loading/publication through ParticleDefinitionRegistry + ParticleResourcePublication.
 * Phase 13 rewires command/network integration through root/MC adapters only.
 * Phase 14 owns final split verification with stable source tests and JetBrains MCP gates.
 */
@NullMarked
package io.github.tt432.eyelibparticle.runtime;

import org.jspecify.annotations.NullMarked;
