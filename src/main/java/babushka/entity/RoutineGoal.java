package babushka.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Всё поведение "в доме погас свет":
 * LEAVING (выйти из дома) -> WANDER (30 с гулять) -> [HUNT: заметила игрока — бежит и бьёт]
 * -> RETURNING (вернуться) -> RESTORING (включить свет: рычаг или факел).
 */
public class RoutineGoal extends Goal {
    private static final double WALK = 0.8;
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
    private BlockPos lampAnchor;            // где искать рычаги: у выключенной лампы
    private Boolean lampEvidence;           // есть ли в доме лампы (значит, нужен рычаг, а не факел)
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
        lampEvidence = null;
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
            // источник "магия": пробивает броню и НЕ снимает с неё прочность
            DamageSource source = level.damageSources().indirectMagic(mob, mob);
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
            finish(false);
            return;
        }
        if (mob.inHouse()) {
            mob.phase = BabushkaEntity.Phase.RESTORING;
            restoreWait = 0;
            mob.getNavigation().stop();
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

    private void tickRestoring(ServerLevel level) {
        if (--restoreWait > 0) {
            return;
        }
        restoreWait = 10;

        if (mob.house.isLit(level, BabushkaEntity.LIGHT_OK)) {
            if (toggledLever != null) {
                mob.debug("Рычаг сработал — свет включён.");
            }
            finish(true);
            return;
        }

        // Дёрнула рычаг, подождала — света нет: возвращаем рычаг как был и пробуем следующий
        if (toggledLever != null) {
            toggleLever(level, toggledLever);
            triedLevers.add(toggledLever.asLong());
            toggledLever = null;
            leverAttempts++;
            mob.debug("Этот рычаг не помог, пробую другой.");
        }

        // Анализ источников света в доме
        if (lampEvidence == null) {
            analyseLamps(level);
        }

        // 1) Есть лампы -> в первую очередь ищем и дёргаем рычаг
        if (lampEvidence && leverAttempts < 6) {
            if (currentLever == null) {
                currentLever = pickLever(level);
                leverWalkTicks = 0;
                if (currentLever == null) {
                    leverAttempts = 99;
                    mob.debug("Рычаг не нашла, ставлю факел.");
                    return;
                }
                mob.debug("Лампа не горит — иду к рычагу.");
            }
            Vec3 leverCenter = Vec3.atCenterOf(currentLever);
            if (mob.distanceToSqr(leverCenter) < 9.0) {
                toggleLever(level, currentLever);
                toggledLever = currentLever;
                currentLever = null;
                restoreWait = 40;          // даём redstone сработать
            } else if (++leverWalkTicks > 60) {
                triedLevers.add(currentLever.asLong());   // не может дойти — следующий рычаг
                currentLever = null;
                leverAttempts++;
            } else {
                BlockPos stand = currentLeverStand != null ? currentLeverStand : currentLever;
                mob.getNavigation().moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, WALK);
            }
            return;
        }

        // 2) Ламп нет (или рычаги не помогли) -> ставим факел в доме
        if (torchesPlaced >= 2) {
            mob.debug("Не получилось включить свет.");
            finish(false);
            return;
        }
        if (currentTorchSpot == null) {
            currentTorchSpot = pickTorchSpot(level, goneFixedSources(level));
            if (currentTorchSpot == null) {
                mob.debug("Некуда поставить факел.");
                finish(false);
                return;
            }
        }
        if (mob.distanceToSqr(Vec3.atCenterOf(currentTorchSpot)) < 9.0) {
            level.setBlock(currentTorchSpot, Blocks.TORCH.defaultBlockState(), 3);
            level.playSound(null, currentTorchSpot, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            mob.swing(InteractionHand.MAIN_HAND);
            mob.debug("Поставила факел.");
            currentTorchSpot = null;
            torchesPlaced++;
            restoreWait = 20;
        } else {
            mob.getNavigation().moveTo(
                currentTorchSpot.getX() + 0.5, currentTorchSpot.getY(), currentTorchSpot.getZ() + 0.5, WALK);
        }
    }

    /** Есть ли у дома переключаемые лампы (известные раньше или выключенные сейчас рядом с домом). */
    private void analyseLamps(ServerLevel level) {
        lampEvidence = false;
        for (Map.Entry<Long, Boolean> e : mob.knownSources.entrySet()) {
            if (e.getValue()) {
                lampEvidence = true;
                lampAnchor = BlockPos.of(e.getKey());
                break;
            }
        }
        if (lampEvidence) {
            return;
        }
        // выключенные лампы, которые видны прямо сейчас (в доме и в стенах вокруг)
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (long l : mob.house.cells) {
            BlockPos c = BlockPos.of(l);
            minX = Math.min(minX, c.getX()); maxX = Math.max(maxX, c.getX());
            minY = Math.min(minY, c.getY()); maxY = Math.max(maxY, c.getY());
            minZ = Math.min(minZ, c.getZ()); maxZ = Math.max(maxZ, c.getZ());
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = minX - 3; x <= maxX + 3; x++) {
            for (int y = minY - 3; y <= maxY + 3; y++) {
                for (int z = minZ - 3; z <= maxZ + 3; z++) {
                    p.set(x, y, z);
                    if (!level.hasChunkAt(p)) {
                        continue;
                    }
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof RedstoneLampBlock || s.getBlock() instanceof CopperBulbBlock) {
                        lampEvidence = true;
                        lampAnchor = p.immutable();
                        mob.debug("Вижу лампу — значит, свет включается рычагом.");
                        return;
                    }
                }
            }
        }
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

    /**
     * Ближайший к лампе рычаг, до которого можно дойти (даже за дверью — рычаги бывают в другой комнате).
     */
    private BlockPos pickLever(ServerLevel level) {
        BlockPos anchor = lampAnchor != null ? lampAnchor : mob.getHomeBed();
        if (anchor == null) {
            return null;
        }
        List<BlockPos> levers = new ArrayList<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -14; x <= 14; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -14; z <= 14; z++) {
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
        int tried = 0;
        for (BlockPos lever : levers) {
            if (tried++ >= 10) {
                break;
            }
            BlockPos stand = findStandCell(level, lever);
            if (stand == null) {
                continue;
            }
            Path path = mob.getNavigation().createPath(stand, 0);
            if (path != null && path.canReach()) {
                currentLeverStand = stand;
                return lever;
            }
        }
        return null;
    }

    /** Свободная клетка рядом с рычагом, где можно встать. */
    private BlockPos findStandCell(ServerLevel level, BlockPos lever) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos c = lever.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(c);
                    BlockState floor = level.getBlockState(c.below());
                    if (s.getCollisionShape(level, c).isEmpty()
                        && level.getBlockState(c.above()).getCollisionShape(level, c.above()).isEmpty()
                        && floor.isFaceSturdy(level, c.below(), Direction.UP)) {
                        double d = c.distSqr(mob.blockPosition());
                        if (d < bestD) {
                            bestD = d;
                            best = c;
                        }
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

    private void finish(boolean lit) {
        mob.wasLit = lit;           // если сдалась — не зацикливаемся, пока свет снова не появится
        mob.phase = BabushkaEntity.Phase.NONE;
        mob.getNavigation().stop();
    }
}
