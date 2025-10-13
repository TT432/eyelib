package io.github.tt432.eyelib.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import lombok.AllArgsConstructor;

import java.util.AbstractList;
import java.util.List;

/**
 * @author TT432
 */
public class Lists {
    public static  <E> List<E> asList(int count, Int2ObjectFunction<E> function) {
        return new AsListView<>(count, function);
    }

    @AllArgsConstructor
    private static final class AsListView<E> extends AbstractList<E> {
        int count;
        Int2ObjectFunction<E> function;

        @Override
        public E get(int index) {
            return function.get(index);
        }

        @Override
        public int size() {
            return count;
        }
    }
}
