package io.github.tt432.eyelib.client.loader;

import com.google.gson.GsonBuilder;
import io.github.tt432.eyelib.bridge.client.loader.SimpleJsonWithSuffixResourceReloadListener;
/**
 * 资源目录加载器的基类：实际扫描路径是 {@code assets/<命名空间>/eyelib/<directory>/*<suffix>}，
 * 不是 {@code assets/<命名空间>/<directory>/}——目录名统一挂到 {@code eyelib/} 前缀下，
 * 避免与其它模组的同名目录（原版 addon 包也按 {@code attachables/}、{@code models/entity/} 布局）冲突。
 * 例：attachable 定义放 {@code assets/<ns>/eyelib/attachables/<name>.json}。
 *
 * @author TT432
 */
public abstract class BrResourcesLoader extends SimpleJsonWithSuffixResourceReloadListener {
    public BrResourcesLoader(String directory, String suffix) {
        super(new GsonBuilder().setLenient().create(), "eyelib/" + directory, suffix);
    }
}