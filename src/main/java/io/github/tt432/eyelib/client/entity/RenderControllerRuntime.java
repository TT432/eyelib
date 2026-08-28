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

    /**
     * 逐帧求值 part_visibility 并产出「隐藏骨骼位集」（Opt18-A 值键分量）。
     * 每骨骼语义与 {@link #evalPartVisibility} 一致：引用列表最后一个表达式为假即隐藏。
     * 表达式逐帧求值不变（BE 动态语义保留）；无副作用表达式的求值次数从「每组×层」收敛为 1 次，
     * 结果等价。位集同时充当组件缓存的键分量与重建路径的叠加输入。
     * 负骨骼 id（GlobalBoneIdHandler 对空白名的 -1 兜底）无法入位集，直接忽略（生产不出现）。
     */
    public long @Nullable [] evalPartVisibilityBits(MolangScope scope) {
        Int2ObjectMap<ReferenceList<MolangValue>> map = this.partVisibility;
        if (map == null || map.isEmpty()) {
            return null;
        }
        int max = -1;
        for (int id : map.keySet()) {
            if (id > max) max = id;
        }
        if (max < 0) {
            return null;
        }
        long[] bits = new long[(max >> 6) + 1];
        for (var e : map.int2ObjectEntrySet()) {
            int id = e.getIntKey();
            if (id < 0) continue;
            boolean lastVisible = true;
            for (MolangValue molangValue : e.getValue()) {
                lastVisible = molangValue.evalAsBool(scope);
            }
            if (!lastVisible) {
                bits[id >> 6] |= 1L << (id & 63);
            }
        }
        return bits;
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
