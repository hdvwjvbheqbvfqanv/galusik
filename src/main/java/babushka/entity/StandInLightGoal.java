package babushka.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;

/** Пока "минута у света" не истекла — стоит на месте. */
public class StandInLightGoal extends Goal {
    private final BabushkaEntity mob;

    public StandInLightGoal(BabushkaEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return mob.getHoldTicks() > 0 && !mob.isSleeping();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
    }
}
