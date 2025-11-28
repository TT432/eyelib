package io.github.tt432.eyelib.client.entity;

import io.github.tt432.eyelib.client.model.GlobalBoneIdHandler;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.MolangValue;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;

import java.util.regex.Pattern;

/**
 * @author TT432
 */
public class RenderControllerRuntime {
    private final ReferenceSet<BrClientEntity> init = new ReferenceOpenHashSet<>();

    private final Reference2ObjectMap<BrClientEntity, Int2ObjectMap<ReferenceList<MolangValue>>> partVisibility =
            new Reference2ObjectOpenHashMap<>();

    public void evalPartVisibility(BrClientEntity clientEntity, RenderControllerEntry renderControllerEntry,
                                   Int2BooleanOpenHashMap partVisibility, MolangScope scope) {
        setup(clientEntity, renderControllerEntry);
        Int2ObjectMap<ReferenceList<MolangValue>> referenceListInt2ObjectMap = this.partVisibility.get(clientEntity);
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

    public void setup(BrClientEntity clientEntity, RenderControllerEntry renderController) {
        if (!init.contains(clientEntity)) {
            init.add(clientEntity);
            Int2ObjectOpenHashMap<ReferenceList<MolangValue>> part = new Int2ObjectOpenHashMap<>();
            partVisibility.put(clientEntity, part);

            ClientEntityRuntimeData data = clientEntity.clientEntityRuntimeData();
            data.setup(clientEntity);
            data.models.values().forEach(model -> {
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
