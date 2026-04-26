package io.github.tt432.eyelibmolang.compiler.corpus;

import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangCorpusCase;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangCorpusIssue;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangAssertionType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.BIND_GOLDEN_EXTENSION;
import static io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.DEBUG_TRACE_GOLDEN_EXTENSION;
import static io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.DIAGNOSTICS_GOLDEN_EXTENSION;
import static io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.PARSE_GOLDEN_EXTENSION;

final class MolangCorpusLinter {
    List<MolangCorpusIssue> lint(List<MolangCorpusCase> cases) {
        List<MolangCorpusIssue> issues = new ArrayList<>();

        Map<String, List<MolangCorpusCase>> byId = new HashMap<>();
        for (MolangCorpusCase corpusCase : cases) {
            if (corpusCase.id().isBlank()) {
                issues.add(new MolangCorpusIssue(corpusCase.filePath(), "<unknown>", "Missing required metadata field 'id'."));
                continue;
            }
            byId.computeIfAbsent(corpusCase.id(), ignored -> new ArrayList<>()).add(corpusCase);

            if (corpusCase.assertions().isEmpty()) {
                issues.add(new MolangCorpusIssue(corpusCase.filePath(), corpusCase.id(), "Missing required metadata field 'assertions'."));
            }
            if (corpusCase.source().isBlank()) {
                issues.add(new MolangCorpusIssue(corpusCase.filePath(), corpusCase.id(), "Case source body is blank."));
            }
            if (corpusCase.assertions().contains(MolangCorpusModel.MolangAssertionType.PARSE_REJECT)
                && corpusCase.expectedDiagnostics().isEmpty()) {
                issues.add(new MolangCorpusIssue(
                        corpusCase.filePath(),
                        corpusCase.id(),
                        "parse-reject cases must declare at least one expected diagnostic."
                ));
            }

            lintAdjacentGoldens(corpusCase, issues);
        }

        for (Map.Entry<String, List<MolangCorpusCase>> entry : byId.entrySet()) {
            List<MolangCorpusCase> sameId = entry.getValue();
            if (sameId.size() > 1) {
                for (MolangCorpusCase corpusCase : sameId) {
                    issues.add(new MolangCorpusIssue(
                            corpusCase.filePath(),
                            corpusCase.id(),
                            "Duplicate case id '" + entry.getKey() + "'."
                    ));
                }
            }
        }

        return issues;
    }

    private void lintAdjacentGoldens(MolangCorpusCase corpusCase, List<MolangCorpusIssue> issues) {
        boolean parseAccept = corpusCase.assertions().contains(MolangAssertionType.PARSE_ACCEPT);
        boolean bindNormalize = corpusCase.assertions().contains(MolangAssertionType.BIND_NORMALIZE);
        if (bindNormalize && !parseAccept) {
            issues.add(new MolangCorpusIssue(
                    corpusCase.filePath(),
                    corpusCase.id(),
                    "'bind-normalize' cases must also declare 'parse-accept' in this slice."
            ));
        }

        for (Path candidate : corpusCase.adjacentExpectationCandidates()) {
            if (!Files.exists(candidate)) {
                continue;
            }

            String fileName = candidate.getFileName().toString();
            if (fileName.endsWith(PARSE_GOLDEN_EXTENSION)) {
                if (!parseAccept) {
                    issues.add(new MolangCorpusIssue(
                            corpusCase.filePath(),
                            corpusCase.id(),
                            "Adjacent parse golden '" + fileName + "' is only supported for cases declaring 'parse-accept'."
                    ));
                }
                continue;
            }

            if (fileName.endsWith(BIND_GOLDEN_EXTENSION)) {
                if (!bindNormalize) {
                    issues.add(new MolangCorpusIssue(
                            corpusCase.filePath(),
                            corpusCase.id(),
                            "Adjacent bind golden '" + fileName + "' is only supported for cases declaring 'bind-normalize'."
                    ));
                }
                continue;
            }

            if (fileName.endsWith(DIAGNOSTICS_GOLDEN_EXTENSION)) {
                if (!bindNormalize) {
                    issues.add(new MolangCorpusIssue(
                            corpusCase.filePath(),
                            corpusCase.id(),
                            "Adjacent diagnostics golden '" + fileName + "' is only supported for cases declaring 'bind-normalize' in this phase 3 slice."
                    ));
                }
                continue;
            }

            if (fileName.endsWith(DEBUG_TRACE_GOLDEN_EXTENSION)) {
                if (!bindNormalize || corpusCase.diagnosticsMode() != MolangCorpusModel.MolangDiagnosticsMode.DEBUG) {
                    issues.add(new MolangCorpusIssue(
                            corpusCase.filePath(),
                            corpusCase.id(),
                            "Adjacent debug-trace golden '" + fileName + "' is only supported for cases declaring 'bind-normalize' with diagnostics mode 'debug'."
                    ));
                }
            }
        }
    }
}
