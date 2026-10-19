package babushka.client;

import babushka.BabushkaMod;
import babushka.entity.PoliceCarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public class CarRenderer extends MobRenderer<PoliceCarEntity, LivingEntityRenderState, CarModel> {

    private static final Identifier TEXTURE = BabushkaMod.id("textures/entity/police_car.png");
    /** Оригинальная модель очень большая (6 блоков в длину) — уменьшаем до ~3 блоков. */
    private static final float MODEL_SCALE = 0.5F;

    public CarRenderer(EntityRendererProvider.Context context) {
        super(context, new CarModel(CarModel.createBodyLayer().bakeRoot()), 1.2F);
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
