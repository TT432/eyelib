package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelibmodel.GlobalBoneIdHandler;
import io.github.tt432.eyelibmodel.Model;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * @author TT432
 */
@NullMarked
public class RenderControllerRuntime {
    @Nullable
    private Int2ObjectMap<ReferenceList<MolangValue>> partVisibility;

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

    public void setup(Collection<Model> models, RenderControllerEntry renderController) {
        Int2ObjectOpenHashMap<ReferenceList<MolangValue>> part = new Int2ObjectOpenHashMap<>();
        partVisibility = part;
        models.stream().filter(java.util.Objects::nonNull).forEach(model -> {
            model.allBones().int2ObjectEntrySet().forEach(entry -> {
                String boneName = GlobalBoneIdHandler.get(entry.getIntKey());
                renderController.part_visibility().forEach((k, v) -> {
                    if (boneName != null && Pattern.compile(k.replace("*", ".*")).matcher(boneName).matches()) {
                        part.computeIfAbsent(entry.getIntKey(), __ -> new ReferenceArrayList<>()).add(v);
                    }
                });
            });
        });
    }
}
