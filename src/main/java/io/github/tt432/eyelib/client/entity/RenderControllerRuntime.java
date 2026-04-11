package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelibimporter.model.GlobalBoneIdHandler;
import io.github.tt432.eyelibimporter.model.Model;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelibmolang.MolangScope;
import io.github.tt432.eyelibmolang.MolangValue;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * @author TT432
 */
public class RenderControllerRuntime {
    @Nullable
    private Int2ObjectMap<ReferenceList<MolangValue>> partVisibility;

    public void evalPartVisibility(Collection<Model> models, RenderControllerEntry renderControllerEntry,
                                   Int2BooleanOpenHashMap partVisibility, MolangScope scope) {
        setup(models, renderControllerEntry);
        Int2ObjectMap<ReferenceList<MolangValue>> referenceListInt2ObjectMap = this.partVisibility;
        if (referenceListInt2ObjectMap != null) {
            referenceListInt2ObjectMap.int2ObjectEntrySet().forEach(e -> {
                for (MolangValue molangValue : e.getValue()) {
                    if (!molangValue.evalAsBool(scope)) {
                        partVisibility.put(e.getIntKey(), false);
                        break;
                    }
                }
            });
        }
    }

    public void setup(Collection<Model> models, RenderControllerEntry renderController) {
        if (partVisibility == null) {
            Int2ObjectOpenHashMap<ReferenceList<MolangValue>> part = new Int2ObjectOpenHashMap<>();
            partVisibility = part;
            models.stream().filter(java.util.Objects::nonNull).forEach(model -> {
                model.allBones().int2ObjectEntrySet().forEach(entry -> {
                    renderController.part_visibility().forEach((k, v) -> {
                        if (Pattern.compile(k.replace("*", ".*"))
                                .matcher(GlobalBoneIdHandler.get(entry.getIntKey())).matches()) {
                            part.computeIfAbsent(entry.getIntKey(), __ -> new ReferenceArrayList<>()).add(v);
                        }
                    });
                });

            });
        }
    }
}
