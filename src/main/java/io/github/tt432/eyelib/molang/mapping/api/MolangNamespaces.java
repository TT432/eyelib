package io.github.tt432.eyelib.molang.mapping.api;

import io.github.tt432.eyelib.molang.MolangScope;
import io.github.tt432.eyelib.molang.type.MolangObject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 动态 Molang 命名空间注册表（domain 层，无平台依赖）。
 *
 * <p>运行期由 mod 注册前缀与提供者，用于支撑 {@code query.owner.health} 这类由运行期
 * 对象提供的成员。匹配规则：
 * <ul>
 *     <li>按点分分段做<b>最长前缀</b>匹配，比较大小写不敏感；</li>
 *     <li>同长度前缀多个注册时，<b>后注册者</b>优先（{@link Registration#close()} 撤销自己的注册）；</li>
 *     <li>命中后把剩余分段以 {@code "."} 拼回作为 {@code path} 交给提供者。</li>
 * </ul>
 *
 * <p>热路径：注册表为空时只有一次 volatile 布尔读，零分配返回 {@code null}；
 * 非空时按被访问的完整点分名（原始拼写）memo「哪个注册命中」——值不缓存，因为
 * 值依赖传入的 {@link MolangScope}。注册/注销使 {@link #epoch()} 自增并整表清空 memo。
 *
 * <p>解析顺序契约（由 {@code MolangRuntimeSupport} 保证，不在本类内实现）：
 * {@link MolangScope} 覆盖 → 本表 → {@link MolangMappingTree} 零参绑定。
 *
 * @author TT432
 */
public final class MolangNamespaces {
    /** 注册句柄：{@link #close()} 撤销本次注册且幂等（重复 close 无效果）。 */
    public interface Registration extends AutoCloseable {
        @Override
        void close();
    }

    /**
     * 一次注册。{@code segments} 已小写化并切分，只在注册期分配一次。
     */
    private record Entry(long seq, String[] segments, MolangNamespace provider) {
    }

    /** 一次匹配结果：命中的注册 + 相对路径（原始拼写）。 */
    private record Match(Entry entry, String path) {
    }

    /** memo 值携带计算时的纪元，纪元错位即视为未命中，避免把旧表结果留在新纪元。 */
    private record Resolution(long epoch, @Nullable Match match) {
    }

    private static final Object LOCK = new Object();
    private static final AtomicLong SEQ = new AtomicLong();
    private static final ConcurrentHashMap<String, Resolution> MEMO = new ConcurrentHashMap<>();

    /** 写序：先 {@link #entries} 后 {@link #empty}；读侧先看 {@link #empty} 再看表。 */
    private static volatile List<Entry> entries = List.of();
    private static volatile boolean empty = true;
    private static volatile long epoch;

    private MolangNamespaces() {
    }

    /**
     * 注册一个动态命名空间。
     *
     * @param namespace 点分命名空间（大小写不敏感，如 {@code "query.owner"}）
     * @param provider  实际取值者
     * @return 注册句柄；{@link Registration#close()} 撤销注册
     * @throws IllegalArgumentException 命名空间为空白或含空分段（如 {@code "a..b"}、{@code "a."}）
     */
    public static Registration register(String namespace, MolangNamespace provider) {
        if (namespace == null) {
            throw new IllegalArgumentException("namespace must not be null");
        }
        if (provider == null) {
            throw new IllegalArgumentException("namespace provider must not be null");
        }

        String trimmed = namespace.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        String[] segments = trimmed.toLowerCase(Locale.ROOT).split("\\.", -1);
        for (String segment : segments) {
            if (segment.isEmpty()) {
                throw new IllegalArgumentException("namespace must not contain empty segments: '" + namespace + "'");
            }
        }

        Entry entry = new Entry(SEQ.incrementAndGet(), segments, provider);
        synchronized (LOCK) {
            List<Entry> next = new ArrayList<>(entries);
            next.add(entry);
            entries = List.copyOf(next);
            empty = false;
            bumpEpoch();
        }
        return () -> remove(entry);
    }

    /** 当前注册数量（诊断/测试用）。 */
    public static int size() {
        return entries.size();
    }

    /** 注册表纪元：每次注册/注销/{@link #clear()} 自增，供缓存失效检测。 */
    public static long epoch() {
        return epoch;
    }

    /** 清空全部注册（仅测试使用；生产路径请用 {@link Registration#close()}）。 */
    public static void clear() {
        synchronized (LOCK) {
            entries = List.of();
            empty = true;
            bumpEpoch();
        }
    }

    /**
     * 解析一次成员访问。
     *
     * @param scope      当前求值作用域，原样交给提供者
     * @param dottedName 完整点分被访问名（如 {@code "query.owner.health"}）
     * @return 提供者的值；未注册、无前缀命中或提供者本次返回 {@code null} 时为 {@code null}
     */
    public static @Nullable MolangObject resolve(MolangScope scope, String dottedName) {
        if (empty || scope == null || dottedName == null || dottedName.isEmpty()) {
            return null;
        }

        long readEpoch = epoch;
        Resolution cached = MEMO.get(dottedName);
        @Nullable Match match;
        if (cached != null && cached.epoch() == readEpoch) {
            match = cached.match();
        } else {
            match = computeMatch(dottedName);
            // 良性竞争：结果只取决于当次快照；纪元在计算期间变化则不写缓存
            if (epoch == readEpoch) {
                MEMO.put(dottedName, new Resolution(readEpoch, match));
            }
        }

        if (match == null) {
            return null;
        }
        return match.entry().provider().resolve(scope, match.path());
    }

    private static void remove(Entry entry) {
        synchronized (LOCK) {
            List<Entry> current = entries;
            if (!current.contains(entry)) {
                return;
            }
            List<Entry> next = new ArrayList<>(current.size());
            for (Entry candidate : current) {
                if (candidate != entry) {
                    next.add(candidate);
                }
            }
            entries = List.copyOf(next);
            empty = next.isEmpty();
            bumpEpoch();
        }
    }

    private static void bumpEpoch() {
        epoch++;
        MEMO.clear();
    }

    /**
     * 计算最长前缀命中（无命中返回 {@code null}）。表遍历只在 memo 未命中时发生。
     */
    private static @Nullable Match computeMatch(String dottedName) {
        List<Entry> snapshot = entries;
        if (snapshot.isEmpty()) {
            return null;
        }

        String[] nameSegments = dottedName.split("\\.", -1);
        @Nullable Match best = null;
        for (Entry candidate : snapshot) {
            String[] prefix = candidate.segments();
            if (prefix.length > nameSegments.length) {
                continue;
            }
            boolean matched = true;
            for (int i = 0; i < prefix.length; i++) {
                if (!prefix[i].equalsIgnoreCase(nameSegments[i])) {
                    matched = false;
                    break;
                }
            }
            if (!matched) {
                continue;
            }
            if (best == null) {
                best = new Match(candidate, joinRemaining(nameSegments, prefix.length));
                continue;
            }
            // 最长前缀优先；等长时注册序号大者（后注册）胜出
            int bestLength = best.entry().segments().length;
            if (prefix.length > bestLength
                    || (prefix.length == bestLength && candidate.seq() > best.entry().seq())) {
                best = new Match(candidate, joinRemaining(nameSegments, prefix.length));
            }
        }
        return best;
    }

    private static String joinRemaining(String[] segments, int from) {
        if (from >= segments.length) {
            return "";
        }
        if (from == segments.length - 1) {
            return segments[from];
        }
        StringBuilder builder = new StringBuilder();
        for (int i = from; i < segments.length; i++) {
            if (i > from) {
                builder.append('.');
            }
            builder.append(segments[i]);
        }
        return builder.toString();
    }
}
