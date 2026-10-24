package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.material.material.BrMaterialEntry;
import io.github.tt432.eyelib.model.Model;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 客户端资产查询 Port：capability 侧需要按标识取模型、取当前材质表，
 * 但注册表在 application 的 {@code client.manager} 下。
 *
 * <p>直接 import 会让 capability ↔ client 形成包级循环；实现由 client 在启动期安装
 * （{@code ClientBootstrap}），capability 只持有接口。
 *
 * @author TT432
 */
public interface ClientAssetPort {
    AtomicReference<@Nullable ClientAssetPort> HOLDER = new AtomicReference<>();

    @Nullable
    Model model(String id);

    Map<String, BrMaterialEntry> materials();

    static void install(ClientAssetPort port) {
        HOLDER.set(port);
    }

    static ClientAssetPort get() {
        ClientAssetPort port = HOLDER.get();
        if (port == null) {
            throw new IllegalStateException("ClientAssetPort not installed");
        }
        return port;
    }
}
