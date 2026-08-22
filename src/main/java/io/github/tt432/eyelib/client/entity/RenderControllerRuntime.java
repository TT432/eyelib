package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * @author TT432
 */
public class RenderControllerRuntime {
    @Nullable
    private Int2ObjectMap<ReferenceList<MolangValue>> partVisibility;
    /** partVisibility 预计算的来源版本：models 内容与 RC 实例不变时跳过重算（骨骼集合与 pattern 均静态）。 */
    private int cachedModelVersion = -1;
    @Nullable
    private RenderControllerEntry cachedRenderController;

    /**
     * 应用预计算的part_visibility条件到指定可见性映射。
     * 需先调用{@link #setup(Collection, RenderControllerEntry)}。
     */
    public void evalPartVisibility(Int2BooleanOpenHashMap partVisibility, MolangScope scope) {
        Int2ObjectMap<ReferenceList<MolangValue>> referenceListInt2ObjectMap = this.partVisibility;
        if (referenceListInt2ObjectMap != null && !referenceListInt2ObjectMap.isEmpty()) {
            referenceListInt2ObjectMap.int2ObjectEntrySet().forEach(e -> {
                boolean lastVisible = true;
                for (MolangValue molangValue : e.getValue()) {
                    lastVisible = molangValue.evalAsBool(scope);
                }
                if (!lastVisible) {
                    partVisibility.put(e.getIntKey(), false);
                }
            });
        }
    }

    public void setup(int modelVersion, Collection<Model> models, RenderControllerEntry renderController) {
        if (cachedModelVersion == modelVersion && cachedRenderController == renderController && partVisibility != null) {
            return;
        }
        cachedModelVersion = modelVersion;
        cachedRenderController = renderController;

        Int2ObjectOpenHashMap<ReferenceList<MolangValue>> part = new Int2ObjectOpenHashMap<>();
        partVisibility = part;
        models.stream().filter(java.util.Objects::nonNull).forEach(model -> {
            model.allBones().int2ObjectEntrySet().forEach(entry -> {
                String boneName = GlobalBoneIdHandler.get(entry.getIntKey());
                renderController.part_visibility().forEach((k, v) -> {
                    if (boneName != null && RenderControllerEntry.matchesBonePattern(k, boneName)) {
                        part.computeIfAbsent(entry.getIntKey(), __ -> new ReferenceArrayList<>()).add(v);
                    }
                });
            });
        });
    }
}
