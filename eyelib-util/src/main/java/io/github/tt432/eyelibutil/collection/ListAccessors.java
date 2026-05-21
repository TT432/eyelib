package io.github.tt432.eyelibutil.collection;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
/** @author TT432 */
public final class ListAccessors {
    public static <T> T first(List<T> list) {
        return list.get(0);
    }

    public static <T> T last(List<T> list) {
        return list.get(list.size() - 1);
    }
}