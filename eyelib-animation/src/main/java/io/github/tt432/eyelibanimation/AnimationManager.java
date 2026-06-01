package io.github.tt432.eyelibanimation;

import io.github.tt432.eyelibutil.repository.Repository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author TT432
 */
@NullMarked
public final class AnimationManager implements Repository<Animation> {
    private final Map<String, Animation> store = new LinkedHashMap<>();
    public static final AnimationManager INSTANCE = new AnimationManager();

    @Override
    public void put(String name, Animation value) { store.put(name, value); }

    @Override
    @Nullable
    public Animation get(String name) { return store.get(name); }

    @Override
    public Map<String, Animation> all() { return store; }

    public Map<String, Animation> getAllData() { return all(); }

    @Override
    public void replaceAll(Map<String, ? extends Animation> replacement) { store.clear(); store.putAll(replacement); }

    @Override
    public void clear() { store.clear(); }
}
