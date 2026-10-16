package babushka.client;

import babushka.BabushkaMod;
import babushka.entity.PoliceEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class PoliceRenderer extends MobRenderer<PoliceEntity, LivingEntityRenderState, PoliceModel> {

    private static final Identifier TEXTURE = BabushkaMod.id("textures/entity/police.png");

    public PoliceRenderer(EntityRendererProvider.Context context) {
        super(context, new PoliceModel(PoliceModel.createBodyLayer().bakeRoot()), 0.45F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
