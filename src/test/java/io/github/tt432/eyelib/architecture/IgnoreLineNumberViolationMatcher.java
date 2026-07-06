package io.github.tt432.eyelib.architecture;

import com.tngtech.archunit.library.freeze.ViolationLineMatcher;

import java.util.regex.Pattern;

/**
 * ArchUnit freeze 自定义匹配器：比较违规描述时忽略源码行号。
 *
 * <p>默认的 {@code FuzzyViolationLineMatcher} 把行号纳入匹配，源码任意编辑导致行号偏移后，
 * 已冻结的 baseline 会失效并误报 new violation。架构规则关心的是"依赖关系是否存在"，
 * 而非"出现在哪一行"，故此处剥离 {@code in (File.java:N)} 中的 {@code :N} 再比较。
 *
 * <p>通过系统属性 {@code archunit.freeze.lineMatcher} 启用。
 *
 * @author TT432
 */
public final class IgnoreLineNumberViolationMatcher implements ViolationLineMatcher {
    private static final Pattern LINE_NUMBER = Pattern.compile(" in \\((.+?\\.java):\\d+\\)");

    @Override
    public boolean matches(String line, String storedLine) {
        return stripLineNumber(line).equals(stripLineNumber(storedLine));
    }

    private static String stripLineNumber(String description) {
        return LINE_NUMBER.matcher(description).replaceAll(" in ($1)");
    }
}
