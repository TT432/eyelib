package io.github.tt432.eyelib.client.model.bedrock;

import it.unimi.dsi.fastutil.Pair;
import lombok.Getter;
import org.joml.Vector2f;
import org.joml.Vector3f;

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

        var uv1 = uv.right().add(uv.left(), new Vector2f());

        this.uv = new Vector2f[]{
                uv.left(),
                new Vector2f(uv.left().x, uv1.y),
                uv1,
                new Vector2f(uv1.x, uv.left().y),
        };
    }
}
