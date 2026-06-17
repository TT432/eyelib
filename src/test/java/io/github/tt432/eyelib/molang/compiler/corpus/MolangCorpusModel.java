package io.github.tt432.eyelib.molang.compiler.corpus;

import io.github.tt432.eyelib.molang.compiler.frontend.ast.MolangAst;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** @author TT432 */
final class MolangCorpusModel {
    private MolangCorpusModel() {
    }

    static final MolangDiagnosticsMode DEFAULT_MODE = MolangDiagnosticsMode.NORMAL;
    static final String DEFAULT_POLICY_PACK = "default-v1";
    static final String CASE_EXTENSION = ".molangcase";
    static final String PARSE_GOLDEN_EXTENSION = ".parse.golden.yaml";
    static final String BIND_GOLDEN_EXTENSION = ".bind.golden.yaml";
    static final String DIAGNOSTICS_GOLDEN_EXTENSION = ".diagnostics.golden.yaml";
    static final String DEBUG_TRACE_GOLDEN_EXTENSION = ".debug-trace.golden.yaml";

    enum MolangAssertionType {
        PARSE_ACCEPT("parse-accept"),
        PARSE_REJECT("parse-reject"),
        BIND_NORMALIZE("bind-normalize"),
        BIND_DIAGNOSTICS("bind-diagnostics"),
        COMPAT_BEHAVIOR("compat-behavior"),
        DEFERRED_NOTE("deferred-note"),
        DEBUG_TRACE("debug-trace");

        private final String serialized;

        MolangAssertionType(String serialized) {
            this.serialized = serialized;
        }

        String serialized() {
            return serialized;
        }

        static Optional<MolangAssertionType> fromSerialized(String value) {
            for (MolangAssertionType type : values()) {
                if (type.serialized.equals(value)) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }
    }

    enum MolangCorpusLayer {
        STARTER,
        OFFICIAL,
        COMMUNITY,
        INTERNAL,
        REJECT,
        BIND,
        COMPAT;

        static Optional<MolangCorpusLayer> fromSerialized(String value) {
            if (value == null) {
                return Optional.empty();
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "starter" -> Optional.of(STARTER);
                case "official" -> Optional.of(OFFICIAL);
                case "community" -> Optional.of(COMMUNITY);
                case "internal" -> Optional.of(INTERNAL);
                case "reject" -> Optional.of(REJECT);
                case "bind" -> Optional.of(BIND);
                case "compat" -> Optional.of(COMPAT);
                default -> Optional.empty();
            };
        }
    }

    enum MolangCorpusEvidence {
        OFFICIAL,
        COMMUNITY,
        IMPLEMENTATION_SURVEY,
        INTERNAL;

        static Optional<MolangCorpusEvidence> fromSerialized(String value) {
            if (value == null) {
                return Optional.empty();
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "official" -> Optional.of(OFFICIAL);
                case "community" -> Optional.of(COMMUNITY);
                case "implementation-survey" -> Optional.of(IMPLEMENTATION_SURVEY);
                case "internal" -> Optional.of(INTERNAL);
                default -> Optional.empty();
            };
        }
    }

    enum MolangDiagnosticsMode {
        NORMAL,
        STRICT,
        DEBUG;

        static Optional<MolangDiagnosticsMode> fromSerialized(String value) {
            if (value == null) {
                return Optional.empty();
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "normal" -> Optional.of(NORMAL);
                case "strict" -> Optional.of(STRICT);
                case "debug" -> Optional.of(DEBUG);
                default -> Optional.empty();
            };
        }
    }

    enum MolangPhase {
        PARSE,
        BIND,
        COMPAT
    }

    enum MolangResultType {
        PASS,
        CORPUS_ERROR,
        ENGINE_FAILURE,
        ASSERTION_FAILURE,
        SKIPPED
    }

    enum MolangDiagnosticPhase {
        LEXER,
        PARSER,
        BINDER,
        COMPAT;

        static Optional<MolangDiagnosticPhase> fromSerialized(String value) {
            if (value == null) {
                return Optional.empty();
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "lexer" -> Optional.of(LEXER);
                case "parser" -> Optional.of(PARSER);
                case "binder" -> Optional.of(BINDER);
                case "compat" -> Optional.of(COMPAT);
                default -> Optional.empty();
            };
        }
    }

    enum MolangDiagnosticSeverity {
        ERROR,
        WARNING,
        INFO;

        static Optional<MolangDiagnosticSeverity> fromSerialized(String value) {
            if (value == null) {
                return Optional.empty();
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "error" -> Optional.of(ERROR);
                case "warning" -> Optional.of(WARNING);
                case "info" -> Optional.of(INFO);
                default -> Optional.empty();
            };
        }
    }

    record MolangDiagnostic(
            MolangDiagnosticPhase phase,
            MolangDiagnosticSeverity severity,
            String code,
            String message
    ) {
    }

    record MolangExpectedDiagnostic(
            MolangDiagnosticPhase phase,
            MolangDiagnosticSeverity severity,
            String code,
            String messageContains
    ) {
    }

    record MolangCorpusCase(
            String id,
            Path filePath,
            MolangCorpusLayer layer,
            MolangCorpusEvidence evidence,
            Set<MolangAssertionType> assertions,
            String source,
            MolangDiagnosticsMode diagnosticsMode,
            String policyPack,
            List<String> notes,
            List<MolangExpectedDiagnostic> expectedDiagnostics
    ) {
        MolangCorpusCase {
            assertions = Collections.unmodifiableSet(EnumSet.copyOf(assertions));
            notes = List.copyOf(notes);
            expectedDiagnostics = List.copyOf(expectedDiagnostics);
        }

        List<MolangPhase> effectivePhases() {
            EnumSet<MolangPhase> phases = EnumSet.noneOf(MolangPhase.class);
            for (MolangAssertionType assertion : assertions) {
                switch (assertion) {
                    case PARSE_ACCEPT, PARSE_REJECT -> phases.add(MolangPhase.PARSE);
                    case BIND_NORMALIZE -> {
                        phases.add(MolangPhase.PARSE);
                        phases.add(MolangPhase.BIND);
                    }
                    case BIND_DIAGNOSTICS, DEBUG_TRACE -> {
                        // marker-only assertions
                    }
                    case COMPAT_BEHAVIOR -> {
                        phases.add(MolangPhase.PARSE);
                        phases.add(MolangPhase.BIND);
                        phases.add(MolangPhase.COMPAT);
                    }
                    case DEFERRED_NOTE -> {
                        // marker-only assertion
                    }
                }
            }
            return List.copyOf(phases);
        }

        Path adjacentParseGoldenPath() {
            String baseName = filePath.getFileName().toString();
            if (baseName.endsWith(CASE_EXTENSION)) {
                baseName = baseName.substring(0, baseName.length() - CASE_EXTENSION.length());
            }
            return filePath.getParent().resolve(baseName + PARSE_GOLDEN_EXTENSION);
        }

        Path adjacentDiagnosticsGoldenPath() {
            String baseName = filePath.getFileName().toString();
            if (baseName.endsWith(CASE_EXTENSION)) {
                baseName = baseName.substring(0, baseName.length() - CASE_EXTENSION.length());
            }
            return filePath.getParent().resolve(baseName + DIAGNOSTICS_GOLDEN_EXTENSION);
        }

        List<Path> adjacentExpectationCandidates() {
            String baseName = filePath.getFileName().toString();
            if (baseName.endsWith(CASE_EXTENSION)) {
                baseName = baseName.substring(0, baseName.length() - CASE_EXTENSION.length());
            }
            Path parent = filePath.getParent();
            List<Path> candidates = new ArrayList<>();
            candidates.add(parent.resolve(baseName + PARSE_GOLDEN_EXTENSION));
            candidates.add(parent.resolve(baseName + BIND_GOLDEN_EXTENSION));
            candidates.add(parent.resolve(baseName + DIAGNOSTICS_GOLDEN_EXTENSION));
            candidates.add(parent.resolve(baseName + DEBUG_TRACE_GOLDEN_EXTENSION));
            return List.copyOf(candidates);
        }
    }

    record MolangParseShape(String root, Set<String> contains) {
        MolangParseShape {
            contains = Set.copyOf(contains);
        }
    }

    record MolangParseGoldenExpectation(Path filePath, String root, List<String> contains) {
        MolangParseGoldenExpectation {
            contains = List.copyOf(contains);
        }
    }

    record MolangBindShape(String root, Set<String> contains, Set<String> diagnosticCodes) {
        MolangBindShape {
            contains = Set.copyOf(contains);
            diagnosticCodes = Set.copyOf(diagnosticCodes);
        }
    }

    record MolangBindGoldenExpectation(
            Path filePath,
            String root,
            List<String> contains,
            List<String> diagnosticCodes
    ) {
        MolangBindGoldenExpectation {
            contains = List.copyOf(contains);
            diagnosticCodes = List.copyOf(diagnosticCodes);
        }
    }

    record MolangCorpusIssue(Path filePath, String caseId, String message) {
        List<MolangPhase> effectivePhases() {
            return List.of(MolangPhase.PARSE);
        }
    }

    record MolangParseResult(
            boolean hadErrors,
            List<MolangDiagnostic> diagnostics,
            MolangParseShape parseShape,
            Optional<MolangAst.ExprSet> ast
    ) {
        MolangParseResult {
            diagnostics = List.copyOf(diagnostics);
        }
    }

    record MolangCaseReport(
            String caseId,
            Path filePath,
            List<MolangPhase> effectivePhases,
            MolangDiagnosticsMode diagnosticsMode,
            String policyPack,
            MolangResultType resultType,
            List<String> details,
            List<MolangDiagnostic> diagnostics
    ) {
        MolangCaseReport {
            effectivePhases = List.copyOf(effectivePhases);
            details = List.copyOf(details);
            diagnostics = List.copyOf(diagnostics);
        }
    }

    record MolangRunSummary(
            int totalCases,
            int passCount,
            int corpusErrorCount,
            int engineFailureCount,
            int assertionFailureCount,
            int skippedCount
    ) {
    }

    record MolangRunReport(List<MolangCaseReport> caseReports, MolangRunSummary summary) {
        MolangRunReport {
            caseReports = List.copyOf(caseReports);
        }

        static MolangRunReport fromCases(List<MolangCaseReport> caseReports) {
            int pass = 0;
            int corpusError = 0;
            int engineFailure = 0;
            int assertionFailure = 0;
            int skipped = 0;

            for (MolangCaseReport report : caseReports) {
                switch (report.resultType()) {
                    case PASS -> pass++;
                    case CORPUS_ERROR -> corpusError++;
                    case ENGINE_FAILURE -> engineFailure++;
                    case ASSERTION_FAILURE -> assertionFailure++;
                    case SKIPPED -> skipped++;
                }
            }

            MolangRunSummary summary = new MolangRunSummary(
                    caseReports.size(),
                    pass,
                    corpusError,
                    engineFailure,
                    assertionFailure,
                    skipped
            );
            return new MolangRunReport(caseReports, summary);
        }
    }

    record MolangLoadResult(List<MolangCorpusCase> cases, List<MolangCorpusIssue> issues) {
        MolangLoadResult {
            cases = List.copyOf(cases);
            issues = List.copyOf(issues);
        }
    }
}