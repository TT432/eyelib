package io.github.tt432.eyelibutil.collection;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import lombok.AllArgsConstructor;

import java.util.AbstractList;
import java.util.List;

/**
 * 提供基于 Int2ObjectFunction 的懒加载列表视图。
 *
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