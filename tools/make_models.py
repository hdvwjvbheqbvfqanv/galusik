"""
Генератор моделей и текстур (бабушка с лицом с фото + полицейский).
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

# ================================================================== ПОЛИЦЕЙСКИЙ
P_SKIN = np.array([222, 176, 148], np.float32)
P_HAIR = np.array([52, 38, 28], np.float32)
SHIRT = np.array([112, 136, 172], np.float32); SHIRT_D = np.array([84, 106, 140], np.float32)
NAVY = np.array([30, 40, 70], np.float32); NAVY_D = np.array([20, 28, 50], np.float32)
BLACK = np.array([22, 22, 26], np.float32); SILVER = np.array([190, 192, 196], np.float32)
P_RED = np.array([176, 34, 36], np.float32)

def p_skin_img(w, h, seed, k=1.0):
    return shade(solid(P_SKIN * k, w, h), w, h, seed, 5.0)

def p_head(face, w, h):
    if face == "front":
        W, H = w * K, h * K
        base = vgrad(p_skin_img(w, h, 70), h, 1.05, 0.93)
        im = Image.fromarray(np.clip(base, 0, 255).astype(np.uint8)).convert("RGBA")
        d = ImageDraw.Draw(im, "RGBA")
        u = K
        def ell(cx, cy, rx, ry, fill):
            d.ellipse([(cx - rx) * u, (cy - ry) * u, (cx + rx) * u, (cy + ry) * u], fill=fill)
        # волосы сверху и виски
        d.rectangle([0, 0, W, 1.7 * u], fill=tuple(int(c) for c in P_HAIR) + (255,))
        d.rectangle([0, 0, 0.55 * u, 3.8 * u], fill=tuple(int(c) for c in P_HAIR) + (255,))
        d.rectangle([W - 0.55 * u, 0, W, 3.8 * u], fill=tuple(int(c) for c in P_HAIR) + (255,))
        # тени щёк и скул
        ell(1.2, 5.3, 0.9, 1.2, (170, 100, 90, 14)); ell(4.8, 5.3, 0.9, 1.2, (170, 100, 90, 14))
        for cx in (1.85, 4.15):
            ell(cx, 4.05, 0.62, 0.30, (236, 236, 232, 255))          # белок
            ell(cx, 4.05, 0.27, 0.27, (82, 108, 96, 255))            # радужка
            ell(cx, 4.05, 0.12, 0.12, (12, 12, 14, 255))             # зрачок
            ell(cx - 0.07, 3.97, 0.045, 0.045, (255, 255, 255, 230)) # блик
            d.arc([(cx - 0.62) * u, (4.05 - 0.34) * u, (cx + 0.62) * u, (4.05 + 0.26) * u], 180, 360,
                  fill=(92, 62, 52, 255), width=int(0.07 * u))       # верхнее веко
            ell(cx, 4.42, 0.55, 0.07, (150, 100, 90, 70))            # мешок под глазом
        for (x0, x1, y0, y1) in ((1.1, 2.7, 3.38, 3.22), (3.3, 4.9, 3.22, 3.38)):
            d.line([x0 * u, y0 * u, x1 * u, y1 * u], fill=(48, 34, 26, 255), width=int(0.20 * u))
        # нос
        d.line([2.78 * u, 4.2 * u, 2.62 * u, 5.45 * u], fill=(170, 112, 94, 130), width=int(0.10 * u))
        d.line([3.22 * u, 4.2 * u, 3.38 * u, 5.45 * u], fill=(170, 112, 94, 130), width=int(0.10 * u))
        ell(2.78, 5.6, 0.14, 0.09, (110, 64, 56, 200)); ell(3.22, 5.6, 0.14, 0.09, (110, 64, 56, 200))
        ell(3.0, 5.45, 0.2, 0.12, (240, 200, 176, 120))
        # рот
        d.line([2.2 * u, 6.55 * u, 3.0 * u, 6.62 * u, 3.8 * u, 6.55 * u], fill=(120, 58, 56, 255), width=int(0.11 * u))
        ell(3.0, 6.82, 0.42, 0.09, (200, 120, 112, 120))
        # щетина на подбородке
        im = im.filter(ImageFilter.GaussianBlur(0.9))
        arr = np.asarray(im.convert("RGB")).astype(np.float32)
        stub = (noise(H, W, 2, 2, 71) > 0.55).astype(np.float32)
        yy = (np.arange(H)[:, None] / K)
        zone = np.clip((yy - 6.6) / 0.5, 0, 1) * 0.35
        arr = arr * (1 - (stub * zone)[..., None] * 0.5)
        return arr
    X, Y = grid(w, h)
    H, W = Y.shape
    skin = p_skin_img(w, h, 72)
    hair = np.ones((H, W, 3), np.float32) * P_HAIR
    hair = np.clip(hair + noise(H, W, 8, 8, 73)[..., None] * 10, 0, 255)
    if face == "top":
        return hair
    if face == "bottom":
        return skin * 0.9
    if face in ("left", "right"):
        t = np.clip((Y / h - 0.40) / 0.08, 0, 1)
        return mix(hair, skin, t)
    t = np.clip((Y / h - 0.60) / 0.1, 0, 1)
    return mix(hair, skin * 0.92, t)

def p_cap(face, w, h):
    X, Y = grid(w, h)
    H, W = Y.shape
    img = np.ones((H, W, 3), np.float32) * NAVY
    img = np.clip(img + noise(H, W, 6, 6, 80)[..., None] * 6, 0, 255)
    if face == "front":
        r = np.sqrt((X - w / 2) ** 2 + (Y - h / 2) ** 2)
        img = mix(img, GOLD[None, None, :], np.clip((0.42 - r) / 0.04, 0, 1))
        img = mix(img, P_RED[None, None, :], np.clip((0.22 - r) / 0.03, 0, 1))
    return img

def p_visor(face, w, h):
    H, W = h * K, w * K
    img = np.ones((H, W, 3), np.float32) * BLACK
    X, Y = grid(w, h)
    return np.clip(img + (np.sin(X * 2 + Y) * 6)[..., None] + 6, 0, 255)

def p_ear(face, w, h):
    return p_skin_img(w, h, 74, 0.97)

def p_neck(face, w, h):
    return p_skin_img(w, h, 75, 0.9)

def p_torso(face, w, h):
    X, Y = grid(w, h)
    H, W = Y.shape
    img = np.ones((H, W, 3), np.float32) * SHIRT
    img = np.clip(img + noise(H, W, 5, 5, 81)[..., None] * 5 + noise(H, W, 30, 30, 82)[..., None] * 4, 0, 255)
    if face in ("top", "bottom"):
        return img
    img = vgrad(img, h, 1.04, 0.94)
    if face == "front":
        cx = w / 2
        # воротник (треугольник) и галстук
        coll = (np.abs(X - cx) < (1.15 - Y * 0.25)) & (Y < 1.4)
        img = mix(img, SKIN_NECK()[None, None, :], coll.astype(np.float32) * 0.0)
        tie = (np.abs(X - cx) < (0.28 + Y * 0.02)) & (Y > 0.5) & (Y < 5.6)
        img = mix(img, NAVY[None, None, :], tie.astype(np.float32))
        # карманы и значок
        for px in (1.0, 5.2):
            pocket = (X > px) & (X < px + 1.8) & (Y > 1.7) & (Y < 3.1)
            edge = pocket & ((X < px + 0.07) | (X > px + 1.73) | (Y > 3.03) | (Y < 1.77))
            img = mix(img, SHIRT_D[None, None, :], edge.astype(np.float32))
        r = np.sqrt((X - 5.6) ** 2 + (Y - 2.0) ** 2)
        img = mix(img, SILVER[None, None, :], np.clip((0.28 - r) / 0.04, 0, 1))
    if face == "back":
        img = mix(img, SHIRT_D[None, None, :], (np.abs(X - w / 2) < 0.04).astype(np.float32))
    # ремень внизу
    belt = Y > (h - 1.5)
    img = mix(img, BLACK[None, None, :], belt.astype(np.float32))
    if face == "front":
        bk = (np.abs(X - w / 2) < 0.55) & (Y > h - 1.3) & (Y < h - 0.3)
        img = mix(img, SILVER[None, None, :], bk.astype(np.float32))
    return img

def SKIN_NECK():
    return P_SKIN * 0.9

def p_belt(face, w, h):
    H, W = h * K, w * K
    img = np.ones((H, W, 3), np.float32) * BLACK
    return img

def p_epaulet(face, w, h):
    X, Y = grid(w, h)
    H, W = Y.shape
    img = np.ones((H, W, 3), np.float32) * NAVY
    if face == "top":
        stripe = (np.abs(X - w / 2) < 0.12) | (np.abs(Y - h / 2) < 0.0)
        img = mix(img, GOLD[None, None, :], stripe.astype(np.float32))
        for cy in (h * 0.35, h * 0.65):
            r = np.sqrt((X - w / 2) ** 2 + (Y - cy) ** 2)
            img = mix(img, GOLD[None, None, :], np.clip((0.2 - r) / 0.03, 0, 1))
    return img

def p_sleeve(face, w, h):
    X, Y = grid(w, h)
    H, W = Y.shape
    img = np.ones((H, W, 3), np.float32) * SHIRT
    img = np.clip(img + noise(H, W, 5, 5, 83)[..., None] * 5, 0, 255)
    if face in ("top", "bottom"):
        return img
    return vgrad(img, h, 1.03, 0.93)

def p_cuff(face, w, h):
    img = p_sleeve(face, w, h)
    H = img.shape[0]
    if face not in ("top", "bottom"):
        img[int(H * 0.82):] = img[int(H * 0.82):] * 0.82
    return img

def p_hand(face, w, h):
    return p_skin_img(w, h, 84, 1.0)

def p_pants(face, w, h):
    X, Y = grid(w, h)
    H, W = Y.shape
    img = np.ones((H, W, 3), np.float32) * NAVY_D
    img = np.clip(img + noise(H, W, 5, 5, 85)[..., None] * 5, 0, 255)
    if face in ("top", "bottom"):
        return img
    img = vgrad(img, h, 1.08, 0.92)
    if face in ("left", "right"):
        img = mix(img, np.array([120, 40, 44], np.float32)[None, None, :], (np.abs(X - w / 2) < 0.1).astype(np.float32) * 0.8)
    return img

def p_boot(face, w, h):
    X, Y = grid(w, h)
    H, W = Y.shape
    img = np.ones((H, W, 3), np.float32) * BLACK
    img = np.clip(img + noise(H, W, 8, 8, 86)[..., None] * 4, 0, 255)
    if face in ("top", "bottom"):
        return img + (6 if face == "top" else -4)
    return np.clip(vgrad(img, h, 1.5, 0.8), 0, 255)

POLICE = dict(
    name="police", class_name="PoliceModel", state="LivingEntityRenderState", tex="police.png",
    parts={
        "head":          ((0, 2.5, 0), None),
        "body":          ((0, 3.3, 0), None),
        "right_arm":     ((-5.5, 4.4, 0), None),
        "right_forearm": ((0, 4, 0), "right_arm"),
        "left_arm":      ((5.5, 4.4, 0), None),
        "left_forearm":  ((0, 4, 0), "left_arm"),
        "right_leg":     ((-2, 13, 0), None),
        "right_shin":    ((0, 5, 0), "right_leg"),
        "left_leg":      ((2, 13, 0), None),
        "left_shin":     ((0, 5, 0), "left_leg"),
    },
    cubes=[
        ("head", "head",   (-3, -8, -3), (6, 8, 6), -0.5, p_head),
        ("head", "cap",    (-3, -8.2, -3), (6, 1, 6), 0.45, p_cap),
        ("head", "visor",  (-2.5, -7.2, -5.6), (5, 1, 3), 0.0, p_visor),
        ("head", "ear_r",  (-3.4, -4.4, -0.5), (1, 2, 1), 0.0, p_ear),
        ("head", "ear_l",  (2.4, -4.4, -0.5),  (1, 2, 1), 0.0, p_ear),
        ("body", "neck",   (-1.5, -1.5, -1.5), (3, 2, 3), 0.0, p_neck),
        ("body", "torso",  (-4, 0, -2), (8, 10, 4), 0.0, p_torso),
        ("body", "epaulet_r", (-4.2, -0.5, -2), (3, 1, 4), 0.15, p_epaulet),
        ("body", "epaulet_l", (1.2, -0.5, -2),  (3, 1, 4), 0.15, p_epaulet),
        ("right_arm", "r_arm",  (-1.5, -1, -1.5), (3, 5, 3), 0.0, p_sleeve),
        ("right_forearm", "r_fore", (-1.5, 0, -1.5), (3, 4, 3), 0.0, p_cuff),
        ("right_forearm", "r_hand", (-1.5, 4, -1.5), (3, 2, 3), 0.0, p_hand),
        ("left_arm", "l_arm",  (-1.5, -1, -1.5), (3, 5, 3), 0.0, p_sleeve),
        ("left_forearm", "l_fore", (-1.5, 0, -1.5), (3, 4, 3), 0.0, p_cuff),
        ("left_forearm", "l_hand", (-1.5, 4, -1.5), (3, 2, 3), 0.0, p_hand),
        ("right_leg", "r_thigh", (-2, 0, -2), (4, 5, 4), 0.0, p_pants),
        ("right_shin", "r_shin", (-2, 0, -2), (4, 4, 4), 0.0, p_pants),
        ("right_shin", "r_boot", (-2, 3, -2.8), (4, 3, 5), 0.15, p_boot),
        ("left_leg", "l_thigh", (-2, 0, -2), (4, 5, 4), 0.0, p_pants),
        ("left_shin", "l_shin", (-2, 0, -2), (4, 4, 4), 0.0, p_pants),
        ("left_shin", "l_boot", (-2, 3, -2.8), (4, 3, 5), 0.15, p_boot),
    ],
    anim='''        float deg = (float) Math.PI / 180F;
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
        leftForearm.xRot = bend;''',
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
    for m in (GRANNY, POLICE):
        pos, atlas = paint(m)
        java(m, pos)
        preview(m, pos, atlas, "/home/claude/preview_%s.png" % m["name"])
    print("ok")
