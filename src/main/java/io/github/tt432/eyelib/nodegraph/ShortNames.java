package io.github.tt432.eyelib.nodegraph;

import com.google.gson.JsonElement;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * 短名派生（规格 nodegraph-shortname-elimination D1/D3）：
 * <b>有效短名 = 显式 {@code short_name} 非空 ? 显式值 : derive(标识符)</b>。
 *
 * <p>短名是输出 JSON 中实体声明表的键、RC/AC 与实体之间的运行时契约；
 * authoring 层不管理它——实体组装器建表与 RC/AC/animate 发射两端对同一标识符
 * 套用同一纯函数，结果必然一致，跨图库契约无需手工同步。
 *
 * <p>派生规则（{@link #sanitize}）：'/' → '.'；lowercase；非 {@code [a-z0-9_.]} → '_'；
 * 段首数字 → 段首插 '_'；连续/首尾 '.' 折叠。产出是合法 molang 成员访问路径
 * （运行时 scope 扁平键查找支持多点键，见 RenderControllerEntry.initArrays /
 * MolangRuntimeSupport.resolveMemberAccess）。
 */
public final class ShortNames {
    private ShortNames() {
    }

    /** 显式覆盖选项 id（各 ref 节点一致）。 */
    public static final String SHORT_NAME_OPTION = "short_name";

    /**
     * ref 节点类型 → 标识符选项 id；非 ref 类型返回 {@code null}。
     * ref.rc 无短名语义（render_controllers 直接写标识符），不在此列。
     */
    public static @Nullable String valueOptionOf(String nodeType) {
        return switch (nodeType) {
            case "ref.geometry", "ref.animation", "ref.ac" -> "identifier";
            case "ref.texture" -> "path";
            case "ref.material" -> "material";
            default -> null;
        };
    }

    /** 该类别 ref 的有效短名会发射为 molang 成员访问（须过 {@link #isSanitized} 校验）。 */
    public static boolean isMolangEmitted(String nodeType) {
        return switch (nodeType) {
            case "ref.geometry", "ref.texture", "ref.material" -> true;
            default -> false;
        };
    }

    /** 派生短名：对标识符做 {@link #sanitize}。 */
    public static String derive(String identifier) {
        return sanitize(identifier);
    }

    /**
     * 规范化 molang 成员路径：'/' → '.'、lowercase、非法字符 → '_'、
     * 段首数字前插 '_'、折叠连续 '.'、剥首末 '.'。空输入原样返回空。
     */
    public static String sanitize(String raw) {
        if (raw.isEmpty()) {
            return raw;
        }
        String s = raw.toLowerCase(Locale.ROOT).replace('/', '.');
        StringBuilder out = new StringBuilder(s.length() + 2);
        boolean segmentStart = true;
        boolean lastWasDot = true; // 视为串首即段首；同时剥前导 '.'
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.') {
                if (!lastWasDot) {
                    out.append('.');
                }
                lastWasDot = true;
                segmentStart = true;
                continue;
            }
            boolean valid = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_';
            char ch = valid ? c : '_';
            if (segmentStart && ch >= '0' && ch <= '9') {
                out.append('_');
            }
            out.append(ch);
            lastWasDot = false;
            segmentStart = false;
        }
        // 剥末尾 '.'
        int len = out.length();
        if (len > 0 && out.charAt(len - 1) == '.') {
            out.setLength(len - 1);
        }
        return out.toString();
    }

    /** 显式覆盖是否已是合法 molang 成员路径（sanitize 后不变）。 */
    public static boolean isSanitized(String explicit) {
        return sanitize(explicit).equals(explicit);
    }

    /**
     * ref 节点的有效短名：显式 {@code short_name} 非空 → 原样返回（外部契约逃生舱）；
     * 否则对标识符选项派生。标识符也空 → 空串（验证器报 INVALID_SHORT_NAME）。
     */
    public static String effective(NodeInstance node, NodeType type) {
        String explicit = node.option(SHORT_NAME_OPTION, type).map(JsonElement::getAsString).orElse("");
        if (!explicit.isEmpty()) {
            return explicit;
        }
        String valueOption = valueOptionOf(node.type());
        if (valueOption == null) {
            return "";
        }
        String identifier = node.option(valueOption, type).map(JsonElement::getAsString).orElse("");
        return derive(identifier);
    }
}
