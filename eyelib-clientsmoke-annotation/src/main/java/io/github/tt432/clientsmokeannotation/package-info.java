/**
 * Client Smoke Test annotation module.
 *
 * <p>This module contains ONLY the {@link io.github.tt432.clientsmokeannotation.ClientSmoke}
 * annotation definition. It has zero Minecraft, Forge, or NeoForge dependencies — it is a
 * plain JVM library that produces a minimal JAR suitable for {@code compileOnly} consumption
 * by both the test framework and target mods under test.</p>
 *
 * <p>The annotation uses {@link java.lang.annotation.RetentionPolicy#CLASS} retention so
 * that Forge's {@code ModFileScanData} can discover annotated classes via ASM bytecode
 * scanning without triggering JVM class initialization.</p>
 */
@org.jspecify.annotations.NullMarked
package io.github.tt432.clientsmokeannotation;
