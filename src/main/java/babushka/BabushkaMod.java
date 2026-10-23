package babushka;

import babushka.entity.PoliceSquads;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public class BabushkaMod implements ModInitializer {
    public static final String MOD_ID = "babushka";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModSounds.register();
        ModBlocks.register();
        ModEntities.register();
        PoliceSquads.init();
    }
}
