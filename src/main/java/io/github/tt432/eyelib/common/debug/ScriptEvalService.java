package io.github.tt432.eyelib.common.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 使用 JDK 内置 javac（{@code javax.tools.JavaCompiler}）在运行时编译 Java 代码片段。
 * 相比 Janino，javac 完整支持当前 JDK 版本的所有语言特性（{@code var}、record、switch expression 等）。
 *
 * @author TT432
 */
public final class ScriptEvalService {

    private static final String TEMPLATE = """
            import net.minecraft.client.Minecraft;
            import net.minecraft.client.multiplayer.ClientLevel;
            import net.minecraft.client.player.LocalPlayer;

            public class _EyelibScript {
                public static Object run(Minecraft minecraft, LocalPlayer player, ClientLevel level) throws Throwable {
            %s
                }
            }
            """;

    public record ScriptResult(
            boolean success,
            @org.jspecify.annotations.Nullable String result,
            @org.jspecify.annotations.Nullable String error
    ) {
    }

    public static ScriptResult evaluate(String code) {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new ScriptResult(false, null,
                    "No JDK compiler available — runClient needs a full JDK, not a JRE");
        }

        try {
            String fullSource = TEMPLATE.formatted(code.strip());

            JavaFileObject sourceFile = new SimpleJavaFileObject(
                    URI.create("string:///_EyelibScript.java"), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return fullSource;
                }
            };

            Map<String, byte[]> compiled = new HashMap<>();
            StandardJavaFileManager std = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);

            String[] cp = System.getProperty("java.class.path").split(File.pathSeparator);
            std.setLocation(StandardLocation.CLASS_PATH,
                    Arrays.stream(cp).map(File::new).filter(File::exists).toList());

            JavaFileManager fm = new ForwardingJavaFileManager<>(std) {
                @Override
                public JavaFileObject getJavaFileForOutput(Location location, String className,
                        JavaFileObject.Kind kind, FileObject sibling) {
                    return new SimpleJavaFileObject(
                            URI.create("memory:///" + className + ".class"), kind) {
                        @Override
                        public OutputStream openOutputStream() {
                            return new ByteArrayOutputStream() {
                                @Override
                                public void close() throws IOException {
                                    compiled.put(className, toByteArray());
                                }
                            };
                        }
                    };
                }
            };

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            boolean ok = compiler.getTask(null, fm, diagnostics, null, null, List.of(sourceFile)).call();

            if (!ok) {
                String errors = diagnostics.getDiagnostics().stream()
                        .map(Object::toString)
                        .collect(Collectors.joining("\n"));
                return new ScriptResult(false, null, "Compile error:\n" + errors);
            }

            byte[] bytecode = compiled.get("_EyelibScript");
            if (bytecode == null) {
                return new ScriptResult(false, null, "Internal error: no bytecode generated");
            }

            Class<?> clazz = new ClassLoader(ScriptEvalService.class.getClassLoader()) {
                @Override
                protected Class<?> findClass(String name) throws ClassNotFoundException {
                    if ("_EyelibScript".equals(name)) {
                        return defineClass(name, bytecode, 0, bytecode.length);
                    }
                    return super.findClass(name);
                }
            }.loadClass("_EyelibScript");

            Minecraft mc = Minecraft.getInstance();
            CompletableFuture<Object> future = new CompletableFuture<>();
            //? if <26.1 {
            mc.tell(() -> {
            //?} else {
            mc.submit(() -> {
            //?}
                if (mc.screen == null && mc.level == null) {
                    future.complete("Game not ready yet — wait for the title screen to appear");
                    return;
                }

                try {
                    Object result = clazz.getMethod("run", Minecraft.class, LocalPlayer.class, ClientLevel.class)
                                         .invoke(null, mc, mc.player, mc.level);
                    future.complete(result);
                } catch (Throwable t) {
                    future.completeExceptionally(t.getCause() != null ? t.getCause() : t);
                }
            });

            Object result = future.get(10, TimeUnit.SECONDS);
            return new ScriptResult(true, result == null ? "null" : result.toString(), null);
        } catch (java.util.concurrent.TimeoutException e) {
            return new ScriptResult(false, null, "Execution timed out (>10s)");
        } catch (Exception e) {
            return new ScriptResult(false, null, "Error: " + e);
        }
    }
}
