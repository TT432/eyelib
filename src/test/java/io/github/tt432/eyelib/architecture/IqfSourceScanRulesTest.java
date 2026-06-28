/** @author TT432 */
package io.github.tt432.eyelib.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ADR-0018 IQF 源码扫描规则（ArchUnit 字节码层面查不到的部分）：
 * 规则 2：Application 包不得有 {@code static {} 业务 wiring（判据 Q-2，机制 C 取代）。
 * 规则 5：{@code getOwner() != entity} 等 init 守卫只能出现在状态所有者类内部（判据 Q-4）。
 *
 * 与 {@link StonecutterCommentPlacementTest} 同 freeze 模式：baseline 存于
 * {@code build/archunit_store/}，首次跑记录违规，后续只检测新违规。
 *
 * 扫的是 {@code src/main/java/} 模板源（Stonecutter 处理前），各版本 node 共享同一份 baseline。
 */
class IqfSourceScanRulesTest {

    private static final Path MAIN_SOURCE_ROOT = Paths.get("src", "main", "java");

    /**
     * Application 层子包根（与 {@code ArchitectureTest.APPLICATION_CLASSES} 对齐）。
     */
    private static final List<String> APPLICATION_PACKAGE_ROOTS = List.of(
            "io/github/tt432/eyelib/client/",
            "io/github/tt432/eyelib/common/",
            "io/github/tt432/eyelib/network/",
            "io/github/tt432/eyelib/capability/",
            "io/github/tt432/eyelib/attachment/",
            "io/github/tt432/eyelib/track/",
            "io/github/tt432/eyelib/event/"
    );

    private static final String STATIC_INITIALIZER_MARKER = "static {";

    /**
     * init 守卫模式：捕获 {@code getOwner() != <var>} 形式（不论具体变量名）。
     */
    private static final Pattern LAZY_INIT_GUARD_PATTERN =
            Pattern.compile("\\bgetOwner\\(\\)\\s*!=\\s*\\w+");

    private static final Path STATIC_INITIALIZER_BASELINE =
            Paths.get("build", "archunit_store", "iqf-static-initializer-baseline.txt");

    private static final Path LAZY_INIT_SCATTER_BASELINE =
            Paths.get("build", "archunit_store", "iqf-lazy-init-scatter-baseline.txt");

    @Test
    void applicationMustNotHaveStaticInitializer() throws IOException {
        List<String> actual = scanViolations(
                file -> linesContaining(file, STATIC_INITIALIZER_MARKER));

        compareWithBaseline(actual, STATIC_INITIALIZER_BASELINE,
                "ADR-0018 Q-2: application 不得有 static { 业务 wiring；"
                        + "修复方式：把 wiring 逻辑迁到 EyelibRuntime composition root（机制 C）");
    }

    @Test
    void noLazyInitScatter() throws IOException {
        List<String> actual = scanViolations(
                file -> linesMatching(file, LAZY_INIT_GUARD_PATTERN));

        compareWithBaseline(actual, LAZY_INIT_SCATTER_BASELINE,
                "ADR-0018 Q-4: init 守卫（getOwner() != xxx）只能出现在状态所有者类内部；"
                        + "修复方式：把守卫收敛到 RenderData / RenderData 各组件的 init(owner) 内");
    }

    private static List<String> scanViolations(Function<Path, Stream<String>> extractor) throws IOException {
        try (Stream<Path> paths = Files.walk(MAIN_SOURCE_ROOT)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(IqfSourceScanRulesTest::isApplicationSource)
                    .flatMap(extractor)
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static boolean isApplicationSource(Path file) {
        String rel = MAIN_SOURCE_ROOT.relativize(file).toString().replace('\\', '/');
        return APPLICATION_PACKAGE_ROOTS.stream().anyMatch(rel::startsWith);
    }

    private static Stream<String> linesContaining(Path file, String marker) {
        try {
            return matchingLines(file, line -> line.contains(marker));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Stream<String> linesMatching(Path file, Pattern pattern) {
        try {
            return matchingLines(file, line -> pattern.matcher(line).find());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Stream<String> matchingLines(Path file, java.util.function.Predicate<String> predicate)
            throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        String rel = MAIN_SOURCE_ROOT.relativize(file).toString().replace('\\', '/');
        List<String> hits = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (predicate.test(lines.get(i))) {
                hits.add(rel + ":" + (i + 1) + ":" + lines.get(i).trim());
            }
        }
        return hits.stream();
    }

    private static void compareWithBaseline(List<String> actual, Path baselineFile, String message)
            throws IOException {
        if (!Files.exists(baselineFile)) {
            Files.createDirectories(baselineFile.getParent());
            Files.write(baselineFile, actual, StandardCharsets.UTF_8);
            return;
        }

        List<String> baseline = Files.readAllLines(baselineFile, StandardCharsets.UTF_8);

        List<String> newViolations = new ArrayList<>(actual);
        newViolations.removeAll(baseline);

        if (!newViolations.isEmpty()) {
            throw new AssertionError(String.format(
                    "%s%n发现 %d 个新违规（baseline=%d，实际=%d）：%n%s",
                    message, newViolations.size(), baseline.size(), actual.size(),
                    String.join("\n", newViolations)
            ));
        }

        if (actual.size() < baseline.size()) {
            Files.write(baselineFile, actual, StandardCharsets.UTF_8);
        }
    }
}
