package babushka.entity;

import babushka.ModSounds;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Бабушка: живёт как житель (ходит, спит ночью в своей кровати, открывает двери),
 * но не торгует. В темноте ищет свет и каждые 2 секунды выдаёт случайную реплику;
 * найдя свет, стоит на месте ещё минуту и продолжает говорить с тем же интервалом.
 */
public class BabushkaEntity extends PathfinderMob {

    // ---- Настройки (меняйте здесь) ----
    /** Пауза между репликами, отсчитывается ПОСЛЕ окончания реплики: 40 тиков = 2 секунды. */
    public static final int SOUND_INTERVAL_TICKS = 40;
    /** Сколько гуляет по округе после того, как включила свет: 200 тиков = 10 секунд. */
    public static final int WANDER_TICKS = 200;
    /** Уровень блочного света, начиная с которого дом считается освещённым (0..15). */
    public static final int LIGHT_OK = 8;
    /** На каком расстоянии замечает игрока во время прогулки. */
    public static final double SEE_PLAYER_RANGE = 24.0;
    public static final int HOME_RADIUS = 32;
    /** Нужен только старым файлам SeekLightGoal/StandInLightGoal (их можно удалить из репозитория). */
    public static final int SEARCH_RADIUS = 20;
    /** На каком расстоянии от игрока появляется отряд полиции. */
    public static final int POLICE_SPAWN_DISTANCE = 40;
    /** Урон по игроку (источник "магия" — не снимает прочность брони). */
    public static final float ATTACK_DAMAGE = 99999.0F;
    /** Шанс вызвать отряд полиции, когда она включила свет. */
    public static final float POLICE_CHANCE = 0.65F;
    /** Радиус, в котором она замечает игрока в голове моба. */
    public static final double MOB_HEAD_RANGE = 8.0;
    /** Этапы: включить свет (RESTORING) -> выйти из дома -> гулять 10 с (или гнаться за игроком) -> вернуться. */
    public enum Phase { NONE, LEAVING, WANDER, HUNT, RETURNING, RESTORING }

    private BlockPos homeBed;
    private int soundTimer;
    private int homeSearchCooldown;
    private int lastLine = -1;
    private int hahaCooldown;

    // состояние "света нет" (доступно RoutineGoal из этого же пакета)
    Phase phase = Phase.NONE;
    int phaseTimer;
    BlockPos exitPos;
    House house = new House();
    private int houseRefresh;
    /** Горящие источники света дома (позиция -> переключаемая лампа?). Во время "света нет" хранит снимок до отключения. */
    Map<Long, Boolean> knownSources = new HashMap<>();
    /** Сколько источников горело до отключения — столько надо вернуть. */
    int litBefore;

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
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.ATTACK_DAMAGE, 99999.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new RoutineGoal(this));
        goalSelector.addGoal(2, new SleepInBedGoal(this));
        goalSelector.addGoal(4, new OpenDoorGoal(this, true));
        goalSelector.addGoal(5, new GoHomeGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    // ---------------------------------------------------------------- время

    /** Ночь (или гроза) — время спать. Единственное место, зависящее от API времени. */
    public boolean isNightTime() {
        return level().isDarkOutside();
    }

    public boolean isRoutineActive() {
        return phase != Phase.NONE;
    }

    /** Бабушка сейчас в своём доме? */
    boolean inHouse() {
        return house.nearCell(blockPosition());
    }

    /** Сколько источников света сейчас горит в доме. */
    int litCount(ServerLevel level) {
        return house.scanSources(level, LIGHT_OK).size();
    }

    // Оставлено для совместимости со старыми файлами SeekLightGoal / StandInLightGoal (их можно удалить).
    public int getHoldTicks() {
        return 0;
    }

    public boolean isDarkHere() {
        return false;
    }

    public boolean isLightEnoughAt(BlockPos pos) {
        return true;
    }

    // ---------------------------------------------------------------- вспомогательное

    private static boolean isMobHead(ItemStack stack) {
        return stack.is(Items.ZOMBIE_HEAD) || stack.is(Items.SKELETON_SKULL)
            || stack.is(Items.WITHER_SKELETON_SKULL) || stack.is(Items.CREEPER_HEAD)
            || stack.is(Items.PIGLIN_HEAD) || stack.is(Items.DRAGON_HEAD)
            || stack.is(Items.CARVED_PUMPKIN) || stack.is(Items.PUMPKIN);
    }

    /** Рядом (в поле зрения) игрок с головой моба или тыквой на голове — "Ха-ха, артисты". */
    private void checkMobHead(ServerLevel level) {
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(MOB_HEAD_RANGE))) {
            if (!p.isAlive() || p.isSpectator() || !isMobHead(p.getItemBySlot(EquipmentSlot.HEAD))) {
                continue;
            }
            if (!hasLineOfSight(p)) {
                continue;
            }
            getLookControl().setLookAt(p, 30.0F, 30.0F);
            playSound(ModSounds.HAHA_ARTISTS, 1.0F, 1.0F);
            hahaCooldown = ModSounds.HAHA_TICKS + 100;
            return;
        }
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
        if (homeBed != null && --houseRefresh <= 0) {
            houseRefresh = 20;
            refreshHouse(level);
        }

        if (hahaCooldown > 0) {
            hahaCooldown--;
        } else if (!isSleeping() && tickCount % 10 == 0) {
            checkMobHead(level);
        }

        if (isSleeping()) {
            // Во сне ИИ-цели не тикают (см. isImmobile), поэтому будим здесь: утром — вставать.
            if (!isNightTime()) {
                stopSleeping();
            }
            soundTimer = 0;
            return;
        }

        // Фразы — только пока в доме "нет света" (выход, прогулка, возвращение, включение света)
        if (phase != Phase.NONE) {
            speakTick();
        } else {
            soundTimer = 0;
        }
    }

    /**
     * Пересчитывает дом и следит за светом. Реагирует на пропажу ЛЮБОГО горевшего источника
     * (лампу выключили, факел сломали), даже если в доме остался другой свет.
     */
    private void refreshHouse(ServerLevel level) {
        house = House.compute(level, homeBed);
        if (house.isEmpty()) {
            house = House.fallback(level, homeBed);
        }
        if (house.isEmpty() || phase != Phase.NONE) {
            return;
        }
        Map<Long, Boolean> now = house.scanSources(level, LIGHT_OK);
        boolean lost = false;
        for (Long key : knownSources.keySet()) {
            if (!now.containsKey(key) && level.hasChunkAt(BlockPos.of(key))) {
                lost = true;
                break;
            }
        }
        if (lost) {
            if (inHouse()) {
                litBefore = knownSources.size();   // снимок "до" остаётся в knownSources
                startLightsOut(level);
                return;
            }
        }
        knownSources = now;
    }

    /** Свет пропал: она сразу идёт его включать (фразы начинаются сразу). */
    private void startLightsOut(ServerLevel level) {
        if (isSleeping()) {
            stopSleeping();
        }
        getNavigation().stop();
        soundTimer = 0;
        exitPos = null;
        phase = Phase.RESTORING;
    }

    /**
     * Свет включён: с шансом 65% вызывает отряд полиции (появится в 40 блоках от игрока),
     * затем выходит из дома и гуляет по округе 10 секунд.
     */
    void afterLightRestored(ServerLevel level) {
        knownSources = house.scanSources(level, LIGHT_OK);
        if (random.nextFloat() < POLICE_CHANCE) {
            PoliceSquads.spawn(level, this);
        }
        exitPos = house.findExit(level, blockPosition());
        if (exitPos != null) {
            phase = Phase.LEAVING;
            phaseTimer = 300;
        } else {
            phase = Phase.WANDER;
            phaseTimer = WANDER_TICKS;
        }
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
            house = new House();
            phase = Phase.NONE;
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
