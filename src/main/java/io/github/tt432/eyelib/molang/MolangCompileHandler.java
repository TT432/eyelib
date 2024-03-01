package io.github.tt432.eyelib.molang;

import io.github.tt432.eyelib.molang.grammer.MolangLexer;
import io.github.tt432.eyelib.molang.grammer.MolangParser;
import javassist.*;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;

/**
 * @author TT432
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MolangCompileHandler {
    private static final List<MolangValue> values = new ArrayList<>();

    public static void register(MolangValue value) {
        values.add(value);
    }

    static class CustomClassLoader extends ClassLoader {
        public CustomClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> createClass(String name, byte[] b, int off, int len) {
            return super.defineClass(name, b, off, len);
        }
    }

    static boolean reloadable = true;

    static class MolangCompileHandlerReloadListener extends SimplePreparableReloadListener {
        @Override
        protected Object prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
            return "";
        }

        @Override
        protected void apply(Object o, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
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

    public static void tryCompileAll(String className) {
        try {
            compileAll(className);
        } catch (NotFoundException | CannotCompileException | NoSuchMethodException | IOException |
                 IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static void compileAll(String className) throws NoSuchMethodException, CannotCompileException, NotFoundException, IOException, IllegalAccessException {
        CtClass ctClass = classPool.makeClass(className);

        CtClass scopeClass = classPool.get(MolangScope.class.getName());

        CtMethod emptyEval = new CtMethod(CtClass.floatType, "emptyEval",
                new CtClass[]{CtClass.floatType}, ctClass);
        emptyEval.setModifiers(Modifier.STATIC | Modifier.PUBLIC);
        emptyEval.setBody("{return (float) $1;}");
        ctClass.addMethod(emptyEval);

        for (int i = 0; i < values.size(); i++) {
            MolangValue valuei = values.get(i);

            CtMethod method = new CtMethod(CtClass.floatType, "eval${i}",
                    new CtClass[]{scopeClass}, ctClass);
            method.setModifiers(Modifier.STATIC | Modifier.PUBLIC);

            var body = visitor.visitExprSet(
                    new MolangParser(
                            new CommonTokenStream(
                                    new MolangLexer(CharStreams.fromString(valuei.getContext())))
                    ).exprSet());

            System.out.println(body);

            method.setBody("{${body}}");

            ctClass.addMethod(method);
        }

        byte[] bytecode = ctClass.toBytecode();
        Class<?> aClass = classLoader.createClass(className, bytecode, 0, bytecode.length);

        MethodHandles.Lookup publicLookup = MethodHandles.publicLookup();
        MethodType mt = MethodType.methodType(float.class, MolangScope.class);

        for (int i = 0; i < values.size(); i++) {
            MolangValue valuei = values.get(i);
            valuei.setMethod(publicLookup.findStatic(aClass, "eval${i}", mt));
        }

        values.clear();
    }
}
