package babushka.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/**
 * ВАЗ-2109 ППС. ГЕНЕРИРУЕТСЯ tools/make_car.py из модели мода [B'A'D] Russian Police Pack
 * (только внешний вид, никакой логики машины).
 */
public class CarModel extends EntityModel<LivingEntityRenderState> {
    public CarModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bb_main = root.addOrReplaceChild("bb_main",
            CubeListBuilder.create()
            .texOffs(0, 51).addBox(-24F, -2F, -18F, 46F, 2F, 45F)
            .texOffs(127, 152).addBox(-24F, -17F, -43F, 46F, 15F, 1F)
            .texOffs(137, 68).addBox(-24F, -17F, -18F, 46F, 15F, 2F)
            .texOffs(137, 51).addBox(-24F, -17F, 49F, 46F, 15F, 2F)
            .texOffs(134, 28).addBox(-24F, -17F, 25F, 46F, 15F, 2F)
            .texOffs(166, 182).addBox(19F, -17F, 27F, 3F, 15F, 3F)
            .texOffs(0, 51).addBox(19F, -17F, 47F, 3F, 15F, 2F)
            .texOffs(105, 127).addBox(19F, -17F, 30F, 3F, 6F, 17F)
            .texOffs(38, 186).addBox(-24F, -17F, 47F, 2F, 15F, 2F)
            .texOffs(0, 225).addBox(-24F, -17F, 30F, 2F, 6F, 17F)
            .texOffs(60, 186).addBox(-24F, -17F, 27F, 2F, 15F, 3F)
            .texOffs(50, 186).addBox(-24F, -17F, -21F, 2F, 15F, 3F)
            .texOffs(150, 222).addBox(-24F, -17F, -39F, 2F, 6F, 18F)
            .texOffs(28, 186).addBox(-24F, -17F, -42F, 2F, 15F, 3F)
            .texOffs(178, 182).addBox(20F, -17F, -42F, 2F, 15F, 3F)
            .texOffs(28, 186).addBox(20F, -17F, -39F, 2F, 6F, 18F)
            .texOffs(12, 0).addBox(20F, -17F, -21F, 2F, 15F, 3F)
            .texOffs(128, 179).addBox(-4F, -4F, -18F, 6F, 2F, 26F)
            .texOffs(0, 37).addBox(-4F, -6F, -3F, 6F, 2F, 11F)
            .texOffs(80, 168).addBox(-22F, -17F, -16F, 42F, 8F, 3F)
            .texOffs(134, 45).addBox(-22F, -17F, -13F, 42F, 3F, 2F)
            .texOffs(21, 61).addBox(-22F, -14F, -13F, 6F, 3F, 2F)
            .texOffs(224, 210).addBox(-22F, -11F, -13F, 25F, 2F, 2F)
            .texOffs(0, 91).addBox(-2F, -14F, -13F, 5F, 3F, 2F)
            .texOffs(26, 0).addBox(18F, -14F, -13F, 2F, 5F, 2F)
            .texOffs(224, 193).addBox(6F, -6F, -5F, 13F, 4F, 13F)
            .texOffs(226, 108).addBox(6F, -17F, 5F, 13F, 11F, 3F)
            .texOffs(222, 32).addBox(-21F, -6F, -5F, 13F, 4F, 13F)
            .texOffs(108, 207).addBox(-21F, -17F, 5F, 13F, 11F, 3F)
            .texOffs(134, 0).addBox(-21F, -6F, 12F, 40F, 4F, 13F)
            .texOffs(170, 168).addBox(-21F, -17F, 22F, 40F, 11F, 3F)
            .texOffs(124, 126).addBox(-21F, -2F, 27F, 40F, 2F, 24F)
            .texOffs(82, 203).addBox(17F, -17F, 27F, 2F, 15F, 22F)
            .texOffs(0, 0).addBox(-21F, -17F, 27F, 2F, 15F, 22F)
            .texOffs(196, 198).addBox(-21F, -17F, -42F, 2F, 15F, 24F)
            .texOffs(52, 186).addBox(17F, -17F, -42F, 2F, 15F, 24F)
            .texOffs(0, 98).addBox(-24F, -18F, -44F, 46F, 1F, 28F)
            .texOffs(82, 240).addBox(-22F, -36F, 25F, 3F, 34F, 3F)
            .texOffs(127, 239).addBox(17F, -36F, 25F, 3F, 34F, 3F)
            .texOffs(0, 0).addBox(-22F, -36F, -12F, 42F, 1F, 50F)
            .texOffs(209, 237).addBox(17F, -36F, 5F, 3F, 34F, 3F)
            .texOffs(134, 17).addBox(-22F, -37F, 0F, 42F, 1F, 10F)
            .texOffs(196, 182).addBox(-16F, -42F, 2F, 30F, 5F, 6F)
            .texOffs(139, 239).addBox(-22F, -35F, 28F, 1F, 18F, 9F)
            .texOffs(134, 0).addBox(-22F, -25F, 37F, 1F, 8F, 5F)
            .texOffs(105, 127).addBox(19F, -25F, 37F, 1F, 8F, 5F)
            .texOffs(0, 98).addBox(19F, -35F, 28F, 1F, 18F, 9F)
            .texOffs(130, 207).addBox(-23F, -17F, 8F, 2F, 15F, 17F)
            .texOffs(0, 186).addBox(-23F, -17F, -16F, 2F, 15F, 24F)
            .texOffs(70, 225).addBox(-22F, -36F, 5F, 3F, 34F, 3F)
            .texOffs(0, 51).addBox(19F, -17F, 8F, 2F, 15F, 17F)
            .texOffs(168, 183).addBox(19F, -17F, -16F, 2F, 15F, 24F)
            .texOffs(120, 98).addBox(-21F, -2F, -44F, 40F, 2F, 26F)
            .texOffs(221, 152).addBox(-16F, -17F, -44F, 30F, 15F, 1F)
            .texOffs(6, 186).addBox(-24F, -11F, -44F, 8F, 9F, 1F)
            .texOffs(128, 127).addBox(14F, -11F, -44F, 8F, 9F, 1F)
            .texOffs(6, 196).addBox(14F, -17F, -44F, 8F, 6F, 1F)
            .texOffs(0, 153).addBox(-24F, -7F, -46F, 46F, 8F, 2F)
            .texOffs(137, 85).addBox(-24F, -7F, 51F, 46F, 8F, 2F)
            .texOffs(177, 233).addBox(-25F, -9F, -36F, 3F, 13F, 13F)
            .texOffs(96, 153).addBox(-25F, -10F, -34F, 3F, 1F, 9F)
            .texOffs(120, 108).addBox(-25F, 4F, -34F, 3F, 1F, 9F)
            .texOffs(151, 207).addBox(-25F, -7F, -37F, 3F, 9F, 1F)
            .texOffs(204, 193).addBox(-25F, -7F, -23F, 3F, 9F, 1F)
            .texOffs(120, 98).addBox(-25F, -10F, 34F, 3F, 1F, 9F)
            .texOffs(96, 200).addBox(-25F, -7F, 45F, 3F, 9F, 1F)
            .texOffs(19, 83).addBox(-25F, 4F, 34F, 3F, 1F, 9F)
            .texOffs(88, 200).addBox(-25F, -7F, 31F, 3F, 9F, 1F)
            .texOffs(233, 49).addBox(-25F, -9F, 32F, 3F, 13F, 13F)
            .texOffs(21, 51).addBox(20F, -10F, 34F, 3F, 1F, 9F)
            .texOffs(80, 200).addBox(20F, -7F, 45F, 3F, 9F, 1F)
            .texOffs(23, 37).addBox(20F, 4F, 34F, 3F, 1F, 9F)
            .texOffs(196, 193).addBox(20F, -7F, 31F, 3F, 9F, 1F)
            .texOffs(226, 82).addBox(20F, -9F, 32F, 3F, 13F, 13F)
            .texOffs(26, 10).addBox(20F, 4F, -34F, 3F, 1F, 9F)
            .texOffs(134, 17).addBox(20F, -7F, -37F, 3F, 9F, 1F)
            .texOffs(0, 37).addBox(20F, -7F, -23F, 3F, 9F, 1F)
            .texOffs(26, 0).addBox(20F, -10F, -34F, 3F, 1F, 9F)
            .texOffs(38, 225).addBox(20F, -9F, -36F, 3F, 13F, 13F)
            .texOffs(128, 137).addBox(-24F, -17F, -44F, 8F, 6F, 1F)
            .texOffs(0, 129).addBox(-22F, -18F, 25F, 41F, 1F, 23F),
            PartPose.offset(1F, 19F, -3.5F));

        bb_main.addOrReplaceChild("cube_r1",
            CubeListBuilder.create()
            .texOffs(16, 127).addBox(-22F, -38F, 7F, 2F, 24F, 1F)
            .texOffs(100, 240).addBox(-22F, -39F, 6F, 2F, 25F, 1F)
            .texOffs(240, 0).addBox(-22F, -40F, 4F, 2F, 26F, 1F)
            .texOffs(106, 240).addBox(-22F, -39F, 5F, 2F, 25F, 1F)
            .texOffs(38, 61).addBox(-22F, -41F, 3F, 2F, 27F, 1F)
            .texOffs(20, 98).addBox(-62F, -41F, 3F, 2F, 27F, 1F)
            .texOffs(94, 240).addBox(-62F, -40F, 4F, 2F, 26F, 1F)
            .texOffs(112, 240).addBox(-62F, -39F, 5F, 2F, 25F, 1F)
            .texOffs(118, 240).addBox(-62F, -39F, 6F, 2F, 25F, 1F)
            .texOffs(245, 237).addBox(-62F, -38F, 7F, 2F, 24F, 1F)
            .texOffs(0, 186).addBox(-62F, -37F, 8F, 2F, 23F, 1F)
            .texOffs(0, 163).addBox(-61F, -37F, 9F, 40F, 23F, 0F)
            .texOffs(0, 127).addBox(-62F, -37F, 8F, 2F, 23F, 2F)
            .texOffs(8, 127).addBox(-22F, -37F, 8F, 2F, 23F, 2F),
            PartPose.offsetAndRotation(40F, 0F, 50F, 0.6109F, 0F, 0F));

        bb_main.addOrReplaceChild("cube_r2",
            CubeListBuilder.create()
            .texOffs(0, 83).addBox(7F, -18F, -3F, 7F, 1F, 7F)
            .texOffs(0, 0).addBox(9F, -17F, -1F, 3F, 15F, 3F),
            PartPose.offsetAndRotation(0F, 0F, -19F, -0.6545F, 0F, 0F));

        bb_main.addOrReplaceChild("cube_r3",
            CubeListBuilder.create()
            .texOffs(80, 179).addBox(-19F, -36F, 0F, 36F, 20F, 1F)
            .texOffs(221, 237).addBox(17F, -36F, -1F, 3F, 34F, 3F)
            .texOffs(233, 237).addBox(-22F, -36F, -1F, 3F, 34F, 3F),
            PartPose.offsetAndRotation(0F, 0F, -19F, -0.2182F, 0F, 0F));

        return LayerDefinition.create(mesh, 288, 288);
    }
}
