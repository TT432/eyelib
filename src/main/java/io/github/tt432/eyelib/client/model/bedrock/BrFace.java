package io.github.tt432.eyelib.client.model.bedrock;

import lombok.Getter;
import org.joml.Vector2f;
import org.joml.Vector3f;
import oshi.util.tuples.Pair;

/**
 * @author TT432
 */
@Getter
public class BrFace {
    Vector3f normal;
    /**
     * uv0 -> uv1
     */
    Vector2f[] uv;
    Vector3f[] vertex;

    public BrFace(Vector3f normal, Pair<Vector2f, Vector2f> uv, Vector3f[] vertex) {
        this.normal = normal;
        this.vertex = vertex;

        var uv1 = uv.getB().add(uv.getA(), new Vector2f());

        this.uv =  new Vector2f[]{
                uv.getA(),
                new Vector2f(uv1.x, uv.getA().y),
                uv1,
                new Vector2f(uv.getA().x, uv1.y)
        };
    }
}
