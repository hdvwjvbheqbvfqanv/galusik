package babushka;

import babushka.entity.BabushkaEntity;
import babushka.entity.PoliceEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public final class ModEntities {
    public static final ResourceKey<EntityType<?>> BABUSHKA_KEY =
        ResourceKey.create(Registries.ENTITY_TYPE, BabushkaMod.id("babushka"));

    public static final EntityType<BabushkaEntity> BABUSHKA = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        BABUSHKA_KEY,
        EntityType.Builder.of(BabushkaEntity::new, MobCategory.MISC)
            .sized(0.55F, 1.55F)
            .eyeHeight(1.4F)
            .clientTrackingRange(10)
            .build(BABUSHKA_KEY));

    public static final ResourceKey<EntityType<?>> POLICE_KEY =
        ResourceKey.create(Registries.ENTITY_TYPE, BabushkaMod.id("police"));

    /** Полицейские появляются только из сценария бабушки (яйца призыва нет). */
    public static final EntityType<PoliceEntity> POLICE = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        POLICE_KEY,
        EntityType.Builder.of(PoliceEntity::new, MobCategory.MISC)
            .sized(0.6F, 1.85F)
            .eyeHeight(1.65F)
            .clientTrackingRange(10)
            .build(POLICE_KEY));

    public static Item SPAWN_EGG;

    private ModEntities() {}

    public static void register() {
        FabricDefaultAttributeRegistry.register(BABUSHKA, BabushkaEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(POLICE, PoliceEntity.createAttributes());

        ResourceKey<Item> eggKey = ResourceKey.create(Registries.ITEM, BabushkaMod.id("babushka_spawn_egg"));
        SPAWN_EGG = Registry.register(
            BuiltInRegistries.ITEM,
            eggKey,
            new SpawnEggItem(new Item.Properties().setId(eggKey).spawnEgg(BABUSHKA)));
    }
}
