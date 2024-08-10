package io.github.tt432.eyelib.molang;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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

    public void add(Object o) {
        list.add(o);
    }

    public <T> void replace(Class<T> clazz, T o) {
        remove(clazz);
        add(o);
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

    private List<?> getOwners(Class<?>... classes) {
        Object[] result = new Object[classes.length];

        for (int i = 0; i < classes.length; i++) {
            for (var owner : list) {
                if (result[i] == null && classes[i].isInstance(owner)) {
                    result[i] = owner;
                }
            }
        }

        for (Object o : result) {
            if (o == null) return List.of();
        }

        return List.of(result);
    }

    @SuppressWarnings("unchecked")
    public <A, R> Optional<R> onHiveOwners(Class<A> classA, OwnersFunction1<A, R> consumer) {
        for (var owner : list) {
            if (classA.isInstance(owner)) {
                return Optional.ofNullable(consumer.apply((A) owner));
            }
        }

        if (parent != null) {
            return parent.onHiveOwners(classA, consumer);
        } else {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public <A, B, R> Optional<R> onHiveOwners(Class<A> classA, Class<B> classB, OwnersConsumer2<A, B, R> consumer) {
        List<?> owners = getOwners(classA, classB);
        if (!owners.isEmpty()) {
            return Optional.ofNullable(consumer.apply((A) owners.get(0), (B) owners.get(1)));
        } else if (parent != null) {
            return parent.onHiveOwners(classA, classB, consumer);
        } else {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public <A, B, C, R> Optional<R> onHiveOwners(Class<A> classA, Class<B> classB, Class<C> classC,
                                                 OwnersConsumer3<A, B, C, R> consumer) {
        List<?> owners = getOwners(classA, classB, classC);
        if (!owners.isEmpty()) {
            return Optional.ofNullable(consumer.apply((A) owners.get(0), (B) owners.get(1), (C) owners.get(2)));
        } else if (parent != null) {
            return parent.onHiveOwners(classA, classB, classC, consumer);
        } else {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public <A, B, C, D, R> Optional<R> onHiveOwners(Class<A> classA, Class<B> classB, Class<C> classC, Class<D> classD,
                                                    OwnersConsumer4<A, B, C, D, R> consumer) {
        List<?> owners = getOwners(classA, classB, classC, classD);
        if (!owners.isEmpty()) {
            return Optional.ofNullable(consumer.apply((A) owners.get(0), (B) owners.get(1), (C) owners.get(2), (D) owners.get(3)));
        } else if (parent != null) {
            return parent.onHiveOwners(classA, classB, classC, classD, consumer);
        } else {
            return Optional.empty();
        }
    }

    public interface OwnersFunction1<A, R> {
        R apply(A o);
    }

    public interface OwnersConsumer2<A, B, R> {
        R apply(A a, B b);
    }

    public interface OwnersConsumer3<A, B, C, R> {
        R apply(A a, B b, C c);
    }

    public interface OwnersConsumer4<A, B, C, D, R> {
        R apply(A a, B b, C c, D d);
    }
}
