package io.github.tt432.eyelib.molang.mapping;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.mapping.api.MolangNamespaces;
import io.github.tt432.eyelib.molang.type.MolangFloat;
import io.github.tt432.eyelib.molang.type.MolangObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 动态 Molang 命名空间注册表契约：前缀匹配、后注册优先、大小写不敏感、
 * 纪元失效与「空表零开销」短路。
 *
 * @author TT432
 */
class MolangNamespacesTest {
    @BeforeEach
    void setUp() {
        MolangNamespaces.clear();
    }

    @AfterEach
    void tearDown() {
        MolangNamespaces.clear();
    }

    @Test
    void registeredNamespaceResolvesRemainingPath() {
        MolangScope scope = new MolangScope();
        AtomicReference<MolangScope> seenScope = new AtomicReference<>();
        AtomicReference<String> seenPath = new AtomicReference<>();

        MolangNamespaces.Registration registration = MolangNamespaces.register("query.owner", (actualScope, path) -> {
            seenScope.set(actualScope);
            seenPath.set(path);
            return "health".equals(path) ? MolangFloat.valueOf(9) : null;
        });
        try {
            assertEquals(1, MolangNamespaces.size());

            MolangObject value = requireNonNull(MolangNamespaces.resolve(scope, "query.owner.health"));
            assertNotNull(value);
            assertEquals(9.0f, value.asFloat(), 0.0f);
            assertSame(scope, seenScope.get());
            assertEquals("health", seenPath.get());

            // 更深成员路径整段保留（含点）
            MolangNamespaces.resolve(scope, "query.owner.pos.x");
            assertEquals("pos.x", seenPath.get());

            // 前缀外 / 提供者不可用 → null
            assertNull(MolangNamespaces.resolve(scope, "query.other"));
            assertNull(MolangNamespaces.resolve(scope, "query.owner.missing"));
        } finally {
            registration.close();
        }
    }

    @Test
    void closeInvalidatesMemoizedMatchAndIsIdempotent() {
        MolangScope scope = new MolangScope();
        MolangNamespaces.Registration registration =
                MolangNamespaces.register("query.owner", (actualScope, path) -> MolangFloat.valueOf(1));

        // 先解析一次（写入 memo），确保 close 必须让缓存失效
        assertEquals(1.0f, requireNonNull(MolangNamespaces.resolve(scope, "query.owner.health")).asFloat(), 0.0f);

        registration.close();
        assertEquals(0, MolangNamespaces.size());
        assertNull(MolangNamespaces.resolve(scope, "query.owner.health"));

        // 幂等：重复 close 不改变注册数量，也不复活命中
        registration.close();
        assertEquals(0, MolangNamespaces.size());
        assertNull(MolangNamespaces.resolve(scope, "query.owner.health"));
    }

    @Test
    void longestPrefixWinsAndLaterRegistrationBreaksTies() {
        MolangScope scope = new MolangScope();
        MolangNamespaces.register("query", (actualScope, path) -> MolangFloat.valueOf(1));
        MolangNamespaces.register("query.owner", (actualScope, path) -> MolangFloat.valueOf(2));

        assertEquals(2.0f, requireNonNull(MolangNamespaces.resolve(scope, "query.owner.health")).asFloat(), 0.0f);
        assertEquals(1.0f, requireNonNull(MolangNamespaces.resolve(scope, "query.other")).asFloat(), 0.0f);

        // 同长度前缀：后注册者优先
        MolangNamespaces.register("query.owner", (actualScope, path) -> MolangFloat.valueOf(3));
        assertEquals(3.0f, requireNonNull(MolangNamespaces.resolve(scope, "query.owner.health")).asFloat(), 0.0f);
        assertEquals(1.0f, requireNonNull(MolangNamespaces.resolve(scope, "query.other")).asFloat(), 0.0f);
    }

    @Test
    void matchingIsCaseInsensitiveButPathKeepsItsSpelling() {
        MolangScope scope = new MolangScope();
        AtomicReference<String> seenPath = new AtomicReference<>();
        MolangNamespaces.register("Query.Owner", (actualScope, path) -> {
            seenPath.set(path);
            return MolangFloat.valueOf(5);
        });

        assertEquals(5.0f, requireNonNull(MolangNamespaces.resolve(scope, "query.owner.health")).asFloat(), 0.0f);
        assertEquals("health", seenPath.get());

        assertEquals(5.0f, requireNonNull(MolangNamespaces.resolve(scope, "QUERY.OWNER.Health")).asFloat(), 0.0f);
        assertEquals("Health", seenPath.get());
    }

    @Test
    void epochChangesOnRegisterAndUnregister() {
        long baseline = MolangNamespaces.epoch();

        MolangNamespaces.Registration registration =
                MolangNamespaces.register("query.owner", (actualScope, path) -> MolangFloat.valueOf(1));
        long afterRegister = MolangNamespaces.epoch();
        assertTrue(afterRegister > baseline, "注册必须推进纪元");

        registration.close();
        long afterClose = MolangNamespaces.epoch();
        assertTrue(afterClose > afterRegister, "注销必须推进纪元");

        MolangNamespaces.clear();
        assertTrue(MolangNamespaces.epoch() > afterClose, "clear 必须推进纪元");
    }

    @Test
    void emptyRegistryResolvesToNullWithoutTouchingProviders() {
        MolangScope scope = new MolangScope();
        assertNull(MolangNamespaces.resolve(scope, "query.owner.health"));
        assertNull(MolangNamespaces.resolve(scope, ""));
    }

    @Test
    void invalidNamespaceRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> MolangNamespaces.register("  ", (scope, path) -> null));
        assertThrows(IllegalArgumentException.class,
                () -> MolangNamespaces.register("query..owner", (scope, path) -> null));
        assertThrows(IllegalArgumentException.class,
                () -> MolangNamespaces.register("query.", (scope, path) -> null));
        assertEquals(0, MolangNamespaces.size());
    }
}
