package io.github.tt432.eyelib.nodegraph.eproject;

import io.github.tt432.eyelib.nodegraph.GraphLibrary;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * eproject 图项目：一个项目 = 名称 + 格式版本 + N 个命名图文档库。
 *
 * @param name      项目名（写入 project.json，仅元信息，不参与库寻址）
 * @param version   项目格式版本（当前 {@link #CURRENT_VERSION}）
 * @param libraries 库 id → 图文档库。保序（LinkedHashMap 语义）且不可变；
 *                  库 id 合法性约束见 {@link EprojectIo}（{@code [a-z0-9_./-]+} 且不含 {@code ".."}）。
 */
public record Eproject(String name, int version, Map<String, GraphLibrary> libraries) {

    /** 当前项目格式版本。读取时高于此版本视为「未来格式」，拒绝读取。 */
    public static final int CURRENT_VERSION = 1;

    public Eproject {
        Objects.requireNonNull(name, "name");
        // 拷贝保序 + 不可变包装，隔绝外部可变 Map 的后续修改
        libraries = Collections.unmodifiableMap(new LinkedHashMap<>(libraries));
    }

    /** 以当前格式版本构造。 */
    public static Eproject of(String name, Map<String, GraphLibrary> libraries) {
        return new Eproject(name, CURRENT_VERSION, libraries);
    }
}
