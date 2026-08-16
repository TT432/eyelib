//? if >=1.20.1 {
package io.github.tt432.eyelib.bridge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 客户端实体视觉状态 Port：披风与表面粒子贴图数据的版本差异收敛（ADR-0016 §5/§6：
 * 版本特定 MC API 与 //? 守卫的唯一栖息地是 bridge/mixin）。
 *
 * <p>消费方：{@code client/molang/MolangQuery} 的 {@code query.has_cape} 与
 * {@code query.surface_particle_*}（官方标注仅客户端资源包侧）。
 *
 * @author TT432
 */
public interface ClientEntityVisualPort {

    /**
     * @return 玩家披风纹理是否存在（1.20.1 {@code getCloakTextureLocation} /
     * 1.21.x {@code getSkin().capeTexture} / 26.1 {@code getSkin().cape}）；非玩家恒 false
     */
    static boolean hasCape(Entity entity) {
        if (!(entity instanceof AbstractClientPlayer player)) {
            return false;
        }
        //? if <1.20.6 {
        return player.getCloakTextureLocation() != null;
        //?} elif <26.1 {
        return player.getSkin().capeTexture() != null;
        //?} else {
        return player.getSkin().cape() != null;
        //?}
    }

    /**
     * 方块表面粒子贴图数据（BE {@code query.surface_particle_*} 语义所需）。
     *
     * @param r/g/b/a 粒子贴图平均色 × 群系着色（0..1；JE 近似，BE 取引擎粒子色）
     * @param u/v     粒子贴图图集原点坐标（0..1；JE 近似）
     * @param width/height 粒子贴图尺寸（像素）
     */
    record SurfaceParticleData(float r, float g, float b, float a,
                               float u, float v, float width, float height) {
    }

    /**
     * 取方块状态的表面粒子贴图数据；26.1 渲染管线重写后 BlockModelShaper 已不存在，
     * 未适配——返回 null（调用方按官方语义回退全 0）。
     */
    static @Nullable SurfaceParticleData surfaceParticle(BlockState state, Level level, BlockPos pos) {
        //? if <26.1 {
        var mc = Minecraft.getInstance();
        var sprite = mc.getBlockRenderer().getBlockModelShaper().getParticleIcon(state);
        var contents = sprite.contents();
        var image = contents.getOriginalImage();
        long r = 0, g = 0, b = 0, a = 0;
        int n = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int color = image.getPixelRGBA(x, y);
                int alpha = color >>> 24;
                if (alpha == 0) {
                    continue;
                }
                r += color & 0xFF;
                g += (color >>> 8) & 0xFF;
                b += (color >>> 16) & 0xFF;
                a += alpha;
                n++;
            }
        }
        float fr = 0, fg = 0, fb = 0, fa = 0;
        if (n > 0) {
            fr = r / 255F / n;
            fg = g / 255F / n;
            fb = b / 255F / n;
            fa = a / 255F / n;
            int tint = mc.getBlockColors().getColor(state, level, pos, 0);
            if (tint != -1) {
                fr *= ((tint >>> 16) & 0xFF) / 255F;
                fg *= ((tint >>> 8) & 0xFF) / 255F;
                fb *= (tint & 0xFF) / 255F;
            }
        }
        return new SurfaceParticleData(fr, fg, fb, fa,
                sprite.getU0(), sprite.getV0(), contents.width(), contents.height());
        //?} else {
        return null;
        //?}
    }
}
//?}
