package io.github.tt432.eyelibanimation;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author TT432
 */
@NullMarked
public final class AnimationManager {
    private final Map<String, Animation> store = new LinkedHashMap<>();
    public static final AnimationManager INSTANCE = new AnimationManager();
    public void put(String name, Animation value) { store.put(name, value); }
    @Nullable public Animation get(String name) { return store.get(name); }
    public Map<String, Animation> getAllData() { return store; }
    public void replaceAll(Map<String, ? extends Animation> replacement) { store.clear(); store.putAll(replacement); }
    public void clear() { store.clear(); }
}