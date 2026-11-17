package io.github.tt432.eyelib.capability.component;

import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import io.github.tt432.eyelib.model.Model;
import io.github.tt432.eyelib.molang.MolangScope;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * 客户端实体绑定状态与运行时投影。
 * <p>
 * 绑定语义二分（G5）：两种绑定对「注册表代际变化」的反应**必须**不同，且必须显式声明，
 * 不能靠「引用是否相同」推断意图：
 * <ul>
 *   <li>{@link BindingKind#AUTO_BY_ID}：按 id 自动解析，注册表每次变更（资源重载/addon 叠加）
 *       后重新查表；解析不到即回落原版渲染。适合「实体类型默认模型」。</li>
 *   <li>{@link BindingKind#INSTANCE}：固定实例，**永不**参与代际失效；资源重载后由持有者负责重设。
 *       适合「服务端下发的自定义模型/attachable（按物品解析出的实例）」。</li>
 * </ul>
 * 历史缺陷：Old {@code setClientEntity} 同时承担「原样设置」和「代际解析回写」，于是
 * {@code setupClientEntity(String, cap)} 会在 stale 时覆盖外部显式设置的实例
 * （「首次特效不渲染、第二次同 id 才生效」即此）。
 *
 * @author TT432
 */
public class ClientEntityComponent {

    /** 绑定来源。 */
    public enum BindingKind {
        /** 按 id 从 {@code ClientEntityManager} 解析，参与注册表代际失效。 */
        AUTO_BY_ID,
        /** 固定实例，不参与代际失效。 */
        INSTANCE
    }

    @Nullable
    private BrClientEntity clientEntity;
    private final ClientEntityRuntimeData runtimeData =
            new ClientEntityRuntimeData(id -> ClientAssetPort.get().model(id));
    private long clientEntityVersion;
    private long appliedClientEntityVersion;
    /** 绑定种类；默认按 id 自动解析。 */
    private BindingKind bindingKind = BindingKind.AUTO_BY_ID;
    /** 自动解析使用的 id；{@code null} 表示「用宿主类型 id」。 */
    private @Nullable String boundId;
    /** 自动解析最近一次解析时的注册表代际；-1 表示尚未解析（首次必然过期）。 */
    private long resolvedGeneration = -1;
    /** 已完成静态 scope 初始化（texture./geometry./material. 短名注入）的 scope 实例。 */
    @Nullable
    private MolangScope staticScopeInited;

    /**
     * 绑定到 id：注册表代际变化后按该 id 重新解析（幂等）。
     * <p>
     * 不清空当前实例——写新 id 与解析之间若出现一帧空档，实体会闪回原版渲染。
     */
    public void bindById(String entityId) {
        if (bindingKind == BindingKind.AUTO_BY_ID && entityId.equals(boundId)) {
            return;
        }
        this.bindingKind = BindingKind.AUTO_BY_ID;
        this.boundId = entityId;
        this.resolvedGeneration = -1;
    }

    /**
     * 绑定固定实例：永不参与代际失效；{@code null} 表示「此宿主显式无模型」（冻结，不随代际恢复）。
     * <p>
     * 同一引用幂等（不重建组件、不 bump 版本——逐帧重设的调用方依赖这一点）。
     */
    public void bindInstance(@Nullable BrClientEntity clientEntity) {
        this.bindingKind = BindingKind.INSTANCE;
        this.boundId = null;
        this.resolvedGeneration = -1;
        if (this.clientEntity != clientEntity) {
            setInstance(clientEntity);
        }
    }

    /**
     * 自动解析的回写（唯一写 {@link #resolvedGeneration} 的入口）。
     * 非 {@link BindingKind#AUTO_BY_ID} 绑定直接丢弃——否则显式实例会被未请求的解析结果覆盖。
     */
    public void applyResolved(@Nullable BrClientEntity resolved, long generation) {
        if (bindingKind != BindingKind.AUTO_BY_ID) {
            return;
        }
        this.resolvedGeneration = generation;
        if (this.clientEntity != resolved) {
            setInstance(resolved);
        }
    }

    /** 是否需要按 id 重新解析：仅自动解析绑定，且解析代际落后于注册表。 */
    public boolean needsRebind(long currentGeneration) {
        return bindingKind == BindingKind.AUTO_BY_ID && resolvedGeneration != currentGeneration;
    }

    public BindingKind bindingKind() {
        return bindingKind;
    }

    /** 自动解析使用的 id；固定实例绑定返回 {@code null}。 */
    public @Nullable String boundId() {
        return boundId;
    }

    /** 内部实例写入（版本/静态 scope 失效）。 */
    private void setInstance(@Nullable BrClientEntity clientEntity) {
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

    public Collection<Model> getModels() {
        return runtimeData.models();
    }

    /** models 内容版本；RC 槽位的骨骼匹配/part_visibility 派生缓存按此失效。 */
    public int getModelVersion() {
        return runtimeData.modelVersion();
    }
}
