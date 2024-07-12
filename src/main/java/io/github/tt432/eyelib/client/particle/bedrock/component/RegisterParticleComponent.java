package io.github.tt432.eyelib.client.particle.bedrock.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author TT432
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterParticleComponent {
    String value();

    String type() default "";

    ComponentTarget target();
}
