/**
 * Client particle runtime, emitters, render manager, and lookup/spawn boundaries.
 * <p>
 * Module {@code :eyelib-particle} owns the canonical runtime.
 * io.github.tt432.eyelibimporter.particle.BrParticle is the canonical raw Bedrock particle schema/codec owner.
 * io.github.tt432.eyelibparticle.runtime.ParticleDefinition is the canonical module runtime definition owner;
 * use {@code ParticleDefinition.identifier()} for the definition key.
 * Root legacy client/particle/bedrock/BrParticle has been deleted.
 * <p>
 * The allowed particle -> importer dependency for ParticleDefinitionAdapter preserves
 * mapped fields: identifier, format version, basic render material/texture, curves, events, raw components, billboard flipbook summary, and Molang value preservation.
 * <p>
 * Phase 12 owns loading/publication through ParticleDefinitionRegistry + ParticleResourcePublication.
 * Phase 13 rewires command/network integration through root/MC adapters only ({@code mc/impl/common/command}).
 * Network contracts live in {@code io.github.tt432.eyelibparticle.network} and are validated via {@code ClientSmoke}.
 * Phase 14 owns final split verification with stable source tests and JetBrains MCP gates ({@code PFUT-03}).
 */
@NullMarked
package io.github.tt432.eyelib.client.particle;

import org.jspecify.annotations.NullMarked;
