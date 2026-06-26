package io.github.tt432.clientsmoke.runtime;

import io.github.tt432.clientsmoke.ClientSmokeMod;
import io.github.tt432.clientsmoke.config.ClientSmokeConfig;
import io.github.tt432.clientsmoke.scanner.ClientSmokeScanner;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 驱动 NeoForge smoke test 的客户端 tick 状态机。
 *
 * @author TT432
 */
@EventBusSubscriber(modid = ClientSmokeMod.MOD_ID, value = Dist.CLIENT)
public final class ClientSmokeStateMachine {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSmokeStateMachine.class);

    record TestResult(String className, String description, int priority, String status, long durationMs, ErrorInfo error) {}

    record ErrorInfo(String message, String stackTrace) {}

    private static ClientSmokeState state = ClientSmokeState.INIT;
    private static List<ClientSmokeScanner.DiscoveredTest> discoveredTests = Collections.emptyList();
    private static final List<TestResult> testResults = new ArrayList<>();
    private static boolean testsSorted = false;
    private static boolean reportWritten = false;
    private static long exitStartMs = -1L;

    public static void setDiscoveredTests(List<ClientSmokeScanner.DiscoveredTest> tests) {
        discoveredTests = new ArrayList<>(tests);
        LOGGER.info("[ClientSmoke] State machine received {} discovered test(s)", discoveredTests.size());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (state == ClientSmokeState.IDLE || state == ClientSmokeState.ERROR) {
            return;
        }

        try {
            switch (state) {
                case INIT -> handleInit();
                case SCAN -> handleScan();
                case TEST_EXEC -> handleTestExec();
                case REPORT -> handleReport();
                case EXIT -> handleExit();
                default -> transitionTo(ClientSmokeState.ERROR, "Unknown state: " + state);
            }
        } catch (Exception e) {
            LOGGER.error("[ClientSmoke] State machine error in state {}: {}", state, e.getMessage(), e);
            state = ClientSmokeState.ERROR;
        }
    }

    @SubscribeEvent
    public static void releaseMouseOnPostTick(ClientTickEvent.Post event) {
        if (ClientSmokeConfig.isPreventMouseGrab() || ClientSmokeConfig.isEnabled()) {
            ClientSmokeMod.releaseMouse(Minecraft.getInstance());
        }
    }

    private static void handleInit() {
        if (!ClientSmokeConfig.isEnabled()) {
            transitionTo(ClientSmokeState.IDLE, "Framework disabled");
            return;
        }
        transitionTo(ClientSmokeState.SCAN, "Framework enabled");
    }

    private static void handleScan() {
        LOGGER.info("[ClientSmoke] Scan complete - {} test(s) in queue", discoveredTests.size());
        transitionTo(discoveredTests.isEmpty() ? ClientSmokeState.REPORT : ClientSmokeState.TEST_EXEC, "Starting smoke run");
    }

    private static void handleTestExec() {
        if (!testsSorted) {
            discoveredTests.sort(Comparator.comparingInt(ClientSmokeScanner.DiscoveredTest::priority));
            testsSorted = true;
        }

        for (ClientSmokeScanner.DiscoveredTest discovered : discoveredTests) {
            executeTest(discovered);
        }
        transitionTo(ClientSmokeState.REPORT, "All tests executed");
    }

    private static void executeTest(ClientSmokeScanner.DiscoveredTest discovered) {
        long startMs = System.currentTimeMillis();
        try {
            Class<?> testClass = Class.forName(discovered.className());
            testClass.getDeclaredConstructor().newInstance();
            long durationMs = System.currentTimeMillis() - startMs;
            testResults.add(new TestResult(discovered.className(), discovered.description(), discovered.priority(), "passed", durationMs, null));
            LOGGER.info("[ClientSmoke] PASS - {} - {}ms", discovered.className(), durationMs);
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            Throwable reported = unwrapInvocationTarget(e);
            testResults.add(new TestResult(discovered.className(), discovered.description(), discovered.priority(), "failed", durationMs, buildErrorInfo(reported)));
            LOGGER.warn("[ClientSmoke] FAIL - {} - {}", discovered.className(), reported.toString());
        }
    }

    private static ErrorInfo buildErrorInfo(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        printWriter.flush();
        String[] lines = stringWriter.toString().split("\\r?\\n");
        StringBuilder stackTrace = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, 6); i++) {
            stackTrace.append(lines[i]).append('\n');
        }
        return new ErrorInfo(e.toString(), stackTrace.toString().stripTrailing());
    }

    private static Throwable unwrapInvocationTarget(Throwable e) {
        if (e instanceof InvocationTargetException invocationTargetException && invocationTargetException.getCause() != null) {
            return invocationTargetException.getCause();
        }
        return e;
    }

    private static void handleReport() throws Exception {
        if (reportWritten) {
            return;
        }
        reportWritten = true;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path outputDir = FMLPaths.GAMEDIR.get().resolve("clientsmoke-reports");
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve("report-" + timestamp + ".json"), new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(new ReportData(testResults.size(), (int) passedCount(), testResults.size() - (int) passedCount(), timestamp, testResults)));
        Files.writeString(outputDir.resolve("junit-" + timestamp + ".xml"), buildJUnitXml(timestamp));
        transitionTo(ClientSmokeState.EXIT, "Report generated");
    }

    private static long passedCount() {
        return testResults.stream().filter(result -> "passed".equals(result.status())).count();
    }

    private static String buildJUnitXml(String timestamp) {
        long failed = testResults.size() - passedCount();
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        builder.append("<testsuite name=\"ClientSmoke\" tests=\"").append(testResults.size()).append("\" failures=\"").append(failed).append("\" errors=\"0\" skipped=\"0\" timestamp=\"").append(escapeXml(timestamp)).append("\">\n");
        for (TestResult result : testResults) {
            builder.append("  <testcase classname=\"").append(escapeXml(result.className())).append("\" name=\"").append(escapeXml(result.description())).append("\">");
            if ("failed".equals(result.status()) && result.error() != null) {
                builder.append("<failure message=\"").append(escapeXml(result.error().message())).append("\">").append(escapeXml(result.error().stackTrace())).append("</failure>");
            }
            builder.append("</testcase>\n");
        }
        builder.append("</testsuite>\n");
        return builder.toString();
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static void handleExit() {
        if (!ClientSmokeConfig.shouldExitAfterSmoke()) {
            transitionTo(ClientSmokeState.IDLE, "exitAfterSmoke=false");
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (exitStartMs < 0) {
            exitStartMs = System.currentTimeMillis();
            mc.stop();
            return;
        }

        if (System.currentTimeMillis() - exitStartMs >= 3000L) {
            Runtime.getRuntime().halt(testResults.size() == passedCount() ? 0 : 1);
        }
    }

    private static void transitionTo(ClientSmokeState newState, String reason) {
        LOGGER.info("[ClientSmoke] {} -> {} - {}", state, newState, reason);
        state = newState;
    }

    private record ReportData(int totalTests, int passed, int failed, String timestamp, List<TestResult> entries) {}

    private ClientSmokeStateMachine() {}
}
