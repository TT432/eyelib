/**
 * Particle module API and core contract boundary for Eyelib.
 * <p>
 * The root runtime may consume this module, but this package must not depend back on root runtime
 * packages, root managers, root registries, root packets, root capability helpers, or
 * {@code io.github.tt432.eyelib.mc.impl} classes. Minecraft/Forge lifecycle wiring and other
 * platform bindings require explicit adapter documentation before introduction.
 */
@NullMarked
package io.github.tt432.eyelibparticle;

import org.jspecify.annotations.NullMarked;
