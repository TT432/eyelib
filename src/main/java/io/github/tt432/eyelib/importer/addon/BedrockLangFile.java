package io.github.tt432.eyelib.importer.addon;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bedrock texts/*.lang 文件解析（key=value 表）。
 * <p>
 * 行规则：{@code #} 开头整行注释跳过；按第一个 {@code =} 切分；
 * 值中 {@code \t#} 起为行内注释（真实包惯例，如 "Actions &amp; Stuff 1.10\t#"）。
 *
 * @author TT432
 */
@org.jspecify.annotations.NullMarked
public final class BedrockLangFile {
    private BedrockLangFile() {
    }

    public static Map<String, String> parse(String content) {
        Map<String, String> table = new LinkedHashMap<>();
        for (String line : content.split("\\R")) {
            int eq = line.indexOf('=');
            if (eq <= 0 || line.startsWith("#")) {
                continue;
            }
            String value = line.substring(eq + 1);
            int inlineComment = value.indexOf("\t#");
            if (inlineComment >= 0) {
                value = value.substring(0, inlineComment);
            }
            value = value.replace('\t', ' ').trim();
            if (value.endsWith("#")) {
                value = value.substring(0, value.length() - 1).trim();
            }
            table.put(line.substring(0, eq).trim(), value);
        }
        return table;
    }
}
