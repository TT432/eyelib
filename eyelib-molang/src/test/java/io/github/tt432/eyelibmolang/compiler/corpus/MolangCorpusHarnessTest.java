package io.github.tt432.eyelibmolang.compiler.corpus;

import io.github.tt432.eyelibmolang.compiler.binding.BindDiagnosticsMode;
import io.github.tt432.eyelibmolang.compiler.binding.BindResult;
import io.github.tt432.eyelibmolang.compiler.binding.MolangBinder;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangCaseReport;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangDiagnosticsMode;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangResultType;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangRunReport;
import io.github.tt432.eyelibmolang.compiler.frontend.HandwrittenMolangAstParserFrontend;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.DEFAULT_MODE;
import static io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.DEFAULT_POLICY_PACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class MolangCorpusHarnessTest {
    private static final boolean RECORD_MODE = Boolean.getBoolean("molang.corpus.record");
    private static final Map<String, String> GOLDEN_BUFFER = new HashMap<>();

    private static final String STARTER_CORPUS_RESOURCE = "io/github/tt432/eyelibmolang/compiler/corpus/phase1/starter";
    private static final String INVALID_PARSE_GOLDEN_RESOURCE = "io/github/tt432/eyelibmolang/compiler/corpus/phase1/invalid-parse-golden";
    private static final String INVALID_ADJACENT_GOLDEN_RESOURCE = "io/github/tt432/eyelibmolang/compiler/corpus/phase1/invalid-adjacent-golden";
    private static final String INVALID_DIAGNOSTICS_GOLDEN_RESOURCE = "io/github/tt432/eyelibmolang/compiler/corpus/phase1/invalid-diagnostics-golden";
    private static final String INVALID_DEBUG_TRACE_GOLDEN_RESOURCE = "io/github/tt432/eyelibmolang/compiler/corpus/phase1/invalid-debug-trace-golden";
    private static final String INVALID_DEBUG_TRACE_MISMATCH_GOLDEN_RESOURCE = "io/github/tt432/eyelibmolang/compiler/corpus/phase1/invalid-debug-trace-mismatch";

    @BeforeEach
    void setUp() {
        if (RECORD_MODE) {
            // 记录模式：跳过所有断言并捕获实际输出用于 golden 文件
        }
    }

    @AfterAll
    static void tearDownAll() {
        if (RECORD_MODE && !GOLDEN_BUFFER.isEmpty()) {
            // 批量从缓冲区写入 golden 文件
            GOLDEN_BUFFER.forEach((path, content) -> {
                try {
                    Path goldenPath = Paths.get(path);
                    Files.createDirectories(goldenPath.getParent());
                    Files.writeString(goldenPath, content);
                    System.out.println("[RECORD] Wrote golden: " + path);
                } catch (IOException e) {
                    System.err.println("[RECORD] Failed to write golden: " + path + " - " + e.getMessage());
                }
            });
        }
    }

    @Test
    void starterCorpusRunsParseOnlyAssertionsAgainstGeneratedParserPath() throws URISyntaxException {
        Path corpusPath = starterCorpusPath();

        MolangRunReport report = new MolangCorpusHarness().run(corpusPath);

        if (RECORD_MODE) {
            // 记录 golden 比较的实际输出
            var ids = report.caseReports().stream().map(MolangCaseReport::caseId).collect(Collectors.toSet());
            GOLDEN_BUFFER.put("molang-corpus-case-ids.txt",
                    ids.stream().sorted().collect(Collectors.joining("\n")));
            return;
        }
        assertEquals(36, report.summary().totalCases());
        assertEquals(36, report.summary().passCount());
        assertEquals(0, report.summary().corpusErrorCount());
        assertEquals(0, report.summary().engineFailureCount());
        assertEquals(0, report.summary().assertionFailureCount());
        assertEquals(0, report.summary().skippedCount());

        Set<String> ids = report.caseReports().stream().map(MolangCaseReport::caseId).collect(Collectors.toSet());
        assertEquals(Set.of(
                "official.simple-expression.sin",
                "official.complex.assign-return",
                "official.control.loop-counter",
                "community.aliases.delta-time",
                "official.ternary.array-index",
                "official.null-coalesce",
                "official.struct.arrow",
                "official.unary.negate-query-time",
                "official.unary.not-grouped-condition",
                "official.comparison.range-window",
                "official.comparison.not-equals-string",
                "official.return.grouped-sum",
                "official.control.for-each.entity-counter",
                "official.member.dot-chain-read",
                "official.grouping.nested-precedence",
                "official.strings.ternary-literals",
                "reject.array-literal.inline",
                "official.control.binary-conditional.short-form",
                "reject.unterminated-string",
                "reject.dangling-dot",
                "bind.alias.normalize",
                "bind.alias.normalize.strict",
                "bind.alias.normalize.debug",
                "bind.arrow.preserve",
                "bind.query.projection",
                "bind.query.projection.explicit-call",
                "bind.alias.normalize.t",
                "bind.invalid-write.context",
                "bind.invalid-write.context.strict",
                "bind.deferred.ternary",
                "bind.loop.deferred.strict",
                "bind.loop.deferred.debug",
                "bind.foreach.deferred.strict",
                "bind.foreach.deferred.debug",
                "bind.deferred.ternary.strict",
                "bind.deferred.ternary.debug"
        ), ids);

        assertTrue(report.caseReports().stream().allMatch(item -> item.resultType() == MolangResultType.PASS), report.caseReports().toString());
        assertTrue(report.caseReports().stream().allMatch(item -> item.policyPack().equals(DEFAULT_POLICY_PACK)));

        Map<String, MolangCaseReport> casesById = report.caseReports().stream()
                .collect(Collectors.toMap(MolangCaseReport::caseId, item -> item));
        assertEquals(MolangDiagnosticsMode.STRICT, casesById.get("bind.loop.deferred.strict").diagnosticsMode());
        assertEquals(MolangDiagnosticsMode.DEBUG, casesById.get("bind.loop.deferred.debug").diagnosticsMode());
        assertEquals(MolangDiagnosticsMode.STRICT, casesById.get("bind.foreach.deferred.strict").diagnosticsMode());
        assertEquals(MolangDiagnosticsMode.DEBUG, casesById.get("bind.foreach.deferred.debug").diagnosticsMode());
        assertEquals(MolangDiagnosticsMode.STRICT, casesById.get("bind.deferred.ternary.strict").diagnosticsMode());
        assertEquals(MolangDiagnosticsMode.DEBUG, casesById.get("bind.deferred.ternary.debug").diagnosticsMode());
        assertEquals(MolangDiagnosticsMode.STRICT, casesById.get("bind.alias.normalize.strict").diagnosticsMode());
        assertEquals(MolangDiagnosticsMode.DEBUG, casesById.get("bind.alias.normalize.debug").diagnosticsMode());
        assertEquals(MolangDiagnosticsMode.STRICT, casesById.get("bind.invalid-write.context.strict").diagnosticsMode());
        assertTrue(casesById.entrySet().stream()
                .filter(entry -> !Set.of(
                        "bind.loop.deferred.strict",
                        "bind.loop.deferred.debug",
                        "bind.foreach.deferred.strict",
                        "bind.foreach.deferred.debug",
                        "bind.deferred.ternary.strict",
                        "bind.deferred.ternary.debug",
                        "bind.alias.normalize.strict",
                        "bind.alias.normalize.debug",
                        "bind.invalid-write.context.strict"
                ).contains(entry.getKey()))
                .allMatch(entry -> entry.getValue().diagnosticsMode() == DEFAULT_MODE));
    }

    @Test
    void malformedOrMissingParseGoldenProducesCorpusErrorInsteadOfEngineFailure() throws URISyntaxException {
        Path corpusPath = invalidParseGoldenCorpusPath();

        MolangRunReport report = new MolangCorpusHarness().run(corpusPath);

        assertEquals(2, report.summary().totalCases());
        assertEquals(0, report.summary().passCount());
        assertEquals(2, report.summary().corpusErrorCount());
        assertEquals(0, report.summary().engineFailureCount());
        assertEquals(0, report.summary().assertionFailureCount());
        assertEquals(0, report.summary().skippedCount());

        assertTrue(report.caseReports().stream().allMatch(item -> item.resultType() == MolangResultType.CORPUS_ERROR));
        assertTrue(report.caseReports().stream().anyMatch(item -> item.details().get(0).contains("Malformed parse golden")));
        assertTrue(report.caseReports().stream().anyMatch(item -> item.details().get(0).contains("missing required field 'root'")));
    }

    @Test
    void unsupportedAdjacentGoldensAndParseGoldenWithoutParseAcceptAreLintCorpusErrors() throws URISyntaxException {
        Path corpusPath = invalidAdjacentGoldenCorpusPath();

        MolangRunReport report = new MolangCorpusHarness().run(corpusPath);

        assertEquals(4, report.summary().totalCases());
        assertEquals(0, report.summary().passCount());
        assertEquals(4, report.summary().corpusErrorCount());
        assertEquals(0, report.summary().engineFailureCount());
        assertEquals(0, report.summary().assertionFailureCount());
        assertEquals(0, report.summary().skippedCount());

        assertTrue(report.caseReports().stream().allMatch(item -> item.resultType() == MolangResultType.CORPUS_ERROR));
        assertTrue(report.caseReports().stream().anyMatch(item -> item.details().get(0).contains("only supported for cases declaring 'parse-accept'")));
        assertTrue(report.caseReports().stream().anyMatch(item -> item.details().get(0).contains("unsupported-adjacent.bind.golden.yaml")));
        assertTrue(report.caseReports().stream().anyMatch(item -> item.details().get(0).contains("unsupported-adjacent.diagnostics.golden.yaml")));
        assertTrue(report.caseReports().stream().anyMatch(item -> item.details().get(0).contains("unsupported-adjacent.debug-trace.golden.yaml")));
    }

    @Test
    void bindDiagnosticsGoldenMismatchProducesAssertionFailureWithStructuredCodeMismatch() throws URISyntaxException {
        Path corpusPath = invalidDiagnosticsGoldenCorpusPath();

        MolangRunReport report = new MolangCorpusHarness().run(corpusPath);

        assertEquals(1, report.summary().totalCases());
        assertEquals(0, report.summary().passCount());
        assertEquals(0, report.summary().corpusErrorCount());
        assertEquals(0, report.summary().engineFailureCount());
        assertEquals(1, report.summary().assertionFailureCount());
        assertEquals(0, report.summary().skippedCount());

        MolangCaseReport caseReport = report.caseReports().get(0);
        assertEquals(MolangResultType.ASSERTION_FAILURE, caseReport.resultType());
        assertTrue(caseReport.details().stream().anyMatch(line -> line.contains("Diagnostics golden assertion failed")));
        assertTrue(caseReport.details().stream().anyMatch(line -> line.contains("BIND_INVALID_WRITE_TARGET")));
    }

    @Test
    void malformedDebugTraceGoldenProducesCorpusErrorInsteadOfAssertionFailure() throws URISyntaxException {
        Path corpusPath = invalidDebugTraceGoldenCorpusPath();

        MolangRunReport report = new MolangCorpusHarness().run(corpusPath);

        assertEquals(1, report.summary().totalCases());
        assertEquals(0, report.summary().passCount());
        assertEquals(1, report.summary().corpusErrorCount());
        assertEquals(0, report.summary().engineFailureCount());
        assertEquals(0, report.summary().assertionFailureCount());
        assertEquals(0, report.summary().skippedCount());

        MolangCaseReport caseReport = report.caseReports().get(0);
        assertEquals(MolangResultType.CORPUS_ERROR, caseReport.resultType());
        assertTrue(caseReport.details().stream().anyMatch(line -> line.contains("Malformed debug trace golden")));
    }

    @Test
    void mismatchedDebugTraceGoldenProducesAssertionFailureWithRenderedActualTokens() throws URISyntaxException {
        Path corpusPath = invalidDebugTraceMismatchCorpusPath();

        MolangRunReport report = new MolangCorpusHarness().run(corpusPath);

        assertEquals(1, report.summary().totalCases());
        assertEquals(0, report.summary().passCount());
        assertEquals(0, report.summary().corpusErrorCount());
        assertEquals(0, report.summary().engineFailureCount());
        assertEquals(1, report.summary().assertionFailureCount());
        assertEquals(0, report.summary().skippedCount());

        MolangCaseReport caseReport = report.caseReports().get(0);
        assertEquals(MolangResultType.ASSERTION_FAILURE, caseReport.resultType());
        assertTrue(caseReport.details().stream().anyMatch(line -> line.contains("Debug trace golden assertion failed")));
        assertTrue(caseReport.details().stream().anyMatch(line -> line.contains("Expected debug trace to contain token 'deferred-note-source-family:NotARealFamily'.")));
        assertTrue(caseReport.details().stream().anyMatch(line -> line.equals("Actual debug trace tokens:")));
        assertTrue(caseReport.details().stream().anyMatch(line -> line.equals("diagnostic-code:BIND_DEBUG_DEFERRED_NOTE")));
    }

    @Test
    void bindShapeIncludesTypedDeferredBreakAndContinueTokensFromHandwrittenLoopAst() {
        BindResult bindResult = bindFromHandwrittenFrontend("loop(2, {break; continue;})", BindDiagnosticsMode.NORMAL);

        MolangCorpusModel.MolangBindShape bindShape = new MolangCorpusHarness().collectBindShape(bindResult);

        assertTrue(bindShape.contains().contains("stmt:break"));
        assertTrue(bindShape.contains().contains("stmt:continue"));
        assertTrue(bindShape.contains().contains("stmt:break:deferred-reason:UNSUPPORTED_IN_THIS_SLICE"));
        assertTrue(bindShape.contains().contains("stmt:continue:deferred-reason:UNSUPPORTED_IN_THIS_SLICE"));
        assertTrue(bindShape.contains().contains("deferred-note-reason:UNSUPPORTED_IN_THIS_SLICE"));
    }

    private BindResult bindFromHandwrittenFrontend(String source, BindDiagnosticsMode diagnosticsMode) {
        MolangAst.ExprSet ast = HandwrittenMolangAstParserFrontend.INSTANCE.parseExprSetAst(source).orElseThrow();
        return new MolangBinder().bind(ast, diagnosticsMode);
    }

    private Path starterCorpusPath() throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(STARTER_CORPUS_RESOURCE), STARTER_CORPUS_RESOURCE).toURI());
    }

    private Path invalidParseGoldenCorpusPath() throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(INVALID_PARSE_GOLDEN_RESOURCE), INVALID_PARSE_GOLDEN_RESOURCE).toURI());
    }

    private Path invalidAdjacentGoldenCorpusPath() throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(INVALID_ADJACENT_GOLDEN_RESOURCE), INVALID_ADJACENT_GOLDEN_RESOURCE).toURI());
    }

    private Path invalidDiagnosticsGoldenCorpusPath() throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(INVALID_DIAGNOSTICS_GOLDEN_RESOURCE), INVALID_DIAGNOSTICS_GOLDEN_RESOURCE).toURI());
    }

    private Path invalidDebugTraceGoldenCorpusPath() throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(INVALID_DEBUG_TRACE_GOLDEN_RESOURCE), INVALID_DEBUG_TRACE_GOLDEN_RESOURCE).toURI());
    }

    private Path invalidDebugTraceMismatchCorpusPath() throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(INVALID_DEBUG_TRACE_MISMATCH_GOLDEN_RESOURCE), INVALID_DEBUG_TRACE_MISMATCH_GOLDEN_RESOURCE).toURI());
    }
}