package io.github.tt432.eyelib.bridge.client.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * {@link BatchScope#NOOP} 的纯逻辑契约（不触碰 GL）。
 *
 * @author TT432
 */
class BatchScopeTest {

    @Test
    @DisplayName("NOOP 作用域始终「已关闭」")
    void noopIsNeverOpen() {
        assertFalse(BatchScope.NOOP.isOpen());
    }

    @Test
    @DisplayName("NOOP 的 close 幂等")
    void noopCloseIsIdempotent() {
        assertDoesNotThrow(BatchScope.NOOP::close);
        assertDoesNotThrow(BatchScope.NOOP::close);
        assertFalse(BatchScope.NOOP.isOpen());
    }

    @Test
    @DisplayName("NOOP 的统计为零值快照")
    void noopStatsAreZero() {
        assertEquals(new BatchStats(0, 0, 0, 0, 0), BatchScope.NOOP.stats());
    }

    @Test
    @DisplayName("AutoCloseable 用法：try-with-resources 不会泄漏作用域")
    void usableAsAutoCloseable() {
        try (BatchScope scope = BatchScope.NOOP) {
            assertEquals(new BatchStats(0, 0, 0, 0, 0), scope.stats());
        }
        assertFalse(BatchScope.NOOP.isOpen());
    }
}
