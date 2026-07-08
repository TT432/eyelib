package io.github.tt432.eyelib.bridge.client.compat.ar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jspecify.annotations.Nullable;
//? if <1.20.6 {
import net.minecraftforge.fml.loading.LoadingModList;
//?} else {
import net.neoforged.fml.loading.LoadingModList;
//?}

/**
 * 加速渲染（AR）模组兼容端口。检测 AR 是否安装，并委托实际渲染到 adapter 子包。
 * 签名仅使用 MC 原始类型（VertexConsumer / PoseStack.Pose / float[] / int），
 * 不引用 application 层的 RenderParams / BakedBone，避免 bridge→application 反向依赖。
 *
 * @author TT432
 */
public interface ARCompat {
    static boolean isArInstalled() {
        return LoadingModList.get().getModFileById("acceleratedrendering") != null;
    }

    static boolean renderWithAR(
            @Nullable VertexConsumer consumer, PoseStack.Pose pose,
            float[] positions, float[] u, float[] v, float[] normals,
            int vertexSize, int light, int overlay) {
        //? if <26.1 {
        return io.github.tt432.eyelib.bridge.client.compat.ar.adapter.ARCompatImpl.renderWithAR(
                consumer, pose, positions, u, v, normals, vertexSize, light, overlay);
        //?} else {
        return false;
        //?}
    }
}
