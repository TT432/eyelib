package io.github.tt432.eyelibmolang.compiler;

import java.util.List;

public class ExpressionCompileException extends RuntimeException {
    private final String sourceExpression;
    private final List<String> diagnostics;

    public ExpressionCompileException(String sourceExpression, String message) {
        super(message);
        this.sourceExpression = sourceExpression;
        this.diagnostics = List.of();
    }

    public ExpressionCompileException(String sourceExpression, String message, Throwable cause) {
        super(message, cause);
        this.sourceExpression = sourceExpression;
        this.diagnostics = List.of();
    }

    public ExpressionCompileException(String sourceExpression, String message, List<String> diagnostics) {
        super(message);
        this.sourceExpression = sourceExpression;
        this.diagnostics = List.copyOf(diagnostics);
    }

    public String sourceExpression() {
        return sourceExpression;
    }

    public List<String> diagnostics() {
        return diagnostics;
    }
}
