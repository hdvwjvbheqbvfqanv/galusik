"""
Генератор моделей и текстур (бабушка с лицом с фото; полицейский — готовый скин police.png, его модель PoliceModel.java написана вручную).
Рисует PNG высокого разрешения (раскладка UV 64x64, масштаб K) и ПИШЕТ Java-классы моделей.
Запуск:  python3 tools/make_models.py     (нужны Pillow и numpy)
"""
import os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from face_prep import face_image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..")
TEX_DIR = os.path.join(ROOT, "src/main/resources/assets/babushka/textures/entity")
JAVA_DIR = os.path.join(ROOT, "src/main/java/babushka/client")
K = 32  # пикселей на юнит модели -> текстура 2048x2048

# ------------------------------------------------------------------ утилиты
def noise(H, W, ch, cw, seed):
    r = np.random.default_rng(seed)
    gh, gw = max(2, H // ch + 2), max(2, W // cw + 2)
    g = (r.random((gh, gw)) * 255).astype(np.uint8)
    im = Image.fromarray(g).resize((W, H), Image.BICUBIC)
    return np.asarray(im).astype(np.float32) / 127.5 - 1.0

def grid(w, h):
    H, W = h * K, w * K
    Y, X = np.mgrid[0:H, 0:W].astype(np.float32)
    return (X + 0.5) / K, (Y + 0.5) / K

def solid(color, w, h):
    return np.ones((h * K, w * K, 3), np.float32) * np.array(color, np.float32)

def shade(img, w, h, seed, amp=7.0, cell=24):
    H, W = h * K, w * K
    n = noise(H, W, cell, cell, seed) * amp + noise(H, W, 5, 5, seed + 1) * amp * 0.35
    return np.clip(img + n[..., None], 0, 255)

def vgrad(img, h, top=1.04, bottom=0.94):
    H = img.shape[0]
    g = np.linspace(top, bottom, H, dtype=np.float32)[:, None, None]
    return np.clip(img * g, 0, 255)

def mix(a, b, t):
    t = t[..., None] if t.ndim == 2 else t
    return a * (1 - t) + b * t

# ------------------------------------------------------------------ палитра
FACE_IMG, HAIR_COL, _ = face_image()
FACE_ARR = np.asarray(FACE_IMG).astype(np.float32)
SKIN_G = np.median(FACE_ARR[640:780, 110:230].reshape(-1, 3), axis=0) * 0.45 + np.array([196, 150, 120], np.float32) * 0.55
SKIN_G_D = SKIN_G * 0.88
HAIR_G = np.array([70, 46, 33], np.float32)
RED = np.array([178, 30, 32], np.float32); RED_D = np.array([132, 20, 24], np.float32)
CREAM = np.array([232, 214, 200], np.float32); PINK = np.array([214, 150, 140], np.float32)
BLUE = np.array([54, 98, 196], np.float32); BLUE_L = np.array([116, 166, 238], np.float32); BLUE_D = np.array([30, 58, 134], np.float32)
GOLD = np.array([232, 196, 110], np.float32)

# ================================================================== БАБУШКА
def floral(X, Y, seed):
    sx, sy = 1.9, 1.7
    row = np.floor(Y / sy)
    Xo = X + (row % 2) * sx / 2
    fx = (Xo % sx) - sx / 2
    fy = (Y % sy) - sy / 2
    r = np.sqrt(fx ** 2 + fy ** 2)
    th = np.arctan2(fy, fx)
    R = 0.50 * (0.78 + 0.22 * np.cos(5 * th))
    a = np.clip((R - r) / 0.05, 0, 1)
    H, W = X.shape
    cloth = RED[None, None, :] * (1 + 0.05 * noise(H, W, 6, 6, seed)[..., None])
    petals = CREAM[None, None, :] * (1 - 0.10 * (r / 0.5))[..., None]
    img = mix(cloth, petals, a)
    c = np.clip((0.17 - r) / 0.04, 0, 1)
    img = mix(img, PINK[None, None, :], c)
    return img

def g_skin(face, w, h, seed=1, k=1.0):
    img = shade(solid(SKIN_G * k, w, h), w, h, seed, 6.0)
    return img

def g_head(face, w, h):
    if face == "front":
        im = FACE_IMG.resize((w * K, h * K), Image.LANCZOS)
        return np.asarray(im).astype(np.float32)
    X, Y = grid(w, h)
    H, W = Y.shape
    hair = np.ones((H, W, 3), np.float32) * HAIR_G
    streak = noise(H, W, 120, 3, 11) * 14 + noise(H, W, 6, 6, 12) * 6
    hair = np.clip(hair + streak[..., None], 0, 255)
    gray = np.clip(noise(H, W, 80, 4, 13) - 0.55, 0, 1) * 2.2
    hair = mix(hair, np.array([150, 138, 128], np.float32)[None, None, :], gray * 0.5)
    skin = g_skin("x", w, h, 3)
    if face == "top":
        return hair
    if face == "bottom":
        return skin * 0.9
    if face in ("left", "right"):
        t = np.clip((Y / h - 0.42) / 0.12, 0, 1)         # сверху волосы, ниже — висок/ухо
        return mix(hair, skin, t)
    # back
    t = np.clip((Y / h - 0.82) / 0.1, 0, 1)
    return mix(hair, skin * 0.9, t)

def g_ear(face, w, h):
    return g_skin(face, w, h, 4, 0.97)

def g_neck(face, w, h):
    return g_skin(face, w, h, 5, 0.9)

def g_torso(face, w, h):
    X, Y = grid(w, h)
    img = floral(X, Y, 20)
    if face in ("top", "bottom"):
        return solid(RED, w, h)
    if face == "front":
        cx = w / 2
        d = ((X - cx) / 1.95) ** 2 + (Y / 2.35) ** 2
        skin = g_skin("x", w, h, 6, 0.96)
        trim = np.clip((1.12 - d) / 0.12, 0, 1) * np.clip((d - 0.80) / 0.12, 0, 1)
        img = mix(img, skin, np.clip((1.0 - d) / 0.06, 0, 1))
        img = mix(img, RED_D[None, None, :], trim)
    if face == "back":
        d = ((X - w / 2) / 1.6) ** 2 + (Y / 1.0) ** 2
        img = mix(img, RED_D[None, None, :], np.clip((1.0 - d) / 0.2, 0, 1))
    return vgrad(img, h, 1.03, 0.95)

def g_skirt(face, w, h):
    X, Y = grid(w, h)
    if face in ("top", "bottom"):
        return solid(RED_D, w, h)
    img = floral(X, Y, 30)
    H, W = Y.shape
    fold = 1.0 + 0.07 * np.sin(X * 3.4 + noise(H, W, 40, 40, 31) * 1.2)
    img = np.clip(img * fold[..., None], 0, 255)
    return vgrad(img, h, 1.02, 0.90)

def g_hem(face, w, h):
    img = g_skirt(face, w, h)
    if face not in ("top", "bottom"):
        H = img.shape[0]
        img[int(H * 0.86):] = img[int(H * 0.86):] * 0.78  # окантовка подола
    return img

def g_strap(face, w, h):
    X, Y = grid(w, h)
    return solid(RED, w, h) if face in ("top", "bottom") else floral(X, Y, 40)

def g_limb(face, w, h):
    return g_skin(face, w, h, 7, 0.97)

def g_hand(face, w, h):
    return g_skin(face, w, h, 8, 1.0)

def g_leg(face, w, h):
    img = g_skin(face, w, h, 9, 1.03)
    return vgrad(img, h, 1.0, 0.96)

def flower(X, Y, cx, cy, R):
    r = np.sqrt((X - cx) ** 2 + (Y - cy) ** 2)
    th = np.arctan2(Y - cy, X - cx)
    Rm = R * (0.75 + 0.25 * np.cos(5 * th))
    return np.clip((Rm - r) / 0.04, 0, 1), np.clip((R * 0.32 - r) / 0.03, 0, 1)

def g_slipper(face, w, h):
    X, Y = grid(w, h)
    H, W = Y.shape
    img = np.ones((H, W, 3), np.float32) * BLUE
    img = np.clip(img + noise(H, W, 10, 10, 50)[..., None] * 9, 0, 255)
    if face == "top":                       # верх тапка: мысок наверху (z-), цветочек
        img = mix(img, BLUE_L[None, None, :], np.clip(1.0 - (Y / h), 0, 1) * 0.5)
        a, c = flower(X, Y, w / 2, 1.1, 0.55)
        img = mix(img, CREAM[None, None, :], a)
        img = mix(img, GOLD[None, None, :], c)
        return img
    if face == "bottom":
        return np.ones((H, W, 3), np.float32) * BLUE_D
    img = vgrad(img, h, 1.12, 0.86)
    img[int(H * 0.82):] = img[int(H * 0.82):] * 0.55   # подошва
    return img

def g_bow(face, w, h):
    X, Y = grid(w, h)
    H, W = Y.shape
    img = np.ones((H, W, 3), np.float32) * BLUE
    img = mix(img, BLUE_L[None, None, :], np.clip(0.5 - np.abs(Y / h - 0.5), 0, 0.5))
    return np.clip(img + noise(H, W, 8, 8, 60)[..., None] * 8, 0, 255)

GRANNY = dict(
    name="babushka", class_name="BabushkaModel", state="BabushkaRenderState", tex="babushka.png",
    parts={
        "head":          ((0, 5.5, 0), None),
        "body":          ((0, 6, 0), None),
        "right_arm":     ((-4.6, 7.2, 0), None),
        "right_forearm": ((0, 3, 0), "right_arm"),
        "left_arm":      ((4.6, 7.2, 0), None),
        "left_forearm":  ((0, 3, 0), "left_arm"),
        "right_leg":     ((-1.8, 14, 0), None),
        "right_shin":    ((0, 4, 0), "right_leg"),
        "left_leg":      ((1.8, 14, 0), None),
        "left_shin":     ((0, 4, 0), "left_leg"),
    },
    cubes=[
        ("head", "head",   (-3, -8, -3), (6, 8, 6), -0.5, g_head),
        ("head", "ear_r",  (-3.4, -4.4, -0.5), (1, 2, 1), 0.0, g_ear),
        ("head", "ear_l",  (2.4, -4.4, -0.5),  (1, 2, 1), 0.0, g_ear),
        # бантик на ЛЕВОЙ стороне головы (левая сторона бабушки = +x модели)
        ("head", "bow_knot",   (2.7, -6.7, -0.5), (1, 1, 1), 0.0, g_bow),
        ("head", "bow_wing_f", (2.8, -7.6, -2.5), (1, 2, 2), 0.0, g_bow),
        ("head", "bow_wing_b", (2.8, -7.6, 0.5),  (1, 2, 2), 0.0, g_bow),
        ("body", "neck",  (-1.5, -1.5, -1.5), (3, 2, 3), 0.0, g_neck),
        ("body", "torso", (-3.5, 0, -2), (7, 6, 4), 0.0, g_torso),
        ("body", "skirt_a", (-4, 5, -2.5), (8, 3, 5), 0.0, g_skirt),
        ("body", "skirt_b", (-4.5, 8, -3), (9, 2, 6), 0.0, g_hem),
        ("right_arm", "r_arm",   (-1, -1, -1), (2, 4, 2), 0.0, g_limb),
        ("right_arm", "r_strap", (-1, -1, -1), (2, 2, 2), 0.25, g_strap),
        ("right_forearm", "r_fore", (-1, 0, -1), (2, 3, 2), 0.0, g_limb),
        ("right_forearm", "r_hand", (-1, 3, -1), (2, 2, 2), 0.0, g_hand),
        ("left_arm", "l_arm",   (-1, -1, -1), (2, 4, 2), 0.0, g_limb),
        ("left_arm", "l_strap", (-1, -1, -1), (2, 2, 2), 0.25, g_strap),
        ("left_forearm", "l_fore", (-1, 0, -1), (2, 3, 2), 0.0, g_limb),
        ("left_forearm", "l_hand", (-1, 3, -1), (2, 2, 2), 0.0, g_hand),
        ("right_leg", "r_thigh", (-1.5, 0, -1.5), (3, 4, 3), 0.0, g_leg),
        ("right_shin", "r_shin", (-1.5, 0, -1.5), (3, 4, 3), 0.0, g_leg),
        ("right_shin", "r_slipper", (-1.5, 4, -3), (3, 2, 5), 0.2, g_slipper),
        ("left_leg", "l_thigh", (-1.5, 0, -1.5), (3, 4, 3), 0.0, g_leg),
        ("left_shin", "l_shin", (-1.5, 0, -1.5), (3, 4, 3), 0.0, g_leg),
        ("left_shin", "l_slipper", (-1.5, 4, -3), (3, 2, 5), 0.2, g_slipper),
    ],
    anim='''        float deg = (float) Math.PI / 180F;
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
        }''',
)

# ================================================================== сборка
def footprint(c):
    w, h, d = c[3]
    return 2 * (w + d), d + h

def pack(cubes):
    order = sorted(range(len(cubes)), key=lambda i: (-footprint(cubes[i])[1], -footprint(cubes[i])[0]))
    pos = {}
    cx = cy = rowh = 0
    for i in order:
        W, H = footprint(cubes[i])
        if cx + W > 64:
            cx = 0; cy += rowh; rowh = 0
        assert cy + H <= 64, "атлас не влезает"
        pos[i] = (cx, cy)
        cx += W; rowh = max(rowh, H)
    return pos, cy + rowh

def paint(model):
    cubes = model["cubes"]
    pos, used = pack(cubes)
    atlas = np.zeros((64 * K, 64 * K, 4), np.uint8)
    def put(u, v, arr):
        h, w = arr.shape[:2]
        a = np.clip(arr, 0, 255).astype(np.uint8)
        atlas[v * K:v * K + h, u * K:u * K + w, :3] = a
        atlas[v * K:v * K + h, u * K:u * K + w, 3] = 255
    for i, c in enumerate(cubes):
        w, h, d = c[3]; u, v = pos[i]; p = c[5]
        put(u + d, v, p("top", w, d))
        put(u + d + w, v, p("bottom", w, d))
        put(u, v + d, p("right", d, h))
        put(u + d, v + d, p("front", w, h))
        put(u + d + w, v + d, p("left", d, h))
        put(u + 2 * d + w, v + d, p("back", w, h))
    os.makedirs(TEX_DIR, exist_ok=True)
    Image.fromarray(atlas, "RGBA").save(os.path.join(TEX_DIR, model["tex"]), optimize=True)
    print(model["name"], "атлас: высота", used, "из 64")
    return pos, atlas

def f(x):
    return ("%g" % x) + "F"

def java(model, pos):
    cubes, parts = model["cubes"], model["parts"]
    def builder(part):
        out = ["CubeListBuilder.create()"]
        for i, c in enumerate(cubes):
            if c[0] != part: continue
            u, v = pos[i]; o = c[2]; w, h, d = c[3]; g = c[4]
            deform = (", new CubeDeformation(%s)" % f(g)) if g else ""
            out.append("            .texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s%s)" %
                       (u, v, f(o[0]), f(o[1]), f(o[2]), f(w), f(h), f(d), deform))
        return "\n".join(out)
    def pose(p):
        return "PartPose.offset(%s, %s, %s)" % (f(p[0]), f(p[1]), f(p[2]))
    names = list(parts.keys())
    camel = lambda n: "".join(s if i == 0 else s.capitalize() for i, s in enumerate(n.split("_")))
    L = []
    L.append("package babushka.client;\n")
    for imp in ("net.minecraft.client.model.EntityModel", "net.minecraft.client.model.geom.ModelPart",
                "net.minecraft.client.model.geom.builders.CubeDeformation",
                "net.minecraft.client.model.geom.builders.CubeListBuilder",
                "net.minecraft.client.model.geom.builders.LayerDefinition",
                "net.minecraft.client.model.geom.builders.MeshDefinition",
                "net.minecraft.client.model.geom.builders.PartDefinition",
                "net.minecraft.client.model.geom.PartPose",
                "net.minecraft.util.Mth"):
        L.append("import %s;" % imp)
    state_import = "net.minecraft.client.renderer.entity.state.LivingEntityRenderState"
    if model["state"] == "LivingEntityRenderState":
        L.append("import %s;" % state_import)
    L.append("\n/** %s (ГЕНЕРИРУЕТСЯ tools/make_models.py вместе с текстурой — не правьте руками). */" % model["class_name"])
    L.append("public class %s extends EntityModel<%s> {" % (model["class_name"], model["state"]))
    for n in names:
        L.append("    private final ModelPart %s;" % camel(n))
    L.append("\n    public %s(ModelPart root) {\n        super(root);" % model["class_name"])
    for n in names:
        parent = parts[n][1]
        src = "root" if parent is None else camel(parent)
        L.append('        this.%s = %s.getChild("%s");' % (camel(n), src, n))
    L.append("    }\n")
    L.append("    public static LayerDefinition createBodyLayer() {\n        MeshDefinition mesh = new MeshDefinition();\n        PartDefinition root = mesh.getRoot();\n")
    for n in names:
        pivot, parent = parts[n]
        src = "root" if parent is None else parent
        L.append('        PartDefinition %s = %s.addOrReplaceChild("%s",\n            %s,\n            %s);\n' % (
            n, src, n, builder(n), pose(pivot)))
    L.append("        return LayerDefinition.create(mesh, 64, 64);\n    }\n")
    L.append("    @Override\n    public void setupAnim(%s state) {\n        super.setupAnim(state);" % model["state"])
    L.append(model["anim"])
    L.append("    }\n}\n")
    open(os.path.join(JAVA_DIR, model["class_name"] + ".java"), "w", encoding="utf-8").write("\n".join(L))

def preview(model, pos, atlas, path):
    cubes, parts = model["cubes"], model["parts"]
    absp = {}
    for n, (pv, par) in parts.items():
        absp[n] = pv if par is None else tuple(a + b for a, b in zip(absp[par], pv))
    S = 14
    cw, ch = 34 * S, 40 * S
    canvas = Image.new("RGBA", (cw * 2, ch), (206, 224, 242, 255))
    items = []
    for i, c in enumerate(cubes):
        pv = absp[c[0]]; o = c[2]; w, h, d = c[3]; g = c[4]
        items.append((pv[0] + o[0] - g, pv[1] + o[1] - g, pv[2] + o[2] - g, w + 2 * g, h + 2 * g, d + 2 * g, i))
    def face(i, kind):
        c = cubes[i]; w, h, d = c[3]; u, v = pos[i]
        box = {"front": (u + d, v + d, w, h), "right": (u, v + d, d, h)}[kind]
        x, y, bw, bh = box
        return Image.fromarray(atlas[y * K:(y + bh) * K, x * K:(x + bw) * K])
    # вид спереди (дальние — первыми)
    for x0, y0, z0, W, H, D, i in sorted(items, key=lambda t: -t[2]):
        im = face(i, "front").resize((max(1, int(W * S)), max(1, int(H * S))), Image.LANCZOS)
        canvas.paste(im, (int((x0 + 17) * S), int((y0 + 8) * S)), im)
    # вид сбоку (справа налево: нос слева)
    for x0, y0, z0, W, H, D, i in sorted(items, key=lambda t: -t[0]):
        im = face(i, "right").resize((max(1, int(D * S)), max(1, int(H * S))), Image.LANCZOS)
        canvas.paste(im, (int((z0 + 17) * S) + cw, int((y0 + 8) * S)), im)
    canvas.convert("RGB").save(path)

if __name__ == "__main__":
    for m in (GRANNY,):
        pos, atlas = paint(m)
        java(m, pos)
        preview(m, pos, atlas, "/home/claude/preview_%s.png" % m["name"])
    print("ok")
