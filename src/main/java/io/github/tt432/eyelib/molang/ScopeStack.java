package io.github.tt432.eyelib.molang;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author DustW
 */
public class ScopeStack implements AutoCloseable {
    public final Deque<MolangVariableScope> tempScope = new ArrayDeque<>();

    public ScopeStack() {
        tempScope.add(MolangParser.getGlobalScope());
    }

    public ScopeStack push(MolangVariableScope scope) {
        tempScope.add(scope);
        return this;
    }

    public void pop() {
        tempScope.removeLast();
    }

    @Override
    public void close() {
        pop();
    }

    public MolangVariableScope last() {
        return tempScope.getLast();
    }
}
