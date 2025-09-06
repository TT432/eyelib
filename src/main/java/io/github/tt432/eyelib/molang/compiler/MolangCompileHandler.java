package io.github.tt432.eyelib.molang.compiler;

import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.ClassHierarchyResolver;
import io.github.dmlloyd.classfile.TypeKind;
import io.github.dmlloyd.classfile.extras.reflect.AccessFlag;
import io.github.tt432.eyelib.molang.MolangUncompilableException;
import io.github.tt432.eyelib.molang.MolangValue;
import io.github.tt432.eyelib.molang.grammer.MolangLexer;
import io.github.tt432.eyelib.molang.grammer.MolangParser;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import org.antlr.v4.runtime.*;

import java.io.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.dmlloyd.classfile.extras.constant.ConstantUtils.referenceClassDesc;
import static io.github.dmlloyd.classfile.extras.reflect.ClassFileFormatVersion.RELEASE_21;
import static io.github.tt432.eyelib.molang.compiler.MolangClassDescs.*;
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

    private static final Map<String, MolangValue.MolangFunction> expressionCache = new HashMap<>();

    @Getter
    private static final Map<String, CompiledClassInfo> classCache = new HashMap<>();

    private static final Path CACHE_DIR = Paths.get(".cache", "eyelib", "compile");
    private static final String CACHE_FILE_NAME = "molang_cache.dat";

    // 自动导出相关字段
    private static final long AUTO_EXPORT_DELAY_MS = 5000; // 5秒
    private static final AtomicReference<ScheduledFuture<?>> currentExportTask = new AtomicReference<>();
    private static final ScheduledExecutorService autoExportExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "MolangCache-AutoExport");
        t.setDaemon(true);
        return t;
    });
    private static volatile boolean initialized = false;

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

    /**
     * 持久化缓存数据结构
     */
    private record CacheData(
            Map<String, CompiledClassInfo> classInfoMap
    ) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private CacheData(Map<String, CompiledClassInfo> classInfoMap) {
            this.classInfoMap = new HashMap<>(classInfoMap);
        }
    }

    public static MolangValue.MolangFunction compile(String content) {
        // 第一次使用时自动初始化
        ensureInitialized();

        String normalizedContent = content.trim();

        if (normalizedContent.isBlank()) return MolangValue.MolangFunction.NULL;

        // 先检查表达式缓存
        MolangValue.MolangFunction cached = expressionCache.get(normalizedContent);
        if (cached != null) {
            return cached;
        }

        // 查找可用的类名，处理哈希冲突
        String className = findAvailableClassName(normalizedContent);

        // 检查类缓存
        CompiledClassInfo classInfo = classCache.get(className);
        if (classInfo != null) {
            // 验证表达式是否匹配（应该总是匹配，因为我们已经处理了冲突）
            if (classInfo.originalExpression.equals(normalizedContent)) {
                try {
                    MyClassLoader.INSTANCE.myDefineClass(className, classInfo.bytecode, 0, classInfo.bytecode.length);
                    var clazz = MyClassLoader.INSTANCE.loadClass(className);
                    MolangValue.MolangFunction result = (MolangValue.MolangFunction) clazz.getDeclaredConstructors()[0].newInstance();
                    expressionCache.put(normalizedContent, result);
                    return result;
                } catch (Exception e) {
                    log.warn("Failed to load cached class {}, recompiling", className, e);
                    classCache.remove(className);
                }
            } else {
                // 这不应该发生，因为 findAvailableClassName 应该处理了所有冲突
                log.error("Unexpected hash collision after findAvailableClassName for {}", normalizedContent);
            }
        }

        CompileContext compileContext = new CompileContext();
        compileContext.compiledClassName = className;

        try {
            MolangValue.MolangFunction result = tryCompile(normalizedContent, compileContext);

            // 缓存编译结果
            CompiledClassInfo newClassInfo = new CompiledClassInfo(normalizedContent, className, compileContext.code);
            classCache.put(className, newClassInfo);
            expressionCache.put(normalizedContent, result);
            markCacheModified();

            return result;
        } catch (Throwable e) {
            exportClass(compileContext.compiledClassName, compileContext.code);
            throw new MolangUncompilableException("can't compile molang: " + normalizedContent + ", class name: " + compileContext.compiledClassName, e);
        }
    }

    /**
     * 查找可用的类名，处理多次哈希冲突
     */
    private static String findAvailableClassName(String content) {
        String hash = generateContentHash(content);
        String baseName = "CompiledMolang$" + hash;

        // 首先尝试无后缀的类名
        String className = baseName;
        CompiledClassInfo existingInfo = classCache.get(className);
        if (existingInfo == null || existingInfo.originalExpression().equals(content)) {
            return className;
        }

        // 如果有冲突，尝试带后缀的类名
        int maxRetries = 1000; // 防止无限循环
        for (int suffix = 1; suffix <= maxRetries; suffix++) {
            className = baseName + "_" + suffix;
            existingInfo = classCache.get(className);
            if (existingInfo == null || existingInfo.originalExpression().equals(content)) {
                if (suffix > 1) {
                    log.warn("Hash collision resolved with suffix {} for expression: {}", suffix, content);
                }
                return className;
            }
        }

        // 如果所有后缀都冲突，生成一个基于时间戳的唯一类名
        String fallbackClassName = baseName + "_" + System.currentTimeMillis();
        log.error("Excessive hash collisions detected, using timestamp-based name: {}", fallbackClassName);
        return fallbackClassName;
    }

    /**
     * 生成内容哈希
     */
    private static String generateContentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).substring(0, 16); // 取前16位避免类名过长
        } catch (NoSuchAlgorithmException e) {
            // 降级到简单哈希
            return Integer.toHexString(content.hashCode());
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * 导出缓存到文件
     */
    public static void exportCache() {
        try {
            Files.createDirectories(CACHE_DIR);
            Path cacheFile = CACHE_DIR.resolve(CACHE_FILE_NAME);

            CacheData cacheData = new CacheData(classCache);

            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(cacheFile))) {
                oos.writeObject(cacheData);
            }

            log.info("Exported {} compiled expressions to cache file: {}", classCache.size(), cacheFile);
        } catch (IOException e) {
            log.error("Failed to export cache", e);
        }
    }

    /**
     * 从文件导入缓存
     */
    public static void importCache() {
        Path cacheFile = CACHE_DIR.resolve(CACHE_FILE_NAME);

        if (!Files.exists(cacheFile)) {
            log.debug("Cache file does not exist: {}", cacheFile);
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(cacheFile))) {
            CacheData cacheData = (CacheData) ois.readObject();

            int importedCount = 0;
            for (Map.Entry<String, CompiledClassInfo> entry : cacheData.classInfoMap.entrySet()) {
                String className = entry.getKey();
                CompiledClassInfo classInfo = entry.getValue();

                // 验证缓存完整性
                if (classInfo.originalExpression != null && classInfo.bytecode != null) {
                    classCache.put(className, classInfo);
                    importedCount++;
                }
            }

            log.info("Imported {} compiled expressions from cache file: {}", importedCount, cacheFile);
        } catch (IOException | ClassNotFoundException e) {
            log.error("Failed to import cache", e);
        }
    }

    /**
     * 清除所有缓存
     */
    public static void clearCache() {
        expressionCache.clear();
        classCache.clear();
        markCacheModified();
        log.info("Cleared all caches");
    }

    /**
     * 获取缓存统计信息
     */
    public static void printCacheStats() {
        log.info("Cache Stats - Expression Cache: {}, Class Cache: {}",
                expressionCache.size(), classCache.size());
    }

    /**
     * 确保系统已初始化（第一次使用时自动调用）
     */
    private static void ensureInitialized() {
        if (!initialized) {
            synchronized (MolangCompileHandler.class) {
                if (!initialized) {
                    try {
                        importCache();
                        initialized = true;
                        log.info("MolangCompileHandler auto-initialized on first use");
                    } catch (Exception e) {
                        log.warn("Auto-initialization failed, continuing without cache", e);
                        initialized = true; // 即使失败也标记为已初始化，避免重复尝试
                    }
                }
            }
        }
    }


    /**
     * 标记缓存已修改
     */
    private static void markCacheModified() {
        scheduleAutoExport();
    }

    /**
     * 调度自动导出任务（取消之前的任务，重新调度）
     */
    private static void scheduleAutoExport() {
        // 取消之前的导出任务（如果存在）
        ScheduledFuture<?> previousTask = currentExportTask.get();
        if (previousTask != null) {
            previousTask.cancel(false);
        }

        // 调度新的导出任务
        ScheduledFuture<?> newTask = autoExportExecutor.schedule(() -> {
            try {
                exportCache();
                log.debug("Auto exported cache after {}ms of inactivity", AUTO_EXPORT_DELAY_MS);
            } catch (Exception e) {
                log.error("Auto export failed", e);
            } finally {
                // 任务完成后清除引用
                currentExportTask.compareAndSet(Thread.currentThread().isInterrupted() ? null : currentExportTask.get(), null);
            }
        }, AUTO_EXPORT_DELAY_MS, TimeUnit.MILLISECONDS);

        // 更新当前任务引用
        currentExportTask.set(newTask);
    }

    @EventBusSubscriber
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
        // 取消当前的导出任务
        ScheduledFuture<?> currentTask = currentExportTask.getAndSet(null);
        if (currentTask != null) {
            currentTask.cancel(false);
        }

        // 最后一次导出
        try {
            exportCache();
            log.info("Final cache export on shutdown");
        } catch (Exception e) {
            log.error("Final export failed", e);
        }

        autoExportExecutor.shutdown();
        try {
            if (!autoExportExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                autoExportExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            autoExportExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
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
                        classBuilder -> classBuilder.withVersion(RELEASE_21.major(), 0)
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
