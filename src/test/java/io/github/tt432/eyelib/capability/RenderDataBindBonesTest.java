package io.github.tt432.eyelib.capability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RenderData#bindBones()} 缓存契约（Opt16）：
 * 命中返回同一实例；{@link RenderData#invalidateBindBones()} 后重建。
 * modelComponents 变更点（setupClientEntity / sync apply）必须走失效——
 * 本测试钉死缓存-失效对的存在性语义。
 *
 * @author TT432
 */
class RenderDataBindBonesTest {

    @Test
    void emptyComponentsYieldEmptyMap() {
        RenderData<Object> data = new RenderData<>();
        assertTrue(data.bindBones().isEmpty());
    }

    @Test
    void cachedInstanceReusedUntilInvalidated() {
        RenderData<Object> data = new RenderData<>();
        var first = data.bindBones();
        assertSame(first, data.bindBones());

        data.invalidateBindBones();
        var second = data.bindBones();
        assertNotSame(first, second);
        assertSame(second, data.bindBones());
    }
}
