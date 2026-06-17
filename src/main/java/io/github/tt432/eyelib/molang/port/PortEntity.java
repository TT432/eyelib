package io.github.tt432.eyelibmolang.port;

import java.util.Map;
import org.jspecify.annotations.NullMarked;

/**
 * 抽象实体接口。提供 Bedrock 查询属性的访问。
 *
 * @author TT432
 */
@NullMarked
public interface PortEntity {
    Map<String, Object> getQueryProperties();
}
