package io.github.tt432.eyelib.molang.type;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Molang struct 值（官方 syntax-guide「Structs」节）：成员按使用隐式定义，可整体赋值
 * （引用语义——{@code v.moo = v.location} 后两者共享成员，与文档「高效传递」目的一致；
 * 深拷贝语义为 Deferred 兼容策略，见 compatibility-semantics-matrix）。
 *
 * <p>成员表插入序。非线程安全——molang 求值单线程约定与 {@code MolangScope} 一致；
 * 跨线程快照请走 {@link #members()} 自行拷贝。
 */
public final class MolangStruct implements MolangObject {
    private final Map<String, MolangObject> members = new LinkedHashMap<>();

    public @Nullable MolangObject get(String member) {
        return members.get(member);
    }

    public void set(String member, MolangObject value) {
        members.put(member, value);
    }

    public void remove(String member) {
        members.remove(member);
    }

    /** 成员只读视图（插入序），供调试展示与格式化。 */
    public Map<String, MolangObject> members() {
        return Collections.unmodifiableMap(members);
    }

    /**
     * 点分相对路径读取（{@code "a.b.c"}）；任一层缺失或落进非标量的成员路径 → null。
     */
    public @Nullable MolangObject getPath(String path) {
        MolangObject current = this;
        int from = 0;
        while (true) {
            int dot = path.indexOf('.', from);
            String segment = dot < 0 ? path.substring(from) : path.substring(from, dot);
            if (!(current instanceof MolangStruct struct)) {
                return null;
            }
            current = struct.get(segment);
            if (current == null) {
                return null;
            }
            if (dot < 0) {
                return current;
            }
            from = dot + 1;
        }
    }

    /**
     * 点分相对路径写入：缺失/非标量的中间层按 BE 隐式 struct 语义重建（覆盖标量）。
     */
    public void setPath(String path, MolangObject value) {
        MolangStruct current = this;
        int from = 0;
        while (true) {
            int dot = path.indexOf('.', from);
            if (dot < 0) {
                current.set(path.substring(from), value);
                return;
            }
            String segment = path.substring(from, dot);
            MolangObject next = current.get(segment);
            MolangStruct nextStruct;
            if (next instanceof MolangStruct s) {
                nextStruct = s;
            } else {
                nextStruct = new MolangStruct();
                current.set(segment, nextStruct);
            }
            current = nextStruct;
            from = dot + 1;
        }
    }

    @Override
    public float asFloat() {
        return 0;
    }

    @Override
    public boolean asBoolean() {
        return false;
    }

    @Override
    public String asString() {
        return members.keySet().toString();
    }

    @Override
    public boolean isNumber() {
        return false;
    }
}
