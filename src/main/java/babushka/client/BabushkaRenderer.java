package babushka.client;

import babushka.BabushkaMod;
import babushka.entity.BabushkaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class BabushkaRenderer extends MobRenderer<BabushkaEntity, BabushkaRenderState, BabushkaModel> {

    private static final Identifier TEXTURE = BabushkaMod.id("textures/entity/babushka.png");

    public BabushkaRenderer(EntityRendererProvider.Context context) {
        // модель запекается напрямую, без регистрации слоя
        super(context, new BabushkaModel(BabushkaModel.createBodyLayer().bakeRoot()), 0.4F);
    }

    @Override
    public BabushkaRenderState createRenderState() {
        return new BabushkaRenderState();
    }

    @Override
    public void extractRenderState(BabushkaEntity entity, BabushkaRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.aggressive = entity.isAggressive();
    }

    @Override
    public Identifier getTextureLocation(BabushkaRenderState state) {
        return TEXTURE;
    }
}
