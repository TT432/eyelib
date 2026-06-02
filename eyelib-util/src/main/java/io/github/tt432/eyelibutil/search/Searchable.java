package io.github.tt432.eyelibutil.search;

import java.util.Map;
import java.util.stream.Stream;

/**
 * 可搜索接口，支持按字符串提供搜索结果流。
 *
 * @author TT432
 */
public interface Searchable<V> {
    Stream<Map.Entry<String, V>> search(String searchStr);

    default SearchResults<V> results() {
        return new SearchResults<>(this);
    }
}