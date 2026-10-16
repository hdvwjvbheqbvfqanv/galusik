package babushka.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.util.Mth;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** PoliceModel (ГЕНЕРИРУЕТСЯ tools/make_models.py вместе с текстурой — не правьте руками). */
public class PoliceModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart rightForearm;
    private final ModelPart leftArm;
    private final ModelPart leftForearm;
    private final ModelPart rightLeg;
    private final ModelPart rightShin;
    private final ModelPart leftLeg;
    private final ModelPart leftShin;

    public PoliceModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.rightForearm = rightArm.getChild("right_forearm");
        this.leftArm = root.getChild("left_arm");
        this.leftForearm = leftArm.getChild("left_forearm");
        this.rightLeg = root.getChild("right_leg");
        this.rightShin = rightLeg.getChild("right_shin");
        this.leftLeg = root.getChild("left_leg");
        this.leftShin = leftLeg.getChild("left_shin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
            .texOffs(0, 0).addBox(-3F, -8F, -3F, 6F, 8F, 6F, new CubeDeformation(-0.5F))
            .texOffs(0, 31).addBox(-3F, -8.2F, -3F, 6F, 1F, 6F, new CubeDeformation(0.45F))
            .texOffs(0, 43).addBox(-2.5F, -7.2F, -5.6F, 5F, 1F, 3F)
            .texOffs(16, 43).addBox(-3.4F, -4.4F, -0.5F, 1F, 2F, 1F)
            .texOffs(20, 43).addBox(2.4F, -4.4F, -0.5F, 1F, 2F, 1F),
            PartPose.offset(0F, 2.5F, 0F));

        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create()
            .texOffs(14, 38).addBox(-1.5F, -1.5F, -1.5F, 3F, 2F, 3F)
            .texOffs(24, 0).addBox(-4F, 0F, -2F, 8F, 10F, 4F)
            .texOffs(48, 31).addBox(-4.2F, -0.5F, -2F, 3F, 1F, 4F, new CubeDeformation(0.15F))
            .texOffs(0, 38).addBox(1.2F, -0.5F, -2F, 3F, 1F, 4F, new CubeDeformation(0.15F)),
            PartPose.offset(0F, 3.3F, 0F));

        PartDefinition right_arm = root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
            .texOffs(32, 23).addBox(-1.5F, -1F, -1.5F, 3F, 5F, 3F),
            PartPose.offset(-5.5F, 4.4F, 0F));

        PartDefinition right_forearm = right_arm.addOrReplaceChild("right_forearm",
            CubeListBuilder.create()
            .texOffs(24, 31).addBox(-1.5F, 0F, -1.5F, 3F, 4F, 3F)
            .texOffs(26, 38).addBox(-1.5F, 4F, -1.5F, 3F, 2F, 3F),
            PartPose.offset(0F, 4F, 0F));

        PartDefinition left_arm = root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
            .texOffs(44, 23).addBox(-1.5F, -1F, -1.5F, 3F, 5F, 3F),
            PartPose.offset(5.5F, 4.4F, 0F));

        PartDefinition left_forearm = left_arm.addOrReplaceChild("left_forearm",
            CubeListBuilder.create()
            .texOffs(36, 31).addBox(-1.5F, 0F, -1.5F, 3F, 4F, 3F)
            .texOffs(38, 38).addBox(-1.5F, 4F, -1.5F, 3F, 2F, 3F),
            PartPose.offset(0F, 4F, 0F));

        PartDefinition right_leg = root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
            .texOffs(48, 0).addBox(-2F, 0F, -2F, 4F, 5F, 4F),
            PartPose.offset(-2F, 13F, 0F));

        PartDefinition right_shin = right_leg.addOrReplaceChild("right_shin",
            CubeListBuilder.create()
            .texOffs(0, 23).addBox(-2F, 0F, -2F, 4F, 4F, 4F)
            .texOffs(16, 14).addBox(-2F, 3F, -2.8F, 4F, 3F, 5F, new CubeDeformation(0.15F)),
            PartPose.offset(0F, 5F, 0F));

        PartDefinition left_leg = root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
            .texOffs(0, 14).addBox(-2F, 0F, -2F, 4F, 5F, 4F),
            PartPose.offset(2F, 13F, 0F));

        PartDefinition left_shin = left_leg.addOrReplaceChild("left_shin",
            CubeListBuilder.create()
            .texOffs(16, 23).addBox(-2F, 0F, -2F, 4F, 4F, 4F)
            .texOffs(34, 14).addBox(-2F, 3F, -2.8F, 4F, 3F, 5F, new CubeDeformation(0.15F)),
            PartPose.offset(0F, 5F, 0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float deg = (float) Math.PI / 180F;
        head.yRot = state.yRot * deg;
        head.xRot = state.xRot * deg;

        float speed = Math.min(1.0F, state.walkAnimationSpeed);
        float swing = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.5F * speed;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        rightShin.xRot = Math.max(0F, swing) * 1.0F;
        leftShin.xRot = Math.max(0F, -swing) * 1.0F;

        rightArm.xRot = -swing * 0.9F;
        leftArm.xRot = swing * 0.9F;
        rightArm.zRot = 0.04F;
        leftArm.zRot = -0.04F;
        // на бегу локти согнуты сильнее
        float bend = -0.2F - speed * 0.9F;
        rightForearm.xRot = bend;
        leftForearm.xRot = bend;
    }
}
