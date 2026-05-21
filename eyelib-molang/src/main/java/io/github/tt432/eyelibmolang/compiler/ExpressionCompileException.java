package io.github.tt432.eyelibmolang.compiler;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Molang 表达式编译异常。
 *
 * @author TT432
 */
@NullMarked
/** @author TT432 */
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