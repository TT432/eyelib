package io.github.tt432.eyelib.material.shared;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.github.tt432.eyelib.util.codec.DispatchedMapCodec;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Bedrock材质定义的纯数据记录。
 *
 * @author TT432
 */
@NullMarked
public record BrMaterial(@Nullable String version, Map<String, BrMaterialEntry> materials) {
    private static final Codec<Map<String, BrMaterialEntry>> ENTRIES_CODEC =
            new DispatchedMapCodec<>(Codec.STRING, BrMaterialEntry.CODEC::apply);

    public static final Codec<BrMaterial> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<BrMaterial, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).flatMap(root -> {
                T matsObj = root.get("materials");
                if (matsObj == null) {
                    return DataResult.error(() -> "Missing 'materials' field");
                }
                return ops.getMap(matsObj).flatMap(mats -> {
                    final String version;
                    T versionNode = mats.get("version");
                    if (versionNode != null) {
                        version = ops.getStringValue(versionNode).result().orElse(null);
                    } else {
                        version = null;
                    }

                    var filtered = new ArrayList<Pair<T, T>>();
                    mats.entries().forEach(e -> {
                        ops.getStringValue(e.getFirst()).result().ifPresent(k -> {
                            if (!"version".equals(k)) {
                                filtered.add(e);
                            }
                        });
                    });
                    T filteredMats = ops.createMap(filtered.stream());

                    DataResult<Pair<Map<String, BrMaterialEntry>, T>> entriesResult = ENTRIES_CODEC.decode(ops, filteredMats);
                    return entriesResult.map(p -> Pair.of(new BrMaterial(version, p.getFirst()), p.getSecond()));
                });
            });
        }

        @Override
        public <T> DataResult<T> encode(BrMaterial input, DynamicOps<T> ops, T prefix) {
            return ENTRIES_CODEC.encodeStart(ops, input.materials()).map(entriesEncoded -> {
                T key = ops.createString("materials");
                return ops.createMap(Stream.of(Pair.of(key, entriesEncoded)));
            });
        }
    };
}