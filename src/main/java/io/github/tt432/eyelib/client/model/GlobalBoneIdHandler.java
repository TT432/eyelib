package io.github.tt432.eyelib.client.model;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
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

        return map.computeIfAbsent(boneName, k -> {
            int result = counter++;
            map2.put(result, boneName);
            return result;
        });
    }
}
