package babushka.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import babushka.ModDamage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Всё поведение "в доме погас свет":
 * RESTORING (сразу идёт включать свет: рычаг, иначе факел) -> LEAVING (выйти из дома)
 * -> WANDER (10 с гулять) -> [HUNT: заметила игрока — бежит и бьёт] -> RETURNING (вернуться домой).
 */
public class RoutineGoal extends Goal {
    private static final double WALK = 0.8;
    private static final double RUN_TO_LIGHT = 1.2;   // торопится включить свет
    private static final double RUN = 1.6;      // 0.3 * 1.6 — быстрее бегущего игрока
    private static final double HUNT_GIVE_UP_DISTANCE = 40.0;

    private final BabushkaEntity mob;

    private Player target;
    private int attackCooldown;
    private int noSightTicks;

    private int restoreWait;
    private int leverAttempts;
    private int torchesPlaced;
    private BlockPos currentLever;
    private BlockPos currentLeverStand;
    private int leverWalkTicks;
    private BlockPos toggledLever;          // рычаг, который только что дёрнула (ждём результата)
    private int countBeforeToggle;          // сколько источников горело до переключения
    private BlockPos lampAnchor;            // где искать рычаги: у выключенной лампы
    private boolean lampLost;               // среди пропавших источников есть переключаемая лампа
    private BlockPos currentTorchSpot;
    private final Set<Long> triedLevers = new HashSet<>();

    public RoutineGoal(BabushkaEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return mob.phase != BabushkaEntity.Phase.NONE;
    }

    @Override
    public boolean canContinueToUse() {
        return mob.phase != BabushkaEntity.Phase.NONE;
    }

    @Override
    public void start() {
        target = null;
        attackCooldown = 0;
        noSightTicks = 0;
        restoreWait = 0;
        leverAttempts = 0;
        torchesPlaced = 0;
        currentLever = null;
        currentLeverStand = null;
        leverWalkTicks = 0;
        toggledLever = null;
        lampAnchor = null;
        lampLost = false;
        countBeforeToggle = 0;
        currentTorchSpot = null;
        triedLevers.clear();
    }

    @Override
    public void stop() {
        mob.setAggressive(false);
        mob.getNavigation().stop();
        target = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        // Во время выхода и прогулки высматривает игрока
        if ((mob.phase == BabushkaEntity.Phase.LEAVING || mob.phase == BabushkaEntity.Phase.WANDER)
            && mob.tickCount % 10 == 0) {
            Player seen = findPlayer(level);
            if (seen != null) {
                target = seen;
                noSightTicks = 0;
                mob.phase = BabushkaEntity.Phase.HUNT;
            }
        }

        switch (mob.phase) {
            case LEAVING -> tickLeaving();
            case WANDER -> tickWander(level);
            case HUNT -> tickHunt(level);
            case RETURNING -> tickReturning();
            case RESTORING -> tickRestoring(level);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ выход

    private void tickLeaving() {
        mob.phaseTimer--;
        boolean near = mob.exitPos != null
            && mob.distanceToSqr(Vec3.atBottomCenterOf(mob.exitPos)) < 4.0;
        if (!mob.inHouse() || near || mob.phaseTimer <= 0 || mob.exitPos == null) {
            mob.phase = BabushkaEntity.Phase.WANDER;
            mob.phaseTimer = BabushkaEntity.WANDER_TICKS;
            mob.getNavigation().stop();
            return;
        }
        if (mob.getNavigation().isDone() || mob.tickCount % 40 == 0) {
            mob.getNavigation().moveTo(
                mob.exitPos.getX() + 0.5, mob.exitPos.getY(), mob.exitPos.getZ() + 0.5, WALK);
        }
    }

    // ------------------------------------------------------------------ прогулка 30 секунд

    private void tickWander(ServerLevel level) {
        mob.phaseTimer--;
        if (mob.phaseTimer <= 0) {
            mob.phase = BabushkaEntity.Phase.RETURNING;
            mob.phaseTimer = 900;
            mob.getNavigation().stop();
            return;
        }
        if (mob.getNavigation().isDone()) {
            Vec3 dest = null;
            BlockPos bed = mob.getHomeBed();
            if (bed != null && mob.blockPosition().distSqr(bed) > 16 * 16) {
                dest = LandRandomPos.getPosTowards(mob, 10, 6, Vec3.atCenterOf(bed));
            }
            if (dest == null) {
                dest = LandRandomPos.getPos(mob, 10, 6);
            }
            if (dest != null) {
                mob.getNavigation().moveTo(dest.x, dest.y, dest.z, WALK);
            }
        }
    }

    private Player findPlayer(ServerLevel level) {
        Player p = level.getNearestPlayer(
            mob.getX(), mob.getY(), mob.getZ(), BabushkaEntity.SEE_PLAYER_RANGE,
            EntitySelector.NO_CREATIVE_OR_SPECTATOR);
        if (p != null && p.isAlive() && mob.hasLineOfSight(p)) {
            return p;
        }
        return null;
    }

    // ------------------------------------------------------------------ погоня

    private void tickHunt(ServerLevel level) {
        if (target == null || !target.isAlive() || target.isCreative() || target.isSpectator()
            || mob.distanceToSqr(target) > HUNT_GIVE_UP_DISTANCE * HUNT_GIVE_UP_DISTANCE) {
            endHunt();
            return;
        }
        if (mob.hasLineOfSight(target)) {
            noSightTicks = 0;
        } else if (++noSightTicks > 200) {
            endHunt();
            return;
        }

        mob.setAggressive(true);
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (mob.tickCount % 4 == 0 || mob.getNavigation().isDone()) {
            mob.getNavigation().moveTo(target, RUN);
        }

        if (attackCooldown <= 0 && mob.distanceToSqr(target) < 3.5) {
            mob.swing(InteractionHand.MAIN_HAND);
            // свой тип урона (в чате: "был убит Галей за плохое поведение"), броня не ломается
            DamageSource source = ModDamage.of(level, ModDamage.GALYA, mob);
            target.hurtServer(level, source, BabushkaEntity.ATTACK_DAMAGE);
            attackCooldown = 20;
        }
    }

    private void endHunt() {
        target = null;
        mob.setAggressive(false);
        mob.getNavigation().stop();
        mob.phase = BabushkaEntity.Phase.RETURNING;
        mob.phaseTimer = 900;
    }

    // ------------------------------------------------------------------ возвращение

    private void tickReturning() {
        BlockPos bed = mob.getHomeBed();
        if (bed == null) {
            if (mob.level() instanceof ServerLevel sl) {
                finish(sl);
            }
            return;
        }
        if (mob.inHouse()) {
            if (mob.level() instanceof ServerLevel sl) {
                finish(sl);
            }
            return;
        }
        mob.phaseTimer--;
        if (mob.phaseTimer <= 0) {
            // застряла снаружи (например, дверь заперта) — просто возвращаем домой
            mob.setPos(bed.getX() + 0.5, bed.getY() + 1.0, bed.getZ() + 0.5);
            mob.getNavigation().stop();
            return;
        }
        if (mob.getNavigation().isDone() || mob.tickCount % 40 == 0) {
            mob.getNavigation().moveTo(bed.getX() + 0.5, bed.getY() + 1.0, bed.getZ() + 0.5, 1.0);
        }
    }

    // ------------------------------------------------------------------ включить свет

    /** Рычаг должен быть в пределах 1 блока от её тела (в любом направлении: выше, ниже, на полу, на стене). */
    private static final double LEVER_REACH = 1.0;

    private double reachDistance(BlockPos lever) {
        return Math.sqrt(mob.getBoundingBox().distanceToSqr(Vec3.atCenterOf(lever)));
    }

    private void tickRestoring(ServerLevel level) {
        if (--restoreWait > 0) {
            return;
        }
        restoreWait = 10;

        int now = mob.litCount(level);
        if (now >= mob.litBefore) {
            mob.afterLightRestored(level);   // свет вернулся: полиция (65%) и прогулка 10 секунд
            return;
        }

        // Дёрнула рычаг и подождала: свет добавился — оставляем; нет — возвращаем рычаг как был и берём другой
        if (toggledLever != null) {
            if (now <= countBeforeToggle) {
                toggleLever(level, toggledLever);
            }
            triedLevers.add(toggledLever.asLong());
            toggledLever = null;
            leverAttempts++;
        }

        // Что погасло: лампа (включается рычагом) или факел?
        if (lampAnchor == null && !lampLost) {
            analyseLost(level);
        }

        // 1) Погасла лампа -> сразу к рычагу
        if (lampLost && leverAttempts < 6) {
            if (currentLever == null) {
                currentLever = pickLever(level);
                leverWalkTicks = 0;
                if (currentLever == null) {
                    leverAttempts = 99;      // рычагов нет — ставит факел
                    return;
                }
            }
            double dist = reachDistance(currentLever);
            boolean arrived = mob.getNavigation().isDone() && leverWalkTicks > 1 && dist <= 2.5;
            if (dist <= LEVER_REACH + 0.05 || arrived) {
                countBeforeToggle = now;
                toggleLever(level, currentLever);
                toggledLever = currentLever;
                currentLever = null;
                restoreWait = 40;            // даём redstone сработать
            } else if (++leverWalkTicks > 60) {
                triedLevers.add(currentLever.asLong());   // не может дойти — следующий рычаг
                currentLever = null;
                leverAttempts++;
            } else {
                BlockPos stand = currentLeverStand != null ? currentLeverStand : currentLever;
                mob.getNavigation().moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, RUN_TO_LIGHT);
            }
            return;
        }

        // 2) Погас факел (или рычаги не помогли) -> ставит факел в доме
        if (torchesPlaced >= 2) {
            finish(level);
            return;
        }
        if (currentTorchSpot == null) {
            currentTorchSpot = pickTorchSpot(level, goneFixedSources(level));
            if (currentTorchSpot == null) {
                finish(level);
                return;
            }
        }
        if (mob.distanceToSqr(Vec3.atCenterOf(currentTorchSpot)) < 9.0) {
            level.setBlock(currentTorchSpot, Blocks.TORCH.defaultBlockState(), 3);
            level.playSound(null, currentTorchSpot, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            mob.swing(InteractionHand.MAIN_HAND);
            currentTorchSpot = null;
            torchesPlaced++;
            restoreWait = 20;
        } else {
            mob.getNavigation().moveTo(
                currentTorchSpot.getX() + 0.5, currentTorchSpot.getY(), currentTorchSpot.getZ() + 0.5, RUN_TO_LIGHT);
        }
    }

    /** Среди пропавших источников ищем переключаемую лампу — рядом с ней будем искать рычаг. */
    private void analyseLost(ServerLevel level) {
        for (Map.Entry<Long, Boolean> e : mob.knownSources.entrySet()) {
            BlockPos p = BlockPos.of(e.getKey());
            BlockState s = level.getBlockState(p);
            boolean emitting = s.getLightEmission() >= BabushkaEntity.LIGHT_OK;
            if (e.getValue() && !emitting) {      // была лампа и сейчас не горит
                lampLost = true;
                lampAnchor = p;
                return;
            }
        }
        lampAnchor = mob.getHomeBed();            // ламп среди пропавших нет — рычаг не нужен
    }

    /** Источники света, которые пропали совсем (факел сломали) — на их место ставим факел. */
    private List<BlockPos> goneFixedSources(ServerLevel level) {
        List<BlockPos> gone = new ArrayList<>();
        for (Map.Entry<Long, Boolean> e : mob.knownSources.entrySet()) {
            BlockPos p = BlockPos.of(e.getKey());
            if (!e.getValue() && level.getBlockState(p).getLightEmission() < BabushkaEntity.LIGHT_OK) {
                gone.add(p);
            }
        }
        return gone;
    }

    /** Ближайший к лампе непроверенный рычаг (где угодно: на стене, на полу, на потолке, в другой комнате). */
    private BlockPos pickLever(ServerLevel level) {
        BlockPos anchor = lampAnchor != null ? lampAnchor : mob.getHomeBed();
        if (anchor == null) {
            return null;
        }
        List<BlockPos> levers = new ArrayList<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -16; x <= 16; x++) {
            for (int y = -10; y <= 10; y++) {
                for (int z = -16; z <= 16; z++) {
                    p.set(anchor.getX() + x, anchor.getY() + y, anchor.getZ() + z);
                    if (!level.hasChunkAt(p) || triedLevers.contains(p.asLong())) {
                        continue;
                    }
                    if (level.getBlockState(p).getBlock() instanceof LeverBlock) {
                        levers.add(p.immutable());
                    }
                }
            }
        }
        levers.sort(Comparator.comparingDouble(lp -> lp.distSqr(anchor)));
        for (BlockPos lever : levers) {
            BlockPos stand = findStandCell(level, lever);
            if (stand != null) {
                currentLeverStand = stand;
                return lever;
            }
            triedLevers.add(lever.asLong());
        }
        return null;
    }

    /**
     * Клетка, где можно встать так, чтобы рычаг был в пределах 1 блока от её тела —
     * неважно, выше он или ниже, на полу или на стене. Если такой нет — самая близкая к рычагу.
     */
    private BlockPos findStandCell(ServerLevel level, BlockPos lever) {
        Vec3 center = Vec3.atCenterOf(lever);
        double halfW = mob.getBbWidth() / 2.0;
        double height = mob.getBbHeight();
        BlockPos best = null;
        double bestScore = Double.MAX_VALUE;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -4; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos c = lever.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(c);
                    BlockState floor = level.getBlockState(c.below());
                    if (!s.getCollisionShape(level, c).isEmpty()
                        || !level.getBlockState(c.above()).getCollisionShape(level, c.above()).isEmpty()
                        || !floor.isFaceSturdy(level, c.below(), Direction.UP)) {
                        continue;
                    }
                    AABB box = new AABB(c.getX() + 0.5 - halfW, c.getY(), c.getZ() + 0.5 - halfW,
                        c.getX() + 0.5 + halfW, c.getY() + height, c.getZ() + 0.5 + halfW);
                    double d = Math.sqrt(box.distanceToSqr(center));
                    // в зоне досягаемости предпочитаем ближайшую к бабушке, иначе — ближайшую к рычагу
                    double score = d <= LEVER_REACH ? c.distSqr(mob.blockPosition()) : 10000.0 + d;
                    if (score < bestScore) {
                        bestScore = score;
                        best = c;
                    }
                }
            }
        }
        return best;
    }

    private void toggleLever(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof LeverBlock)) {
            return;
        }
        boolean on = state.getValue(BlockStateProperties.POWERED);
        level.setBlock(pos, state.setValue(BlockStateProperties.POWERED, !on), 3);
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, on ? 0.5F : 0.6F);
        level.updateNeighborsAt(pos, state.getBlock());
        for (Direction d : Direction.values()) {
            level.updateNeighborsAt(pos.relative(d), state.getBlock());
        }
        mob.swing(InteractionHand.MAIN_HAND);
    }

    /** Место для факела: где раньше был факел, иначе ближайшая к кровати свободная клетка пола в доме. */
    private BlockPos pickTorchSpot(ServerLevel level, List<BlockPos> goneFixed) {
        BlockState torch = Blocks.TORCH.defaultBlockState();
        for (BlockPos p : goneFixed) {
            if (level.getBlockState(p).isAir() && torch.canSurvive(level, p) && mob.house.contains(p)) {
                return p;
            }
        }
        BlockPos bed = mob.getHomeBed();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (long l : mob.house.cells) {
            BlockPos c = BlockPos.of(l);
            if (!level.getBlockState(c).isAir() || !torch.canSurvive(level, c)) {
                continue;
            }
            double d = bed == null ? 0 : c.distSqr(bed);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return best;
    }

    private void finish(ServerLevel level) {
        // новый "эталон" света, чтобы не срабатывать повторно на то, что уже не починить
        mob.knownSources = mob.house.scanSources(level, BabushkaEntity.LIGHT_OK);
        mob.phase = BabushkaEntity.Phase.NONE;
        mob.getNavigation().stop();
    }
}
