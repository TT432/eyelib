package io.github.tt432.eyelib.molang.port;

import java.util.Map;

/**
 * 抽象实体接口。提供 Bedrock 查询属性和位置访问。
 *
 * @author TT432
 */
public interface PortEntity {
    Map<String, Object> getQueryProperties();

    float getX();

    float getY();

    float getZ();
}
