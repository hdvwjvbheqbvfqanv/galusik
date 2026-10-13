package babushka.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class BabushkaRenderState extends LivingEntityRenderState {
    /** Бабушка бежит за игроком — поднимает руки вперёд. */
    public boolean aggressive;
}
