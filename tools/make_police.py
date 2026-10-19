"""
Собирает текстуру полицейского police.png (ваш скин 64x64 + снизу 32 строки под автомат АК-47, итого 64x96)
и ПИШЕТ client/PoliceModel.java (скин «Алекс» с внешними слоями + автомат из кубиков в руке).
Запуск:  python3 tools/make_police.py   (нужны Pillow и numpy)
"""
import os
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..")
rng = np.random.default_rng(21)

METAL = (58, 60, 64); METAL_L = (88, 90, 96); METAL_D = (34, 35, 38)
WOOD = (132, 86, 48); WOOD_D = (98, 62, 34); MAG = (72, 40, 24)

# (имя, родитель, цвет, origin, dims, deform)   — ствол смотрит в -z, верх = -y
GUN = [
    ("receiver",   "gun", METAL,   (-0.5, -2.0, -3.0),  (1, 2, 6), 0.0),
    ("dust_cover", "gun", METAL_L, (-0.5, -2.5, -3.0),  (1, 1, 6), -0.15),
    ("hand_low",   "gun", WOOD,    (-0.5, -1.5, -8.0),  (1, 2, 5), 0.15),
    ("hand_up",    "gun", WOOD_D,  (-0.5, -2.4, -8.0),  (1, 1, 5), -0.1),
    ("barrel",     "gun", METAL_D, (-0.5, -1.5, -13.0), (1, 1, 5), -0.3),
    ("gas_tube",   "gun", METAL,   (-0.5, -2.5, -12.5), (1, 1, 4), -0.3),
    ("front_sight","gun", METAL_D, (-0.5, -3.1, -12.5), (1, 1, 1), -0.3),
    ("muzzle",     "gun", METAL_D, (-0.5, -1.6, -13.9), (1, 1, 1), -0.2),
    ("rear_sight", "gun", METAL_D, (-0.5, -3.0, -3.5),  (1, 1, 1), -0.3),
    ("stock",      "gun", WOOD,    (-0.5, -1.5, 3.0),   (1, 3, 4), 0.0),
    ("stock_end",  "gun", WOOD_D,  (-0.5, -1.0, 7.0),   (1, 3, 2), -0.1),
    ("mag1",       "mag1", MAG,    (-0.5, 0.0, -1.0),   (1, 3, 2), 0.0),
    ("mag2",       "mag2", MAG,    (-0.5, 0.0, -1.0),   (1, 3, 2), 0.0),
    ("grip",       "grip", WOOD_D, (-0.5, 0.0, -0.5),   (1, 3, 1), 0.1),
]
# части автомата: имя -> (родитель, позиция, поворот X)
GUN_PARTS = {"gun": (None, (-0.5, 8.5, 0.0), 1.5707964),
             "mag1": ("gun", (0.0, 0.0, -1.5), -0.18),
             "mag2": ("mag1", (0.0, 3.0, -0.2), -0.3),
             "grip": ("gun", (0.0, 0.0, 2.0), 0.35)}

def f(x):
    return ("%g" % x) + "F"

# ---- упаковка раскладок автомата в строки 64..96
def foot(c):
    w, h, d = c[4]
    return 2 * (w + d), d + h

order = sorted(range(len(GUN)), key=lambda i: (-foot(GUN[i])[1], -foot(GUN[i])[0]))
pos = {}
cx, cy, rowh = 0, 64, 0
for i in order:
    W, H = foot(GUN[i])
    if cx + W > 64:
        cx = 0; cy += rowh; rowh = 0
    assert cy + H <= 96, "автомат не влез в текстуру"
    pos[i] = (cx, cy)
    cx += W; rowh = max(rowh, H)

# ---- текстура
skin = Image.open(os.path.join(HERE, "police_skin.png")).convert("RGBA")
assert skin.size == (64, 64)
tex = Image.new("RGBA", (64, 96), (0, 0, 0, 0))
tex.paste(skin, (0, 0))
px = tex.load()
for i, c in enumerate(GUN):
    w, h, d = c[4]; u, v = pos[i]; col = np.array(c[2], float)
    wood = col[0] > 90 and col[2] < 60
    regions = [(u + d, v, w, d), (u + d + w, v, w, d), (u, v + d, d, h), (u + d, v + d, w, h),
               (u + d + w, v + d, d, h), (u + 2 * d + w, v + d, w, h)]
    for (x0, y0, rw, rh) in regions:
        for yy in range(rh):
            for xx in range(rw):
                k = rng.normal(0, 5)
                if wood:
                    k += 6 * np.sin((x0 + xx + y0 + yy) * 1.7)
                px[x0 + xx, y0 + yy] = tuple(int(np.clip(col[j] + k, 0, 255)) for j in range(3)) + (255,)
tex.save(os.path.join(ROOT, "src/main/resources/assets/babushka/textures/entity/police.png"))

# ---- Java
def gun_builder(part):
    out = ["CubeListBuilder.create()"]
    for i, c in enumerate(GUN):
        if c[1] != part: continue
        u, v = pos[i]; o = c[3]; w, h, d = c[4]; g = c[5]
        deform = (", new CubeDeformation(%s)" % f(g)) if g else ""
        out.append("                .texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s%s)" % (
            u, v, f(o[0]), f(o[1]), f(o[2]), f(w), f(h), f(d), deform))
    return "\n".join(out)

gun_code = []
p = GUN_PARTS
gun_code.append('        PartDefinition gun = right_arm.addOrReplaceChild("gun",\n            %s,\n            PartPose.offsetAndRotation(%s, %s, %s, %s, 0F, 0F));' % (
    gun_builder("gun"), f(p["gun"][1][0]), f(p["gun"][1][1]), f(p["gun"][1][2]), f(p["gun"][2])))
gun_code.append('        PartDefinition mag1 = gun.addOrReplaceChild("mag1",\n            %s,\n            PartPose.offsetAndRotation(%s, %s, %s, %s, 0F, 0F));' % (
    gun_builder("mag1"), f(p["mag1"][1][0]), f(p["mag1"][1][1]), f(p["mag1"][1][2]), f(p["mag1"][2])))
gun_code.append('        mag1.addOrReplaceChild("mag2",\n            %s,\n            PartPose.offsetAndRotation(%s, %s, %s, %s, 0F, 0F));' % (
    gun_builder("mag2"), f(p["mag2"][1][0]), f(p["mag2"][1][1]), f(p["mag2"][1][2]), f(p["mag2"][2])))
gun_code.append('        gun.addOrReplaceChild("grip",\n            %s,\n            PartPose.offsetAndRotation(%s, %s, %s, %s, 0F, 0F));' % (
    gun_builder("grip"), f(p["grip"][1][0]), f(p["grip"][1][1]), f(p["grip"][1][2]), f(p["grip"][2])))

JAVA = '''package babushka.client;

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
@GUN@

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
'''.replace("@GUN@", "\n\n".join(gun_code))
open(os.path.join(ROOT, "src/main/java/babushka/client/PoliceModel.java"), "w", encoding="utf-8").write(JAVA)
print("ok, автомат занимает строки до", cy + rowh)
