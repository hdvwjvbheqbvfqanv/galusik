package babushka.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.PartPose;
import net.minecraft.util.Mth;

/** BabushkaModel (ГЕНЕРИРУЕТСЯ tools/make_models.py вместе с текстурой — не правьте руками). */
public class BabushkaModel extends EntityModel<BabushkaRenderState> {
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

    public BabushkaModel(ModelPart root) {
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
            .texOffs(52, 36).addBox(-3.4F, -4.4F, -0.5F, 1F, 2F, 1F)
            .texOffs(56, 36).addBox(2.4F, -4.4F, -0.5F, 1F, 2F, 1F)
            .texOffs(60, 36).addBox(2.7F, -6.7F, -0.5F, 1F, 1F, 1F)
            .texOffs(40, 36).addBox(2.8F, -7.6F, -2.5F, 1F, 2F, 2F)
            .texOffs(46, 36).addBox(2.8F, -7.6F, 0.5F, 1F, 2F, 2F),
            PartPose.offset(0F, 5.5F, 0F));

        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create()
            .texOffs(40, 29).addBox(-1.5F, -1.5F, -1.5F, 3F, 2F, 3F)
            .texOffs(24, 0).addBox(-3.5F, 0F, -2F, 7F, 6F, 4F)
            .texOffs(30, 14).addBox(-4F, 5F, -2.5F, 8F, 3F, 5F)
            .texOffs(0, 14).addBox(-4.5F, 8F, -3F, 9F, 2F, 6F),
            PartPose.offset(0F, 6F, 0F));

        PartDefinition right_arm = root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
            .texOffs(24, 29).addBox(-1F, -1F, -1F, 2F, 4F, 2F)
            .texOffs(8, 36).addBox(-1F, -1F, -1F, 2F, 2F, 2F, new CubeDeformation(0.25F)),
            PartPose.offset(-4.6F, 7.2F, 0F));

        PartDefinition right_forearm = right_arm.addOrReplaceChild("right_forearm",
            CubeListBuilder.create()
            .texOffs(52, 29).addBox(-1F, 0F, -1F, 2F, 3F, 2F)
            .texOffs(16, 36).addBox(-1F, 3F, -1F, 2F, 2F, 2F),
            PartPose.offset(0F, 3F, 0F));

        PartDefinition left_arm = root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
            .texOffs(32, 29).addBox(-1F, -1F, -1F, 2F, 4F, 2F)
            .texOffs(24, 36).addBox(-1F, -1F, -1F, 2F, 2F, 2F, new CubeDeformation(0.25F)),
            PartPose.offset(4.6F, 7.2F, 0F));

        PartDefinition left_forearm = left_arm.addOrReplaceChild("left_forearm",
            CubeListBuilder.create()
            .texOffs(0, 36).addBox(-1F, 0F, -1F, 2F, 3F, 2F)
            .texOffs(32, 36).addBox(-1F, 3F, -1F, 2F, 2F, 2F),
            PartPose.offset(0F, 3F, 0F));

        PartDefinition right_leg = root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
            .texOffs(32, 22).addBox(-1.5F, 0F, -1.5F, 3F, 4F, 3F),
            PartPose.offset(-1.8F, 14F, 0F));

        PartDefinition right_shin = right_leg.addOrReplaceChild("right_shin",
            CubeListBuilder.create()
            .texOffs(44, 22).addBox(-1.5F, 0F, -1.5F, 3F, 4F, 3F)
            .texOffs(0, 22).addBox(-1.5F, 4F, -3F, 3F, 2F, 5F, new CubeDeformation(0.2F)),
            PartPose.offset(0F, 4F, 0F));

        PartDefinition left_leg = root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
            .texOffs(0, 29).addBox(-1.5F, 0F, -1.5F, 3F, 4F, 3F),
            PartPose.offset(1.8F, 14F, 0F));

        PartDefinition left_shin = left_leg.addOrReplaceChild("left_shin",
            CubeListBuilder.create()
            .texOffs(12, 29).addBox(-1.5F, 0F, -1.5F, 3F, 4F, 3F)
            .texOffs(16, 22).addBox(-1.5F, 4F, -3F, 3F, 2F, 5F, new CubeDeformation(0.2F)),
            PartPose.offset(0F, 4F, 0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(BabushkaRenderState state) {
        super.setupAnim(state);
        float deg = (float) Math.PI / 180F;
        head.yRot = state.yRot * deg;
        head.xRot = state.xRot * deg;

        float swing = Mth.cos(state.walkAnimationPos * 0.6662F) * 1.2F * state.walkAnimationSpeed;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        rightShin.xRot = Math.max(0F, swing) * 0.9F + 0.03F;   // колени сгибаются при ходьбе
        leftShin.xRot = Math.max(0F, -swing) * 0.9F + 0.03F;

        // лёгкое дыхание и согнутые локти
        float breathe = Mth.sin(state.ageInTicks * 0.09F) * 0.03F;
        rightArm.zRot = 0.05F + breathe;
        leftArm.zRot = -0.05F - breathe;
        rightForearm.xRot = -0.25F;
        leftForearm.xRot = -0.25F;
        rightArm.xRot = -swing * 0.8F;
        leftArm.xRot = swing * 0.8F;

        if (state.aggressive) {
            // бежит с вытянутыми вперёд руками
            rightArm.xRot = -1.45F + Mth.sin(state.ageInTicks * 0.8F) * 0.08F;
            leftArm.xRot = -1.45F - Mth.sin(state.ageInTicks * 0.8F) * 0.08F;
            rightForearm.xRot = -0.1F;
            leftForearm.xRot = -0.1F;
            rightArm.zRot = 0.1F;
            leftArm.zRot = -0.1F;
        }
    }
}
