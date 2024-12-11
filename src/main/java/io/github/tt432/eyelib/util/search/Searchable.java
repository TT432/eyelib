package io.github.tt432.eyelib.util.search;

import java.util.Map;
import java.util.stream.Stream;

/**
 * @author TT432
 */
public interface Searchable<V> {
    Stream<Map.Entry<String, V>> search(String searchStr);

    default SearchResults<V> results() {
        return new SearchResults<>(this);
    }
}
