package io.github.tt432.eyelib.processor;

import io.github.tt432.eyelib.processor.anno.AnnoProcessorHolder;
import io.github.tt432.eyelib.util.molang.MolangParser;
import io.github.tt432.eyelib.util.molang.math.functions.MolangFunction;

/**
 * @author DustW
 */
@AnnoProcessorHolder
public class MolangAnnoProcessor implements AnnoProcessor {
    @Override
    public void process() {
        EyelibProcessors.getClasses(io.github.tt432.eyelib.processor.anno.MolangFunction.class, MolangFunction.class).forEach((data, clazz) ->
                MolangParser.getInstance().register(data.annotationData().get("value").toString(), clazz));
    }
}
