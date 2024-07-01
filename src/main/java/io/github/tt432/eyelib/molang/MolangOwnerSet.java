package io.github.tt432.eyelib.molang;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Optional;

/**
 * @author TT432
 */
public class MolangOwnerSet {
    private final ObjectArrayList<Object> list = new ObjectArrayList<>();

    @SuppressWarnings("unchecked")
    public <N> Optional<N> ownerAs(Class<N> tClass) {
        for (var owner : list) {
            if (tClass.isInstance(owner)) {
                return Optional.of((N) owner);
            }
        }

        return Optional.empty();
    }

    public void add(Object o) {
        list.add(o);
    }
}
