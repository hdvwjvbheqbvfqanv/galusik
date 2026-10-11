package babushka.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

/** В темноте ищет ближайшее достижимое место с достаточным освещением. */
public class SeekLightGoal extends Goal {
    private final BabushkaEntity mob;
    private long nextSearch;
    private Path path;

    public SeekLightGoal(BabushkaEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.isSleeping() || mob.getHoldTicks() > 0 || !mob.isDarkHere()) {
            return false;
        }
        if (!(mob.level() instanceof ServerLevel level) || level.getGameTime() < nextSearch) {
            return false;
        }
        nextSearch = level.getGameTime() + 40;

        BlockPos target = findReachableLight(level);
        return target != null;
    }

    private BlockPos findReachableLight(ServerLevel level) {
        BlockPos origin = mob.blockPosition();
        List<BlockPos> candidates = new ArrayList<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int r = BabushkaEntity.SEARCH_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    p.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!level.hasChunkAt(p) || !mob.isLightEnoughAt(p) || !isStandable(level, p)) {
                        continue;
                    }
                    candidates.add(p.immutable());
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(origin::distSqr));
        int tried = 0;
        for (BlockPos c : candidates) {
            if (tried++ >= 8) {
                break;
            }
            Path candidatePath = mob.getNavigation().createPath(c, 0);
            if (candidatePath != null && candidatePath.canReach()) {
                this.path = candidatePath;
                return c;
            }
        }
        return null;
    }

    private static boolean isStandable(ServerLevel level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState floor = level.getBlockState(pos.below());
        return feet.getCollisionShape(level, pos).isEmpty()
            && head.getCollisionShape(level, pos.above()).isEmpty()
            && feet.getFluidState().isEmpty()
            && !feet.is(BlockTags.FIRE)
            && floor.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    public boolean canContinueToUse() {
        return !mob.isSleeping() && mob.isDarkHere() && !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        mob.getNavigation().moveTo(path, 0.75);
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        path = null;
    }
}
