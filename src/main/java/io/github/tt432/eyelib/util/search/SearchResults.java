package io.github.tt432.eyelibutil.search;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 支持实时搜索的搜索结果缓存，包装 Searchable 并维护建议列表。
 *
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