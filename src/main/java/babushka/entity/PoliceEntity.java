package babushka.entity;

import babushka.ModDamage;
import java.util.EnumSet;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
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

/**
 * Полицейский из отряда бабушки. Приказы (ходить/бегать) даёт PoliceSquad.
 * Если полицейского ударить — он достаёт автомат и стреляет в ударившего (урон выстрела 100, броня не ломается).
 */
public class PoliceEntity extends PathfinderMob {

    /**
     * Скорость бега. У мобов блоков/сек ≈ значение * 20: 0.28 * 1.0 ≈ 5.6 — как у бегущего игрока.
     * Идут к бабушке пешком (множитель 0.77 ≈ 4.3, как у идущего игрока).
     */
    public static final double POLICE_SPEED = 0.28;
    /** Урон одного выстрела из автомата. */
    public static final float SHOT_DAMAGE = 100.0F;
    /** Пауза между выстрелами, тиков. */
    public static final int SHOT_INTERVAL = 8;
    public static final double SHOOT_RANGE = 48.0;

    public enum Mode { IDLE, GATHER, SEARCH, CHASE, GUARD }

    UUID squadId;
    Mode mode = Mode.IDLE;
    Entity followTarget;
    BlockPos center;
    boolean reached;

    /** Кто ударил полицейского (в него он стреляет). */
    private UUID gunTargetId;
    private int shotCooldown;

    public PoliceEntity(EntityType<? extends PoliceEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.setCanFloat(true);
        }
        setPersistenceRequired();
        setInvulnerable(true);
    }

    /** Машина переопределяет. */
    boolean isCar() {
        return false;
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
        goalSelector.addGoal(1, new ShootGoal());
        goalSelector.addGoal(2, new OrderGoal());
        goalSelector.addGoal(3, new OpenDoorGoal(this, true));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /** Удар игрока: полицейский бессмертен, но достаёт автомат и начинает стрелять в ударившего. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isCar()) {
            return super.hurtServer(level, source, amount);
        }
        if (source.getEntity() instanceof ServerPlayer player && player.isAlive() && !player.isSpectator()) {
            gunTargetId = player.getUUID();
            setAggressive(true); // флаг синхронизируется с клиентом: модель показывает автомат
        }
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

    private ServerPlayer gunTarget() {
        if (gunTargetId == null || !(level() instanceof ServerLevel sl)) {
            return null;
        }
        ServerPlayer p = sl.getServer().getPlayerList().getPlayer(gunTargetId);
        if (p == null || p.isRemoved() || !p.isAlive() || p.level() != level()
            || p.isCreative() || p.isSpectator()) {
            return null;
        }
        return p;
    }

    private void fire(ServerLevel level, ServerPlayer target) {
        Vec3 from = getEyePosition().add(getViewVector(1.0F).scale(0.7)).add(0, -0.25, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.65, 0);
        Vec3 dir = to.subtract(from);
        double len = dir.length();
        if (len < 0.01) {
            return;
        }
        dir = dir.scale(1.0 / len);
        // трассер и вспышка
        for (double d = 0.8; d < len; d += 0.8) {
            level.sendParticles(ParticleTypes.CRIT, from.x + dir.x * d, from.y + dir.y * d, from.z + dir.z * d,
                1, 0.0, 0.0, 0.0, 0.0);
        }
        level.sendParticles(ParticleTypes.FLAME, from.x, from.y, from.z, 3, 0.04, 0.04, 0.04, 0.01);
        level.sendParticles(ParticleTypes.SMOKE, from.x, from.y, from.z, 2, 0.05, 0.05, 0.05, 0.01);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_BLAST,
            SoundSource.HOSTILE, 3.0F, 1.6F);
        swing(InteractionHand.MAIN_HAND);
        // свой тип урона: сообщение "был убит за нападение на сотрудника полиции", броня не ломается
        target.hurtServer(level, ModDamage.of(level, ModDamage.POLICE_SHOT, this), SHOT_DAMAGE);
    }

    /** Достаёт автомат и стреляет в того, кто ударил. */
    private class ShootGoal extends Goal {
        ShootGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (isCar() || gunTargetId == null) {
                return false;
            }
            if (gunTarget() == null) {
                gunTargetId = null;
                setAggressive(false);
                return false;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            setAggressive(true);
            shotCooldown = 8;
        }

        @Override
        public void stop() {
            setAggressive(gunTargetId != null);
            getNavigation().stop();
        }

        @Override
        public void tick() {
            ServerPlayer target = gunTarget();
            if (target == null || !(level() instanceof ServerLevel level)) {
                return;
            }
            getLookControl().setLookAt(target, 60.0F, 60.0F);
            boolean sees = hasLineOfSight(target);
            double dist = distanceToSqr(target);
            if (!sees || dist > SHOOT_RANGE * SHOOT_RANGE) {
                if (tickCount % 5 == 0 || getNavigation().isDone()) {
                    getNavigation().moveTo(target, 1.0);   // подходит, пока не увидит
                }
            } else {
                getNavigation().stop();
            }
            if (shotCooldown > 0) {
                shotCooldown--;
            } else if (sees && dist <= SHOOT_RANGE * SHOOT_RANGE) {
                fire(level, target);
                shotCooldown = SHOT_INTERVAL;
            }
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
