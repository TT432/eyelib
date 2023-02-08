package io.github.tt432.eyelib.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author DustW
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EyelibLists {
    public static class Node<T> {
        public T prev;
        public T next;
    }

    public static <T extends Node<T>> void link(List<T> source) {
        T prev = null;

        for (T node : source) {
            if (prev != null) {
                node.prev = prev;
                prev.next = node;
            }

            prev = node;
        }
    }
}
