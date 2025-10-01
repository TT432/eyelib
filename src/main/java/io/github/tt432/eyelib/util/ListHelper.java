package io.github.tt432.eyelib.util;

import java.util.List;

public class ListHelper {
    public static <T> T getFirst(List<T> list) {
        return list.get(0);
    }

    public static <T> T getLast(List<T> list) {
        return list.get(list.size() - 1);
    }
}
