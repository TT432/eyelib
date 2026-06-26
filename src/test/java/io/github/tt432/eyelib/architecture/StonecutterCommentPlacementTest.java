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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 源码扫描规则（ADR-0016 §6）：{@code //?} Stonecutter 条件化注释的唯一合法栖息地是
 * ACL(bridge) 和 Infrastructure(mixin/smoke/debug)。
 *
 * <p>ArchUnit 字节码层面查不到源码注释，本测试做文本扫描。与 ArchitectureTest 同 freeze 模式：
 * baseline 存于 {@code build/archunit_store/stonecutter-comment-baseline.txt}。
 *
 * <p>扫的是 {@code src/main/java/} 模板源（Stonecutter 处理前），各版本 node 共享同一份 baseline。
 */
class StonecutterCommentPlacementTest {

    private static final Path MAIN_SOURCE_ROOT = Paths.get("src", "main", "java");

    /**
     * 允许包含 {@code //?} 的包根：与 ArchitectureTest.ALLOWED_VERSION_SPECIFIC_MC_HOSTS 对齐
     * （ADR-0016 §1 表格的 ACL + Infrastructure 全集）。
     */
    private static final List<String> ALLOWED_PACKAGE_ROOTS = List.of(
            "io/github/tt432/eyelib/bridge/",
            "io/github/tt432/eyelib/mixin/",
            "io/github/tt432/eyelib/smoke/",
            "io/github/tt432/eyelib/debug/"
    );

    private static final String STONECUTTER_MARKER = "//?";

    private static final Path BASELINE_FILE =
            Paths.get("build", "archunit_store", "stonecutter-comment-baseline.txt");

    @Test
    void stonecutterCommentsOnlyInBridgeOrInfrastructure() throws IOException {
        List<String> actual = scanViolations();

        if (!Files.exists(BASELINE_FILE)) {
            Files.createDirectories(BASELINE_FILE.getParent());
            Files.write(BASELINE_FILE, actual, StandardCharsets.UTF_8);
            return;
        }

        List<String> baseline = Files.readAllLines(BASELINE_FILE, StandardCharsets.UTF_8);

        List<String> newViolations = new ArrayList<>(actual);
        newViolations.removeAll(baseline);

        if (!newViolations.isEmpty()) {
            throw new AssertionError(String.format(
                    "ADR-0016 §6 //? 散布约束：发现 %d 个新违规（baseline=%d，实际=%d）：%n%s%n"
                            + "修复方式：把含 //? 的代码段迁到 bridge/ 或 mixin/，或抽 Port 让版本差异收敛到 ACL。",
                    newViolations.size(), baseline.size(), actual.size(),
                    String.join("\n", newViolations)
            ));
        }

        if (actual.size() < baseline.size()) {
            Files.write(BASELINE_FILE, actual, StandardCharsets.UTF_8);
        }
    }

    private static List<String> scanViolations() throws IOException {
        try (Stream<Path> paths = Files.walk(MAIN_SOURCE_ROOT)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(StonecutterCommentPlacementTest::containsStonecutterComment)
                    .map(p -> MAIN_SOURCE_ROOT.relativize(p).toString().replace('\\', '/'))
                    .filter(rel -> ALLOWED_PACKAGE_ROOTS.stream().noneMatch(rel::startsWith))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static boolean containsStonecutterComment(Path file) {
        try {
            return Files.lines(file, StandardCharsets.UTF_8)
                    .anyMatch(line -> line.contains(STONECUTTER_MARKER));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
