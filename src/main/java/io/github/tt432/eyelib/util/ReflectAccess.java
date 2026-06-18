package io.github.tt432.eyelib.util;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * 1.21.1 private 化的 vanilla 字段/构造器的反射访问工具。
 * 仅在 Stonecutter {@code //? if >=1.20.6} 分支中使用，1.20.1 保持直接字段访问。
 *
 * @author TT432
 */
public final class ReflectAccess {
    private ReflectAccess() {
    }

    /**
     * 读取对象的 private/protected 字段值。
     *
     * @param owner 目标对象
     * @param name  字段名（mojmap/parchment 编译时映射名）
     * @return 字段值
     */
    public static Object readField(Object owner, String name) {
        try {
            Field field = owner.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(owner);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot access field " + name + " on " + owner.getClass(), e);
        }
    }

    /**
     * 构造 1.21.1 的 {@code PoseStack.Pose}，其构造器在 1.21.1 变为 private。
     *
     * @param matrix4f pose 的 4x4 矩阵
     * @param matrix3f pose 的 3x3 法线矩阵
     * @return 新构造的 PoseStack.Pose 实例
     */
    public static PoseStack.Pose createPose(Matrix4f matrix4f, Matrix3f matrix3f) {
        try {
            Constructor<? extends PoseStack.Pose> constructor =
                    PoseStack.Pose.class.getDeclaredConstructor(Matrix4f.class, Matrix3f.class);
            constructor.setAccessible(true);
            return constructor.newInstance(matrix4f, matrix3f);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot construct PoseStack.Pose", e);
        }
    }
}
