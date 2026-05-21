package io.github.tt432.eyelib.common.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.SimpleCompiler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NullMarked;

/** @author TT432 */
@NullMarked
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

    public record ScriptResult(boolean success, @org.jspecify.annotations.Nullable String result, @org.jspecify.annotations.Nullable String error) {}

    public static ScriptResult evaluate(String code) {
        try {
            String indentedCode = code.strip();
            String fullSource = TEMPLATE.formatted(indentedCode);
            SimpleCompiler compiler = new SimpleCompiler();
            compiler.setParentClassLoader(ScriptEvalService.class.getClassLoader());
            compiler.cook(fullSource);
            Class<?> clazz = compiler.getClassLoader().loadClass("_EyelibScript");

            Minecraft mc = Minecraft.getInstance();

            CompletableFuture<Object> future = new CompletableFuture<>();
            mc.tell(() -> {
                try {
                    Object result = clazz.getMethod("run", Minecraft.class, LocalPlayer.class, ClientLevel.class)
                            .invoke(null, mc, mc.player, mc.level);
                    future.complete(result);
                } catch (Throwable t) {
                    Throwable cause = t.getCause() != null ? t.getCause() : t;
                    future.completeExceptionally(cause);
                }
            });

            Object result = future.get(10, TimeUnit.SECONDS);
            return new ScriptResult(true, result == null ? "null" : result.toString(), null);
        } catch (CompileException e) {
            return new ScriptResult(false, null, "Compile error:\n" + e.getMessage());
        } catch (java.util.concurrent.TimeoutException e) {
            return new ScriptResult(false, null, "Execution timed out (>10s)");
        } catch (Exception e) {
            return new ScriptResult(false, null, "Error: " + e);
        }
    }
}
