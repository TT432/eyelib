package io.github.tt432.eyelibmolang.compiler.corpus;

import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangAssertionType;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangCorpusCase;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangCorpusEvidence;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangCorpusIssue;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangCorpusLayer;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangDiagnosticPhase;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangDiagnosticSeverity;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangDiagnosticsMode;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangExpectedDiagnostic;
import io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.MolangLoadResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.CASE_EXTENSION;
import static io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.DEFAULT_MODE;
import static io.github.tt432.eyelibmolang.compiler.corpus.MolangCorpusModel.DEFAULT_POLICY_PACK;

final class MolangCorpusLoader {
    MolangLoadResult load(Path corpusRoot) {
        List<MolangCorpusCase> cases = new ArrayList<>();
        List<MolangCorpusIssue> issues = new ArrayList<>();

        if (!Files.exists(corpusRoot)) {
            issues.add(new MolangCorpusIssue(corpusRoot, "<unknown>", "Corpus root does not exist."));
            return new MolangLoadResult(cases, issues);
        }

        List<Path> files;
        try (Stream<Path> stream = Files.walk(corpusRoot)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(CASE_EXTENSION))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException e) {
            issues.add(new MolangCorpusIssue(corpusRoot, "<unknown>", "Failed to discover corpus files: " + e.getMessage()));
            return new MolangLoadResult(cases, issues);
        }

        for (Path file : files) {
            parseCase(file, cases, issues);
        }

        return new MolangLoadResult(cases, issues);
    }

    private void parseCase(Path file, List<MolangCorpusCase> cases, List<MolangCorpusIssue> issues) {
        String content;
        try {
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            issues.add(new MolangCorpusIssue(file, "<unknown>", "Failed to read corpus case: " + e.getMessage()));
            return;
        }

        Optional<FrontMatterSplit> splitOptional = splitFrontMatter(file, content, issues);
        if (splitOptional.isEmpty()) {
            return;
        }

        FrontMatterSplit split = splitOptional.get();
        FrontMatterMetadata metadata = parseFrontMatter(file, split.frontMatter(), issues);
        if (metadata == null) {
            return;
        }

        cases.add(new MolangCorpusCase(
                metadata.id,
                file,
                metadata.layer,
                metadata.evidence,
                metadata.assertions,
                split.source(),
                metadata.mode,
                metadata.policyPack,
                metadata.notes,
                metadata.expectedDiagnostics
        ));
    }

    private Optional<FrontMatterSplit> splitFrontMatter(Path file, String content, List<MolangCorpusIssue> issues) {
        List<String> lines = content.lines().toList();
        if (lines.isEmpty() || !"---".equals(lines.get(0).trim())) {
            issues.add(new MolangCorpusIssue(file, "<unknown>", "Missing front matter start delimiter '---'."));
            return Optional.empty();
        }

        int endDelimiter = -1;
        for (int i = 1; i < lines.size(); i++) {
            if ("---".equals(lines.get(i).trim())) {
                endDelimiter = i;
                break;
            }
        }

        if (endDelimiter < 0) {
            issues.add(new MolangCorpusIssue(file, "<unknown>", "Missing front matter end delimiter '---'."));
            return Optional.empty();
        }

        List<String> frontMatter = lines.subList(1, endDelimiter);
        List<String> source = lines.subList(endDelimiter + 1, lines.size());
        return Optional.of(new FrontMatterSplit(frontMatter, String.join("\n", source)));
    }

    private FrontMatterMetadata parseFrontMatter(Path file, List<String> frontMatter, List<MolangCorpusIssue> issues) {
        Map<String, String> scalars = new HashMap<>();
        Map<String, List<String>> lists = new HashMap<>();
        List<Map<String, String>> diagnosticEntries = new ArrayList<>();

        String currentListKey = null;
        Map<String, String> currentDiagnosticEntry = null;
        for (int index = 0; index < frontMatter.size(); index++) {
            String line = frontMatter.get(index);
            String trimmed = line.trim();

            if (trimmed.isBlank() || trimmed.startsWith("#")) {
                continue;
            }

            if (trimmed.startsWith("- ")) {
                if (currentListKey == null) {
                    issues.add(new MolangCorpusIssue(file, "<unknown>", "List value without key at front matter line " + (index + 2) + "."));
                    return null;
                }
                if ("expected-diagnostics".equals(currentListKey)) {
                    String inline = trimmed.substring(2).trim();
                    currentDiagnosticEntry = new HashMap<>();
                    diagnosticEntries.add(currentDiagnosticEntry);
                    if (!inline.isBlank()) {
                        int inlineColonIndex = inline.indexOf(':');
                        if (inlineColonIndex <= 0) {
                            issues.add(new MolangCorpusIssue(file, "<unknown>", "Malformed expected-diagnostics entry at front matter line " + (index + 2) + "."));
                            return null;
                        }
                        currentDiagnosticEntry.put(
                                inline.substring(0, inlineColonIndex).trim().toLowerCase(Locale.ROOT),
                                inline.substring(inlineColonIndex + 1).trim()
                        );
                    }
                    continue;
                }
                lists.computeIfAbsent(currentListKey, key -> new ArrayList<>()).add(trimmed.substring(2).trim());
                continue;
            }

            boolean indented = !line.isEmpty() && Character.isWhitespace(line.charAt(0));
            if (indented && "expected-diagnostics".equals(currentListKey) && currentDiagnosticEntry != null) {
                int nestedColonIndex = trimmed.indexOf(':');
                if (nestedColonIndex <= 0) {
                    issues.add(new MolangCorpusIssue(file, "<unknown>", "Malformed expected-diagnostics field at front matter line " + (index + 2) + "."));
                    return null;
                }
                currentDiagnosticEntry.put(
                        trimmed.substring(0, nestedColonIndex).trim().toLowerCase(Locale.ROOT),
                        trimmed.substring(nestedColonIndex + 1).trim()
                );
                continue;
            }

            int colonIndex = trimmed.indexOf(':');
            if (colonIndex <= 0) {
                issues.add(new MolangCorpusIssue(file, "<unknown>", "Malformed front matter line " + (index + 2) + ": " + trimmed));
                return null;
            }

            String key = trimmed.substring(0, colonIndex).trim().toLowerCase(Locale.ROOT);
            String value = trimmed.substring(colonIndex + 1).trim();
            if (value.isEmpty()) {
                currentListKey = key;
                lists.computeIfAbsent(key, ignored -> new ArrayList<>());
            } else {
                currentListKey = null;
                scalars.put(key, value);
            }
        }

        String id = scalar(scalars, "id");
        String layerRaw = scalar(scalars, "layer");
        String evidenceRaw = scalar(scalars, "evidence");
        List<String> assertionsRaw = list(lists, "assertions");

        String caseId = id == null ? "<unknown>" : id;

        if (id == null || id.isBlank()) {
            issues.add(new MolangCorpusIssue(file, caseId, "Missing required metadata field 'id'."));
            return null;
        }

        Optional<MolangCorpusLayer> layerOptional = MolangCorpusLayer.fromSerialized(layerRaw);
        if (layerOptional.isEmpty()) {
            issues.add(new MolangCorpusIssue(file, caseId, "Invalid or missing required metadata field 'layer'."));
            return null;
        }

        Optional<MolangCorpusEvidence> evidenceOptional = MolangCorpusEvidence.fromSerialized(evidenceRaw);
        if (evidenceOptional.isEmpty()) {
            issues.add(new MolangCorpusIssue(file, caseId, "Invalid or missing required metadata field 'evidence'."));
            return null;
        }

        if (assertionsRaw.isEmpty()) {
            issues.add(new MolangCorpusIssue(file, caseId, "Missing required metadata field 'assertions'."));
            return null;
        }

        Set<MolangAssertionType> assertions = EnumSet.noneOf(MolangAssertionType.class);
        for (String assertionRaw : assertionsRaw) {
            Optional<MolangAssertionType> assertion = MolangAssertionType.fromSerialized(assertionRaw);
            if (assertion.isEmpty()) {
                issues.add(new MolangCorpusIssue(file, caseId, "Unknown assertion type: '" + assertionRaw + "'."));
                return null;
            }
            assertions.add(assertion.get());
        }

        MolangDiagnosticsMode mode = MolangDiagnosticsMode.fromSerialized(scalar(scalars, "mode"))
                .orElse(DEFAULT_MODE);
        String policyPack = Optional.ofNullable(scalar(scalars, "policy-pack"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElse(DEFAULT_POLICY_PACK);

        List<String> notes = list(lists, "notes");
        List<MolangExpectedDiagnostic> expectedDiagnostics = parseExpectedDiagnostics(file, caseId, diagnosticEntries, issues);
        if (expectedDiagnostics == null) {
            return null;
        }

        return new FrontMatterMetadata(
                id,
                layerOptional.get(),
                evidenceOptional.get(),
                assertions,
                mode,
                policyPack,
                notes,
                expectedDiagnostics
        );
    }

    private List<MolangExpectedDiagnostic> parseExpectedDiagnostics(Path file,
                                                                    String caseId,
                                                                    List<Map<String, String>> diagnosticEntries,
                                                                    List<MolangCorpusIssue> issues) {
        if (diagnosticEntries.isEmpty()) {
            return List.of();
        }

        List<MolangExpectedDiagnostic> expectedDiagnostics = new ArrayList<>();
        for (Map<String, String> entry : diagnosticEntries) {
            Optional<MolangDiagnosticPhase> phase = MolangDiagnosticPhase.fromSerialized(entry.get("phase"));
            Optional<MolangDiagnosticSeverity> severity = MolangDiagnosticSeverity.fromSerialized(entry.get("severity"));
            String code = entry.get("code");

            if (phase.isEmpty() || severity.isEmpty() || code == null || code.isBlank()) {
                issues.add(new MolangCorpusIssue(file, caseId, "Each expected diagnostic must define phase, severity, and code."));
                return null;
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

        return List.copyOf(expectedDiagnostics);
    }

    private String scalar(Map<String, String> scalars, String key) {
        return scalars.get(key);
    }

    private List<String> list(Map<String, List<String>> lists, String key) {
        List<String> values = lists.get(key);
        if (values == null) {
            return List.of();
        }
        return List.copyOf(values);
    }

    private record FrontMatterSplit(List<String> frontMatter, String source) {
    }

    private static final class FrontMatterMetadata {
        private final String id;
        private final MolangCorpusLayer layer;
        private final MolangCorpusEvidence evidence;
        private final Set<MolangAssertionType> assertions;
        private final MolangDiagnosticsMode mode;
        private final String policyPack;
        private final List<String> notes;
        private final List<MolangExpectedDiagnostic> expectedDiagnostics;

        private FrontMatterMetadata(String id,
                                    MolangCorpusLayer layer,
                                    MolangCorpusEvidence evidence,
                                    Set<MolangAssertionType> assertions,
                                    MolangDiagnosticsMode mode,
                                    String policyPack,
                                    List<String> notes,
                                    List<MolangExpectedDiagnostic> expectedDiagnostics) {
            this.id = id;
            this.layer = layer;
            this.evidence = evidence;
            this.assertions = assertions;
            this.mode = mode;
            this.policyPack = policyPack;
            this.notes = notes;
            this.expectedDiagnostics = expectedDiagnostics;
        }
    }
}
