package io.github.tt432.eyelib.client.jsonview;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import io.github.tt432.eyelib.behavior.BehaviorEntity;
import io.github.tt432.eyelib.behavior.BehaviorEntityRegistry;
import io.github.tt432.eyelib.client.manager.ClientEntityManager;
import io.github.tt432.eyelib.importer.entity.BrClientEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Entity/ClientEntity JSON 查看服务：从运行时注册表读取实体定义，
 * 并经 DFU Codec 编码回带缩进的 JSON 文本供查看器展示。
 *
 * <p>纯 client 服务：只依赖 domain 注册表与 DFU/Gson 序列化基础设施，不 import MC 类。
 *
 * @author TT432
 */
public final class EntityJsonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityJsonService.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 已注册的客户端实体（client_entity）id，字母序。
     */
    public List<String> clientEntityIds() {
        return sortedIds(ClientEntityManager.INSTANCE.all());
    }

    /**
     * 已注册的行为实体（behavior entity）id，字母序。
     *
     * <p>读权威注册表 {@link BehaviorEntityRegistry}（common 侧）而非 client 镜像
     * BehaviorEntityManager——镜像只在资源重载时同步，渲染链懒加载 vanilla 行为包后
     * 镜像可能仍为空；权威表在渲染链 ensureVanillaLoaded 后即可用。
     */
    public List<String> behaviorEntityIds() {
        return sortedIds(BehaviorEntityRegistry.all());
    }

    /**
     * 将指定客户端实体定义经 {@link BrClientEntity#CODEC} 编码为格式化 JSON。
     * id 不存在或编码失败时返回 {@link Optional#empty()}（失败会记日志）。
     */
    public Optional<String> clientEntityJson(String id) {
        return Optional.ofNullable(ClientEntityManager.INSTANCE.get(id))
                .flatMap(entity -> encodeToPrettyJson(BrClientEntity.CODEC, entity, id));
    }

    /**
     * 将指定行为实体定义经 {@link BehaviorEntity#CODEC} 编码为格式化 JSON。
     * id 不存在或编码失败时返回 {@link Optional#empty()}（失败会记日志）。
     */
    public Optional<String> behaviorEntityJson(String id) {
        return Optional.ofNullable(BehaviorEntityRegistry.get(id))
                .flatMap(entity -> encodeToPrettyJson(BehaviorEntity.CODEC, entity, id));
    }

    private static List<String> sortedIds(Map<String, ?> entries) {
        return entries.keySet().stream().sorted().toList();
    }

    private static <T> Optional<String> encodeToPrettyJson(Codec<T> codec, T value, String id) {
        DataResult<JsonElement> result = codec.encodeStart(JsonOps.INSTANCE, value);
        Optional<JsonElement> json = result.result();
        if (json.isEmpty()) {
            LOGGER.warn("[jsonview] encode entity json failed: id={}, error={}", id,
                    result.error().map(Object::toString).orElse("unknown"));
            return Optional.empty();
        }
        return Optional.of(GSON.toJson(json.get()));
    }
}
