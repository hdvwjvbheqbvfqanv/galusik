package babushka.entity;

import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Полицейский из отряда бабушки. Всем управляет PoliceSquad: этот класс только ходит/бегает по приказу. */
public class PoliceEntity extends PathfinderMob {

    /**
     * Скорость бега. У мобов блоков/сек ≈ значение * 20: 0.28 * 1.0 ≈ 5.6 — как у бегущего игрока.
     * Идут к бабушке пешком (множитель 0.77 ≈ 4.3, как у идущего игрока).
     */
    public static final double POLICE_SPEED = 0.28;

    public enum Mode { IDLE, GATHER, SEARCH, CHASE, GUARD }

    UUID squadId;
    Mode mode = Mode.IDLE;
    Entity followTarget;
    BlockPos center;
    boolean reached;

    public PoliceEntity(EntityType<? extends PoliceEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.setCanFloat(true);
        }
        setPersistenceRequired();
        setInvulnerable(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, POLICE_SPEED)
            .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OrderGoal());
        goalSelector.addGoal(2, new OpenDoorGoal(this, true));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // отряд исчез (например, после перезапуска мира) — полицейский тоже исчезает
        if (level() instanceof ServerLevel && (squadId == null || !PoliceSquads.exists(squadId))) {
            discard();
        }
    }

    private class OrderGoal extends Goal {
        OrderGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return mode != Mode.IDLE;
        }

        @Override
        public boolean canContinueToUse() {
            return mode != Mode.IDLE;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            switch (mode) {
                case GATHER -> {
                    if (followTarget != null && (tickCount % 10 == 0 || getNavigation().isDone())) {
                        getNavigation().moveTo(followTarget, 0.77);
                    }
                }
                case SEARCH -> {
                    if (getNavigation().isDone()) {
                        Vec3 dest = null;
                        if (center != null && blockPosition().distSqr(center) > 18 * 18) {
                            dest = LandRandomPos.getPosTowards(PoliceEntity.this, 14, 6, Vec3.atCenterOf(center));
                        }
                        if (dest == null) {
                            dest = LandRandomPos.getPos(PoliceEntity.this, 14, 6);
                        }
                        if (dest != null) {
                            getNavigation().moveTo(dest.x, dest.y, dest.z, 1.0);
                        }
                    }
                }
                case CHASE -> {
                    if (followTarget != null) {
                        getLookControl().setLookAt(followTarget, 30.0F, 30.0F);
                        if (tickCount % 4 == 0 || getNavigation().isDone()) {
                            getNavigation().moveTo(followTarget, 1.0);
                        }
                    }
                }
                case GUARD -> {
                    if (followTarget != null && getNavigation().isDone()) {
                        double ang = getRandom().nextDouble() * Math.PI * 2;
                        double r = 2.5 + getRandom().nextDouble() * 2.0;
                        Vec3 c = followTarget.position();
                        getNavigation().moveTo(c.x + Math.cos(ang) * r, c.y, c.z + Math.sin(ang) * r, 0.77);
                    }
                }
                default -> { }
            }
        }
    }
}
