package babushka.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * Один отряд. Этапы:
 * GATHER — идут к бабушке; как только дошли — SEARCH (бегают по округе, 30 с; никого не нашли — исчезают);
 * CHASE — увидели игрока и бегут за ним; поймали — ARREST: игрок на своём спавне, 15 с не может двигаться,
 * вокруг ходят полицейские, потом все исчезают.
 */
final class PoliceSquad {
    enum State { GATHER, SEARCH, CHASE, ARREST }

    static final int SEARCH_TICKS = 600;     // 30 секунд
    static final int ARREST_TICKS = 300;     // 15 секунд
    static final double SEE_RANGE = 24.0;
    static final String ARREST_TEXT = "вы арестованы на 15 суток";

    final UUID id = UUID.randomUUID();
    final UUID babushkaId;
    final ServerLevel level;
    final List<PoliceEntity> members = new ArrayList<>();
    BlockPos center;
    boolean done;

    private State state = State.GATHER;
    private int timer;
    private int gatherTimer = 6000;          // страховка: 5 минут на дорогу (они могут быть далеко)
    private ServerPlayer target;
    private int noSight;
    private Vec3 freezePos;
    private boolean arrestOnly;

    PoliceSquad(ServerLevel level, UUID babushkaId, BlockPos center) {
        this.level = level;
        this.babushkaId = babushkaId;
        this.center = center;
    }

    void tick() {
        members.removeIf(m -> m.isRemoved() || !m.isAlive());
        if (members.isEmpty() && !(state == State.ARREST && arrestOnly)) {
            release();
            return;
        }
        Entity babushka = level.getEntity(babushkaId);
        if (babushka != null) {
            center = babushka.blockPosition();
        }
        switch (state) {
            case GATHER -> tickGather(babushka);
            case SEARCH -> tickSearch(babushka);
            case CHASE -> tickChase();
            case ARREST -> tickArrest();
        }
    }

    // ------------------------------------------------------------------ идут к бабушке

    private void tickGather(Entity babushka) {
        if (--gatherTimer <= 0) {
            dismiss();
            return;
        }
        boolean anyReached = false;
        for (PoliceEntity m : members) {
            if (babushka != null && m.distanceToSqr(babushka) < 9.0) {
                m.reached = true;
            }
            anyReached |= m.reached;
            m.followTarget = babushka;
            m.center = center;
            m.mode = m.reached ? PoliceEntity.Mode.SEARCH : PoliceEntity.Mode.GATHER;
        }
        if (anyReached) {
            state = State.SEARCH;
            timer = SEARCH_TICKS;
        }
        lookForPlayer();
    }

    // ------------------------------------------------------------------ бегают по округе

    private void tickSearch(Entity babushka) {
        for (PoliceEntity m : members) {
            if (babushka != null && m.distanceToSqr(babushka) < 9.0) {
                m.reached = true;
            }
            m.followTarget = babushka;
            m.center = center;
            m.mode = m.reached ? PoliceEntity.Mode.SEARCH : PoliceEntity.Mode.GATHER;
        }
        if (--timer <= 0) {
            dismiss(); // за 30 секунд никого не нашли
            return;
        }
        lookForPlayer();
    }

    private void lookForPlayer() {
        for (PoliceEntity m : members) {
            if (m.tickCount % 5 != 0) {
                continue;
            }
            Player p = level.getNearestPlayer(
                m.getX(), m.getY(), m.getZ(), SEE_RANGE, EntitySelector.NO_SPECTATORS);
            if (p instanceof ServerPlayer sp && sp.isAlive() && m.hasLineOfSight(sp)) {
                target = sp;
                noSight = 0;
                state = State.CHASE;
                return;
            }
        }
    }

    // ------------------------------------------------------------------ погоня

    private void tickChase() {
        if (target == null || target.isRemoved() || !target.isAlive() || target.level() != level
            || members.get(0).distanceToSqr(target) > 64.0 * 64.0) {
            backToSearch();
            return;
        }
        boolean seen = false;
        for (PoliceEntity m : members) {
            m.mode = PoliceEntity.Mode.CHASE;
            m.followTarget = target;
            if (m.distanceToSqr(target) < 30.0 * 30.0 && m.hasLineOfSight(target)) {
                seen = true;
            }
        }
        noSight = seen ? 0 : noSight + 1;
        if (noSight > 300) {
            backToSearch();
            return;
        }
        for (PoliceEntity m : members) {
            if (m.distanceToSqr(target) < 2.6) {
                arrest();
                return;
            }
        }
    }

    private void backToSearch() {
        target = null;
        state = State.SEARCH;
        timer = 300;
        for (PoliceEntity m : members) {
            m.mode = PoliceEntity.Mode.SEARCH;
            m.getNavigation().stop();
        }
    }

    // ------------------------------------------------------------------ арест

    private void arrest() {
        state = State.ARREST;
        timer = ARREST_TICKS;
        ServerPlayer p = target;

        // на спавн игрока (кровать/якорь возрождения или мировой спавн)
        ServerLevel dest = level;
        TeleportTransition t = p.findRespawnPositionAndUseSpawnBlock(true, TeleportTransition.DO_NOTHING);
        if (t != null) {
            dest = t.newLevel();
            Entity moved = p.teleport(t);
            if (moved instanceof ServerPlayer sp) {
                p = sp;
            }
        }
        target = p;
        freezePos = p.position();

        // надпись обычным шрифтом майнкрафта
        // Подзаголовок вдвое мельче заголовка — длинная фраза целиком помещается на экране
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 270, 20));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(" ")));
        p.connection.send(new ClientboundSetSubtitleTextPacket(
            Component.literal(ARREST_TEXT).withStyle(ChatFormatting.RED)));

        // 15 секунд не может двигаться. Замедление НЕ используем — оно даёт "приближение" экрана.
        // Держим на месте возвратом позиции каждый тик, прыжок отключаем эффектом.
        p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, ARREST_TICKS + 20, 128, false, false, false));

        if (dest == level) {
            int n = members.size();
            for (int i = 0; i < n; i++) {
                PoliceEntity m = members.get(i);
                double ang = Math.PI * 2 * i / n;
                m.setPos(freezePos.x + Math.cos(ang) * 3.0, freezePos.y, freezePos.z + Math.sin(ang) * 3.0);
                m.getNavigation().stop();
                m.followTarget = p;
                m.mode = PoliceEntity.Mode.GUARD;
            }
        } else {
            // другое измерение — полиция не может следовать
            arrestOnly = true;
            for (PoliceEntity m : members) {
                m.discard();
            }
            members.clear();
        }
    }

    private void tickArrest() {
        if (target == null || target.isRemoved() || !target.isAlive()) {
            release();
            return;
        }
        // держим на месте (на случай прыжков/эндер-жемчуга и т. п.)
        target.setSprinting(false);
        if (target.position().distanceToSqr(freezePos) > 0.0004) {
            target.connection.teleport(freezePos.x, freezePos.y, freezePos.z, target.getYRot(), target.getXRot());
        }
        target.setDeltaMovement(Vec3.ZERO);
        for (PoliceEntity m : members) {
            m.followTarget = target;
            m.mode = PoliceEntity.Mode.GUARD;
        }
        if (--timer <= 0) {
            release();
        }
    }

    // ------------------------------------------------------------------ конец

    /** Отряд исчезает. */
    private void dismiss() {
        release();
    }

    private void release() {
        if (target != null && state == State.ARREST) {
            target.removeEffect(MobEffects.JUMP_BOOST);
        }
        for (PoliceEntity m : members) {
            m.discard();
        }
        members.clear();
        done = true;
    }
}
