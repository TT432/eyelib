package io.github.tt432.eyelib.debug;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/**
 * 程序化生成全场生物展示场地。
 *
 * @author TT432
 */
public final class MobArenaGenerator {

    private static final int MAX_ROW_WIDTH = 60;

    private MobArenaGenerator() {}

    /** 一站式生成：清空区域 → 建 cage → 召唤实体。 */
    public static void generate(ServerLevel level, BlockPos origin) {
        List<Cage> small   = new ArrayList<>();
        List<Cage> medium  = new ArrayList<>();
        List<Cage> large   = new ArrayList<>();
        List<Cage> flying  = new ArrayList<>();
        List<Cage> aquatic = new ArrayList<>();

        classifyEntities(level, small, medium, large, flying, aquatic);

        Comparator<Cage> byWidth = Comparator.comparingInt(c -> -c.w); // 宽降序
        small.sort(byWidth);
        medium.sort(byWidth);
        large.sort(byWidth);
        flying.sort(byWidth);
        aquatic.sort(byWidth);

        // 清除场地
        BlockPos areaEnd = origin.offset(MAX_ROW_WIDTH, 16, -30);
        clearArea(level, origin, areaEnd);

        // 排布
        BlockPos tier1 = origin;
        BlockPos tier2 = origin.offset(0, 2, -5);
        BlockPos tier3 = origin.offset(0, 4, -10);
        BlockPos tier4 = origin.offset(0, 7, -15);
        BlockPos waterPos = origin.offset(0, -2, 4);

        placeRow(level, small,  tier1, false);
        placeRow(level, medium, tier2, false);
        placeRow(level, large,  tier3, false);
        placeRow(level, flying, tier4, true);
        placeWaterRow(level, aquatic, waterPos);

        summonAll(level, small,  tier1);
        summonAll(level, medium, tier2);
        summonAll(level, large,  tier3);
        summonAll(level, flying, tier4);
        summonAll(level, aquatic, waterPos);
    }

    // ---------- classification ----------

    private static void classifyEntities(ServerLevel level,
                                         List<Cage> small, List<Cage> medium,
                                         List<Cage> large, List<Cage> flying,
                                         List<Cage> aquatic) {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER || type == EntityType.PLAYER)
                continue;

            Entity entity;
            try {
                entity = type.create(level);
                if (!(entity instanceof LivingEntity)) continue;
            } catch (Exception e) {
                continue; // 某些类型无法在 overworld 创建（如 Warden 需要 sculk）
            }

            try {
                LivingEntity living = (LivingEntity) entity;
                double bw = living.getBbWidth();
                double bh = living.getBbHeight();
                if (bw < 0.01) bw = 0.5;

                int cageW = (int) Math.ceil(bw) + 1;
                int cageH = (int) Math.ceil(bh) + 1;
                int cageD = (int) Math.ceil(bw) + 1; // 宽=深

                if (entity instanceof FlyingMob || type == EntityType.GHAST) cageH = Math.max(cageH, 3);

                Cage cage = new Cage(type, cageW, cageH, cageD);

                if (entity instanceof WaterAnimal || type == EntityType.SQUID
                        || type == EntityType.GLOW_SQUID || type == EntityType.AXOLOTL) {
                    aquatic.add(cage);
                } else if (entity instanceof FlyingMob || type == EntityType.GHAST
                        || type == EntityType.BLAZE || type == EntityType.VEX
                        || type == EntityType.ALLAY) {
                    flying.add(cage);
                } else if (cageH <= 1) {
                    small.add(cage);
                } else if (cageH <= 2) {
                    medium.add(cage);
                } else {
                    large.add(cage);
                }
            } finally {
                entity.discard();
            }
        }
    }

    // ---------- cage building ----------

    private static void placeRow(ServerLevel level, List<Cage> cages, BlockPos origin, boolean roof) {
        int x = origin.getX();
        int y = origin.getY();
        int z = origin.getZ();
        int rowWidth = 0;
        int rowMaxH = 0;

        for (Cage cage : cages) {
            if (rowWidth + cage.w + 1 > MAX_ROW_WIDTH) {
                y += rowMaxH + 1;
                x = origin.getX();
                z = origin.getZ(); // 固定 Z，不做多行，改为 Y 堆叠
                rowWidth = 0;
                rowMaxH = 0;
            }
            cage.x = x;
            cage.y = y;
            cage.z = z;
            buildCage(level, cage, roof);
            x += cage.w + 1;
            rowWidth += cage.w + 1;
            rowMaxH = Math.max(rowMaxH, cage.h);
        }
    }

    private static void placeWaterRow(ServerLevel level, List<Cage> cages, BlockPos origin) {
        int x = origin.getX();
        int y = origin.getY();
        int z = origin.getZ();
        for (Cage cage : cages) {
            cage.x = x;
            cage.y = y;
            cage.z = z;
            buildWaterTank(level, cage);
            x += cage.w + 1;
            if (x - origin.getX() > MAX_ROW_WIDTH) {
                x = origin.getX();
                z += 4;
            }
        }
    }

    // ---------- cage building ----------

    private static void buildCage(ServerLevel level, Cage cage, boolean roof) {
        int x0 = cage.x, y0 = cage.y, z0 = cage.z;
        int x1 = x0 + cage.w - 1, y1 = y0 + cage.h - 1, z1 = z0 + cage.d - 1;

        // 地板
        fill(level, x0, y0, z0, x1, y0, z1, Blocks.SMOOTH_STONE.defaultBlockState());

        // 后墙 (z0 - 1)
        fill(level, x0, y0 + 1, z0 - 1, x1, y1, z0 - 1, Blocks.GLASS.defaultBlockState());
        // 前墙 (z1 + 1)
        fill(level, x0, y0 + 1, z1 + 1, x1, y1, z1 + 1, Blocks.GLASS.defaultBlockState());
        // 左墙 (x0 - 1)
        fill(level, x0 - 1, y0 + 1, z0, x0 - 1, y1, z1, Blocks.GLASS.defaultBlockState());
        // 右墙 (x1 + 1)
        fill(level, x1 + 1, y0 + 1, z0, x1 + 1, y1, z1, Blocks.GLASS.defaultBlockState());
        // 天花板（飞行生物）
        if (roof) fill(level, x0, y1 + 1, z0, x1, y1 + 1, z1, Blocks.GLASS.defaultBlockState());
    }

    private static void buildWaterTank(ServerLevel level, Cage cage) {
        int x0 = cage.x, y0 = cage.y, z0 = cage.z;
        int x1 = x0 + cage.w - 1, y1 = y0 + cage.h - 1, z1 = z0 + cage.d - 1;
        var glass = Blocks.GLASS.defaultBlockState();
        var water = Blocks.WATER.defaultBlockState();

        // 玻璃底
        fill(level, x0, y0 - 1, z0, x1, y0 - 1, z1, glass);
        // 水
        fill(level, x0, y0, z0, x1, y1, z1, water);
        // 四壁 + 顶
        fill(level, x0, y0, z0 - 1, x1, y1, z0 - 1, glass);
        fill(level, x0, y0, z1 + 1, x1, y1, z1 + 1, glass);
        fill(level, x0 - 1, y0, z0, x0 - 1, y1, z1, glass);
        fill(level, x1 + 1, y0, z0, x1 + 1, y1, z1, glass);
        fill(level, x0, y1 + 1, z0, x1, y1 + 1, z1, glass);
    }

    private static void fill(ServerLevel level, int x0, int y0, int z0,
                              int x1, int y1, int z1,
                              net.minecraft.world.level.block.state.BlockState state) {
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++)
                for (int z = z0; z <= z1; z++)
                    level.setBlock(new BlockPos(x, y, z), state, 3);
    }

    // ---------- clearing ----------

    private static void clearArea(ServerLevel level, BlockPos from, BlockPos to) {
        var air = Blocks.AIR.defaultBlockState();
        int x0 = Math.min(from.getX(), to.getX());
        int y0 = Math.min(from.getY(), to.getY());
        int z0 = Math.min(from.getZ(), to.getZ());
        int x1 = Math.max(from.getX(), to.getX());
        int y1 = Math.max(from.getY(), to.getY());
        int z1 = Math.max(from.getZ(), to.getZ());
        fill(level, x0, y0, z0, x1, y1, z1, air);
    }

    // ---------- summoning ----------

    private static void summonAll(ServerLevel level, List<Cage> cages, BlockPos tierOrigin) {
        for (Cage cage : cages) {
            try {
                Entity entity = cage.type.create(level);
                entity.setPos(cage.x + cage.w / 2.0, cage.y + 0.5, cage.z + cage.d / 2.0);
                level.addFreshEntity(entity);
            } catch (Exception e) {
                // 跳过无法生成的实体（如 Warden 需要 sculk shrieker 触发等）
            }
        }
    }

    // ---------- data ----------

    private static final class Cage {
        final EntityType<?> type;
        final int w, h, d;
        int x, y, z;

        Cage(EntityType<?> type, int w, int h, int d) {
            this.type = type;
            this.w = w;
            this.h = h;
            this.d = d;
        }
    }
}
