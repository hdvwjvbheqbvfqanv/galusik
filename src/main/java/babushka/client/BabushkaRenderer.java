package babushka.client;

import babushka.BabushkaMod;
import babushka.entity.BabushkaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
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

    /** Маленькая бабушка вдвое меньше (если игра сама ещё не уменьшила детёныша). */
    @Override
    protected void scale(BabushkaRenderState state, PoseStack poseStack) {
        if (state.isBaby && state.scale > 0.9F) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }

    @Override
    public Identifier getTextureLocation(BabushkaRenderState state) {
        return TEXTURE;
    }
}
