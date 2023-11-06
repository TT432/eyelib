package io.github.tt432.eyelib.client.render;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author TT432
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PoolHandler {
    public static final GenericObjectPool<Matrix4f> m4f;
    public static final GenericObjectPool<Matrix3f> m3f;

    static {
        GenericObjectPoolConfig<Matrix4f> poolConfig4 = new GenericObjectPoolConfig<>();
        poolConfig4.setMaxTotal(-1);
        poolConfig4.setLifo(false);
        m4f = new GenericObjectPool<>(new M4F(), poolConfig4);

        GenericObjectPoolConfig<Matrix3f> poolConfig3 = new GenericObjectPoolConfig<>();
        poolConfig3.setMaxTotal(-1);
        poolConfig3.setLifo(false);
        m3f = new GenericObjectPool<>(new M3F(), poolConfig3);
    }

    public static final Deque<Matrix4f> m4l = new ArrayDeque<>();
    public static final Deque<Matrix3f> m3l = new ArrayDeque<>();

    private static class M4F extends BasePooledObjectFactory<Matrix4f> {

        @Override
        public Matrix4f create() throws Exception {
            return new Matrix4f();
        }

        @Override
        public PooledObject<Matrix4f> wrap(Matrix4f obj) {
            return new DefaultPooledObject<>(obj);
        }
    }

    private static class M3F extends BasePooledObjectFactory<Matrix3f> {

        @Override
        public Matrix3f create() throws Exception {
            return new Matrix3f();
        }

        @Override
        public PooledObject<Matrix3f> wrap(Matrix3f obj) {
            return new DefaultPooledObject<>(obj);
        }
    }
}
