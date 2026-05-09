/**
 * Module-owned pure runtime definition contracts, extraction ports, support helpers, and schema adapter seams.
 * <p>
 * Importer {@code io.github.tt432.eyelibimporter.particle.BrParticle} is the canonical raw Bedrock
 * particle schema and codec owner. Particle {@link io.github.tt432.eyelibparticle.runtime.ParticleDefinition}
 * is the canonical module runtime definition owner for data that later runtime/loading phases consume.
 * The root {@code io.github.tt432.eyelib.client.particle.bedrock.BrParticle} type is legacy and
 * non-canonical until Phase 11/12 migrate executable runtime and loading behavior through explicit seams.
 * Root {@code src/main/java/io/github/tt432/eyelib/client/particle/bedrock/BrParticle.java} is a
 * legacy/non-canonical runtime adapter target, not the canonical raw schema.
 * <p>
 * The allowed particle -> importer dependency for ParticleDefinitionAdapter preserves mapped fields: identifier, format version, basic render material/texture, curves, events, raw components, billboard flipbook summary, and Molang value preservation.
 * Phase 11 moves executable runtime core, Phase 12 rewires loading/publication, and Phase 13 rewires
 * command/network integration; Phase 10 does not move those behaviors.
 * <p>
 * This package owns pure runtime state only. Minecraft and Forge-facing client integration belongs in an
 * explicitly documented particle client integration package outside {@code runtime/**}, where side-safe
 * adapters can translate platform objects into these platform-free contracts.
 * <p>
 * This package may depend on importer schema data for the adapter boundary, but it must not depend back
 * on root runtime packages, root managers, root registries, root packets, root capability helpers,
 * Minecraft, Forge, or root platform implementation classes.
 */
@NullMarked
package io.github.tt432.eyelibparticle.runtime;

import org.jspecify.annotations.NullMarked;
