package babushka.client;

import babushka.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class BabushkaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.BABUSHKA, BabushkaRenderer::new);
        EntityRendererRegistry.register(ModEntities.POLICE, PoliceRenderer::new);
    }
}
