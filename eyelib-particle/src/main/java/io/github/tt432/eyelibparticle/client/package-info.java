/**
 * This package is the explicit client integration layer for particle render managers, render adapters, and Forge hooks.
 * <p>
 * Classes in this package may bind Minecraft and Forge client-only types behind {@code Dist.CLIENT}
 * side-safe entrypoints. Pure particle {@code runtime/** remains root/MC/Forge-clean} and must not
 * import these client integration classes.
 */
package io.github.tt432.eyelibparticle.client;
