package babushka.entity;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Дом бабушки = вся закрытая территория вокруг кровати: клетки, до которых можно дойти от кровати,
 * не пройдя сквозь твёрдые блоки/двери и не выйдя туда, где видно небо.
 */
public final class House {
    public static final int MAX_CELLS = 4000;
    private static final int RADIUS_XZ = 12;

    public final Set<Long> cells = new HashSet<>();

    public boolean isEmpty() {
        return cells.isEmpty();
    }

    public boolean contains(BlockPos pos) {
        return cells.contains(pos.asLong());
    }

    private static boolean passable(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    /** Строит дом заливкой от клетки над изголовьем кровати. Открытая территория (видно небо) в дом не входит. */
    public static House compute(ServerLevel level, BlockPos bed) {
        House house = new House();
        BlockPos start = bed.above();
        if (!passable(level, start) || level.canSeeSky(start)) {
            return house;
        }
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        house.cells.add(start.asLong());
        while (!queue.isEmpty()) {
            BlockPos c = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = c.relative(d);
                if (Math.abs(n.getX() - bed.getX()) > RADIUS_XZ
                    || Math.abs(n.getZ() - bed.getZ()) > RADIUS_XZ
                    || n.getY() < bed.getY() - 2 || n.getY() > bed.getY() + 6) {
                    continue;
                }
                if (house.cells.contains(n.asLong()) || !passable(level, n) || level.canSeeSky(n)) {
                    continue;
                }
                house.cells.add(n.asLong());
                queue.add(n);
                if (house.cells.size() >= MAX_CELLS) {
                    // слишком большая "коробка" — это не дом, а открытая местность/пещера
                    house.cells.clear();
                    return house;
                }
            }
        }
        return house;
    }

    /** Есть ли в доме источник БЛОЧНОГО света (лампа, факел...). Дневной/ночной свет не учитывается. */
    public boolean isLit(ServerLevel level, int threshold) {
        for (long l : cells) {
            if (level.getBrightness(LightLayer.BLOCK, BlockPos.of(l)) >= threshold) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSwitchable(BlockState state) {
        return state.getBlock() instanceof RedstoneLampBlock || state.getBlock() instanceof CopperBulbBlock;
    }

    /** Запоминает источники света в доме и вокруг (в стенах): позиция -> переключаемый (лампа) или нет (факел и т.п.). */
    public Map<Long, Boolean> scanSources(ServerLevel level, int threshold) {
        Map<Long, Boolean> found = new HashMap<>();
        for (long l : cells) {
            BlockPos c = BlockPos.of(l);
            scanOne(level, c, threshold, found);
            for (Direction d : Direction.values()) {
                scanOne(level, c.relative(d), threshold, found);
            }
            if (found.size() > 64) {
                break;
            }
        }
        return found;
    }

    private static void scanOne(ServerLevel level, BlockPos p, int threshold, Map<Long, Boolean> found) {
        if (found.containsKey(p.asLong()) || !level.hasChunkAt(p)) {
            return;
        }
        BlockState s = level.getBlockState(p);
        if (s.getLightEmission() >= threshold) {
            found.put(p.asLong(), isSwitchable(s));
        }
    }

    public BlockPos nearestCell(BlockPos from) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (long l : cells) {
            BlockPos c = BlockPos.of(l);
            double d = c.distSqr(from);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return best;
    }

    /** Выход из дома: клетка снаружи за дверью/калиткой/проёмом, ближайшая к бабушке. */
    public BlockPos findExit(ServerLevel level, BlockPos from) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (long l : cells) {
            BlockPos c = BlockPos.of(l);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = c.relative(d);
                if (cells.contains(n.asLong()) || !level.hasChunkAt(n)) {
                    continue;
                }
                BlockState ns = level.getBlockState(n);
                BlockPos target = null;
                if (ns.is(BlockTags.DOORS) || ns.is(BlockTags.FENCE_GATES)) {
                    target = n.relative(d);
                } else if (passable(level, n)) {
                    target = n;
                }
                if (target == null || cells.contains(target.asLong()) || !passable(level, target)) {
                    continue;
                }
                BlockPos floor = target.below();
                if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                    continue;
                }
                double dist = target.distSqr(from);
                if (dist < bestD) {
                    bestD = dist;
                    best = target;
                }
            }
        }
        return best;
    }
}
