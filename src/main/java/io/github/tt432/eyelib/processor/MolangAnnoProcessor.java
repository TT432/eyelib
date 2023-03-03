package io.github.tt432.eyelib.processor;

import io.github.tt432.eyelib.molang.MolangParser;
import io.github.tt432.eyelib.molang.functions.MolangFunction;
import io.github.tt432.eyelib.processor.anno.AnnoProcessorHolder;
import io.github.tt432.eyelib.processor.anno.MolangFunctionHolder;

/**
 * @author DustW
 */
@AnnoProcessorHolder
public class MolangAnnoProcessor implements AnnoProcessor {
    @Override
    public void process() {
        MolangParser parser = MolangParser.getInstance();
        EyelibProcessors.getClasses(MolangFunctionHolder.class, MolangFunction.class).forEach((data, clazz) ->
                parser.register(data.annotationData().get("value").toString(), clazz));
        parser.loadConstructors();
    }
}
