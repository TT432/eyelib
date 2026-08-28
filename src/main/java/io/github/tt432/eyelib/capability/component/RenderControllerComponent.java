package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.client.entity.RenderControllerRuntime;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.util.PortResourceLocation;
import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * @author TT432
 */
public class RenderControllerComponent {
    private static final AtomicInteger TEXTURE_STATE_VERSION = new AtomicInteger();
    /** setupModel 分组/基础可见性缓存开关（benchmark A/B 对照用）。 */
    private static final boolean GROUP_CACHE = Boolean.parseBoolean(System.getProperty("eyelib.rc.groupCache", "true"));

    public static void onTextureStateChanged() {
        TEXTURE_STATE_VERSION.incrementAndGet();
    }

    private final List<Slot> slots = new ArrayList<>();
    /** 最近一次 setup 时各 RC 条件的启用位掩码（bit i = render_controllers[i] 启用）。 */
    private int conditionMask = -1;
    /** 最近一次执行完整 setupClientEntity 的帧号；同帧内 TickStage 不再重复做条件翻转检测。 */
    private long setupFrameStamp = -1;

    public long setupFrameStamp() {
        return setupFrameStamp;
    }

    public void markSetupFrame(long setupFrameStamp) {
        this.setupFrameStamp = setupFrameStamp;
    }

    public int conditionMask() {
        return conditionMask;
    }

    public void setConditionMask(int conditionMask) {
        this.conditionMask = conditionMask;
    }

    public Slot syncSlot(int index, @Nullable RenderControllerEntry renderController) {
        while (slots.size() <= index) {
            slots.add(new Slot(null));
        }

        Slot slot = slots.get(index);
        if (slot.renderController != renderController) {
            slot = new Slot(renderController);
            slots.set(index, slot);
        }

        return slot;
    }

    public void trim(int size) {
        while (slots.size() > size) {
            slots.remove(slots.size() - 1);
        }
    }

    public void clear() {
        slots.clear();
    }

    public static final class Slot {
        @Nullable
        private final RenderControllerEntry renderController;
        private final RenderControllerRuntime runtime = new RenderControllerRuntime();
        private int textureStateVersion;

        /** 以下缓存随 models 内容版本失效（modelVersion 变化 = 骨骼集合可能变化）。 */
        private int cachedModelVersion = -1;
        private final Map<String, Set<Integer>> boneMatchCache = new HashMap<>();
        @Nullable
        private Set<Integer> cachedAllBoneIds;
        private final Map<String, Optional<PortResourceLocation>> meshTextureCache = new HashMap<>();
        /** setupModel 派生缓存键：几何解析值 + 逐材质解析值（与 renderController.materials 顺序对齐）+ rcColor。 */
        private String cachedGeometryKey;
        private final List<String> cachedMaterialKey = new ArrayList<>();
        private float @Nullable [] cachedColorKey;
        /** 派生值：材质名 → 骨骼集（键序 = 首现顺序，与重建逻辑一致）。 */
        private @Nullable LinkedHashMap<String, Set<Integer>> cachedGroups;
        /** 派生值：每组的基础可见性表（与 cachedGroups 值序对齐），逐帧 clone 后再叠加 part_visibility 表达式。 */
        private final List<Int2BooleanOpenHashMap> cachedBaseVis = new ArrayList<>();
        // ---- Opt18-A：组件级值键缓存 ----
        /** 组件缓存开关（benchmark A/B 对照用）。 */
        private static final boolean COMPONENT_CACHE =
                Boolean.parseBoolean(System.getProperty("eyelib.rc.componentCache", "true"));
        /**
         * setupModel 全产物值键缓存：键 = 本帧求值出的全部动态输入
         * （geometry 解析值 + 逐材质解析值 + 逐组纹理路径 + rcColor + part_visibility 隐藏位集），
         * 值 = 组件列表。molang 表达式逐帧求值不变（BE 动态语义保留——A&S 史莱姆 RC 全部表达式
         * 均读 variable./query.，编译期常量门在真实资产上永不命中，故用值键而非静态门）；
         * 值相等（准静态资产逐帧命中）时消除下游全部重建：PortResourceLocation.parse、
         * clamped 纹理路径、ModelComponent/ModelComponentInfo 分配、vis putAll 与叠加。
         * 失效通道：RC 引用变化（syncSlot 换新 Slot）、clientEntity 变化（clear）、
         * modelVersion 变化（checkModelVersion）、纹理状态版本（needsTextureReload 时绕行重建）。
         */
        private @Nullable String compKeyGeometry;
        private final List<String> compKeyMaterials = new ArrayList<>();
        private final List<List<String>> compKeyTextures = new ArrayList<>();
        private float @Nullable [] compKeyColor;
        private long @Nullable [] compKeyPvHidden;
        private @Nullable List<ModelComponent> cachedComponents;

        /** 值键全等且缓存可用时返回跨帧复用的组件列表（同一实例序）；否则 null。 */
        public @Nullable List<ModelComponent> cachedComponentsHit(int modelVersion, String geometry,
                List<String> materialValues, List<List<String>> texturePathsByGroup,
                float @Nullable [] color, long @Nullable [] pvHidden) {
            checkModelVersion(modelVersion);
            if (!COMPONENT_CACHE || cachedComponents == null || needsTextureReload()) {
                return null;
            }
            if (geometry.equals(compKeyGeometry)
                    && materialValues.equals(compKeyMaterials)
                    && texturePathsByGroup.equals(compKeyTextures)
                    && Arrays.equals(color, compKeyColor)
                    && Arrays.equals(pvHidden, compKeyPvHidden)) {
                return cachedComponents;
            }
            return null;
        }

        /** 重建路径末尾回填：拷贝键分量，持有组件列表。 */
        public void storeCachedComponents(String geometry, List<String> materialValues,
                List<List<String>> texturePathsByGroup, float @Nullable [] color,
                long @Nullable [] pvHidden, List<ModelComponent> components) {
            compKeyGeometry = geometry;
            compKeyMaterials.clear();
            compKeyMaterials.addAll(materialValues);
            compKeyTextures.clear();
            for (List<String> paths : texturePathsByGroup) {
                compKeyTextures.add(List.copyOf(paths));
            }
            compKeyColor = color == null ? null : color.clone();
            compKeyPvHidden = pvHidden == null ? null : pvHidden.clone();
            cachedComponents = components;
        }

        private Slot(@Nullable RenderControllerEntry renderController) {
            this.renderController = renderController;
            this.textureStateVersion = renderController == null
                    ? TEXTURE_STATE_VERSION.get()
                    : TEXTURE_STATE_VERSION.get() - 1;
        }

        @Nullable
        public RenderControllerEntry renderController() {
            return renderController;
        }

        public RenderControllerRuntime runtime() {
            return runtime;
        }

        public boolean needsTextureReload() {
            return textureStateVersion != TEXTURE_STATE_VERSION.get();
        }

        public void markTextureUploaded() {
            textureStateVersion = TEXTURE_STATE_VERSION.get();
        }

        private void checkModelVersion(int modelVersion) {
            if (modelVersion != cachedModelVersion) {
                boneMatchCache.clear();
                meshTextureCache.clear();
                cachedAllBoneIds = null;
                cachedGroups = null;
                cachedBaseVis.clear();
                cachedComponents = null;
                compKeyGeometry = null;
                compKeyColor = null;
                compKeyPvHidden = null;
                cachedModelVersion = modelVersion;
            }
        }

        /** 按骨骼名模式匹配全部模型的骨骼；同一 modelVersion 内按 pattern 缓存（骨骼集合与名称静态）。 */
        public Set<Integer> matchBones(String pattern, Collection<Model> models, int modelVersion) {
            checkModelVersion(modelVersion);
            return boneMatchCache.computeIfAbsent(pattern, p -> RenderControllerEntry.matchBonePattern(p, models));
        }

        /** 全部模型的非负骨骼 id 集合；同一 modelVersion 内只计算一次。 */
        public Set<Integer> allBoneIds(Collection<Model> models, int modelVersion) {
            checkModelVersion(modelVersion);
            Set<Integer> result = cachedAllBoneIds;
            if (result == null) {
                result = new HashSet<>();
                for (Model model : models) {
                    if (model != null) {
                        for (int id : model.allBones().keySet()) {
                            if (id >= 0) {
                                result.add(id);
                            }
                        }
                    }
                }
                cachedAllBoneIds = result;
            }
            return result;
        }

        /** texture_meshes 体素化贴图解析缓存；同一 modelVersion 内按几何名缓存。 */
        public Optional<PortResourceLocation> meshTexture(String geometryName, int modelVersion,
                                                          Supplier<Optional<PortResourceLocation>> resolver) {
            checkModelVersion(modelVersion);
            return meshTextureCache.computeIfAbsent(geometryName, g -> resolver.get());
        }
        /**
         * setupModel 分组缓存：键 = (geometry, 逐材质解析值, rcColor) 值相等比较。
         * molang 表达式逐帧求值不变（动态性保留），仅消除值不变时的 map 重建。
         * 命中返回缓存分组；未命中由 rebuild 重建并连带重建 baseVis。
         */
        public @Nullable LinkedHashMap<String, Set<Integer>> materialGroups(
                String geometry, List<String> materialValues, float @Nullable [] color,
                Supplier<LinkedHashMap<String, Set<Integer>>> rebuild) {
            LinkedHashMap<String, Set<Integer>> groups = cachedGroups;
            if (GROUP_CACHE
                    && groups != null
                    && geometry.equals(cachedGeometryKey)
                    && materialValues.equals(cachedMaterialKey)
                    && Arrays.equals(color, cachedColorKey)) {
                return groups;
            }
            groups = rebuild.get();
            cachedGeometryKey = geometry;
            cachedMaterialKey.clear();
            cachedMaterialKey.addAll(materialValues);
            cachedColorKey = color == null ? null : color.clone();
            cachedGroups = groups;
            return groups;
        }

        /** 重建路径回填 baseVis（与 groups 值序对齐）；调用方保证仅在 rebuild 后调用。 */
        public void storeBaseVis(List<Int2BooleanOpenHashMap> baseVis) {
            cachedBaseVis.clear();
            cachedBaseVis.addAll(baseVis);
        }

        /** 与当前 cachedGroups 值序对齐的基础可见性表；调用方须先经 materialGroups 确保一致。 */
        public List<Int2BooleanOpenHashMap> baseVis() {
            return cachedBaseVis;
        }
    }
}