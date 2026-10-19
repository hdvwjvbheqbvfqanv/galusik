package babushka.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Полицейский: стандартный скин "Алекс" (тонкие руки) с внешними слоями (шапка, куртка, рукава, штаны)
 * + автомат АК-47 из кубиков (виден, когда полицейского ударили). ГЕНЕРИРУЕТСЯ tools/make_police.py.
 */
public class PoliceModel extends EntityModel<PoliceRenderState> {
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart gun;

    public PoliceModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.gun = rightArm.getChild("gun");
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

        PartDefinition right_arm = root.addOrReplaceChild("right_arm",
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

        // ---- автомат АК-47 (в правой руке; ствол направлен вдоль руки)
        PartDefinition gun = right_arm.addOrReplaceChild("gun",
            CubeListBuilder.create()
                .texOffs(0, 64).addBox(-0.5F, -2F, -3F, 1F, 2F, 6F)
                .texOffs(14, 64).addBox(-0.5F, -2.5F, -3F, 1F, 1F, 6F, new CubeDeformation(-0.15F))
                .texOffs(28, 64).addBox(-0.5F, -1.5F, -8F, 1F, 2F, 5F, new CubeDeformation(0.15F))
                .texOffs(50, 64).addBox(-0.5F, -2.4F, -8F, 1F, 1F, 5F, new CubeDeformation(-0.1F))
                .texOffs(0, 72).addBox(-0.5F, -1.5F, -13F, 1F, 1F, 5F, new CubeDeformation(-0.3F))
                .texOffs(12, 72).addBox(-0.5F, -2.5F, -12.5F, 1F, 1F, 4F, new CubeDeformation(-0.3F))
                .texOffs(44, 72).addBox(-0.5F, -3.1F, -12.5F, 1F, 1F, 1F, new CubeDeformation(-0.3F))
                .texOffs(48, 72).addBox(-0.5F, -1.6F, -13.9F, 1F, 1F, 1F, new CubeDeformation(-0.2F))
                .texOffs(52, 72).addBox(-0.5F, -3F, -3.5F, 1F, 1F, 1F, new CubeDeformation(-0.3F))
                .texOffs(40, 64).addBox(-0.5F, -1.5F, 3F, 1F, 3F, 4F)
                .texOffs(22, 72).addBox(-0.5F, -1F, 7F, 1F, 3F, 2F, new CubeDeformation(-0.1F)),
            PartPose.offsetAndRotation(-0.5F, 8.5F, 0F, 1.5708F, 0F, 0F));

        PartDefinition mag1 = gun.addOrReplaceChild("mag1",
            CubeListBuilder.create()
                .texOffs(28, 72).addBox(-0.5F, 0F, -1F, 1F, 3F, 2F),
            PartPose.offsetAndRotation(0F, 0F, -1.5F, -0.18F, 0F, 0F));

        mag1.addOrReplaceChild("mag2",
            CubeListBuilder.create()
                .texOffs(34, 72).addBox(-0.5F, 0F, -1F, 1F, 3F, 2F),
            PartPose.offsetAndRotation(0F, 3F, -0.2F, -0.3F, 0F, 0F));

        gun.addOrReplaceChild("grip",
            CubeListBuilder.create()
                .texOffs(40, 72).addBox(-0.5F, 0F, -0.5F, 1F, 3F, 1F, new CubeDeformation(0.1F)),
            PartPose.offsetAndRotation(0F, 0F, 2F, 0.35F, 0F, 0F));

        return LayerDefinition.create(mesh, 64, 96);
    }

    @Override
    public void setupAnim(PoliceRenderState state) {
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
        rightArm.yRot = 0F;
        leftArm.yRot = 0F;
        rightArm.zRot = 0.04F;
        leftArm.zRot = -0.04F;

        gun.visible = state.armed;
        if (state.armed) {
            // целится: обе руки вперёд, левая поддерживает цевьё
            rightArm.xRot = -1.4F;
            rightArm.yRot = -0.3F;
            rightArm.zRot = 0F;
            leftArm.xRot = -1.3F;
            leftArm.yRot = 0.7F;
            leftArm.zRot = 0F;
        }
    }
}
