package io.github.tt432.eyelib;

import com.mojang.serialization.DataResult;

import java.util.function.Function;

/**
 * 测试专用的 DataResult 解包工具，封装 DFU 6→8 的 getOrThrow 签名差异。
 *
 * @author TT432
 */
public final class TestCodecUtil {
    private TestCodecUtil() {
    }

    /**
     * 解包 DataResult，失败时直接抛出 AssertionError。
     */
    public static <T> T unwrap(DataResult<T> result) {
        //? if <1.20.6 {
        return result.getOrThrow(false, s -> { throw new AssertionError(s); });
        //?} else {
        return result.getOrThrow(AssertionError::new);
        //?}
    }

    /**
     * 解包 DataResult，失败时用 errorFactory 构造异常抛出。
     */
    public static <T, E extends Throwable> T unwrap(DataResult<T> result, Function<String, E> errorFactory) throws E {
        //? if <1.20.6 {
        return result.getOrThrow(false, s -> { throw new RuntimeException(errorFactory.apply(s)); });
        //?} else {
        return result.getOrThrow(errorFactory);
        //?}
    }
}
