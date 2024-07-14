package io.github.tt432.eyelib.client.particle.effekseer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import io.github.tt432.eyelib.client.loader.EfkefcLoader;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * @author DustW
 */
public class EfkefcRenderer {
    public static void addToCore(EfkefcObject object, EfkefcRenderHandler.RenderInfo info) {
        int pointer = EfkefcLoader.getCore().Play(object.getEffectCore());
        info.setPointer(pointer);
        update(info);
    }

    public static void update(EfkefcRenderHandler.RenderInfo info) {
        var core = EfkefcLoader.getCore();
        int pointer = info.getPointer();

        var size = info.getSize();
        core.SetEffectScale(pointer, size.x, size.y, size.z);
        var pos = info.getPosition();
        core.SetEffectPosition(pointer, pos.x(), pos.y(), pos.z());
        Vector3f rot = info.getRotation();
        core.SetEffectRotation(pointer, rot.x, rot.y, rot.z);
    }

    public static void begin() {
        var core = EfkefcLoader.getCore();
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        core.SetBackground(target.getColorTextureId(), false);
        core.SetDepth(target.getDepthTextureId(), false);
    }

    public static void coreUpdate(Matrix4f projection, Matrix4f cameraMatrix, Matrix4f modelView) {
        var core = EfkefcLoader.getCore();
        Minecraft mc = Minecraft.getInstance();

        Matrix4f mul = projection.mul(modelView, new Matrix4f());
        setMatrix(core::SetProjectionMatrix, mul);
        setMatrix(core::SetCameraMatrix, cameraMatrix);
        core.Update(mc.isPaused() ? 0 : mc.getTimer().getGameTimeDeltaTicks());

        core.DrawBack();
        core.DrawFront();
    }

    private static void setMatrix(MatrixHolder holder, Matrix4f matrix) {
        holder.setMatrix(
                matrix.m00(), matrix.m10(), matrix.m20(), matrix.m30(),
                matrix.m01(), matrix.m11(), matrix.m21(), matrix.m31(),
                matrix.m02(), matrix.m12(), matrix.m22(), matrix.m32(),
                matrix.m03(), matrix.m13(), matrix.m23(), matrix.m33()
        );
    }

    @FunctionalInterface
    public interface MatrixHolder {
        void setMatrix(
                float var1, float var2, float var3, float var4,
                float var5, float var6, float var7, float var8,
                float var9, float var10, float var11, float var12,
                float var13, float var14, float var15, float var16
        );
    }
}
