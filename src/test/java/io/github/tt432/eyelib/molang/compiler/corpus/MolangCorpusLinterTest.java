package io.github.tt432.eyelib.molang.compiler.corpus;

import io.github.tt432.eyelib.molang.compiler.corpus.MolangCorpusModel.MolangResultType;
import io.github.tt432.eyelib.molang.compiler.corpus.MolangCorpusModel.MolangRunReport;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author TT432 */
class MolangCorpusLinterTest {
    private static final String INVALID_CORPUS_RESOURCE = "io/github/tt432/eyelibmolang/compiler/corpus/phase1/invalid";

    @Test
    void invalidCorpusReportsMissingRequiredMetadataAndDuplicateIdsAsCorpusErrors() throws URISyntaxException {
        Path corpusPath = invalidCorpusPath();

        MolangRunReport report = new MolangCorpusHarness().run(corpusPath);

        assertEquals(4, report.summary().totalCases());
        assertEquals(0, report.summary().passCount());
        assertEquals(4, report.summary().corpusErrorCount());
        assertEquals(0, report.summary().engineFailureCount());
        assertEquals(0, report.summary().assertionFailureCount());
        assertEquals(0, report.summary().skippedCount());

        assertTrue(report.caseReports().stream().allMatch(item -> item.resultType() == MolangResultType.CORPUS_ERROR));
        assertTrue(report.caseReports().stream().anyMatch(item -> item.details().get(0).contains("Missing required metadata field 'id'")));
        assertTrue(report.caseReports().stream().anyMatch(item -> item.details().get(0).contains("Duplicate case id 'invalid.duplicate-id'")));
        assertTrue(report.caseReports().stream().anyMatch(item -> item.details().get(0).contains("parse-reject cases must declare at least one expected diagnostic")));
    }

    private Path invalidCorpusPath() throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(INVALID_CORPUS_RESOURCE), INVALID_CORPUS_RESOURCE).toURI());
    }
}