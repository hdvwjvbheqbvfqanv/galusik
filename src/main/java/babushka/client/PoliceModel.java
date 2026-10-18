package babushka.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * Модель полицейского — стандартный скин "Алекс" (тонкие руки) с внешними слоями
 * (шапка, куртка, рукава, штаны). Текстура: textures/entity/police.png (ваш скин 64x64).
 */
public class PoliceModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public PoliceModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation outer = new CubeDeformation(0.25F);

        root.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F)
                .texOffs(32, 0).addBox(-4F, -8F, -4F, 8F, 8F, 8F, new CubeDeformation(0.5F)),
            PartPose.offset(0F, 0F, 0F));

        root.addOrReplaceChild("body",
            CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4F, 0F, -2F, 8F, 12F, 4F)
                .texOffs(16, 32).addBox(-4F, 0F, -2F, 8F, 12F, 4F, outer),
            PartPose.offset(0F, 0F, 0F));

        root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
                .texOffs(40, 16).addBox(-2F, -2F, -2F, 3F, 12F, 4F)
                .texOffs(40, 32).addBox(-2F, -2F, -2F, 3F, 12F, 4F, outer),
            PartPose.offset(-5F, 2.5F, 0F));

        root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
                .texOffs(32, 48).addBox(-1F, -2F, -2F, 3F, 12F, 4F)
                .texOffs(48, 48).addBox(-1F, -2F, -2F, 3F, 12F, 4F, outer),
            PartPose.offset(5F, 2.5F, 0F));

        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2F, 0F, -2F, 4F, 12F, 4F)
                .texOffs(0, 32).addBox(-2F, 0F, -2F, 4F, 12F, 4F, outer),
            PartPose.offset(-1.9F, 12F, 0F));

        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
                .texOffs(16, 48).addBox(-2F, 0F, -2F, 4F, 12F, 4F)
                .texOffs(0, 48).addBox(-2F, 0F, -2F, 4F, 12F, 4F, outer),
            PartPose.offset(1.9F, 12F, 0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float deg = (float) Math.PI / 180F;
        head.yRot = state.yRot * deg;
        head.xRot = state.xRot * deg;

        float speed = Math.min(1.0F, state.walkAnimationSpeed);
        float swing = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.4F * speed;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        rightArm.xRot = -swing * 0.9F;
        leftArm.xRot = swing * 0.9F;
        rightArm.zRot = 0.04F;
        leftArm.zRot = -0.04F;
    }
}
