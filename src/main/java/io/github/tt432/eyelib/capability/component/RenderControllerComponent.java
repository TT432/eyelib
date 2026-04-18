package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.client.entity.RenderControllerRuntime;
import io.github.tt432.eyelib.client.render.controller.RenderControllerEntry;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author TT432
 */
public class RenderControllerComponent {
    private static final AtomicInteger TEXTURE_STATE_VERSION = new AtomicInteger();

    public static void onTextureStateChanged() {
        TEXTURE_STATE_VERSION.incrementAndGet();
    }

    private final List<Slot> slots = new ArrayList<>();

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
    }
}

