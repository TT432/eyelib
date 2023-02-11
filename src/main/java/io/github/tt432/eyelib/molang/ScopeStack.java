package io.github.tt432.eyelib.molang;

import java.util.LinkedList;
import java.util.List;

/**
 * @author DustW
 */
public class ScopeStack implements AutoCloseable {
    public final List<MolangVariableScope> tempScope = new LinkedList<>();

    public ScopeStack() {
        tempScope.add(MolangParser.getGlobalScope());
    }

    public ScopeStack push(MolangVariableScope scope) {
        tempScope.add(scope);
        return this;
    }

    public void pop() {
        tempScope.remove(tempScope.size() - 1);
    }

    @Override
    public void close() {
        pop();
    }

    public MolangVariableScope last() {
        return tempScope.get(tempScope.size() - 1);
    }
}
