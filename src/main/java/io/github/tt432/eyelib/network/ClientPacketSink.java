package io.github.tt432.eyelib.network;

import io.github.tt432.eyelib.bridge.network.animation.AnimationComponentSyncPacket;
import io.github.tt432.eyelib.bridge.network.model.ModelComponentSyncPacket;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 客户端渲染同步的落地 Port：network 只负责收包分发，具体应用到渲染状态由 client 实现。
 *
 * <p>存在的理由：network 包声明“不持版本特定 MC API”的版本无关分发层，直接 import
 * {@code client.render.sync.ClientRenderSyncService} 会让分发层反向绑定渲染实现。
 *
 * @author TT432
 */
public interface ClientPacketSink {
    AtomicReference<@Nullable ClientPacketSink> HOLDER = new AtomicReference<>();

    void applyModelSync(ModelComponentSyncPacket packet);

    void applyAnimationSync(AnimationComponentSyncPacket packet);

    static void install(ClientPacketSink sink) {
        HOLDER.set(sink);
    }

    static ClientPacketSink get() {
        ClientPacketSink sink = HOLDER.get();
        if (sink == null) {
            throw new IllegalStateException("ClientPacketSink not installed");
        }
        return sink;
    }
}
