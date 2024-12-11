package io.github.tt432.eyelib.util.search;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author TT432
 */
@RequiredArgsConstructor
public class SearchResults<V> {
    private final Searchable<V> searchable;
    @Getter
    private final List<Map.Entry<String, V>> suggestions = new ArrayList<>();

    public void update(String searchString) {
        suggestions.clear();
        searchable.search(searchString).forEach(suggestions::add);
    }
}
