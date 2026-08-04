package io.github.tt432.eyelib.nodegraph.decompile;

/**
 * 反编译/导入诊断码（对齐 {@code GraphValidator} 风格：常量集中、机器可读）。
 */
public final class DecompileDiagnostics {
    private DecompileDiagnostics() {
    }

    /** molang 结构超出图语言表达域（arrow / 下标 / 非法 member access 等）：便签留原文 + const 0 占位。 */
    public static final String UNSUPPORTED_IMPORT = "UNSUPPORTED_IMPORT";
    /** molang 文本解析失败：便签留原文 + const 0 占位（值上下文）/ 语句跳过（语句上下文）。 */
    public static final String PARSE_FAILURE = "PARSE_FAILURE";
    /** 导入目标缺失（client_entity 包装 / description、指定 RC/AC 名不在文件中）。 */
    public static final String MISSING_ENTRY = "MISSING_ENTRY";
    /** JSON 含图语言无法表达的字段：便签留原文。 */
    public static final String UNKNOWN_FIELD = "UNKNOWN_FIELD";
    /** 已知字段但值类型非法：跳过该字段，便签留原文。 */
    public static final String INVALID_FIELD = "INVALID_FIELD";
    /** animate 短名不在声明表（animations / animation_controllers）中：补仅 short_name 的 ref.animation。 */
    public static final String UNKNOWN_REFERENCE = "UNKNOWN_REFERENCE";
    /** 实体引用的 RC 文档解析器未命中：以 ref.rc 外部引用导入（v4 内联导入）。 */
    public static final String RC_INLINE_MISS = "RC_INLINE_MISS";
}
