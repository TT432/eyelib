package io.github.tt432.eyelibmodel;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.Locale;

/**
 * 管理骨骼 ID 与字符串名称之间的双向映射。
 *
 * @author TT432
 */
public class GlobalBoneIdHandler {
    public static final Codec<Integer> STRING_ID_CODEC = Codec.STRING.xmap(GlobalBoneIdHandler::get, GlobalBoneIdHandler::get);

    public static <S> Codec<Int2ObjectMap<S>> map(Codec<S> valueCodec) {
        return Codec.unboundedMap(GlobalBoneIdHandler.STRING_ID_CODEC, valueCodec).xmap(Int2ObjectOpenHashMap::new, m -> m);
    }

    private static final Object2IntMap<String> map = new Object2IntOpenHashMap<>();
    private static final Int2ObjectMap<String> map2 = new Int2ObjectOpenHashMap<>();
    private static int counter;

    public static String get(int id) {
        return map2.get(id);
    }

    public static int get(String boneName) {
        if (boneName.isBlank()) return -1;

        return map.computeIfAbsent(boneName.toLowerCase(Locale.ROOT), k -> {
            int result = counter++;
            map2.put(result, boneName.toLowerCase(Locale.ROOT));
            return result;
        });
    }
}