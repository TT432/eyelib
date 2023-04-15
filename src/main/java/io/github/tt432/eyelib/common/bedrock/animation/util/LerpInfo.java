package io.github.tt432.eyelib.common.bedrock.animation.util;

import com.mojang.math.Vector3d;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * @author DustW
 */
@RequiredArgsConstructor
@Data
public class LerpInfo {
    final Vector3d value;

    @Override
    public String toString() {
        return value != null ? "LerpInfo{" +
                " x: " + value.x +
                " y: " + value.y +
                " z: " + value.z +
                '}' : "LerpInfo{value=null}";
    }
}