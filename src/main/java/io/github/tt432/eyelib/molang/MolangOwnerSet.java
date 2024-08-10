package io.github.tt432.eyelib.molang;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * @author TT432
 */
public class MolangOwnerSet {
    @Nullable
    @Setter
    private MolangOwnerSet parent;
    private final ObjectArrayList<Object> list = new ObjectArrayList<>();

    public void remove(Class<?> clazz) {
        list.removeIf(clazz::isInstance);
    }

    @SuppressWarnings("unchecked")
    public <N> Optional<N> ownerAs(Class<N> tClass) {
        for (var owner : list) {
            if (tClass.isInstance(owner)) {
                return Optional.of((N) owner);
            }
        }

        return Optional.ofNullable(parent).flatMap(p -> p.ownerAs(tClass));
    }

    public void add(Object o) {
        list.add(o);
    }
}
