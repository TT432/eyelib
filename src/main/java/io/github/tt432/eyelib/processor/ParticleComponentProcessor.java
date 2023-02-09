package io.github.tt432.eyelib.processor;

import io.github.tt432.eyelib.common.bedrock.particle.component.ParticleComponent;
import io.github.tt432.eyelib.processor.anno.AnnoProcessorHolder;
import io.github.tt432.eyelib.processor.anno.ParticleComponentHolder;

/**
 * @author DustW
 */
@AnnoProcessorHolder
public class ParticleComponentProcessor implements AnnoProcessor {
    @Override
    public void process() {
        EyelibProcessors.getClasses(ParticleComponentHolder.class, ParticleComponent.class)
                .forEach((data, clazz) -> {
                    String componentName = data.annotationData().get("value").toString();
                    Class<? extends ParticleComponent> old = ParticleComponent.getForName().put(componentName, clazz);

                    if (old != null) {
                        throw new IllegalArgumentException("component already registered: " + componentName);
                    }
                });
    }
}
