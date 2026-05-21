package io.github.tt432.eyelibmolang.compiler.corpus;

import io.github.tt432.eyelibmolang.compiler.binding.BindResult;
import io.github.tt432.eyelibmolang.compiler.binding.BindDiagnostic;
import io.github.tt432.eyelibmolang.compiler.binding.BindDiagnosticsMode;
import io.github.tt432.eyelibmolang.compiler.binding.BoundMolang;
import io.github.tt432.eyelibmolang.compiler.binding.MolangBinder;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangAssertionType;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangBindGoldenExpectation;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangBindShape;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangCaseReport;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangCorpusCase;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangCorpusIssue;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangParseResult;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangParseShape;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangParseGoldenExpectation;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangDiagnosticPhase;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangDiagnosticSeverity;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangDiagnosticsMode;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangResultType;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangRunReport;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangDiagnostic;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangExpectedDiagnostic;
import io.github.tt432.eyelibmolang.compiler.frontend.ast.MolangAst;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.Optional;

import static io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.DEFAULT_MODE;
import static io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.DEFAULT_POLICY_PACK;

/** @author TT432 */
final class MolangCorpusHarness {
    private final MolangCorpusLoader loader = new MolangCorpusLoader();
    private final MolangCorpusLinter linter = new MolangCorpusLinter();
    private final MolangCorpusParseRunner parseRunner = new MolangCorpusParseRunner();
    private final MolangBinder binder = new MolangBinder();

    MolangRunReport run(Path corpusRoot) {
        List<MolangCaseReport> reports = new ArrayList<>();

        MolangCorpusModel.MolangLoadResult loadResult = loader.load(corpusRoot);
        appendIssueReports(reports, loadResult.issues());

        List<MolangCorpusIssue> lintIssues = linter.lint(loadResult.cases());
        appendIssueReports(reports, lintIssues);

        Set<Path> lintedFiles = new HashSet<>();
        for (MolangCorpusIssue issue : lintIssues) {
            lintedFiles.add(issue.filePath());
        }

        for (MolangCorpusCase corpusCase : loadResult.cases()) {
            if (lintedFiles.contains(corpusCase.filePath())) {
                continue;
            }
            reports.add(runCase(corpusCase));
        }

        return MolangRunReport.fromCases(reports);
    }

    private MolangCaseReport runCase(MolangCorpusCase corpusCase) {
        boolean hasParseAccept = corpusCase.assertions().contains(MolangAssertionType.PARSE_ACCEPT);
        boolean hasParseReject = corpusCase.assertions().contains(MolangAssertionType.PARSE_REJECT);
        boolean hasBindNormalize = corpusCase.assertions().contains(MolangAssertionType.BIND_NORMALIZE);
        if (!hasParseAccept && !hasParseReject && !hasBindNormalize) {
            return new MolangCaseReport(
                    corpusCase.id(),
                    corpusCase.filePath(),
                    corpusCase.effectivePhases(),
                    corpusCase.diagnosticsMode(),
                    corpusCase.policyPack(),
                    MolangResultType.SKIPPED,
                    List.of("No parse or bind assertion in this case; execution intentionally skipped."),
                    List.of()
            );
        }

        MolangParseGoldenExpectation parseGolden = null;
        if (hasParseAccept) {
            ParseGoldenLoadResult parseGoldenLoadResult = loadParseGolden(corpusCase);
            if (parseGoldenLoadResult.issue() != null) {
                return new MolangCaseReport(
                        corpusCase.id(),
                        corpusCase.filePath(),
                        corpusCase.effectivePhases(),
                        corpusCase.diagnosticsMode(),
                        corpusCase.policyPack(),
                        MolangResultType.CORPUS_ERROR,
                        List.of(parseGoldenLoadResult.issue().message()),
                        List.of()
                );
            }
            parseGolden = parseGoldenLoadResult.expectation();
        }

        MolangBindGoldenExpectation bindGolden = null;
        MolangDiagnosticsGoldenExpectation diagnosticsGolden = null;
        MolangDebugTraceGoldenExpectation debugTraceGolden = null;
        if (hasBindNormalize) {
            BindGoldenLoadResult bindGoldenLoadResult = loadBindGolden(corpusCase);
            if (bindGoldenLoadResult.issue() != null) {
                return new MolangCaseReport(
                        corpusCase.id(),
                        corpusCase.filePath(),
                        corpusCase.effectivePhases(),
                        corpusCase.diagnosticsMode(),
                        corpusCase.policyPack(),
                        MolangResultType.CORPUS_ERROR,
                        List.of(bindGoldenLoadResult.issue().message()),
                        List.of()
                );
            }
            bindGolden = bindGoldenLoadResult.expectation();

            DiagnosticsGoldenLoadResult diagnosticsGoldenLoadResult = loadDiagnosticsGolden(corpusCase);
            if (diagnosticsGoldenLoadResult.issue() != null) {
                return new MolangCaseReport(
                        corpusCase.id(),
                        corpusCase.filePath(),
                        corpusCase.effectivePhases(),
                        corpusCase.diagnosticsMode(),
                        corpusCase.policyPack(),
                        MolangResultType.CORPUS_ERROR,
                        List.of(diagnosticsGoldenLoadResult.issue().message()),
                        List.of()
                );
            }
            diagnosticsGolden = diagnosticsGoldenLoadResult.expectation();

            DebugTraceGoldenLoadResult debugTraceGoldenLoadResult = loadDebugTraceGolden(corpusCase);
            if (debugTraceGoldenLoadResult.issue() != null) {
                return new MolangCaseReport(
                        corpusCase.id(),
                        corpusCase.filePath(),
                        corpusCase.effectivePhases(),
                        corpusCase.diagnosticsMode(),
                        corpusCase.policyPack(),
                        MolangResultType.CORPUS_ERROR,
                        List.of(debugTraceGoldenLoadResult.issue().message()),
                        List.of()
                );
            }
            debugTraceGolden = debugTraceGoldenLoadResult.expectation();
        }

        try {
            MolangParseResult parseResult = parseRunner.parseOnly(corpusCase.source());

            if (hasParseAccept && parseResult.hadErrors()) {
                return new MolangCaseReport(
                        corpusCase.id(),
                        corpusCase.filePath(),
                        corpusCase.effectivePhases(),
                        corpusCase.diagnosticsMode(),
                        corpusCase.policyPack(),
                        MolangResultType.ASSERTION_FAILURE,
                        joinWithContext("Expected parse-accept but parser reported diagnostics", parseResult.diagnostics()),
                        parseResult.diagnostics()
                );
            }

            if (hasParseReject && !parseResult.hadErrors()) {
                return new MolangCaseReport(
                        corpusCase.id(),
                        corpusCase.filePath(),
                        corpusCase.effectivePhases(),
                        corpusCase.diagnosticsMode(),
                        corpusCase.policyPack(),
                        MolangResultType.ASSERTION_FAILURE,
                        List.of("Expected parse-reject but parser completed without diagnostics."),
                        List.of()
                );
            }

            if (hasParseReject && !matchesExpectedDiagnostics(corpusCase.expectedDiagnostics(), parseResult.diagnostics())) {
                return new MolangCaseReport(
                        corpusCase.id(),
                        corpusCase.filePath(),
                        corpusCase.effectivePhases(),
                        corpusCase.diagnosticsMode(),
                        corpusCase.policyPack(),
                        MolangResultType.ASSERTION_FAILURE,
                        joinWithContext("Expected diagnostics subset did not match actual parse diagnostics", parseResult.diagnostics()),
                        parseResult.diagnostics()
                );
            }

            if (hasParseAccept && parseGolden != null) {
                List<String> shapeMismatches = parseShapeMismatches(parseGolden, parseResult.parseShape());
                if (!shapeMismatches.isEmpty()) {
                    List<String> details = new ArrayList<>();
                    details.add("Parse golden assertion failed for '" + parseGolden.filePath().getFileName() + "'.");
                    details.addAll(shapeMismatches);
                    details.add("Actual parse shape root='" + parseResult.parseShape().root() + "', contains=" + sortedRules(parseResult.parseShape()));
                    return new MolangCaseReport(
                            corpusCase.id(),
                            corpusCase.filePath(),
                            corpusCase.effectivePhases(),
                            corpusCase.diagnosticsMode(),
                            corpusCase.policyPack(),
                            MolangResultType.ASSERTION_FAILURE,
                            details,
                            parseResult.diagnostics()
                    );
                }
            }

            if (hasBindNormalize) {
                Optional<MolangAst.ExprSet> ast = parseResult.ast();
                if (ast.isEmpty()) {
                    return new MolangCaseReport(
                            corpusCase.id(),
                            corpusCase.filePath(),
                            corpusCase.effectivePhases(),
                            corpusCase.diagnosticsMode(),
                            corpusCase.policyPack(),
                            MolangResultType.ENGINE_FAILURE,
                            List.of("Active frontend did not provide AST required for bind-normalize."),
                            parseResult.diagnostics()
                    );
                }

                BindResult bindResult = binder.bind(ast.get(), toBindDiagnosticsMode(corpusCase.diagnosticsMode()));
                List<MolangDiagnostic> bindDiagnostics = collectBindDiagnostics(bindResult);
                if (bindGolden != null) {
                    List<String> bindMismatches = bindShapeMismatches(bindGolden, collectBindShape(bindResult));
                    if (!bindMismatches.isEmpty()) {
                        List<String> details = new ArrayList<>();
                        details.add("Bind golden assertion failed for '" + bindGolden.filePath().getFileName() + "'.");
                        details.addAll(bindMismatches);
                        return new MolangCaseReport(
                                corpusCase.id(),
                                corpusCase.filePath(),
                                corpusCase.effectivePhases(),
                                corpusCase.diagnosticsMode(),
                                corpusCase.policyPack(),
                                MolangResultType.ASSERTION_FAILURE,
                                details,
                                parseResult.diagnostics()
                        );
                    }
                }

                if (diagnosticsGolden != null) {
                    List<String> diagnosticsMismatches = diagnosticsMismatches(diagnosticsGolden.expectedDiagnostics(), bindDiagnostics);
                    if (!diagnosticsMismatches.isEmpty()) {
                        List<String> details = new ArrayList<>();
                        details.add("Diagnostics golden assertion failed for '" + diagnosticsGolden.filePath().getFileName() + "'.");
                        details.addAll(diagnosticsMismatches);
                        details.addAll(renderActualDiagnostics(bindDiagnostics));
                        return new MolangCaseReport(
                                corpusCase.id(),
                                corpusCase.filePath(),
                                corpusCase.effectivePhases(),
                                corpusCase.diagnosticsMode(),
                                corpusCase.policyPack(),
                                MolangResultType.ASSERTION_FAILURE,
                                details,
                                parseResult.diagnostics()
                        );
                    }
                }

                if (debugTraceGolden != null) {
                    Set<String> actualDebugTraceTokens = collectDebugTraceTokens(bindResult);
                    List<String> debugTraceMismatches = debugTraceMismatches(debugTraceGolden.expectedTraceTokens(), actualDebugTraceTokens);
                    if (!debugTraceMismatches.isEmpty()) {
                        List<String> details = new ArrayList<>();
                        details.add("Debug trace golden assertion failed for '" + debugTraceGolden.filePath().getFileName() + "'.");
                        details.addAll(debugTraceMismatches);
                        details.addAll(renderActualDebugTraceTokens(actualDebugTraceTokens));
                        return new MolangCaseReport(
                                corpusCase.id(),
                                corpusCase.filePath(),
                                corpusCase.effectivePhases(),
                                corpusCase.diagnosticsMode(),
                                corpusCase.policyPack(),
                                MolangResultType.ASSERTION_FAILURE,
                                details,
                                parseResult.diagnostics()
                        );
                    }
                }
            }

            return new MolangCaseReport(
                    corpusCase.id(),
                    corpusCase.filePath(),
                    corpusCase.effectivePhases(),
                    corpusCase.diagnosticsMode(),
                    corpusCase.policyPack(),
                    MolangResultType.PASS,
                    List.of(),
                    parseResult.diagnostics()
            );
        } catch (RuntimeException exception) {
            return new MolangCaseReport(
                    corpusCase.id(),
                    corpusCase.filePath(),
                    corpusCase.effectivePhases(),
                    corpusCase.diagnosticsMode(),
                    corpusCase.policyPack(),
                    MolangResultType.ENGINE_FAILURE,
                    List.of(exception.getClass().getSimpleName() + ": " + exception.getMessage()),
                    List.of()
            );
        }
    }

    private BindGoldenLoadResult loadBindGolden(MolangCorpusCase corpusCase) {
        Path bindGoldenPath = corpusCase.filePath()
                .getParent()
                .resolve(baseName(corpusCase.filePath()) + MolangCorpusModel.BIND_GOLDEN_EXTENSION);
        if (!Files.exists(bindGoldenPath)) {
            return new BindGoldenLoadResult(null, null);
        }
        if (!Files.isRegularFile(bindGoldenPath)) {
            return new BindGoldenLoadResult(null, new MolangCorpusIssue(
                    corpusCase.filePath(),
                    corpusCase.id(),
                    "Bind golden path exists but is not a regular file: " + bindGoldenPath
            ));
        }

        String content;
        try {
            content = Files.readString(bindGoldenPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new BindGoldenLoadResult(null, new MolangCorpusIssue(
                    corpusCase.filePath(),
                    corpusCase.id(),
                    "Failed to read bind golden '" + bindGoldenPath.getFileName() + "': " + e.getMessage()
            ));
        }

        List<String> contains = new ArrayList<>();
        List<String> diagnosticCodes = new ArrayList<>();
        String root = null;
        String currentListKey = null;
        List<String> lines = content.lines().toList();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            String trimmed = line.trim();

            if (trimmed.isBlank() || trimmed.startsWith("#")) {
                continue;
            }

            if (trimmed.startsWith("- ")) {
                if (!"contains".equals(currentListKey) && !"diagnostic-codes".equals(currentListKey)) {
                    return malformedBindGolden(corpusCase, bindGoldenPath, lineIndex + 1, "List item found outside supported list key.");
                }
                String listValue = trimmed.substring(2).trim();
                if (listValue.isBlank()) {
                    return malformedBindGolden(corpusCase, bindGoldenPath, lineIndex + 1, "List item cannot be blank.");
                }
                if ("contains".equals(currentListKey)) {
                    contains.add(listValue);
                } else {
                    diagnosticCodes.add(listValue);
                }
                continue;
            }

            int colonIndex = trimmed.indexOf(':');
            if (colonIndex <= 0) {
                return malformedBindGolden(corpusCase, bindGoldenPath, lineIndex + 1, "Malformed line; expected 'key: value'.");
            }

            String key = trimmed.substring(0, colonIndex).trim().toLowerCase(Locale.ROOT);
            String value = trimmed.substring(colonIndex + 1).trim();
            if (!"root".equals(key) && !"contains".equals(key) && !"diagnostic-codes".equals(key)) {
                return malformedBindGolden(corpusCase, bindGoldenPath, lineIndex + 1, "Unsupported key '" + key + "'.");
            }

            if ("root".equals(key)) {
                if (value.isBlank()) {
                    return malformedBindGolden(corpusCase, bindGoldenPath, lineIndex + 1, "Field 'root' must provide a value.");
                }
                root = value;
                currentListKey = null;
                continue;
            }

            if (value.isBlank()) {
                currentListKey = key;
            } else {
                if ("contains".equals(key)) {
                    contains.add(value);
                } else {
                    diagnosticCodes.add(value);
                }
                currentListKey = null;
            }
        }

        if (root == null || root.isBlank()) {
            return new BindGoldenLoadResult(null, new MolangCorpusIssue(
                    corpusCase.filePath(),
                    corpusCase.id(),
                    "Bind golden '" + bindGoldenPath.getFileName() + "' is missing required field 'root'."
            ));
        }

        return new BindGoldenLoadResult(
                new MolangBindGoldenExpectation(bindGoldenPath, root, contains, diagnosticCodes),
                null
        );
    }

    private ParseGoldenLoadResult loadParseGolden(MolangCorpusCase corpusCase) {
        Path parseGoldenPath = corpusCase.adjacentParseGoldenPath();
        if (!Files.exists(parseGoldenPath)) {
            return new ParseGoldenLoadResult(null, null);
        }
        if (!Files.isRegularFile(parseGoldenPath)) {
            return new ParseGoldenLoadResult(null, new MolangCorpusIssue(
                    corpusCase.filePath(),
                    corpusCase.id(),
                    "Parse golden path exists but is not a regular file: " + parseGoldenPath
            ));
        }

        String content;
        try {
            content = Files.readString(parseGoldenPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new ParseGoldenLoadResult(null, new MolangCorpusIssue(
                    corpusCase.filePath(),
                    corpusCase.id(),
                    "Failed to read parse golden '" + parseGoldenPath.getFileName() + "': " + e.getMessage()
            ));
        }

        List<String> contains = new ArrayList<>();
        String root = null;
        String currentListKey = null;

        List<String> lines = content.lines().toList();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            String trimmed = line.trim();

            if (trimmed.isBlank() || trimmed.startsWith("#")) {
                continue;
            }

            if (trimmed.startsWith("- ")) {
                if (!"contains".equals(currentListKey)) {
                    return malformedParseGolden(corpusCase, parseGoldenPath, lineIndex + 1, "List item found outside 'contains' list.");
                }
                String listValue = trimmed.substring(2).trim();
                if (listValue.isBlank()) {
                    return malformedParseGolden(corpusCase, parseGoldenPath, lineIndex + 1, "contains list item cannot be blank.");
                }
                contains.add(listValue);
                continue;
            }

            int colonIndex = trimmed.indexOf(':');
            if (colonIndex <= 0) {
                return malformedParseGolden(corpusCase, parseGoldenPath, lineIndex + 1, "Malformed line; expected 'key: value'.");
            }

            String key = trimmed.substring(0, colonIndex).trim().toLowerCase(Locale.ROOT);
            String value = trimmed.substring(colonIndex + 1).trim();

            if (!"root".equals(key) && !"contains".equals(key)) {
                return malformedParseGolden(corpusCase, parseGoldenPath, lineIndex + 1, "Unsupported key '" + key + "'.");
            }

            if ("root".equals(key)) {
                if (value.isBlank()) {
                    return malformedParseGolden(corpusCase, parseGoldenPath, lineIndex + 1, "Field 'root' must provide a value.");
                }
                root = value;
                currentListKey = null;
                continue;
            }

            if (value.isBlank()) {
                currentListKey = "contains";
            } else {
                contains.add(value);
                currentListKey = null;
            }
        }

        if (root == null || root.isBlank()) {
            return new ParseGoldenLoadResult(null, new MolangCorpusIssue(
                    corpusCase.filePath(),
                    corpusCase.id(),
                    "Parse golden '" + parseGoldenPath.getFileName() + "' is missing required field 'root'."
            ));
        }

        return new ParseGoldenLoadResult(new MolangParseGoldenExpectation(parseGoldenPath, root, contains), null);
    }

    private ParseGoldenLoadResult malformedParseGolden(MolangCorpusCase corpusCase,
                                                       Path parseGoldenPath,
                                                       int line,
                                                       String reason) {
        return new ParseGoldenLoadResult(null, new MolangCorpusIssue(
                corpusCase.filePath(),
                corpusCase.id(),
                "Malformed parse golden '" + parseGoldenPath.getFileName() + "' at line " + line + ": " + reason
        ));
    }

    private DiagnosticsGoldenLoadResult loadDiagnosticsGolden(MolangCorpusCase corpusCase) {
        Path diagnosticsGoldenPath = corpusCase.adjacentDiagnosticsGoldenPath();
        if (!Files.exists(diagnosticsGoldenPath)) {
            return new DiagnosticsGoldenLoadResult(null, null);
        }
        if (!Files.isRegularFile(diagnosticsGoldenPath)) {
            return new DiagnosticsGoldenLoadResult(null, new MolangCorpusIssue(
                    corpusCase.filePath(),
                    corpusCase.id(),
                    "Diagnostics golden path exists but is not a regular file: " + diagnosticsGoldenPath
            ));
        }

        String content;
        try {
            content = Files.readString(diagnosticsGoldenPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new DiagnosticsGoldenLoadResult(null, new MolangCorpusIssue(
                    corpusCase.filePath(),
                    corpusCase.id(),
                    "Failed to read diagnostics golden '" + diagnosticsGoldenPath.getFileName() + "': " + e.getMessage()
            ));
        }

        String currentListKey = null;
        boolean declaredDiagnostics = false;
        List<HashMap<String, String>> diagnosticEntries = new ArrayList<>();
        HashMap<String, String> currentDiagnosticEntry = null;

        List<String> lines = content.lines().toList();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            String trimmed = line.trim();

            if (trimmed.isBlank() || trimmed.startsWith("#")) {
                continue;
            }

            if (trimmed.startsWith("- ")) {
                if (!"diagnostics".equals(currentListKey)) {
                    return malformedDiagnosticsGolden(corpusCase, diagnosticsGoldenPath, lineIndex + 1, "List item found outside 'diagnostics' list.");
                }
                String inline = trimmed.substring(2).trim();
                currentDiagnosticEntry = new HashMap<>();
                diagnosticEntries.add(currentDiagnosticEntry);
                if (!inline.isBlank()) {
                    int inlineColonIndex = inline.indexOf(':');
                    if (inlineColonIndex <= 0) {
                        return malformedDiagnosticsGolden(corpusCase, diagnosticsGoldenPath, lineIndex + 1, "Malformed diagnostics entry; expected 'key: value'.");
                    }
                    currentDiagnosticEntry.put(
                            inline.substring(0, inlineColonIndex).trim().toLowerCase(Locale.ROOT),
                            inline.substring(inlineColonIndex + 1).trim()
                    );
                }
                continue;
            }

            boolean indented = !line.isEmpty() && Character.isWhitespace(line.charAt(0));
            if (indented && "diagnostics".equals(currentListKey) && currentDiagnosticEntry != null) {
                int nestedColonIndex = trimmed.indexOf(':');
                if (nestedColonIndex <= 0) {
                    return malformedDiagnosticsGolden(corpusCase, diagnosticsGoldenPath, lineIndex + 1, "Malformed diagnostics field; expected 'key: value'.");
                }
                currentDiagnosticEntry.put(
                        trimmed.substring(0, nestedColonIndex).trim().toLowerCase(Locale.ROOT),
                        trimmed.substring(nestedColonIndex + 1).trim()
                );
                continue;
            }

            int colonIndex = trimmed.indexOf(':');
            if (colonIndex <= 0) {
                return malformedDiagnosticsGolden(corpusCase, diagnosticsGoldenPath, lineIndex + 1, "Malformed line; expected 'key: value'.");
            }

            String key = trimmed.substring(0, colonIndex).trim().toLowerCase(Locale.ROOT);
            String value = trimmed.substring(colonIndex + 1).trim();
            if (!"diagnostics".equals(key)) {
                return malformedDiagnosticsGolden(corpusCase, diagnosticsGoldenPath, lineIndex + 1, "Unsupported key '" + key + "'.");
            }

            declaredDiagnostics = true;
            currentDiagnosticEntry = null;
            if (value.isBlank()) {
                currentListKey = "diagnostics";
                continue;
            }

            if (!"[]".equals(value)) {
                return malformedDiagnosticsGolden(corpusCase, diagnosticsGoldenPath, lineIndex + 1, "Field 'diagnostics' must be a list or [].");
            }
            currentListKey = null;
        }

        if (!declaredDiagnostics) {
            return new DiagnosticsGoldenLoadResult(null, new MolangCorpusIssue(
                    corpusCase.filePath(),
                    corpusCase.id(),
                    "Diagnostics golden '" + diagnosticsGoldenPath.getFileName() + "' is missing required field 'diagnostics'."
            ));
        }

        List<MolangExpectedDiagnostic> expectedDiagnostics = new ArrayList<>();
        for (HashMap<String, String> entry : diagnosticEntries) {
            Optional<MolangDiagnosticPhase> phase = MolangDiagnosticPhase.fromSerialized(entry.get("phase"));
            Optional<MolangDiagnosticSeverity> severity = MolangDiagnosticSeverity.fromSerialized(entry.get("severity"));
            String code = entry.get("code");
            if (phase.isEmpty() || severity.isEmpty() || code == null || code.isBlank()) {
                return new DiagnosticsGoldenLoadResult(null, new MolangCorpusIssue(
                        corpusCase.filePath(),
                        corpusCase.id(),
                        "Each diagnostics golden entry must define phase, severity, and code."
                ));
            }

            String messageContains = Optional.ofNullable(entry.get("message-contains"))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .orElse(null);

            expectedDiagnostics.add(new MolangExpectedDiagnostic(
                    phase.get(),
                    severity.get(),
                    code.trim(),
                    messageContains
            ));
        }

        return new DiagnosticsGoldenLoadResult(
                new MolangDiagnosticsGoldenExpectation(diagnosticsGoldenPath, List.copyOf(expectedDiagnostics)),
                null
        );
    }

    private DebugTraceGoldenLoadResult loadDebugTraceGolden(MolangCorpusCase corpusCase) {
        Path debugTraceGoldenPath = corpusCase.filePath()
                .getParent()
                .resolve(baseName(corpusCase.filePath()) + MolangCorpusModel.DEBUG_TRACE_GOLDEN_EXTENSION);
        if (!Files.exists(debugTraceGoldenPath)) {
            return new DebugTraceGoldenLoadResult(null, null);
        }
        if (!Files.isRegularFile(debugTraceGoldenPath)) {
            return new DebugTraceGoldenLoadResult(null, new MolangCorpusIssue(
                    corpusCase.filePath(),
                    corpusCase.id(),
                    "Debug trace golden path exists but is not a regular file: " + debugTraceGoldenPath
            ));
        }

        String content;
        try {
            content = Files.readString(debugTraceGoldenPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new DebugTraceGoldenLoadResult(null, new MolangCorpusIssue(
                    corpusCase.filePath(),
                    corpusCase.id(),
                    "Failed to read debug trace golden '" + debugTraceGoldenPath.getFileName() + "': " + e.getMessage()
            ));
        }

        String currentListKey = null;
        boolean declaredTrace = false;
        List<String> expectedTraceTokens = new ArrayList<>();

        List<String> lines = content.lines().toList();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            String trimmed = line.trim();

            if (trimmed.isBlank() || trimmed.startsWith("#")) {
                continue;
            }

            if (trimmed.startsWith("- ")) {
                if (!"trace".equals(currentListKey)) {
                    return malformedDebugTraceGolden(corpusCase, debugTraceGoldenPath, lineIndex + 1, "List item found outside 'trace' list.");
                }
                String traceToken = trimmed.substring(2).trim();
                if (traceToken.isBlank()) {
                    return malformedDebugTraceGolden(corpusCase, debugTraceGoldenPath, lineIndex + 1, "Trace token cannot be blank.");
                }
                expectedTraceTokens.add(traceToken);
                continue;
            }

            int colonIndex = trimmed.indexOf(':');
            if (colonIndex <= 0) {
                return malformedDebugTraceGolden(corpusCase, debugTraceGoldenPath, lineIndex + 1, "Malformed line; expected 'key: value'.");
            }

            String key = trimmed.substring(0, colonIndex).trim().toLowerCase(Locale.ROOT);
            String value = trimmed.substring(colonIndex + 1).trim();
            if (!"trace".equals(key)) {
                return malformedDebugTraceGolden(corpusCase, debugTraceGoldenPath, lineIndex + 1, "Unsupported key '" + key + "'.");
            }

            declaredTrace = true;
            if (value.isBlank()) {
                currentListKey = "trace";
                continue;
            }

            if ("[]".equals(value)) {
                currentListKey = null;
                continue;
            }

            expectedTraceTokens.add(value);
            currentListKey = null;
        }

        if (!declaredTrace) {
            return new DebugTraceGoldenLoadResult(null, new MolangCorpusIssue(
                    corpusCase.filePath(),
                    corpusCase.id(),
                    "Debug trace golden '" + debugTraceGoldenPath.getFileName() + "' is missing required field 'trace'."
            ));
        }

        return new DebugTraceGoldenLoadResult(
                new MolangDebugTraceGoldenExpectation(debugTraceGoldenPath, List.copyOf(expectedTraceTokens)),
                null
        );
    }

    private DiagnosticsGoldenLoadResult malformedDiagnosticsGolden(MolangCorpusCase corpusCase,
                                                                   Path diagnosticsGoldenPath,
                                                                   int line,
                                                                   String reason) {
        return new DiagnosticsGoldenLoadResult(null, new MolangCorpusIssue(
                corpusCase.filePath(),
                corpusCase.id(),
                "Malformed diagnostics golden '" + diagnosticsGoldenPath.getFileName() + "' at line " + line + ": " + reason
        ));
    }

    private DebugTraceGoldenLoadResult malformedDebugTraceGolden(MolangCorpusCase corpusCase,
                                                                 Path debugTraceGoldenPath,
                                                                 int line,
                                                                 String reason) {
        return new DebugTraceGoldenLoadResult(null, new MolangCorpusIssue(
                corpusCase.filePath(),
                corpusCase.id(),
                "Malformed debug trace golden '" + debugTraceGoldenPath.getFileName() + "' at line " + line + ": " + reason
        ));
    }

    private BindGoldenLoadResult malformedBindGolden(MolangCorpusCase corpusCase,
                                                     Path bindGoldenPath,
                                                     int line,
                                                     String reason) {
        return new BindGoldenLoadResult(null, new MolangCorpusIssue(
                corpusCase.filePath(),
                corpusCase.id(),
                "Malformed bind golden '" + bindGoldenPath.getFileName() + "' at line " + line + ": " + reason
        ));
    }

    private List<MolangDiagnostic> collectBindDiagnostics(BindResult bindResult) {
        List<MolangDiagnostic> diagnostics = new ArrayList<>();
        bindResult.diagnostics().forEach(diagnostic -> diagnostics.add(new MolangDiagnostic(
                MolangDiagnosticPhase.BINDER,
                mapSeverity(diagnostic.severity()),
                diagnostic.code(),
                diagnostic.message()
        )));
        return List.copyOf(diagnostics);
    }

    private MolangDiagnosticSeverity mapSeverity(BindDiagnostic.Severity severity) {
        return switch (severity) {
            case ERROR -> MolangDiagnosticSeverity.ERROR;
            case WARNING -> MolangDiagnosticSeverity.WARNING;
            case INFO -> MolangDiagnosticSeverity.INFO;
        };
    }

    private BindDiagnosticsMode toBindDiagnosticsMode(MolangDiagnosticsMode diagnosticsMode) {
        return switch (diagnosticsMode) {
            case NORMAL -> BindDiagnosticsMode.NORMAL;
            case STRICT -> BindDiagnosticsMode.STRICT;
            case DEBUG -> BindDiagnosticsMode.DEBUG;
        };
    }

    private List<String> diagnosticsMismatches(List<MolangExpectedDiagnostic> expectedDiagnostics,
                                               List<MolangDiagnostic> actualDiagnostics) {
        List<String> mismatches = new ArrayList<>();
        for (MolangExpectedDiagnostic expectedDiagnostic : expectedDiagnostics) {
            boolean matched = actualDiagnostics.stream().anyMatch(actual ->
                    actual.phase() == expectedDiagnostic.phase()
                    && actual.severity() == expectedDiagnostic.severity()
                    && actual.code().equals(expectedDiagnostic.code())
                    && (expectedDiagnostic.messageContains() == null
                        || actual.message().contains(expectedDiagnostic.messageContains()))
            );
            if (!matched) {
                String messageFragment = expectedDiagnostic.messageContains() == null
                        ? ""
                        : " and message containing '" + expectedDiagnostic.messageContains() + "'";
                mismatches.add("Expected diagnostics to include "
                               + expectedDiagnostic.phase() + "/"
                               + expectedDiagnostic.severity() + "/"
                               + expectedDiagnostic.code()
                               + messageFragment
                               + ".");
            }
        }
        return mismatches;
    }

    private List<String> renderActualDiagnostics(List<MolangDiagnostic> diagnostics) {
        List<String> details = new ArrayList<>();
        details.add("Actual bind diagnostics:");
        for (MolangDiagnostic diagnostic : diagnostics) {
            details.add(diagnostic.phase() + "/" + diagnostic.severity() + "/" + diagnostic.code() + ": " + diagnostic.message());
        }
        return details;
    }

    private Set<String> collectDebugTraceTokens(BindResult bindResult) {
        Set<String> tokens = new HashSet<>();
        bindResult.diagnostics().forEach(diagnostic -> {
            tokens.add("diagnostic-code:" + diagnostic.code());
            tokens.add("diagnostic-severity:" + diagnostic.severity().name());
        });
        bindResult.deferredNotes().forEach(note -> {
            tokens.add("deferred-note-reason:" + note.reason().name());
            tokens.add("deferred-note-source-family:" + note.sourceFamily());
        });
        return Set.copyOf(tokens);
    }

    private List<String> debugTraceMismatches(List<String> expectedTraceTokens, Set<String> actualTraceTokens) {
        List<String> mismatches = new ArrayList<>();
        for (String expectedTraceToken : expectedTraceTokens) {
            if (!actualTraceTokens.contains(expectedTraceToken)) {
                mismatches.add("Expected debug trace to contain token '" + expectedTraceToken + "'.");
            }
        }
        return mismatches;
    }

    private List<String> renderActualDebugTraceTokens(Set<String> actualTraceTokens) {
        List<String> sorted = new ArrayList<>(actualTraceTokens);
        Collections.sort(sorted);

        List<String> details = new ArrayList<>();
        details.add("Actual debug trace tokens:");
        for (String token : sorted) {
            details.add(token);
        }
        return details;
    }

    MolangBindShape collectBindShape(BindResult bindResult) {
        Set<String> contains = new HashSet<>();
        collectBindTokens(bindResult.root().root(), contains);
        bindResult.deferredNotes().forEach(note -> contains.add("deferred-note-reason:" + note.reason().name()));

        Set<String> diagnosticCodes = new HashSet<>();
        bindResult.diagnostics().forEach(diagnostic -> diagnosticCodes.add(diagnostic.code()));

        return new MolangBindShape(bindResult.root().root().getClass().getSimpleName(), contains, diagnosticCodes);
    }

    private void collectBindTokens(BoundMolang.BoundExpr expression, Set<String> contains) {
        contains.add(expression.getClass().getSimpleName());

        if (expression instanceof BoundMolang.BoundIdentifierExpr identifierExpr) {
            contains.add("id:" + identifierExpr.name());
            return;
        }
        if (expression instanceof BoundMolang.BoundMemberAccessExpr memberAccessExpr) {
            contains.add("access:dot");
            collectBindTokens(memberAccessExpr.owner(), contains);
            return;
        }
        if (expression instanceof BoundMolang.BoundArrowAccessExpr arrowAccessExpr) {
            contains.add("access:arrow");
            collectBindTokens(arrowAccessExpr.left(), contains);
            collectBindTokens(arrowAccessExpr.right(), contains);
            return;
        }
        if (expression instanceof BoundMolang.BoundCallExpr callExpr) {
            collectBindTokens(callExpr.callee(), contains);
            for (BoundMolang.BoundExpr argument : callExpr.arguments()) {
                collectBindTokens(argument, contains);
            }
            return;
        }
        if (expression instanceof BoundMolang.BoundIndexExpr indexExpr) {
            collectBindTokens(indexExpr.owner(), contains);
            collectBindTokens(indexExpr.index(), contains);
            return;
        }
        if (expression instanceof BoundMolang.BoundQueryAccessExpr queryAccessExpr) {
            contains.add("query:" + queryAccessExpr.projectionKind().name());
            collectBindTokens(queryAccessExpr.access(), contains);
            return;
        }
        if (expression instanceof BoundMolang.BoundAssignmentExpr assignmentExpr) {
            assignmentExpr.targetRoot().ifPresent(root -> contains.add("assign-root:" + root));
            contains.add("assign-writable:" + assignmentExpr.writableTarget());
            collectBindTokens(assignmentExpr.target(), contains);
            collectBindTokens(assignmentExpr.value(), contains);
            return;
        }
        if (expression instanceof BoundMolang.BoundBlockExpr blockExpr) {
            for (BoundMolang.BoundStmt statement : blockExpr.statements()) {
                if (statement instanceof BoundMolang.BoundExprStmt exprStmt) {
                    collectBindTokens(exprStmt.expression(), contains);
                } else if (statement instanceof BoundMolang.BoundReturnStmt returnStmt) {
                    contains.add("stmt:return");
                    collectBindTokens(returnStmt.expression(), contains);
                } else if (statement instanceof BoundMolang.BoundBreakStmt breakStmt) {
                    contains.add("stmt:break");
                    contains.add("stmt:break:deferred-reason:" + breakStmt.deferredReason().name());
                } else if (statement instanceof BoundMolang.BoundContinueStmt continueStmt) {
                    contains.add("stmt:continue");
                    contains.add("stmt:continue:deferred-reason:" + continueStmt.deferredReason().name());
                }
            }
            return;
        }
        if (expression instanceof BoundMolang.BoundLoopExpr loopExpr) {
            contains.add("loop-iteration-count-raw:" + loopExpr.iterationCountRawText());
            collectBindTokens(loopExpr.body(), contains);
            return;
        }
        if (expression instanceof BoundMolang.BoundForEachExpr forEachExpr) {
            contains.add("foreach:deferred-reason:" + forEachExpr.deferredReason().name());
            collectBindTokens(forEachExpr.variable(), contains);
            collectBindTokens(forEachExpr.collection(), contains);
            collectBindTokens(forEachExpr.body(), contains);
            return;
        }
        if (expression instanceof BoundMolang.BoundUnaryExpr unaryExpr) {
            collectBindTokens(unaryExpr.expression(), contains);
            return;
        }
        if (expression instanceof BoundMolang.BoundBinaryExpr binaryExpr) {
            collectBindTokens(binaryExpr.left(), contains);
            collectBindTokens(binaryExpr.right(), contains);
            return;
        }
        if (expression instanceof BoundMolang.BoundGroupingExpr groupingExpr) {
            collectBindTokens(groupingExpr.expression(), contains);
            return;
        }
        if (expression instanceof BoundMolang.BoundNullCoalesceExpr nullCoalesceExpr) {
            collectBindTokens(nullCoalesceExpr.left(), contains);
            collectBindTokens(nullCoalesceExpr.right(), contains);
        }
    }

    private List<String> bindShapeMismatches(MolangBindGoldenExpectation expected, MolangBindShape actual) {
        List<String> mismatches = new ArrayList<>();
        if (!expected.root().equals(actual.root())) {
            mismatches.add("Expected bind root '" + expected.root() + "' but was '" + actual.root() + "'.");
        }
        for (String token : expected.contains()) {
            if (!actual.contains().contains(token)) {
                mismatches.add("Expected bind shape to contain token '" + token + "'.");
            }
        }
        for (String code : expected.diagnosticCodes()) {
            if (!actual.diagnosticCodes().contains(code)) {
                mismatches.add("Expected bind diagnostics to include code '" + code + "'.");
            }
        }
        return mismatches;
    }

    private String baseName(Path filePath) {
        String name = filePath.getFileName().toString();
        if (name.endsWith(MolangCorpusModel.CASE_EXTENSION)) {
            return name.substring(0, name.length() - MolangCorpusModel.CASE_EXTENSION.length());
        }
        return name;
    }

    private List<String> parseShapeMismatches(MolangParseGoldenExpectation expected, MolangParseShape actual) {
        List<String> mismatches = new ArrayList<>();
        if (!expected.root().equals(actual.root())) {
            mismatches.add("Expected parse root '" + expected.root() + "' but was '" + actual.root() + "'.");
        }
        for (String expectedRule : expected.contains()) {
            if (!actual.contains().contains(expectedRule)) {
                mismatches.add("Expected parse shape to contain rule '" + expectedRule + "'.");
            }
        }
        return mismatches;
    }

    private List<String> sortedRules(MolangParseShape parseShape) {
        List<String> sorted = new ArrayList<>(parseShape.contains());
        Collections.sort(sorted);
        return List.copyOf(sorted);
    }

    private boolean matchesExpectedDiagnostics(List<MolangExpectedDiagnostic> expectedDiagnostics,
                                               List<MolangDiagnostic> actualDiagnostics) {
        return diagnosticsMismatches(expectedDiagnostics, actualDiagnostics).isEmpty();
    }

    private List<String> joinWithContext(String first, List<MolangDiagnostic> diagnostics) {
        List<String> details = new ArrayList<>();
        details.add(first);
        for (MolangDiagnostic diagnostic : diagnostics) {
            details.add(diagnostic.phase() + "/" + diagnostic.severity() + "/" + diagnostic.code() + ": " + diagnostic.message());
        }
        return details;
    }

    private void appendIssueReports(List<MolangCaseReport> reports, List<MolangCorpusIssue> issues) {
        for (MolangCorpusIssue issue : issues) {
            reports.add(new MolangCaseReport(
                    issue.caseId(),
                    issue.filePath(),
                    issue.effectivePhases(),
                    DEFAULT_MODE,
                    DEFAULT_POLICY_PACK,
                    MolangResultType.CORPUS_ERROR,
                    List.of(issue.message()),
                    List.of()
            ));
        }
    }

    private record ParseGoldenLoadResult(MolangParseGoldenExpectation expectation, MolangCorpusIssue issue) {
    }

    private record BindGoldenLoadResult(MolangBindGoldenExpectation expectation, MolangCorpusIssue issue) {
    }

    private record DiagnosticsGoldenLoadResult(MolangDiagnosticsGoldenExpectation expectation, MolangCorpusIssue issue) {
    }

    private record DebugTraceGoldenLoadResult(MolangDebugTraceGoldenExpectation expectation, MolangCorpusIssue issue) {
    }

    private record MolangDiagnosticsGoldenExpectation(Path filePath, List<MolangExpectedDiagnostic> expectedDiagnostics) {
    }

    private record MolangDebugTraceGoldenExpectation(Path filePath, List<String> expectedTraceTokens) {
    }
}