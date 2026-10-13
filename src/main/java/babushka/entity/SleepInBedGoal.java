package babushka.entity;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Ночью идёт к своей кровати и ложится спать; утром просыпается. */
public class SleepInBedGoal extends Goal {
    private final BabushkaEntity mob;
    private long nextTry;
    private int giveUpTicks;

    public SleepInBedGoal(BabushkaEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP, Flag.LOOK));
    }

    private boolean recentlyHurt() {
        int t = mob.getLastHurtByMobTimestamp();
        return t != 0 && mob.tickCount - t < 400;
    }

    private boolean bedUsable() {
        BlockPos bed = mob.getHomeBed();
        if (bed == null) {
            return false;
        }
        BlockState state = mob.level().getBlockState(bed);
        if (!(state.getBlock() instanceof BedBlock)) {
            return false;
        }
        return mob.isSleeping() || !state.getValue(BedBlock.OCCUPIED);
    }

    @Override
    public boolean canUse() {
        return mob.level().getGameTime() >= nextTry
            && !mob.isRoutineActive()
            && mob.isNightTime()
            && !recentlyHurt()
            && bedUsable();
    }

    @Override
    public boolean canContinueToUse() {
        return !mob.isRoutineActive() && mob.isNightTime() && !recentlyHurt() && bedUsable() && giveUpTicks < 600;
    }

    @Override
    public void start() {
        giveUpTicks = 0;
        walkToBed();
    }

    private void walkToBed() {
        BlockPos bed = mob.getHomeBed();
        mob.getNavigation().moveTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5, 0.6);
    }

    @Override
    public void tick() {
        if (mob.isSleeping()) {
            return;
        }
        BlockPos bed = mob.getHomeBed();
        Vec3 center = Vec3.atCenterOf(bed);
        double dx = mob.getX() - center.x;
        double dz = mob.getZ() - center.z;
        if (dx * dx + dz * dz < 2.25 && Math.abs(mob.getY() - bed.getY()) < 1.5) {
            mob.getNavigation().stop();
            mob.startSleeping(bed);
            return;
        }
        giveUpTicks++;
        if (mob.getNavigation().isDone()) {
            walkToBed();
        }
    }

    @Override
    public void stop() {
        if (mob.isSleeping()) {
            mob.stopSleeping();
        }
        if (giveUpTicks >= 600) {
            nextTry = mob.level().getGameTime() + 1200; // не получилось дойти — попробуем позже
        }
    }
}
