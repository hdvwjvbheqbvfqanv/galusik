package babushka.client;

import babushka.BabushkaMod;
import babushka.entity.BabushkaEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Модель — ванильная "слим" гуманоидная (худые руки, как у Алекс),
 * вся "бабушкость" живёт в текстуре textures/entity/babushka.png.
 */
public class BabushkaRenderer
        extends HumanoidMobRenderer<BabushkaEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {

    private static final Identifier TEXTURE = BabushkaMod.id("textures/entity/babushka.png");

    public BabushkaRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM)), 0.5F);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }
}
