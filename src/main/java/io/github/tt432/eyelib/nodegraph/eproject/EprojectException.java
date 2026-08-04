package io.github.tt432.eyelib.nodegraph.eproject;

import java.io.Serial;
import java.nio.file.Path;

/**
 * eproject 读写失败：坏格式、未来版本、非法库 id、底层 IO 错误的统一出口。
 *
 * <p>消息中总是携带源/目标路径；{@link #path()} 提供程序化访问。
 */
public class EprojectException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 路径字符串形式（Path 不可序列化，故存字符串）。 */
    private final String path;

    public EprojectException(Path path, String message) {
        super(message + " [path=" + path + "]");
        this.path = path.toString();
    }

    public EprojectException(Path path, String message, Throwable cause) {
        super(message + " [path=" + path + "]", cause);
        this.path = path.toString();
    }

    /** 出错时操作的源/目标路径（字符串形式）。 */
    public String path() {
        return path;
    }
}
