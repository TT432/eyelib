package io.github.tt432.eyelib.api.bedrock.animation;

import io.github.tt432.eyelib.api.bedrock.AnimatableModel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Function;

/**
 * @author DustW
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ModelFetcherManager {
    private static final List<ModelFetcher<?>> modelFetchers = new ObjectArrayList<>();

    public static void addModelFetcher(ModelFetcher<?> fetcher) {
        modelFetchers.add(fetcher);
    }

    public static void removeModelFetcher(ModelFetcher<?> fetcher) {
        if (fetcher == null)
            return;

        modelFetchers.remove(fetcher);
    }

    public static <T extends Animatable> AnimatableModel<T> getModel(T animatable) {
        for (ModelFetcher<?> modelFetcher : modelFetchers) {
            AnimatableModel<T> model = (AnimatableModel<T>) modelFetcher.apply(animatable);

            if (model != null)
                return model;
        }

        log.error("Could not find suitable model for animatable of type {}. Did you register a Model Fetcher?%n",
                animatable.getClass());

        return null;
    }

    @FunctionalInterface
    public interface ModelFetcher<T extends Animatable> extends Function<Animatable, AnimatableModel<T>> {
    }
}
