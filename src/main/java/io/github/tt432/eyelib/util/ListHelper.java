package io.github.tt432.eyelib.util;

import io.github.tt432.eyelib.core.util.collection.ListAccessors;

import java.util.List;

public class ListHelper {
    public static <T> T getFirst(List<T> list) {
        return ListAccessors.first(list);
    }

    public static <T> T getLast(List<T> list) {
        return ListAccessors.last(list);
    }
}
