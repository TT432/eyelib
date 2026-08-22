package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.client.entity.ClientEntityRuntimeData;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.molang.MolangScope;
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
    /** 已完成静态 scope 初始化（texture./geometry./material. 短名注入）的 scope 实例。 */
    @Nullable
    private MolangScope staticScopeInited;

    public void setClientEntity(@Nullable BrClientEntity clientEntity) {
        this.clientEntity = clientEntity;
        // clientEntity 更换后 texture./geometry./material. 短名集合变化，静态 scope 初始化需重做
        this.staticScopeInited = null;
        if (runtimeData.sync(clientEntity)) {
            clientEntityVersion++;
        }
    }

    /**
     * 静态 scope 初始化守卫：同一 scope 实例只初始化一次（texture./geometry./material. 短名不逐帧重建）。
     * scope 被重建（owner 变更）或 clientEntity 更换后返回 true 要求重新初始化。
     *
     * @return true = 本次调用需要执行静态初始化
     */
    public boolean consumeStaticScopeInit(MolangScope scope) {
        if (staticScopeInited == scope) {
            return false;
        }
        staticScopeInited = scope;
        return true;
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

    /** models 内容版本；RC 槽位的骨骼匹配/part_visibility 派生缓存按此失效。 */
    public int getModelVersion() {
        return runtimeData.modelVersion();
    }
}