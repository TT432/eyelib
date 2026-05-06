package io.github.tt432.clientsmokeannotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a client smoke test target.
 *
 * <p>Classes annotated with {@code @ClientSmoke} are discovered at mod construction time
 * via {@code ModFileScanData} bytecode-level scanning — <strong>no class loading occurs
 * during discovery</strong>. The framework reads annotation metadata from {@code .class}
 * file constant pools using ASM, then defers {@code Class.forName()} and instantiation
 * to the test execution phase (Phase 4), after the Minecraft world is fully loaded and
 * stabilized.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * @ClientSmoke(description = "Validates login flow renders correctly",
 *              priority = 0,
 *              modId = "examplemod")
 * public class LoginScreenTest {
 *     // Test implementation
 * }
 * }</pre>
 *
 * <p>Per D-06: {@link RetentionPolicy#CLASS} ensures the annotation is visible to ASM
 * scanners in {@code ModFileScanData} but {@link java.lang.reflect.AnnotatedElement#getAnnotation(Class)}
 * returns {@code null} at runtime — preventing accidental class initialization during
 * reflection-based discovery.</p>
 *
 * @see <a href="https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.7.16">JVM Spec §4.7.16 — RuntimeVisibleAnnotations vs RuntimeInvisibleAnnotations</a>
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ClientSmoke {

    /**
     * Human-readable description of what this test verifies.
     * Included in the JSON report for manual review.
     *
     * @return test description (default: empty string)
     */
    String description() default "";

    /**
     * Execution priority. Lower values execute first.
     * Tests with equal priority execute in discovery order
     * (determined by {@code ModFileScanData} scan order).
     *
     * @return priority value (default: 0)
     */
    int priority() default 0;

    /**
     * Optional mod ID namespace.
     * When set to a non-empty value, this test is associated with
     * a specific mod and may be skipped if that mod is not loaded.
     * When empty (default), the test is considered global and
     * always eligible for execution.
     *
     * @return mod ID or empty string (default: empty)
     */
    String modId() default "";
}
