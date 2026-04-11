package io.github.tt432.eyelibmolang.compiler;

import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.ClassHierarchyResolver;
import io.github.dmlloyd.classfile.TypeKind;
import io.github.dmlloyd.classfile.extras.reflect.AccessFlag;
import io.github.dmlloyd.classfile.extras.reflect.ClassFileFormatVersion;
import io.github.tt432.eyelibmolang.MolangCompiledFunction;
import io.github.tt432.eyelibmolang.MolangUncompilableException;
import io.github.tt432.eyelibmolang.generated.MolangLexer;
import io.github.tt432.eyelibmolang.generated.MolangParser;
import io.github.tt432.eyelibmolang.type.MolangFloat;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Method;

import static io.github.dmlloyd.classfile.extras.constant.ConstantUtils.referenceClassDesc;
import static io.github.tt432.eyelibmolang.compiler.MolangClassDescs.CD_MolangObject;
import static io.github.tt432.eyelibmolang.compiler.MolangClassDescs.CD_MolangScope;
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
 * MolangCompiledFunction func = MolangCompileHandler.compile("math.sin(x)");
 * // 第一次调用时会自动导入之前保存的缓存，并开始自动导出监控
 *
 * // 程序结束时清理（可选，推荐）
 * MolangCompileHandler.shutdown(); // 会自动导出缓存并清理资源
 * </pre>
 *
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MolangCompileHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MolangCompileHandler.class);

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

    public static MolangCompiledFunction compile(String content) {
        cache.ensureInitialized();

        String normalizedContent = content.trim();

        if (normalizedContent.isBlank()) return MolangCompiledFunction.NULL;
        try {
            MolangFloat constant = MolangFloat.valueOf(Float.parseFloat(normalizedContent));
            return scope -> constant;
        } catch (NumberFormatException ignored) {
        }

        MolangCompiledFunction cached = cache.getCachedFunction(normalizedContent);
        if (cached != null) {
            return cached;
        }

        CompiledClassInfo exprInfo = cache.getClassInfoByExpression(normalizedContent);
        if (exprInfo != null) {
            try {
                MyClassLoader classLoader = newClassLoader();
                classLoader.myDefineClass(exprInfo.className, exprInfo.bytecode, 0, exprInfo.bytecode.length);
                var clazz = classLoader.loadClass(exprInfo.className);
                MolangCompiledFunction result = adaptToCompiledFunction(clazz.getDeclaredConstructors()[0].newInstance());
                cache.putFunctionCache(normalizedContent, result);
                return result;
            } catch (Throwable e) {
                LOGGER.debug("Failed to load H2 cached class {}, will recompile", exprInfo.className, e);
            }
        }

        String className = cache.reserveClassNameForExpression(normalizedContent).className();
        CompileContext compileContext = new CompileContext();
        compileContext.compiledClassName = (exprInfo != null ? exprInfo.className : className);

        try {
            MolangCompiledFunction result = tryCompile(normalizedContent, compileContext);

            CompiledClassInfo newClassInfo = new CompiledClassInfo(normalizedContent, compileContext.compiledClassName, compileContext.code);
            cache.upsertCompiledClassInfo(newClassInfo);
            cache.putFunctionCache(normalizedContent, result);

            return result;
        } catch (Throwable e) {
            exportClass(compileContext.compiledClassName, compileContext.code);
            throw new MolangUncompilableException("can't compile molang: " + normalizedContent + ", class name: " + compileContext.compiledClassName, e);
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

    private static MyClassLoader newClassLoader() {
        return new MyClassLoader(MolangCompileHandler.class.getClassLoader());
    }

    public static MolangCompiledFunction tryCompile(String molangString, CompileContext context) throws Throwable {
        if (molangString.isEmpty()) {
            return MolangCompiledFunction.NULL;
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
                        cl = Class.forName(result.replace('/', '.'), false, MolangCompileHandler.class.getClassLoader());
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
                                .withInterfaceSymbols(ClassDesc.of(MolangCompiledFunction.class.getName()))
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
                                                    LOGGER.error("parsing: {} with error:{}", molangString, e.getMessage());
                                                }
                                            });

                                            visitor.visit(molangParser.exprSet());

                                            codeBuilder.return_(TypeKind.REFERENCE);
                                        })));

        MyClassLoader classLoader = newClassLoader();
        classLoader.myDefineClass(compiledClassName, code, 0, code.length);
        var clazz = classLoader.loadClass(compiledClassName);
        return adaptToCompiledFunction(clazz.getDeclaredConstructors()[0].newInstance());
    }

    private static MolangCompiledFunction adaptToCompiledFunction(Object functionInstance) {
        if (functionInstance instanceof MolangCompiledFunction compiledFunction) {
            return compiledFunction;
        }

        try {
            Method apply = functionInstance.getClass().getMethod("apply", io.github.tt432.eyelibmolang.MolangScope.class);
            return scope -> {
                try {
                    return (io.github.tt432.eyelibmolang.type.MolangObject) apply.invoke(functionInstance, scope);
                } catch (ReflectiveOperationException invokeError) {
                    throw new RuntimeException(invokeError);
                }
            };
        } catch (ReflectiveOperationException e) {
            throw new ClassCastException("Cannot adapt cached compiled function of type " + functionInstance.getClass().getName());
        }
    }
}
