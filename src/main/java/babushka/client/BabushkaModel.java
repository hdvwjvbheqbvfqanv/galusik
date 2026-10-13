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

/**
 * Модель бабушки (ГЕНЕРИРУЕТСЯ tools/make_model.py вместе с текстурой — не правьте руками).
 * Ниже игрока (~1.55 блока), голова с носом/ушами/причёской, бантик слева,
 * платье с расширяющейся юбкой, руки с локтями, синие тапочки.
 */
public class BabushkaModel extends EntityModel<BabushkaRenderState> {
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightForearm;
    private final ModelPart leftForearm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public BabushkaModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightForearm = rightArm.getChild("right_forearm");
        this.leftForearm = leftArm.getChild("left_forearm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
            CubeListBuilder.create()
            .texOffs(0, 0).addBox(-3.5F, -7F, -3.5F, 7F, 7F, 7F)
            .texOffs(0, 14).addBox(-3.5F, -7F, -3.5F, 7F, 2F, 7F, new CubeDeformation(0.3F))
            .texOffs(0, 31).addBox(-3.5F, -7F, 1.5F, 7F, 5F, 2F, new CubeDeformation(0.3F))
            .texOffs(14, 45).addBox(-0.5F, -3.5F, -4.5F, 1F, 2F, 1F)
            .texOffs(18, 45).addBox(-4.5F, -4.5F, -0.5F, 1F, 2F, 1F)
            .texOffs(22, 45).addBox(3.5F, -4.5F, -0.5F, 1F, 2F, 1F)
            .texOffs(26, 45).addBox(3.7F, -6.7F, -0.5F, 1F, 1F, 1F)
            .texOffs(56, 38).addBox(3.8F, -7.4F, -2.6F, 1F, 2F, 2F)
            .texOffs(0, 45).addBox(3.8F, -7.4F, 0.6F, 1F, 2F, 2F),
            PartPose.offset(0F, 6F, 0F));

        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create()
            .texOffs(6, 45).addBox(-1F, -1F, -1F, 2F, 1F, 2F)
            .texOffs(28, 14).addBox(-3F, 0F, -2F, 6F, 5F, 4F)
            .texOffs(0, 23).addBox(-3.5F, 4F, -2.5F, 7F, 3F, 5F)
            .texOffs(24, 23).addBox(-4.5F, 7F, -3F, 9F, 1F, 6F, new CubeDeformation(0.25F)),
            PartPose.offset(0F, 7F, 0F));

        PartDefinition right_arm = root.addOrReplaceChild("right_arm",
            CubeListBuilder.create()
            .texOffs(50, 31).addBox(-1F, -1F, -1F, 2F, 5F, 2F)
            .texOffs(24, 38).addBox(-1F, -1F, -1F, 2F, 2F, 2F, new CubeDeformation(0.25F)),
            PartPose.offset(-4F, 8F, 0F));

        PartDefinition left_arm = root.addOrReplaceChild("left_arm",
            CubeListBuilder.create()
            .texOffs(0, 38).addBox(-1F, -1F, -1F, 2F, 5F, 2F)
            .texOffs(40, 38).addBox(-1F, -1F, -1F, 2F, 2F, 2F, new CubeDeformation(0.25F)),
            PartPose.offset(4F, 8F, 0F));

        PartDefinition right_leg = root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
            .texOffs(28, 0).addBox(-1.5F, 0F, -1.5F, 3F, 9F, 3F, new CubeDeformation(-0.1F))
            .texOffs(18, 31).addBox(-1.5F, 9F, -3F, 3F, 2F, 5F, new CubeDeformation(0.2F)),
            PartPose.offset(-1.9F, 13F, 0F));

        PartDefinition left_leg = root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
            .texOffs(40, 0).addBox(-1.5F, 0F, -1.5F, 3F, 9F, 3F, new CubeDeformation(-0.1F))
            .texOffs(34, 31).addBox(-1.5F, 9F, -3F, 3F, 2F, 5F, new CubeDeformation(0.2F)),
            PartPose.offset(1.9F, 13F, 0F));

        right_arm.addOrReplaceChild("right_forearm",
            CubeListBuilder.create()
            .texOffs(8, 38).addBox(-1F, 0F, -1F, 2F, 3F, 2F)
            .texOffs(32, 38).addBox(-1F, 3F, -1F, 2F, 2F, 2F, new CubeDeformation(-0.2F)),
            PartPose.offset(0F, 5F, 0F));

        left_arm.addOrReplaceChild("left_forearm",
            CubeListBuilder.create()
            .texOffs(16, 38).addBox(-1F, 0F, -1F, 2F, 3F, 2F)
            .texOffs(48, 38).addBox(-1F, 3F, -1F, 2F, 2F, 2F, new CubeDeformation(-0.2F)),
            PartPose.offset(0F, 5F, 0F));

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
