package babushka;

import babushka.block.ElectricalPanelBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    public static Block ELECTRICAL_PANEL;
    public static Item ELECTRICAL_PANEL_ITEM;

    private ModBlocks() {}

    public static void register() {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, BabushkaMod.id("electrical_panel"));
        ELECTRICAL_PANEL = Registry.register(BuiltInRegistries.BLOCK, blockKey,
            new ElectricalPanelBlock(BlockBehaviour.Properties.of()
                .setId(blockKey)
                .strength(2.0F, 6.0F)
                .sound(SoundType.METAL)
                .noOcclusion()));

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, BabushkaMod.id("electrical_panel"));
        ELECTRICAL_PANEL_ITEM = Registry.register(BuiltInRegistries.ITEM, itemKey,
            new BlockItem(ELECTRICAL_PANEL, new Item.Properties().setId(itemKey)));
    }
}
