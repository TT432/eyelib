package io.github.tt432.eyelib.molang.scratch;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scratch 积木实例（可变树节点）。
 *
 * <p>结构：{@code kind} 决定规格；{@code fields} 存字面值/运算符/名称等内联文本；
 * {@code sockets} 存表达式子积木（PILL/BOOL 形状嵌入）；{@code bodies} 存 C 形积木的
 * 语句体（语句列表）。语句积木不持有 next 指针——堆叠关系由其所在的语句列表
 * （脚本顶层或某个 body）表达。
 *
 * <p>身份：实例身份即引用身份（UI 拖拽、命中测试按引用比对），无 uid。
 *
 * @author TT432
 */
public final class ScratchBlock {
    private final ScratchKind kind;
    private final Map<String, String> fields;
    private final List<Socket> sockets;
    private final List<List<ScratchBlock>> bodies;

    private ScratchBlock(ScratchKind kind) {
        this.kind = kind;
        this.fields = new LinkedHashMap<>(kind.fieldDefaults());
        this.sockets = new ArrayList<>();
        for (ScratchKind.SocketDef def : kind.sockets()) {
            this.sockets.add(new Socket(def.name(), def.optional()));
        }
        this.bodies = new ArrayList<>();
        if (kind.hasBody()) {
            this.bodies.add(new ArrayList<>());
        }
    }

    /** 按 kind 规格创建实例（默认字段值 + 静态插槽 + 空语句体）。 */
    public static ScratchBlock of(ScratchKind kind) {
        return new ScratchBlock(kind);
    }

    /** 深拷贝（调色板拖出 = 模板复制）。 */
    public ScratchBlock copy() {
        ScratchBlock copy = new ScratchBlock(kind);
        copy.fields.putAll(fields);
        // 构造器已按规格建了静态插槽，先清再按实例实况拷贝（CALL 变长插槽同理）
        copy.sockets.clear();
        for (int i = 0; i < sockets.size(); i++) {
            Socket src = sockets.get(i);
            copy.sockets.add(new Socket(src.name(), src.optional(),
                    src.child() == null ? null : src.child().copy()));
        }
        copy.bodies.clear();
        for (List<ScratchBlock> body : bodies) {
            List<ScratchBlock> bodyCopy = new ArrayList<>();
            for (ScratchBlock child : body) {
                bodyCopy.add(child.copy());
            }
            copy.bodies.add(bodyCopy);
        }
        return copy;
    }

    public ScratchKind kind() {
        return kind;
    }

    /** 字段值（无此字段 → 空串，便于 UI 直接绑定）。 */
    public String field(String name) {
        return fields.getOrDefault(name, "");
    }

    public void setField(String name, String value) {
        fields.put(name, value);
    }

    /** 字段名表（规格顺序）。 */
    public List<String> fieldNames() {
        return List.copyOf(fields.keySet());
    }

    public List<Socket> sockets() {
        return sockets;
    }

    public Socket socket(int index) {
        return sockets.get(index);
    }

    /** 按名查插槽（无 → null）。 */
    public @Nullable Socket socket(String name) {
        for (Socket socket : sockets) {
            if (socket.name().equals(name)) {
                return socket;
            }
        }
        return null;
    }

    /** 按名取规格保证存在的插槽（缺失 = 编程错误）。 */
    public Socket requireSocket(String name) {
        Socket socket = socket(name);
        if (socket == null) {
            throw new IllegalStateException("积木 " + kind + " 缺少插槽 " + name);
        }
        return socket;
    }

    /** 追加表达式插槽（CALL 变长参数）。 */
    public Socket addSocket(String name) {
        Socket socket = new Socket(name, false);
        sockets.add(socket);
        return socket;
    }

    /** 删除末尾插槽（CALL 变长参数；保底 0 个）。 */
    public void removeLastSocket() {
        if (!sockets.isEmpty()) {
            sockets.remove(sockets.size() - 1);
        }
    }

    /** 语句体（C 形积木；非 C 形为空表）。 */
    public List<List<ScratchBlock>> bodies() {
        return bodies;
    }

    /** 第 0 语句体（C 形积木便利访问）。 */
    public List<ScratchBlock> body() {
        return bodies.get(0);
    }

    /** 表达式插槽：可含一个表达式子积木或为空。 */
    public static final class Socket {
        private final String name;
        private final boolean optional;
        private @Nullable ScratchBlock child;

        private Socket(String name, boolean optional) {
            this(name, optional, null);
        }

        private Socket(String name, boolean optional, @Nullable ScratchBlock child) {
            this.name = name;
            this.optional = optional;
            this.child = child;
        }

        public String name() {
            return name;
        }

        /** 可空插槽留空 = 语法省略（break/continue 无值）；必选插槽留空导出为 0。 */
        public boolean optional() {
            return optional;
        }

        public @Nullable ScratchBlock child() {
            return child;
        }

        public void setChild(@Nullable ScratchBlock child) {
            this.child = child;
        }
    }
}
