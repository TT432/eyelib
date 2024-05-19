package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.grammer.MolangLexer;
import io.github.tt432.eyelib.molang.grammer.MolangParser;
import javassist.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * @author TT432
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MolangCompileHandler {
    private static int currIdx;

    public static MethodHandle compile(MolangValue value) {
        try {
            return tryCompile(value);
        } catch (NotFoundException | CannotCompileException | NoSuchMethodException | IOException |
                 IllegalAccessException e) {
            throw new MolangUncompilableException(e);
        }
    }

    public static MethodHandle tryCompile(MolangValue value) throws NotFoundException, CannotCompileException, IOException, NoSuchMethodException, IllegalAccessException {
        currIdx++;
        String classname = "CompiledMolang$" + currIdx;
        CtClass ctClass = classPool.makeClass(classname);

        CtClass scopeClass = classPool.get(MolangScope.class.getName());
        CtMethod method = new CtMethod(CtClass.floatType, "eval", new CtClass[]{scopeClass}, ctClass);
        method.setModifiers(Modifier.STATIC | Modifier.PUBLIC);

        var body = visitor.visitExprSet(
                new MolangParser(
                        new CommonTokenStream(
                                new MolangLexer(CharStreams.fromString(value.getContext())))
                ).exprSet());

        method.setBody("{" + body + "}");

        ctClass.addMethod(method);

        byte[] bytecode = ctClass.toBytecode();
        Class<?> aClass = classLoader.createClass(classname, bytecode, 0, bytecode.length);

        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        MethodType mt = MethodType.methodType(float.class, MolangScope.class);

        return publicLookup.findStatic(aClass, "eval", mt);
    }

    private static class CustomClassLoader extends ClassLoader {
        public CustomClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> createClass(String name, byte[] b, int off, int len) {
            return super.defineClass(name, b, off, len);
        }
    }

    private static boolean reloadable = true;

    private static final class MolangCompileHandlerReloadListener extends SimplePreparableReloadListener<Object> {
        @Override
        protected @NotNull Object prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
            return "";
        }

        @Override
        protected void apply(Object o, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
            reloadable = true;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEvent(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new MolangCompileHandlerReloadListener());
    }

    private static CustomClassLoader classLoader;
    private static ClassPool classPool;

    public static void onReload() {
        if (reloadable) {
            reloadable = false;
            classLoader = new CustomClassLoader(MolangScope.class.getClassLoader());
            classPool = new ClassPool();
            classPool.appendClassPath(new LoaderClassPath(classLoader));
        }
    }

    private static final MolangCompileVisitor visitor = new MolangCompileVisitor();
}
