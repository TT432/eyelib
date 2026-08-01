package io.github.tt432.eyelib.nodegraph;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tt432.eyelib.util.PortStringRepresentable;
import java.util.Locale;
import java.util.Optional;

/**
 * 验证/代码生成诊断。
 *
 * @param severity 级别
 * @param code     机器可读码（如 {@code CYCLE}、{@code TYPE_MISMATCH}）
 * @param message  人类可读消息
 * @param nodeUid  关联节点（可空）
 */
public record Diagnostic(
        Severity severity,
        String code,
        String message,
        Optional<String> nodeUid
) {
    public static final Codec<Diagnostic> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Severity.CODEC.fieldOf("severity").forGetter(Diagnostic::severity),
            Codec.STRING.fieldOf("code").forGetter(Diagnostic::code),
            Codec.STRING.fieldOf("message").forGetter(Diagnostic::message),
            Codec.STRING.optionalFieldOf("node").forGetter(Diagnostic::nodeUid)
    ).apply(ins, Diagnostic::new));

    public static Diagnostic error(String code, String message) {
        return new Diagnostic(Severity.ERROR, code, message, Optional.empty());
    }

    public static Diagnostic error(String code, String message, String nodeUid) {
        return new Diagnostic(Severity.ERROR, code, message, Optional.of(nodeUid));
    }

    public static Diagnostic warning(String code, String message) {
        return new Diagnostic(Severity.WARNING, code, message, Optional.empty());
    }

    public static Diagnostic warning(String code, String message, String nodeUid) {
        return new Diagnostic(Severity.WARNING, code, message, Optional.of(nodeUid));
    }

    public enum Severity implements PortStringRepresentable {
        ERROR,
        WARNING,
        INFO;

        public static final Codec<Severity> CODEC = PortStringRepresentable.fromEnum(Severity::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
