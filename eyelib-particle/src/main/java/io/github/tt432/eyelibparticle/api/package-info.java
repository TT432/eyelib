/**
 * Root-consumed particle API contracts for lookup, store, publication, lifecycle, and spawn seams.
 * <p>
 * The root runtime may consume this package, but this package must not depend back on root runtime
 * packages, root managers, root registries, root packets, root capability helpers, Minecraft,
 * Forge, or root platform implementation classes. Platform-specific validation and
 * lifecycle wiring belong in explicitly documented adapters outside this pure API boundary.
 */
@NullMarked
package io.github.tt432.eyelibparticle.api;

import org.jspecify.annotations.NullMarked;
