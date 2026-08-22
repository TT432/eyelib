package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.client.entity.RenderControllerRuntime;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.util.PortResourceLocation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
    }
}