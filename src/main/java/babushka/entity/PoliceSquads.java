package babushka.entity;

import babushka.ModEntities;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

/** Список активных отрядов полиции и их создание. */
public final class PoliceSquads {
    public static final int SQUAD_SIZE = 10;
    private static final List<PoliceSquad> SQUADS = new ArrayList<>();

    private PoliceSquads() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SQUADS.clear());
    }

    public static boolean exists(UUID squadId) {
        for (PoliceSquad s : SQUADS) {
            if (s.id.equals(squadId) && !s.done) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasSquadFor(UUID babushkaId) {
        for (PoliceSquad s : SQUADS) {
            if (s.babushkaId.equals(babushkaId) && !s.done) {
                return true;
            }
        }
        return false;
    }

    private static void tick() {
        Iterator<PoliceSquad> it = SQUADS.iterator();
        while (it.hasNext()) {
            PoliceSquad s = it.next();
            s.tick();
            if (s.done) {
                it.remove();
            }
        }
    }

    /** Вызывает отряд из 10 полицейских на расстоянии 20–28 блоков от бабушки. */
    public static void spawn(ServerLevel level, BabushkaEntity babushka) {
        if (hasSquadFor(babushka.getUUID())) {
            return;
        }
        PoliceSquad squad = new PoliceSquad(level, babushka.getUUID(), babushka.blockPosition());
        RandomSource r = babushka.getRandom();
        int attempts = 0;
        while (squad.members.size() < SQUAD_SIZE && attempts++ < 120) {
            double ang = r.nextDouble() * Math.PI * 2;
            double dist = (attempts < 60 ? 20 : 8) + r.nextDouble() * 8;
            int x = babushka.getBlockX() + (int) (Math.cos(ang) * dist);
            int z = babushka.getBlockZ() + (int) (Math.sin(ang) * dist);
            BlockPos probe = new BlockPos(x, babushka.getBlockY(), z);
            if (!level.hasChunkAt(probe)) {
                continue;
            }
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe);
            if (!level.getFluidState(top).isEmpty() || !level.getFluidState(top.below()).isEmpty()
                || !level.getBlockState(top.below()).isFaceSturdy(level, top.below(), Direction.UP)) {
                continue;
            }
            PoliceEntity cop = new PoliceEntity(ModEntities.POLICE, level);
            cop.setPos(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
            cop.squadId = squad.id;
            cop.center = squad.center;
            cop.mode = PoliceEntity.Mode.GATHER;
            level.addFreshEntity(cop);
            squad.members.add(cop);
        }
        if (!squad.members.isEmpty()) {
            SQUADS.add(squad);
        }
    }
}
