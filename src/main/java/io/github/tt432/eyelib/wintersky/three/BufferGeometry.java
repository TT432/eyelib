package io.github.tt432.eyelib.wintersky.three;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * three.js r134 BufferGeometry 移植（属性容器子集：setAttribute/getAttribute/setIndex/dispose）。
 */
public class BufferGeometry extends EventDispatcher {

    private static int bufferGeometryId = 0;

    public final int id = bufferGeometryId++;
    public String type = "BufferGeometry";

    /** 几何参数（PlaneGeometry 记录 width/height/segments），与 three 的 parameters 字段对应。 */
    public final Map<String, Object> parameters = new LinkedHashMap<>();

    private int @Nullable [] index;
    public final Map<String, BufferAttribute> attributes = new LinkedHashMap<>();

    public void setIndex(int[] index) {
        this.index = index;
    }

    public int @Nullable [] getIndex() {
        return index;
    }

    public BufferGeometry setAttribute(String name, BufferAttribute attribute) {
        this.attributes.put(name, attribute);
        return this;
    }

    public @Nullable BufferAttribute getAttribute(String name) {
        return this.attributes.get(name);
    }

    public BufferGeometry deleteAttribute(String name) {
        this.attributes.remove(name);
        return this;
    }

    public boolean hasAttribute(String name) {
        return this.attributes.containsKey(name);
    }

    /** three dispose()：仅派发 dispose 事件（GL 资源由渲染后端管理，本移植层无资源）。 */
    public void dispose() {
        this.dispatchEvent(new Event("dispose"));
    }
}
