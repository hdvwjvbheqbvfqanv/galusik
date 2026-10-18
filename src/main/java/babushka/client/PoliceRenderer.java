package babushka.client;

import babushka.BabushkaMod;
import babushka.entity.PoliceEntity;
import com.mojang.blaze3d.vertex.PoseStack;
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

    /** Как у игрока: модель чуть уменьшена, чтобы рост совпадал с хитбоксом 1.8. */
    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
