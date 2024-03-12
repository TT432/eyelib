package io.github.tt432.eyelib.util;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * {@link QuickAccessEntityList} is a specialized collection that enables fast lookup, addition,
 * and removal of elements based on their unique IDs. It implements the Collection interface
 * and contains elements of type {@link I}, which must extend the {@link IdentifiableObject} interface.
 * <p>
 * Internally, this collection uses a List to store the elements and a Map to maintain
 * a mapping from element IDs to their corresponding indices in the List. This allows
 * for constant-time lookups, additions, and removals.
 * <p>
 * Usage example:
 * <pre>{@code
 *     QuickAccessEntityList<User> users = new QuickAccessEntityList<>();
 *     users.add(new User(1, "Alice"));
 *     users.add(new User(2, "Bob"));
 *     boolean exists = users.contains(new User(1, "Alice")); // true
 * }</pre>
 * <p>
 * Time Complexity:
 * - Add: O(1)
 * - Remove: O(1)
 * - Lookup: O(1)
 * <p>
 * Note: This collection does not allow duplicate IDs. Adding an element with an
 * existing ID will overwrite the existing element.
 *
 * @author TT432
 */
public class QuickAccessEntityList<I extends IdentifiableObject> implements Collection<I> {

    Int2IntOpenHashMap idToIdxMap = new Int2IntOpenHashMap();
    List<I> entities = new ReferenceArrayList<>();

    public QuickAccessEntityList() {
        idToIdxMap.defaultReturnValue(-1);
    }

    @Override
    public int size() {
        return entities.size();
    }

    @Override
    public boolean isEmpty() {
        return entities.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return idToIdxMap.containsKey(((I) o).id());
    }

    @NotNull
    @Override
    public Iterator<I> iterator() {
        return new Iterator<>() {
            private final Iterator<I> it = entities.iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public I next() {
                return it.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("QuickAccessEntityList's iterator can't use remove()");
            }
        };
    }

    @NotNull
    @Override
    public Object @NotNull [] toArray() {
        return entities.toArray();
    }

    @NotNull
    @Override
    public <T> T @NotNull [] toArray(@NotNull T[] a) {
        return entities.toArray(a);
    }

    @Override
    public boolean add(I i) {
        idToIdxMap.put(i.id(), entities.size());
        return entities.add(i);
    }

    public boolean remove(int id) {
        var i = idToIdxMap.get(id);

        if (i == -1) {
            return false;
        }

        if (i == 0) {
            entities.remove(0);
        } else if (i == (entities.size() - 1)) {
            int size = entities.size();
            entities.remove(size - 1);
        } else {
            int size = entities.size();
            var lastElement = entities.get(size - 1);
            entities.set(i, lastElement);
            idToIdxMap.put(lastElement.id(), i);
            entities.remove(size - 1);
        }

        idToIdxMap.remove(id);

        return true;
    }

    @Override
    public boolean remove(Object o) {
        return remove(((IdentifiableObject) o).id());
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        for (Object o : c) {
            if (!idToIdxMap.containsKey(((I) o).id())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends I> c) {
        boolean isModified = false;
        for (I item : c) {
            if (idToIdxMap.putIfAbsent(item.id(), entities.size()) == -1) {
                entities.add(item);
                isModified = true;
            }
        }
        return isModified;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        boolean isModified = false;
        for (Object o : c) {
            if (remove(o)) {
                isModified = true;
            }
        }
        return isModified;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        boolean isModified = false;
        Iterator<I> it = entities.iterator();

        while (it.hasNext()) {
            I item = it.next();
            if (!c.contains(item)) {
                it.remove();
                idToIdxMap.remove(item.id());
                isModified = true;
            }
        }

        return isModified;
    }

    @Override
    public void clear() {
        entities.clear();
        idToIdxMap.clear();
    }
}