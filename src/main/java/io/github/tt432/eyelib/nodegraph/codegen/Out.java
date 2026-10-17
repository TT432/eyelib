package io.github.tt432.eyelib.nodegraph.codegen;

import java.util.List;

/** 值发射结果：前置语句（temp 提取/子图语句）+ 表达式文本。 */
record Out(List<String> preludes, String expr) {
    static Out of(String expr) {
        return new Out(List.of(), expr);
    }
}
