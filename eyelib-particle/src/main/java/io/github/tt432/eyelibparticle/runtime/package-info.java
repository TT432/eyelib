/**
 * Module-owned particle runtime definition contracts and schema adapter seams.
 * <p>
 * Importer {@code io.github.tt432.eyelibimporter.particle.BrParticle} is the canonical raw Bedrock
 * particle schema and codec owner. Particle {@link io.github.tt432.eyelibparticle.runtime.ParticleDefinition}
 * is the canonical module runtime definition owner for data that later runtime/loading phases consume.
 * The root {@code io.github.tt432.eyelib.client.particle.bedrock.BrParticle} type is legacy and
 * non-canonical until Phase 11/12 migrate executable runtime and loading behavior through explicit seams.
 * <p>
 * This package may depend on importer schema data for the adapter boundary, but it must not depend back
 * on root runtime packages, root managers, root registries, root packets, root capability helpers,
 * Minecraft, Forge, or root platform implementation classes.
 */
@NullMarked
package io.github.tt432.eyelibparticle.runtime;

import org.jspecify.annotations.NullMarked;
