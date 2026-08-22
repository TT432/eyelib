package io.github.tt432.eyelib.importer.render.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.ToIntFunction;

/** @author TT432 */
public record BrRenderControllers(
        Map<String, BrRenderControllerEntry> renderControllers
) {
    public static final Codec<BrRenderControllers> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, BrRenderControllerEntry.CODEC)
                    .fieldOf("render_controllers")
                    .forGetter(BrRenderControllers::renderControllers)
    ).apply(instance, BrRenderControllers::new));

    /**
     * 渲染控制器合并规则（loader / aggregate / runtime bridge 三处共用）：
     * part_visibility 条目数多者获胜，相等时后到的覆盖。
     */
    public static <E> boolean incomingWins(@Nullable E existing, E incoming, ToIntFunction<E> partVisibilitySize) {
        return existing == null
                || partVisibilitySize.applyAsInt(existing) <= partVisibilitySize.applyAsInt(incoming);
    }
}
