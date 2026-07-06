package io.github.tt432.eyelib.model;

import io.github.tt432.eyelib.model.locator.ModelLocator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

/**
 * {@link Model} 的默认实现：构造时根据 allBones 计算派生索引（toplevelBones、Bone.children、locator 索引）。
 *
 * @author TT432
 */
final class SimpleModel implements Model {
    private final String name;
    private final Int2ObjectMap<Bone> toplevelBones;
    private final Int2ObjectMap<Bone> allBones;
    private final ModelLocator locator;
    private final VisibleBox visibleBox;

    SimpleModel(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator, VisibleBox visibleBox) {
        this.name = name;
        this.allBones = allBones;
        this.locator = locator;
        this.visibleBox = visibleBox;
        this.toplevelBones = new Int2ObjectOpenHashMap<>();

        IndexInitializer.fillIndices(this.allBones, this.toplevelBones, this.locator);
    }

    static SimpleModel of(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator, VisibleBox visibleBox) {
        return new SimpleModel(name, allBones, locator, visibleBox);
    }

    static SimpleModel of(String name, Int2ObjectMap<Bone> allBones, ModelLocator locator) {
        return new SimpleModel(name, allBones, locator, EMPTY_VISIBLE_BOX);
    }

    static SimpleModel of(String name, Int2ObjectMap<Bone> allBones, VisibleBox visibleBox) {
        return new SimpleModel(name, allBones, new ModelLocator(new Int2ObjectOpenHashMap<>()), visibleBox);
    }

    static SimpleModel of(String name, Int2ObjectMap<Bone> allBones) {
        return of(name, allBones, EMPTY_VISIBLE_BOX);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Int2ObjectMap<Bone> toplevelBones() {
        return toplevelBones;
    }

    @Override
    public Int2ObjectMap<Bone> allBones() {
        return allBones;
    }

    @Override
    public ModelLocator locator() {
        return locator;
    }

    @Override
    public VisibleBox visibleBox() {
        return visibleBox;
    }
}
