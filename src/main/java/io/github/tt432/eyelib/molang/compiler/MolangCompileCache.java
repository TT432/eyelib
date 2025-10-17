package io.github.tt432.eyelib.molang.compiler;

import io.github.tt432.eyelib.molang.MolangValue;

/**
 * 统一的 Molang 编译缓存接口，供编译处理器依赖。
 */
public interface MolangCompileCache {

    /** 为表达式分配或获取稳定的数字ID与类名 */
    record ClassNameId(long id, String className) {}

    void ensureInitialized();

    MolangValue.MolangFunction getCachedFunction(String expression);

    void putFunctionCache(String expression, MolangValue.MolangFunction func);

    MolangCompileHandler.CompiledClassInfo getClassInfoByClassName(String className);

    MolangCompileHandler.CompiledClassInfo getClassInfoByExpression(String expression);

    ClassNameId reserveClassNameForExpression(String expression);

    void upsertCompiledClassInfo(MolangCompileHandler.CompiledClassInfo info);

    void exportCache();

    void importCache();

    void shutdown();
}