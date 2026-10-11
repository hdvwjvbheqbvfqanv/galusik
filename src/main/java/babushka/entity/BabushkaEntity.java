package babushka.entity;

import babushka.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Бабушка: живёт как житель (ходит, спит ночью в своей кровати, открывает двери),
 * но не торгует. В темноте ищет свет и каждые 2 секунды выдаёт случайную реплику;
 * найдя свет, стоит на месте ещё минуту и продолжает говорить с тем же интервалом.
 */
public class BabushkaEntity extends PathfinderMob {

    // ---- Настройки (меняйте здесь) ----
    /** Пауза между репликами, отсчитывается ПОСЛЕ окончания реплики: 40 тиков = 2 секунды. */
    public static final int SOUND_INTERVAL_TICKS = 40;
    /** Сколько стоит на месте после того, как нашла свет: 1200 тиков = 60 секунд. */
    public static final int HOLD_TICKS = 1200;
    /** Уровень света, ниже которого считается "темно" (0..15). */
    public static final int LIGHT_OK = 8;
    /** Говорить ли во сне (в тёмной спальне). */
    public static final boolean TALK_WHILE_SLEEPING = false;
    /** Радиус поиска света и кровати. */
    public static final int SEARCH_RADIUS = 20;
    public static final int HOME_RADIUS = 32;

    private BlockPos homeBed;
    private int holdTicks;
    private int soundTimer;
    private boolean wasDark;
    private int homeSearchCooldown;
    private int lastLine = -1;

    public BabushkaEntity(EntityType<? extends BabushkaEntity> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.setCanFloat(true);
        }
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.5);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SleepInBedGoal(this));
        goalSelector.addGoal(2, new StandInLightGoal(this));
        goalSelector.addGoal(3, new SeekLightGoal(this));
        goalSelector.addGoal(4, new OpenDoorGoal(this, true));
        goalSelector.addGoal(5, new GoHomeGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // ---------------------------------------------------------------- освещение / время

    public boolean isDarkHere() {
        return level().getMaxLocalRawBrightness(blockPosition()) < LIGHT_OK;
    }

    public boolean isLightEnoughAt(BlockPos pos) {
        return level().getMaxLocalRawBrightness(pos) >= LIGHT_OK;
    }

    /** Ночь (или гроза) — время спать. Единственное место, зависящее от API времени. */
    public boolean isNightTime() {
        return level().isDarkOutside();
    }

    public int getHoldTicks() {
        return holdTicks;
    }

    // ---------------------------------------------------------------- тик

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel serverLevel) {
            serverTick(serverLevel);
        }
    }

    private void serverTick(ServerLevel level) {
        if (--homeSearchCooldown <= 0) {
            homeSearchCooldown = homeBed == null ? 200 : 100;
            validateOrFindHome(level);
        }

        if (isSleeping()) {
            holdTicks = 0;
            wasDark = false;
            if (TALK_WHILE_SLEEPING && isDarkHere()) {
                speakTick();
            } else {
                soundTimer = 0;
            }
            return;
        }

        boolean dark = isDarkHere();
        if (dark) {
            holdTicks = 0;
            speakTick();
        } else {
            if (wasDark) {
                holdTicks = HOLD_TICKS; // только что нашла свет — стоим минуту
            }
            if (holdTicks > 0) {
                holdTicks--;
                speakTick();
            } else {
                soundTimer = 0;
            }
        }
        wasDark = dark;
    }

    private void speakTick() {
        if (soundTimer > 0) {
            soundTimer--;
            return;
        }
        int lineTicks = playRandomLine();
        soundTimer = lineTicks + SOUND_INTERVAL_TICKS; // длина реплики + 2 секунды паузы
    }

    /** Проигрывает случайную реплику, возвращает её длительность в тиках. */
    private int playRandomLine() {
        int n = ModSounds.LINES.size();
        if (n == 0) {
            return 0;
        }
        int i = random.nextInt(n);
        if (n > 1 && i == lastLine) {
            i = (i + 1 + random.nextInt(n - 1)) % n; // не повторяем одну и ту же подряд
        }
        lastLine = i;
        SoundEvent event = ModSounds.LINES.get(i);
        playSound(event, 1.0F, 1.0F);
        return ModSounds.LINE_TICKS[i];
    }

    // ---------------------------------------------------------------- дом и кровать

    public BlockPos getHomeBed() {
        return homeBed;
    }

    private void setHome(BlockPos pos) {
        homeBed = pos;
    }

    private void validateOrFindHome(ServerLevel level) {
        if (homeBed != null && !isBed(level, homeBed)) {
            homeBed = null;
        }
        if (homeBed == null) {
            BlockPos found = findBed(level);
            if (found != null) {
                setHome(found);
            }
        }
    }

    private static boolean isBed(ServerLevel level, BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof BedBlock;
    }

    /** Ближайшая свободная кровать (изголовье) в радиусе. */
    private BlockPos findBed(ServerLevel level) {
        BlockPos origin = blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int r = 16;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -6; dy <= 6; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!level.hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (state.getBlock() instanceof BedBlock
                        && state.getValue(BedBlock.PART) == BedPart.HEAD
                        && !state.getValue(BedBlock.OCCUPIED)) {
                        double d = origin.distSqr(cursor);
                        if (d < bestDist) {
                            bestDist = d;
                            best = cursor.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Если ушла от дома дальше HOME_RADIUS/2 — возвращается к своей кровати. */
    private class GoHomeGoal extends Goal {
        GoHomeGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return homeBed != null && !isSleeping()
                && blockPosition().distSqr(homeBed) > (HOME_RADIUS / 2.0) * (HOME_RADIUS / 2.0);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() && !getNavigation().isDone();
        }

        @Override
        public void start() {
            getNavigation().moveTo(homeBed.getX() + 0.5, homeBed.getY(), homeBed.getZ() + 0.5, 0.6);
        }
    }

    // ---------------------------------------------------------------- поведение жителя

    /** Во сне не двигается. */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isSleeping();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // ---------------------------------------------------------------- сохранение

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (homeBed != null) {
            output.store("HomeBed", BlockPos.CODEC, homeBed);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read("HomeBed", BlockPos.CODEC).ifPresent(this::setHome);
    }
}
