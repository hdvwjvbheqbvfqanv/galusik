package babushka;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

/**
 * Свои типы урона (описаны в data/babushka/damage_type/*.json, тексты смерти — в lang):
 * "был убит за нападение на сотрудника полиции" и "был убит Галей за плохое поведение".
 * Оба добавлены в тег bypasses_armor — броня от них не ломается.
 */
public final class ModDamage {
    public static final ResourceKey<DamageType> POLICE_SHOT =
        ResourceKey.create(Registries.DAMAGE_TYPE, BabushkaMod.id("police_shot"));
    public static final ResourceKey<DamageType> GALYA =
        ResourceKey.create(Registries.DAMAGE_TYPE, BabushkaMod.id("galya"));

    private ModDamage() {}

    public static DamageSource of(ServerLevel level, ResourceKey<DamageType> key, Entity attacker) {
        Holder<DamageType> type = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(key);
        return new DamageSource(type, attacker);
    }
}
