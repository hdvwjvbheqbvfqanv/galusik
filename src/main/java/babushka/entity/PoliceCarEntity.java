package babushka.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/**
 * ВАЗ-2109 ППС: просто сущность-моделька (никакой логики автомобиля). Ездит вместе с отрядом,
 * 1000 сердечек здоровья, чуть быстрее полицейских, из неё идёт сирена.
 */
public class PoliceCarEntity extends PoliceEntity {
    /** 0.32 ≈ 6.4 блока/с — чуть быстрее полицейских (0.28 ≈ 5.6). */
    public static final double CAR_SPEED = 0.32;

    public PoliceCarEntity(EntityType<? extends PoliceCarEntity> type, Level level) {
        super(type, level);
        setInvulnerable(false);
    }

    @Override
    boolean isCar() {
        return true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 2000.0)           // 1000 сердечек
            .add(Attributes.MOVEMENT_SPEED, CAR_SPEED)
            .add(Attributes.FOLLOW_RANGE, 64.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }
}
