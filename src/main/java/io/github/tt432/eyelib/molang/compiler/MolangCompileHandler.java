package io.github.tt432.eyelib.molang.compiler;

import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.ClassHierarchyResolver;
import io.github.dmlloyd.classfile.TypeKind;
import io.github.dmlloyd.classfile.extras.reflect.AccessFlag;
import io.github.dmlloyd.classfile.extras.reflect.ClassFileFormatVersion;
import io.github.tt432.eyelib.molang.MolangUncompilableException;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.grammer.MolangLexer;
import io.github.tt432.eyelib.molang.grammer.MolangParser;
import io.github.tt432.eyelib.molang.type.MolangFloat;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.antlr.v4.runtime.*;

import java.io.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

import static io.github.dmlloyd.classfile.extras.constant.ConstantUtils.referenceClassDesc;
import static io.github.tt432.eyelib.molang.compiler.MolangClassDescs.CD_MolangObject;
import static io.github.tt432.eyelib.molang.compiler.MolangClassDescs.CD_MolangScope;
import static java.lang.constant.ConstantDescs.*;

/**
 * Molang编译处理器，提供表达式编译和缓存功能
 * <p>
 * 特性：
 * - 自动初始化：第一次使用时自动导入缓存
 * - 二级缓存：表达式级缓存 + 类级缓存
 * - 哈希冲突处理：支持多次哈希冲突的安全处理
 * - 自动导出：缓存5秒无修改后自动导出到文件
 * - 持久化缓存：支持导入/导出缓存到 .cache/eyelib/compile/ 目录
 * <p>
 * 使用示例：
 * <pre>
 * // 直接使用即可，无需手动初始化
 * MolangValue.MolangFunction func = MolangCompileHandler.compile("math.sin(x)");
 * // 第一次调用时会自动导入之前保存的缓存，并开始自动导出监控
 *
 * // 程序结束时清理（可选，推荐）
 * MolangCompileHandler.shutdown(); // 会自动导出缓存并清理资源
 * </pre>
 *
 * @author TT432
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MolangCompileHandler {
    private static final MolangCompileVisitor visitor = new MolangCompileVisitor();
    public static MolangCompileCache cache = MolangCompilorCacheHandler.getInstance();

    public static class CompileContext {
        String compiledClassName = "";
        byte[] code = new byte[0];
    }

    /**
     * 编译类信息
     */
    public record CompiledClassInfo(
            String originalExpression,
            String className,
            byte[] bytecode
    ) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

    }

    public static MolangValue.MolangFunction compile(String content) {
        cache.ensureInitialized();

        String normalizedContent = content.trim();

        if (normalizedContent.isBlank()) return MolangValue.MolangFunction.NULL;
        try {
            return new MolangValue.ConstMolangFunction(MolangFloat.valueOf(Float.parseFloat(normalizedContent)));
        } catch (NumberFormatException ignored) {
        }

        MolangValue.MolangFunction cached = cache.getCachedFunction(normalizedContent);
        if (cached != null) {
            return cached;
        }

        CompiledClassInfo exprInfo = cache.getClassInfoByExpression(normalizedContent);
        if (exprInfo != null) {
            try {
                MyClassLoader.INSTANCE.myDefineClass(exprInfo.className, exprInfo.bytecode, 0, exprInfo.bytecode.length);
                var clazz = MyClassLoader.INSTANCE.loadClass(exprInfo.className);
                MolangValue.MolangFunction result = (MolangValue.MolangFunction) clazz.getDeclaredConstructors()[0].newInstance();
                cache.putFunctionCache(normalizedContent, result);
                return result;
            } catch (Throwable e) {
                log.debug("Failed to load H2 cached class {}, will recompile", exprInfo.className, e);
            }
        }

        String className = cache.reserveClassNameForExpression(normalizedContent).className();
        CompileContext compileContext = new CompileContext();
        compileContext.compiledClassName = (exprInfo != null ? exprInfo.className : className);

        try {
            MolangValue.MolangFunction result = tryCompile(normalizedContent, compileContext);

            CompiledClassInfo newClassInfo = new CompiledClassInfo(normalizedContent, compileContext.compiledClassName, compileContext.code);
            cache.upsertCompiledClassInfo(newClassInfo);
            cache.putFunctionCache(normalizedContent, result);

            return result;
        } catch (Throwable e) {
            exportClass(compileContext.compiledClassName, compileContext.code);
            throw new MolangUncompilableException("can't compile molang: " + normalizedContent + ", class name: " + compileContext.compiledClassName, e);
        }
    }

    @Mod.EventBusSubscriber
    public static final class Events {
        @SubscribeEvent
        public static void onEvent(GameShuttingDownEvent event) {
            shutdown();
        }
    }

    /**
     * 关闭自动导出服务（用于程序结束时清理）
     */
    public static void shutdown() {
        cache.shutdown();
    }

    public static void exportClass(String className, byte[] code) {
        new File("eyelib_generatedClasses").mkdirs();
        try (var fs = new FileOutputStream("./eyelib_generatedClasses/" + className + ".class")) {
            fs.write(code);
        } catch (IOException ee) {
            throw new RuntimeException(ee);
        }
    }

    static class MyClassLoader extends ClassLoader {
        private static final MyClassLoader INSTANCE = new MyClassLoader(MolangValue.class.getClassLoader());

        protected MyClassLoader(ClassLoader parent) {
            super(parent);
        }

        public void myDefineClass(String className, byte[] b, int off, int len) {
            // 检查类是否已经加载
            try {
                loadClass(className);
                return; // 类已经存在，不需要重复定义
            } catch (ClassNotFoundException e) {
                // 类不存在，继续定义
            }
            defineClass(className, b, off, len);
        }
    }

    public static MolangValue.MolangFunction tryCompile(String molangString, CompileContext context) throws Throwable {
        if (molangString.isEmpty()) {
            return MolangValue.MolangFunction.NULL;
        }

        var compiledClassName = context.compiledClassName;

        var code = context.code = ClassFile.of()
                .withOptions(ClassFile.ClassHierarchyResolverOption.of(cd -> {
                    if (!cd.isClassOrInterface())
                        return null;

                    if (cd.equals(CD_Object))
                        return ClassHierarchyResolver.ClassHierarchyInfo.ofClass(null);

                    Class<?> cl = null;
                    try {
                        String result;
                        if (cd.isClassOrInterface()) {
                            String desc = cd.descriptorString();
                            result = desc.substring(1, desc.length() - 1);
                        } else {
                            throw new IllegalArgumentException(cd.descriptorString());
                        }
                        cl = Class.forName(result.replace('/', '.'), false, MolangValue.class.getClassLoader());
                    } catch (ClassNotFoundException ignored) {
                    }
                    if (cl == null) {
                        return null;
                    }

                    return cl.isInterface() ? ClassHierarchyResolver.ClassHierarchyInfo.ofInterface()
                            : ClassHierarchyResolver.ClassHierarchyInfo.ofClass(referenceClassDesc(cl.getSuperclass()));
                }))
                .build(ClassDesc.of(compiledClassName),
                        classBuilder -> classBuilder.withVersion(ClassFileFormatVersion.valueOf(Runtime.version()).major(), 0)
                                .withInterfaceSymbols(ClassDesc.of(MolangValue.MolangFunction.class.getName()))
                                .withField("originalString", CD_String, fieldBuilder -> {
                                    fieldBuilder.withFlags(AccessFlag.FINAL, AccessFlag.STATIC);
                                })
                                .withMethod("<clinit>", MethodTypeDesc.of(CD_void), ClassFile.ACC_STATIC, methodBuilder -> methodBuilder.withCode(codeBuilder -> {
                                    codeBuilder.ldc(molangString).putstatic(ClassDesc.of(compiledClassName), "originalString", CD_String).return_();
                                }))
                                .withMethod("<init>", MethodTypeDesc.of(CD_void), ClassFile.ACC_PUBLIC, methodBuilder -> methodBuilder.withCode(codeBuilder -> {
                                    codeBuilder.aload(0)
                                            .invokespecial(CD_Object, "<init>", MethodTypeDesc.of(CD_void))
                                            .return_();
                                }))
                                .withMethod("apply", MethodTypeDesc.of(CD_MolangObject, CD_MolangScope), ClassFile.ACC_PUBLIC,
                                        methodBuilder -> methodBuilder.withCode(codeBuilder -> {
                                            visitor.startVisitor(codeBuilder);

                                            MolangParser molangParser = new MolangParser(
                                                    new CommonTokenStream(
                                                            new MolangLexer(CharStreams.fromString(molangString)))
                                            );
                                            molangParser.addErrorListener(new BaseErrorListener() {
                                                @Override
                                                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                                                    log.error("parsing: {} with error:{}", molangString, e.getMessage());
                                                }
                                            });

                                            visitor.visit(molangParser.exprSet());

                                            codeBuilder.return_(TypeKind.REFERENCE);
                                        })));

        MyClassLoader.INSTANCE.myDefineClass(compiledClassName, code, 0, code.length);
        var clazz = MyClassLoader.INSTANCE.loadClass(compiledClassName);
        return (MolangValue.MolangFunction) clazz.getDeclaredConstructors()[0].newInstance();
    }
}
