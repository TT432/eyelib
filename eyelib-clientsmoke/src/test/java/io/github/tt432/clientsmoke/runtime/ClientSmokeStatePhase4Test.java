package io.github.tt432.clientsmoke.runtime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Phase 4 state machine additions — enum values, TestResult
 * record, new handler methods, and source file content assertions.
 */
class ClientSmokeStatePhase4Test {

    // ── Enum value existence ──

    @Test
    @DisplayName("TEST_EXEC enum value exists")
    void testExecEnumValue_exists() {
        assertDoesNotThrow(() -> ClientSmokeState.valueOf("TEST_EXEC"),
                "TEST_EXEC should be a valid ClientSmokeState enum value");
    }

    @Test
    @DisplayName("REPOSITION enum value exists")
    void repositionEnumValue_exists() {
        assertDoesNotThrow(() -> ClientSmokeState.valueOf("REPOSITION"),
                "REPOSITION should be a valid ClientSmokeState enum value");
    }

    @Test
    @DisplayName("REPORT enum value exists")
    void reportEnumValue_exists() {
        assertDoesNotThrow(() -> ClientSmokeState.valueOf("REPORT"),
                "REPORT should be a valid ClientSmokeState enum value");
    }

    @Test
    @DisplayName("TEST_EXEC, REPOSITION, REPORT declared in correct order")
    void phase4EnumOrder() {
        ClientSmokeState[] values = ClientSmokeState.values();
        int testExec = -1, reposition = -1, report = -1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == ClientSmokeState.TEST_EXEC) testExec = i;
            if (values[i] == ClientSmokeState.REPOSITION) reposition = i;
            if (values[i] == ClientSmokeState.REPORT) report = i;
        }
        assertTrue(testExec >= 0, "TEST_EXEC not found in enum values");
        assertTrue(reposition >= 0, "REPOSITION not found in enum values");
        assertTrue(report >= 0, "REPORT not found in enum values");
        assertTrue(testExec < reposition,
                "TEST_EXEC should be declared before REPOSITION");
        assertTrue(reposition < report,
                "REPOSITION should be declared before REPORT");
    }

    // ── Field existence ──

    @Test
    @DisplayName("testResults field exists — List<TestResult>, static")
    void testResultsFieldExists() throws Exception {
        Field field = ClientSmokeStateMachine.class.getDeclaredField("testResults");
        assertEquals(List.class, field.getType(),
                "Expected List type for testResults");
        assertTrue(Modifier.isStatic(field.getModifiers()),
                "testResults should be static");
    }

    @Test
    @DisplayName("testsSorted field exists — boolean, static")
    void testsSortedFieldExists() throws Exception {
        Field field = ClientSmokeStateMachine.class.getDeclaredField("testsSorted");
        assertEquals(boolean.class, field.getType(),
                "testsSorted should be boolean");
        assertTrue(Modifier.isStatic(field.getModifiers()),
                "testsSorted should be static");
    }

    @Test
    @DisplayName("reportWritten field exists — boolean, static")
    void reportWrittenFieldExists() throws Exception {
        Field field = ClientSmokeStateMachine.class.getDeclaredField("reportWritten");
        assertEquals(boolean.class, field.getType(),
                "reportWritten should be boolean");
        assertTrue(Modifier.isStatic(field.getModifiers()),
                "reportWritten should be static");
    }

    // ── Method existence ──

    @Test
    @DisplayName("handleTestExec() method exists — private static")
    void handleTestExecMethodExists() throws Exception {
        Method method = getDeclaredMethod(ClientSmokeStateMachine.class, "handleTestExec");
        assertNotNull(method, "handleTestExec should exist");
        assertEquals(0, method.getParameterCount(),
                "handleTestExec should have no parameters");
        assertTrue(Modifier.isPrivate(method.getModifiers()),
                "handleTestExec should be private");
        assertTrue(Modifier.isStatic(method.getModifiers()),
                "handleTestExec should be static");
    }

    @Test
    @DisplayName("handleReposition() method exists — private static")
    void handleRepositionMethodExists() throws Exception {
        Method method = getDeclaredMethod(ClientSmokeStateMachine.class, "handleReposition");
        assertNotNull(method, "handleReposition should exist");
        assertTrue(Modifier.isPrivate(method.getModifiers()),
                "handleReposition should be private");
    }

    @Test
    @DisplayName("handleReport() method exists — private static")
    void handleReportMethodExists() throws Exception {
        Method method = getDeclaredMethod(ClientSmokeStateMachine.class, "handleReport");
        assertNotNull(method, "handleReport should exist");
        assertTrue(Modifier.isPrivate(method.getModifiers()),
                "handleReport should be private");
    }

    // ── TestResult + ErrorInfo record existence ──

    @Test
    @DisplayName("TestResult record has expected record components")
    void testResultRecordStructure() throws Exception {
        java.lang.reflect.RecordComponent[] components =
                ClientSmokeStateMachine.TestResult.class.getRecordComponents();
        assertEquals(6, components.length,
                "TestResult should have 6 record components");
        assertEquals("className", components[0].getName());
        assertEquals("description", components[1].getName());
        assertEquals("priority", components[2].getName());
        assertEquals("status", components[3].getName());
        assertEquals("durationMs", components[4].getName());
        assertEquals("error", components[5].getName());
    }

    @Test
    @DisplayName("ErrorInfo record has expected record components")
    void errorInfoRecordStructure() throws Exception {
        java.lang.reflect.RecordComponent[] components =
                ClientSmokeStateMachine.ErrorInfo.class.getRecordComponents();
        assertEquals(2, components.length,
                "ErrorInfo should have 2 record components");
        assertEquals("message", components[0].getName());
        assertEquals("stackTrace", components[1].getName());
    }

    // ── Source file content assertions ──

    @Test
    @DisplayName("Source contains Class.forName for test loading (D-01)")
    void source_containsForName() throws Exception {
        String src = readSourceFile();
        assertNotNull(src, "Source file should be accessible");
        assertTrue(src.contains("Class.forName"),
                "Source should contain Class.forName for test class loading per D-01");
    }

    @Test
    @DisplayName("Source contains getDeclaredConstructor for instantiation (D-01)")
    void source_containsGetDeclaredConstructor() throws Exception {
        String src = readSourceFile();
        assertNotNull(src, "Source file should be accessible");
        assertTrue(src.contains("getDeclaredConstructor"),
                "Source should contain getDeclaredConstructor per D-01");
    }

    @Test
    @DisplayName("Source contains newInstance for test invocation (D-01)")
    void source_containsNewInstance() throws Exception {
        String src = readSourceFile();
        assertNotNull(src, "Source file should be accessible");
        assertTrue(src.contains(".newInstance()"),
                "Source should contain .newInstance() for constructor invocation per D-01");
    }

    @Test
    @DisplayName("Source contains System.currentTimeMillis for timing")
    void source_containsTiming() throws Exception {
        String src = readSourceFile();
        assertNotNull(src, "Source file should be accessible");
        assertTrue(src.contains("System.currentTimeMillis"),
                "Source should contain System.currentTimeMillis for execution timing");
    }

    @Test
    @DisplayName("Source contains buildErrorInfo for error formatting (D-12)")
    void source_containsBuildErrorInfo() throws Exception {
        String src = readSourceFile();
        assertNotNull(src, "Source file should be accessible");
        assertTrue(src.contains("buildErrorInfo"),
                "Source should contain buildErrorInfo method per D-12");
    }

    @Test
    @DisplayName("Source contains Comparator.comparingInt for priority sort (EXEC-03)")
    void source_containsPrioritySort() throws Exception {
        String src = readSourceFile();
        assertNotNull(src, "Source file should be accessible");
        assertTrue(src.contains("Comparator.comparingInt") || src.contains("comparingInt"),
                "Source should contain Comparator.comparingInt for priority sort per EXEC-03");
    }

    @Test
    @DisplayName("Source contains GsonBuilder for JSON serialization (D-16)")
    void source_containsGson() throws Exception {
        String src = readSourceFile();
        assertNotNull(src, "Source file should be accessible");
        assertTrue(src.contains("GsonBuilder") || src.contains("Gson"),
                "Source should contain Gson for JSON serialization per D-16");
    }

    // ── Helpers ──

    private static Method getDeclaredMethod(Class<?> clazz, String name) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        return null;
    }

    private static String readSourceFile() throws Exception {
        String className = ClientSmokeStateMachine.class.getSimpleName() + ".java";
        String path = "src/main/java/io/github/tt432/clientsmoke/runtime/" + className;
        java.nio.file.Path file = Paths.get(path);
        if (Files.exists(file)) {
            return Files.readString(file);
        }
        return null;
    }
}
