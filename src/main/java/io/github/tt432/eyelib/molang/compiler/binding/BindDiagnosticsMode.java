package io.github.tt432.eyelib.molang.compiler.binding;

import org.jspecify.annotations.NullMarked;

/**
 * 绑定诊断模式：正常、严格、调试。
 *
 * @author TT432
 */
@NullMarked
public enum BindDiagnosticsMode {
    NORMAL,
    STRICT,
    DEBUG
}