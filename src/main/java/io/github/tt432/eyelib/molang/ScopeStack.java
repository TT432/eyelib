package io.github.tt432.eyelib.molang;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author DustW
 */
@Slf4j
public class ScopeStack {
    public final Deque<MolangVariableScope> tempScope = new ArrayDeque<>();

    int pushed;

    public ScopeStack() {
        tempScope.addLast(MolangParser.getGlobalScope());
    }

    public void push(MolangVariableScope scope) {
        pushed++;
        tempScope.addLast(scope);
    }

    public void pop() {
        if (pushed > 0) {
            pushed--;
            tempScope.removeLast();
        } else {
            log.error("why pop on not pushed?");
        }
    }

    public MolangVariableScope last() {
        return tempScope.getLast();
    }
}
