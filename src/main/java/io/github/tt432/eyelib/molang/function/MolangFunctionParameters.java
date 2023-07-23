package io.github.tt432.eyelib.molang.function;

import io.github.tt432.eyelib.molang.MolangScope;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author TT432
 */
public class MolangFunctionParameters {
    MolangScope scope;
    List<Object> parameters;

    public MolangFunctionParameters(MolangScope scope, List<Object> parameters) {
        this.scope = scope;
        this.parameters = parameters;
    }

    public int size() {
        return parameters.size();
    }

    public float value(int index) {
        return (float) parameters.get(index);
    }

    public String svalue(int index) {
        return parameters.get(index).toString();
    }

    public Stream<String> svalues() {
        return parameters.stream().map(Object::toString);
    }

    public MolangScope scope() {
        return scope;
    }
}
