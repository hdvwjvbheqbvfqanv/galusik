"""
Генератор модели бабушки: рисует текстуру 64x64 И пишет BabushkaModel.java
(чтобы UV-координаты в коде и в картинке никогда не расходились).
Запуск:  python3 tools/make_model.py
"""
import random, os
from PIL import Image

random.seed(11)
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
TEX_PATH = os.path.join(ROOT, "src/main/resources/assets/babushka/textures/entity/babushka.png")
JAVA_PATH = os.path.join(ROOT, "src/main/java/babushka/client/BabushkaModel.java")

# ---- палитра ----
SKIN = (226, 176, 146, 255); SKIN_D = (205, 150, 122, 255); SKIN_L = (236, 190, 160, 255)
HAIR = (112, 82, 60, 255); HAIR_G = (170, 158, 148, 255); HAIR_D = (86, 62, 46, 255)
RED = (190, 36, 34, 255); RED_D = (146, 24, 26, 255)
CREAM = (232, 212, 196, 255); PINK = (214, 150, 140, 255)
BLUE = (58, 104, 200, 255); BLUE_L = (112, 162, 236, 255); BLUE_D = (34, 64, 140, 255)
WHITE = (240, 240, 240, 255); EYE = (70, 92, 124, 255); BROW = (92, 68, 50, 255)
MOUTH = (112, 40, 42, 255); GOLD = (232, 196, 110, 255)

def speck(c, d, p=0.1):
    return d if random.random() < p else c

def floral(x, y):
    px = (x + (y // 3) * 2) % 4
    py = y % 3
    if px == 1 and py == 1: return PINK
    if (px == 1 and py in (0, 2)) or (py == 1 and px in (0, 2)): return CREAM
    return speck(RED, RED_D, 0.08)

# ---- painters: f(face, x, y, fw, fh) -> color ----
def p_head(face, x, y, fw, fh):
    if face == "front":
        if y == 2 and x in (1, 2, 4, 5): return BROW
        if y == 3:
            if x in (1, 5): return WHITE
            if x in (2, 4): return EYE
        if y == 4 and x in (0, 6): return SKIN_D
        if y == 5 and x == 3: return MOUTH
        if y == 6 and x == 3: return MOUTH
        if y == 5 and x in (1, 5): return SKIN_D
        if y == 4 and x == 3: return SKIN_D
    return speck(SKIN, SKIN_D, 0.05)

def p_hair(face, x, y, fw, fh):
    r = random.random()
    if r < 0.18: return HAIR_G
    if r < 0.30: return HAIR_D
    return HAIR

def p_skin(face, x, y, fw, fh):
    return speck(SKIN, SKIN_D, 0.07)

def p_nose(face, x, y, fw, fh):
    return SKIN_L if face == "front" else SKIN

def p_bow(face, x, y, fw, fh):
    return BLUE_L if random.random() < 0.25 else BLUE

def p_torso(face, x, y, fw, fh):
    if face == "front":
        # декольте: кожа с красной окантовкой
        if y == 0 and 1 <= x <= fw - 2: return SKIN
        if y == 1 and 2 <= x <= fw - 3: return SKIN
        if y == 1 and x in (1, fw - 2): return RED_D
        if y == 2 and fw // 2 - 1 <= x <= fw // 2: return RED_D
        if y == 0 and x in (0, fw - 1): return RED_D
    if face == "back" and y == 0 and 2 <= x <= fw - 3: return RED_D
    if face in ("top", "bottom"): return RED
    return floral(x, y)

def p_neck(face, x, y, fw, fh):
    return SKIN_D if face != "top" else SKIN

def p_skirt(face, x, y, fw, fh):
    if face in ("top", "bottom"): return RED
    return floral(x, y)

def p_skirt_hem(face, x, y, fw, fh):
    if face in ("top", "bottom"): return RED_D
    return floral(x, y)

def p_strap(face, x, y, fw, fh):
    return floral(x, y) if face not in ("top", "bottom") else RED

def p_arm(face, x, y, fw, fh):
    return speck(SKIN, SKIN_D, 0.1)

def p_leg(face, x, y, fw, fh):
    return speck(SKIN, SKIN_D, 0.1)

def p_slipper(face, x, y, fw, fh):
    if face == "top":
        # маленький цветочек на мыске (мысок = z-, y=0 строки)
        if (x, y) == (1, 1): return GOLD
        if (x, y) in ((0, 1), (2, 1), (1, 0), (1, 2)): return CREAM
        return BLUE
    if face == "bottom": return BLUE_D
    if face in ("front", "right", "left", "back") and y == fh - 1: return BLUE_D
    return BLUE_L if random.random() < 0.12 else BLUE

# ---- описание частей ----
# part: (pivot, parent)
PARTS = {
    "head":          ((0, 6, 0), None),
    "body":          ((0, 7, 0), None),
    "right_arm":     ((-4, 8, 0), None),
    "right_forearm": ((0, 5, 0), "right_arm"),
    "left_arm":      ((4, 8, 0), None),
    "left_forearm":  ((0, 5, 0), "left_arm"),
    "right_leg":     ((-1.9, 13, 0), None),
    "left_leg":      ((1.9, 13, 0), None),
}

# cube: (part, name, origin, dims(w,h,d), deform, painter)
CUBES = [
    ("head", "head",       (-3.5, -7, -3.5), (7, 7, 7), 0.0,  p_head),
    ("head", "hair_top",   (-3.5, -7, -3.5), (7, 2, 7), 0.3,  p_hair),
    ("head", "hair_back",  (-3.5, -7, 1.5),  (7, 5, 2), 0.3,  p_hair),
    ("head", "nose",       (-0.5, -3.5, -4.5), (1, 2, 1), 0.0, p_nose),
    ("head", "ear_r",      (-4.5, -4.5, -0.5), (1, 2, 1), 0.0, p_skin),
    ("head", "ear_l",      (3.5, -4.5, -0.5),  (1, 2, 1), 0.0, p_skin),
    # бантик на ЛЕВОЙ стороне головы (левая сторона бабушки = +x модели)
    ("head", "bow_knot",   (3.7, -6.7, -0.5), (1, 1, 1), 0.0, p_bow),
    ("head", "bow_wing_f", (3.8, -7.4, -2.6), (1, 2, 2), 0.0, p_bow),
    ("head", "bow_wing_b", (3.8, -7.4, 0.6),  (1, 2, 2), 0.0, p_bow),
    ("body", "neck",       (-1, -1, -1), (2, 1, 2), 0.0, p_neck),
    ("body", "torso",      (-3, 0, -2),  (6, 5, 4), 0.0, p_torso),
    ("body", "skirt_top",  (-3.5, 4, -2.5), (7, 3, 5), 0.0, p_skirt),
    ("body", "skirt_hem",  (-4.5, 7, -3),   (9, 1, 6), 0.25, p_skirt_hem),
    ("right_arm", "r_arm",   (-1, -1, -1), (2, 5, 2), 0.0,  p_arm),
    ("right_arm", "r_strap", (-1, -1, -1), (2, 2, 2), 0.25, p_strap),
    ("right_forearm", "r_fore", (-1, 0, -1), (2, 3, 2), 0.0, p_arm),
    ("right_forearm", "r_hand", (-1, 3, -1), (2, 2, 2), -0.2, p_skin),
    ("left_arm", "l_arm",   (-1, -1, -1), (2, 5, 2), 0.0,  p_arm),
    ("left_arm", "l_strap", (-1, -1, -1), (2, 2, 2), 0.25, p_strap),
    ("left_forearm", "l_fore", (-1, 0, -1), (2, 3, 2), 0.0, p_arm),
    ("left_forearm", "l_hand", (-1, 3, -1), (2, 2, 2), -0.2, p_skin),
    ("right_leg", "r_leg",     (-1.5, 0, -1.5), (3, 9, 3), -0.1, p_leg),
    ("right_leg", "r_slipper", (-1.5, 9, -3),   (3, 2, 5), 0.2,  p_slipper),
    ("left_leg",  "l_leg",     (-1.5, 0, -1.5), (3, 9, 3), -0.1, p_leg),
    ("left_leg",  "l_slipper", (-1.5, 9, -3),   (3, 2, 5), 0.2,  p_slipper),
]

# ---- упаковка раскладок в атлас 64x64 ----
def footprint(c):
    w, h, d = c[3]
    return 2 * (w + d), d + h

order = sorted(range(len(CUBES)), key=lambda i: (-footprint(CUBES[i])[1], -footprint(CUBES[i])[0]))
pos = {}
cx = cy = rowh = 0
for i in order:
    W, H = footprint(CUBES[i])
    if cx + W > 64:
        cx = 0; cy += rowh; rowh = 0
    assert cy + H <= 64, "атлас не влезает"
    pos[i] = (cx, cy)
    cx += W; rowh = max(rowh, H)
print("атлас использует высоту", cy + rowh, "из 64")

img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
px = img.load()

def fill(rect, painter, face):
    x0, y0, w, h = rect
    for yy in range(h):
        for xx in range(w):
            px[x0 + xx, y0 + yy] = painter(face, xx, yy, w, h)

for i, c in enumerate(CUBES):
    w, h, d = c[3]
    u, v = pos[i]
    p = c[5]
    fill((u + d, v, w, d), p, "top")
    fill((u + d + w, v, w, d), p, "bottom")
    fill((u, v + d, d, h), p, "right")
    fill((u + d, v + d, w, h), p, "front")
    fill((u + d + w, v + d, d, h), p, "left")
    fill((u + 2 * d + w, v + d, w, h), p, "back")

os.makedirs(os.path.dirname(TEX_PATH), exist_ok=True)
img.save(TEX_PATH)

# ---- Java ----
def f(x):
    s = ("%g" % x)
    return s + "F"

lines = []
lines.append('''package babushka.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.PartPose;
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
''')

def builder(part):
    out = ["CubeListBuilder.create()"]
    for i, c in enumerate(CUBES):
        if c[0] != part: continue
        u, v = pos[i]
        o = c[2]; w, h, d = c[3]; g = c[4]
        deform = (", new CubeDeformation(%s)" % f(g)) if g else ""
        out.append("            .texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s%s)" %
                   (u, v, f(o[0]), f(o[1]), f(o[2]), f(w), f(h), f(d), deform))
    return "\n".join(out)

def pose(p):
    return "PartPose.offset(%s, %s, %s)" % (f(p[0]), f(p[1]), f(p[2]))

for name, (pivot, parent) in PARTS.items():
    if parent is None:
        lines.append('        PartDefinition %s = root.addOrReplaceChild("%s",\n            %s,\n            %s);\n' % (
            name, name, builder(name), pose(pivot)))
for name, (pivot, parent) in PARTS.items():
    if parent is not None:
        lines.append('        %s.addOrReplaceChild("%s",\n            %s,\n            %s);\n' % (
            parent, name, builder(name), pose(pivot)))

lines.append('''        return LayerDefinition.create(mesh, 64, 64);
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
''')
open(JAVA_PATH, "w", encoding="utf-8").write("\n".join(lines))

# ---- превью спереди (для проверки) ----
S = 14
prev = Image.new("RGBA", (20 * S, 30 * S), (200, 220, 240, 255))
pp = prev.load()
abs_pivot = {}
for name, (pv, par) in PARTS.items():
    abs_pivot[name] = pv if par is None else tuple(a + b for a, b in zip(abs_pivot[par], pv))
items = []
for i, c in enumerate(CUBES):
    pv = abs_pivot[c[0]]
    o = c[2]; w, h, d = c[3]; g = c[4]
    x0 = pv[0] + o[0] - g; y0 = pv[1] + o[1] - g; z0 = pv[2] + o[2] - g
    items.append((z0, i, x0, y0, w + 2 * g, h + 2 * g))
items.sort(key=lambda t: -t[0])
for z0, i, x0, y0, W, H in items:
    c = CUBES[i]; w, h, d = c[3]; u, v = pos[i]
    for yy in range(h):
        for xx in range(w):
            col = img.getpixel((u + d + xx, v + d + yy))
            if col[3] == 0: continue
            sx0 = int(round((x0 + xx * W / w + 10) * S)); sx1 = int(round((x0 + (xx + 1) * W / w + 10) * S))
            sy0 = int(round((y0 + yy * H / h + 3) * S)); sy1 = int(round((y0 + (yy + 1) * H / h + 3) * S))
            for sy in range(max(sy0, 0), min(sy1, 30 * S)):
                for sx in range(max(sx0, 0), min(sx1, 20 * S)):
                    pp[sx, sy] = col
prev.save("/home/claude/model_front_preview.png")
print("ok")
