package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.client.entity.ClientEntityRuntimeData;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.model.Model;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * @author TT432
 */
public class ClientEntityComponent {
    @Nullable
    private BrClientEntity clientEntity;
    private final ClientEntityRuntimeData runtimeData = new ClientEntityRuntimeData();
    private long clientEntityVersion;
    private long appliedClientEntityVersion;
    /** clientEntity 解析来源（ClientEntityManager）的代际；初始 -1 保证首次解析视为过期。 */
    private long resolvedGeneration = -1;

    public void setClientEntity(@Nullable BrClientEntity clientEntity) {
        this.clientEntity = clientEntity;
        if (runtimeData.sync(clientEntity)) {
            clientEntityVersion++;
        }
    }

    public boolean consumeChanged() {
        if (appliedClientEntityVersion == clientEntityVersion) {
            return false;
        }

        appliedClientEntityVersion = clientEntityVersion;
        return true;
    }

    @Nullable
    public BrClientEntity getClientEntity() {
        return clientEntity;
    }

    /**
     * 记录本次解析时来源注册表的代际（仅按 id 从注册表解析的路径调用；
     * 外部直接 {@link #setClientEntity} 设置的值不记录，不受代际失效影响）。
     */
    public void markResolvedFrom(long generation) {
        this.resolvedGeneration = generation;
    }

    /** 来源注册表代际与解析时不同 = 缓存的 clientEntity 已过期，需重新解析（含解析结果为 null 的情况）。 */
    public boolean isStale(long currentGeneration) {
        return resolvedGeneration != currentGeneration;
    }

    public Collection<Model> getModels() {
        return runtimeData.models();
    }
}