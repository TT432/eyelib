package io.github.tt432.clientsmoke.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Static verification of exit code propagation (CORR-04) and JUnit XML
 * report format (OVRD-04) through source file assertions and reflection.
 *
 * <p>All tests operate directly on {@link ClientSmokeStateMachine} source
 * and class structure — no Minecraft runtime required. The hardware
 * verification checklist (Task 2) completes CORR-04 end-to-end.</p>
 *
 * <h3>Test coverage</h3>
 * <ul>
 *   <li><b>Part A</b> (tests #1–#5): exit code logic — conditional
 *       computation, failed count aggregation, log parameterization,
 *       halt call presence, and absence of hardcoded {@code halt(0)}
 *       in active code.</li>
 *   <li><b>Part B</b> (tests #6–#12): JUnit XML format — method
 *       existence via reflection, XML declaration, testsuite
 *       attributes, failure element handling, empty test set
 *       handling, and all five XML entity escapes.</li>
 * </ul>
 */
class ClientSmokeExitCodeTest {

    // ═══════════════════════════════════════════════════════════════
    // Part A — Exit code logic (CORR-04)
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Exit code is conditionally computed — (failedCount > 0) ? 1 : 0")
    void source_exitCode_isConditional_notHardcoded() throws Exception {
        String src = readSourceFile();
        assertNotNull(src, "Source file should be accessible");

        // CORR-02: exit code must be conditionally computed
        assertTrue(src.contains("int exitCode = (failedCount > 0) ? 1 : 0"),
                "Source should contain conditional exit code: "
                        + "int exitCode = (failedCount > 0) ? 1 : 0");

        // The halt call uses the exitCode VARIABLE, not a literal
        assertTrue(src.contains("halt(exitCode)"),
                "Source should contain halt(exitCode) — the variable, not a literal");
    }

    @Test
    @DisplayName("Exit code uses failedCount stream aggregation")
    void source_exitCode_usesFailedCountStream() throws Exception {
        String src = readSourceFile();
        assertNotNull(src, "Source file should be accessible");

        // Failed count is aggregated from test results
        assertTrue(src.contains("testResults.stream()")
                        && src.contains("failed\".equals(r.status())"),
                "Source should aggregate failed count via testResults.stream()");
    }

    @Test
    @DisplayName("handleExit logs exit code as a parameter")
    void source_handleExit_logsExitCode() throws Exception {
        String src = readSourceFile();
        assertNotNull(src, "Source file should be accessible");

        // The log message includes exitCode as a slf4j placeholder parameter
        assertTrue(src.contains("exit code {}"),
                "Source should contain 'exit code {}' — exit code appears "
                        + "as a slf4j log parameter, confirming runtime value is logged");
    }

    @Test
    @DisplayName("handleExit contains Runtime.getRuntime().halt call")
    void source_handleExit_containsHaltCall() throws Exception {
        String src = readSourceFile();
        assertNotNull(src, "Source file should be accessible");

        // Isolate the handleExit method body to localize the assertion
        String handleExitBody = extractMethodBody(src, "handleExit");
        assertNotNull(handleExitBody, "Should be able to extract handleExit method body");
        assertTrue(handleExitBody.contains("Runtime.getRuntime().halt("),
                "handleExit method body should contain Runtime.getRuntime().halt(");
    }

    @Test
    @DisplayName("No hardcoded halt(0) in active (non-comment) code")
    void source_noHardcodedHalt0_inActiveCode() throws Exception {
        String src = readSourceFile();
        assertNotNull(src, "Source file should be accessible");

        // Strip comments to isolate active code (halt(0) may appear in Javadoc)
        String active = stripComments(src);
        assertNotNull(active, "Stripped source should not be null");

        // After stripping comments, there should be NO halt(0) literal
        // The only halt call must use the exitCode variable
        assertFalse(active.contains("halt(0)"),
                "Active code should not contain hardcoded halt(0); "
                        + "halt(0) may appear in Javadoc but must not be in executable code");
    }

    // ═══════════════════════════════════════════════════════════════
    // Part B — JUnit XML format (OVRD-04)
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("buildJUnitXml method exists — private static, (List, String)")
    void source_buildJUnitXml_methodExists() throws Exception {
        Method method = ClientSmokeStateMachine.class
                .getDeclaredMethod("buildJUnitXml", List.class, String.class);
        assertNotNull(method, "buildJUnitXml(List, String) should exist");
        assertTrue(Modifier.isPrivate(method.getModifiers()),
                "buildJUnitXml should be private");
        assertTrue(Modifier.isStatic(method.getModifiers()),
                "buildJUnitXml should be static");
    }

    @Test
    @DisplayName("escapeXml method exists — private static, (String)")
    void source_escapeXml_methodExists() throws Exception {
        Method method = ClientSmokeStateMachine.class
                .getDeclaredMethod("escapeXml", String.class);
        assertNotNull(method, "escapeXml(String) should exist");
        assertTrue(Modifier.isPrivate(method.getModifiers()),
                "escapeXml should be private");
        assertTrue(Modifier.isStatic(method.getModifiers()),
                "escapeXml should be static");
    }

    @Test
    @DisplayName("buildJUnitXml contains XML declaration")
    void source_buildJUnitXml_containsXmlDeclaration() throws Exception {
        String src = readSourceFile();
        assertNotNull(src, "Source file should be accessible");

        String body = extractMethodBody(src, "buildJUnitXml");
        assertNotNull(body, "Should extract buildJUnitXml method body");
        // Source file has raw escape sequences (\" = backslash + quote, \n = backslash + n)
        // Match against the RAW source text, not the compiler-interpreted values.
        assertTrue(body.contains("<?xml") && body.contains("1.0") && body.contains("encoding=") && body.contains("UTF-8"),
                "buildJUnitXml should contain XML declaration");
    }

    @Test
    @DisplayName("buildJUnitXml contains testsuite element with all required attributes")
    void source_buildJUnitXml_containsTestSuiteElement() throws Exception {
        String src = readSourceFile();
        assertNotNull(src, "Source file should be accessible");

        String body = extractMethodBody(src, "buildJUnitXml");
        assertNotNull(body, "Should extract buildJUnitXml method body");

        assertTrue(body.contains("testsuite name=\\\"ClientSmoke\\\""),
                "buildJUnitXml should contain <testsuite name=\"ClientSmoke\"");
        assertTrue(body.contains("tests="),
                "buildJUnitXml should contain tests= attribute");
        assertTrue(body.contains("failures="),
                "buildJUnitXml should contain failures= attribute");
        assertTrue(body.contains("errors="),
                "buildJUnitXml should contain errors= attribute");
        assertTrue(body.contains("skipped="),
                "buildJUnitXml should contain skipped= attribute");
        assertTrue(body.contains("time="),
                "buildJUnitXml should contain time= attribute");
        assertTrue(body.contains("timestamp="),
                "buildJUnitXml should contain timestamp= attribute");
    }

    @Test
    @DisplayName("buildJUnitXml handles failure element for failed tests")
    void source_buildJUnitXml_handlesFailureElement() throws Exception {
        String src = readSourceFile();
        assertNotNull(src, "Source file should be accessible");

        String body = extractMethodBody(src, "buildJUnitXml");
        assertNotNull(body, "Should extract buildJUnitXml method body");
        assertTrue(body.contains("failure message=\\"),
                "buildJUnitXml should contain <failure message= for failed test representation");
    }

    @Test
    @DisplayName("buildJUnitXml uses results.size() for tests attribute (no hardcoded count)")
    void source_buildJUnitXml_handlesEmptyTestSet() throws Exception {
        String src = readSourceFile();
        assertNotNull(src, "Source file should be accessible");

        String body = extractMethodBody(src, "buildJUnitXml");
        assertNotNull(body, "Should extract buildJUnitXml method body");

        // The tests attribute must use results.size() — not a hardcoded number
        assertTrue(body.contains("results.size()"),
                "buildJUnitXml should use results.size() for the tests attribute, "
                        + "ensuring empty test sets produce tests=\"0\"");
    }

    @Test
    @DisplayName("escapeXml handles all five XML special characters")
    void source_escapeXml_handlesAllFiveEntities() throws Exception {
        String src = readSourceFile();
        assertNotNull(src, "Source file should be accessible");

        String body = extractMethodBody(src, "escapeXml");
        assertNotNull(body, "Should extract escapeXml method body");

        // All five XML entities must be escaped per XML spec
        assertTrue(body.contains("replace(\"&\"") && body.contains("&amp;"),
                "escapeXml should escape & → &amp;");
        assertTrue(body.contains("replace(\"<\"") && body.contains("&lt;"),
                "escapeXml should escape < → &lt;");
        assertTrue(body.contains("replace(\">\"") && body.contains("&gt;"),
                "escapeXml should escape > → &gt;");
        assertTrue(body.contains("replace(\"\\\"\"") && body.contains("&quot;"),
                "escapeXml should escape \" → &quot;");
        assertTrue(body.contains("replace(\"'\"") && body.contains("&apos;"),
                "escapeXml should escape ' → &apos;");
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════

    /**
     * Reads the source file of {@link ClientSmokeStateMachine} from disk.
     * Path is relative to the working directory (subproject root).
     *
     * @return source file contents, or {@code null} if the file cannot be read
     */
    private static String readSourceFile() throws Exception {
        String className = ClientSmokeStateMachine.class.getSimpleName() + ".java";
        String path = "src/main/java/io/github/tt432/clientsmoke/runtime/" + className;
        Path file = Paths.get(path);
        if (Files.exists(file)) {
            return Files.readString(file);
        }
        return null;
    }

    /**
     * Strips Java comments from source text to isolate active code.
     *
     * <p>Handles both block comments ({@code /* ... * /}) including Javadoc
     * and line comments ({@code //}). Used to distinguish comment references
     * from executable code in the {@code halt(0)} assertion.</p>
     *
     * @param source raw source file contents
     * @return source with comments removed
     */
    private static String stripComments(String source) {
        // Remove block comments: /* ... */ (includes Javadoc /** ... */)
        source = source.replaceAll("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", "");
        // Remove line comments (but NOT inside string literals — simplified)
        source = source.replaceAll("//[^\n]*", "");
        return source;
    }

    /**
     * Extracts the body of a method from source text by locating the
     * method signature and returning everything from the opening brace
     * to the matching closing brace.
     *
     * <p>Simplified implementation: finds the method declaration
     * (name followed by {@code (}), then uses brace counting to
     * find the matching close. Sufficient for source assertions.</p>
     *
     * @param source     raw source file contents
     * @param methodName the method name to locate
     * @return method body (between outermost braces), or {@code null} if not found
     */
    private static String extractMethodBody(String source, String methodName) {
        // Search for methodName( that appears in a DECLARATION context
        // (the line containing the match must also contain "private static").
        // This avoids false matches on call sites like buildJUnitXml(args).
        String search = methodName + "(";
        int index = 0;
        while (index < source.length()) {
            int pos = source.indexOf(search, index);
            if (pos < 0) return null;

            // Find the start of the line containing this occurrence
            int lineStart = source.lastIndexOf('\n', pos);
            if (lineStart < 0) {
                lineStart = 0;
            } else {
                lineStart++; // skip the newline character
            }
            String line = source.substring(lineStart, pos + search.length());

            // A declaration line has "private static" before the method name
            if (line.contains("private static")) {
                // This is the declaration — extract the method body
                int openBrace = source.indexOf('{', pos);
                if (openBrace < 0) return null;

                // Find the matching closing brace using simple depth counting
                int depth = 1;
                int i = openBrace + 1;
                while (i < source.length() && depth > 0) {
                    char c = source.charAt(i);
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                    i++;
                }

                if (depth != 0) return null; // unbalanced braces
                return source.substring(openBrace + 1, i - 1);
            }

            // Move past this occurrence and continue searching
            index = pos + 1;
        }

        return null;
    }
}
