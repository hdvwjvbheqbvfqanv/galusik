"""
Электрощиток: текстуры + JSON-модели + blockstate. Запуск: python3 tools/make_panel.py (нужны Pillow, numpy).
Раскладка задана "как видит игрок": u — слева направо от лица щитка, y — вверх.
Для блока, смотрящего на север, игроку слева соответствует БОЛЬШИЙ x — поэтому x = 16 - u.
Блок 16 x 16, глубина 8 (z от 8 до 16, лицо на z = 8).
Верхняя левая дверца (петли слева), справа — высокая дверь, снизу слева — панель счётчиков.
Внутри: 4 автомата с жёлтыми рычажками; двигается и работает только 2-й слева.
"""
import json, os, math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "../src/main/resources/assets/babushka")
TEX = os.path.join(RES, "textures/block")
for d in (TEX, RES + "/models/block", RES + "/blockstates", RES + "/items"):
    os.makedirs(d, exist_ok=True)
rng = np.random.default_rng(9)
U = 20  # пикселей на юнит

GREY = (86, 90, 84); GREY_D = (56, 60, 56); GREY_L = (112, 116, 108); GREY_VD = (34, 36, 34)
YELLOW = (244, 196, 24); BLACK = (18, 18, 20); WHITE = (232, 232, 226)

# ---------------------------------------------------------------- раскладка (юниты)
DOOR_U = (0.0, 9.0)            # верхняя левая дверца по горизонтали
DOOR_Y = (9.2, 16.0)           # и по вертикали
METER_Y = (0.0, 9.2)           # панель счётчиков: тоже u 0..9
RIGHT_U = (9.0, 16.0)          # правая высокая дверь
CAV_U = (0.9, 8.1); CAV_Y = (9.4, 15.4)   # проём (внутри открытой дверцы)
MOD_W = 1.4
MODULES = [(0.9, 2.3), (2.6, 4.0), (4.3, 5.7), (6.0, 7.4)]   # u-диапазоны 4 автоматов
MOD_Y = (10.0, 13.6)
H_UP = (12.0, 13.2); H_DOWN = (10.4, 11.6)                   # положения жёлтого рычажка
HANDLE_STATE = ["down", "move", "up", "up"]                  # двигается только 2-й слева

def X(u0, u1):
    return 16.0 - u1, 16.0 - u0

# ---------------------------------------------------------------- текстуры
def noisy(im, amp=4):
    a = np.asarray(im.convert("RGB")).astype(np.float32)
    n = rng.normal(0, amp, a.shape[:2])[..., None]
    return Image.fromarray(np.clip(a + n, 0, 255).astype(np.uint8))

def grime(im, strength=14):
    """потёки и затёртости по вертикали"""
    a = np.asarray(im.convert("RGB")).astype(np.float32)
    h, w = a.shape[:2]
    col = rng.normal(0, 1, (1, max(4, w // 6))).astype(np.float32)
    col = np.asarray(Image.fromarray(((col - col.min()) / (np.ptp(col) + 1e-6) * 255).astype(np.uint8)).resize((w, 1), Image.BICUBIC)).astype(np.float32) / 255
    streak = (col - 0.5) * strength
    grad = np.linspace(0, -strength * 0.6, h)[:, None]
    a = a + streak[..., None] + grad[..., None]
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))

def save(im, name, amp=4, dirty=True):
    if dirty:
        im = grime(im)
    noisy(im, amp).save(os.path.join(TEX, name + ".png"))

def plate(w, h, rim=9, raised=True):
    """дверца: фаска по краю, утопленная внутренняя панель, заклёпки"""
    im = Image.new("RGB", (w, h), GREY_D); d = ImageDraw.Draw(im)
    d.rectangle([2, 2, w - 3, h - 3], fill=GREY)
    d.rectangle([2, 2, w - 3, 5], fill=GREY_L); d.rectangle([2, 2, 5, h - 3], fill=GREY_L)        # блик слева/сверху
    d.rectangle([2, h - 6, w - 3, h - 3], fill=GREY_D); d.rectangle([w - 6, 2, w - 3, h - 3], fill=GREY_D)
    d.rectangle([rim, rim, w - rim - 1, h - rim - 1], fill=GREY_D)                                 # канавка
    d.rectangle([rim + 2, rim + 2, w - rim - 3, h - rim - 3], fill=(92, 96, 90))                   # внутренняя панель
    d.line([rim + 2, rim + 2, w - rim - 3, rim + 2], fill=GREY_L, width=2)
    d.line([rim + 2, h - rim - 3, w - rim - 3, h - rim - 3], fill=(66, 70, 66), width=2)
    for (x, y) in ((6, 6), (w - 7, 6), (6, h - 7), (w - 7, h - 7)):
        d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=GREY_VD); d.ellipse([x - 1, y - 1, x, y], fill=GREY_L)
    return im

def scratches(im, n=14):
    d = ImageDraw.Draw(im)
    w, h = im.size
    for _ in range(n):
        x = int(rng.integers(8, w - 8)); y = int(rng.integers(8, h - 8)); L = int(rng.integers(4, 14))
        d.line([x, y, x + L, y + int(rng.integers(-2, 3))], fill=(120, 124, 116), width=1)
    return im

# --- корпус / ободки
im = Image.new("RGB", (64, 64), GREY); d = ImageDraw.Draw(im)
d.rectangle([0, 0, 63, 2], fill=GREY_L); d.rectangle([0, 61, 63, 63], fill=GREY_D)
d.rectangle([0, 0, 2, 63], fill=GREY_L); d.rectangle([61, 0, 63, 63], fill=GREY_D)
save(im, "ep_body")
im = Image.new("RGB", (32, 32), GREY_L); d = ImageDraw.Draw(im); d.rectangle([0, 24, 31, 31], fill=GREY_D)
save(im, "ep_rim")
save(Image.new("RGB", (32, 32), (24, 26, 28)), "ep_cavity", dirty=False)

# --- верхняя дверца: 3 наклейки, знак опасности с молнией, замок
W, H = int(9 * U), int(6.8 * U)
im = plate(W, H); d = ImageDraw.Draw(im)
d.rectangle([22, 22, 62, 44], fill=(240, 202, 44));  [d.line([26, 28 + i * 5, 58, 28 + i * 5], fill=BLACK, width=2) for i in range(3)]
d.rectangle([22, 50, 62, 72], fill=(238, 224, 176)); d.rectangle([26, 54, 38, 68], fill=(190, 42, 42)); d.line([42, 56, 58, 56], fill=BLACK, width=2); d.line([42, 62, 58, 62], fill=BLACK, width=2)
d.rectangle([22, 78, 62, 106], fill=(240, 176, 44)); d.rectangle([28, 84, 56, 100], fill=(250, 240, 222)); d.line([30, 92, 54, 92], fill=(190, 60, 40), width=3)
cx, cy = 112, 62
d.polygon([(cx, cy - 36), (cx - 38, cy + 30), (cx + 38, cy + 30)], fill=BLACK)
d.polygon([(cx, cy - 27), (cx - 29, cy + 24), (cx + 29, cy + 24)], fill=(242, 202, 64))
d.polygon([(cx + 3, cy - 14), (cx - 9, cy + 6), (cx - 1, cy + 6), (cx - 5, cy + 20), (cx + 11, cy - 2), (cx + 3, cy - 2)], fill=BLACK)
lx, ly = 160, 66
d.ellipse([lx - 11, ly - 11, lx + 11, ly + 11], fill=GREY_VD); d.ellipse([lx - 8, ly - 8, lx + 8, ly + 8], fill=GREY_L)
d.rectangle([lx - 2, ly - 5, lx + 2, ly + 5], fill=GREY_VD)
save(scratches(im), "ep_door_top")

# --- панель счётчиков: 4 окошка со стеклом, прорези с язычками, защёлка
W, H = int(9 * U), int(9.2 * U)
im = plate(W, H, 9); d = ImageDraw.Draw(im)
WINS = [(1.7, 1.0), (4.5, 1.0), (1.7, 5.0), (4.5, 5.0)]
for (u, v) in WINS:
    x0, y0 = int(u * U) + 12, int(v * U) + 18
    x1, y1 = x0 + int(1.8 * U), y0 + int(1.4 * U)
    d.rectangle([x0 - 4, y0 - 4, x1 + 4, y1 + 4], fill=GREY_VD)
    d.rectangle([x0, y0, x1, y1], fill=(16, 18, 20))
    d.polygon([(x0 + 4, y0 + 3), (x0 + 16, y0 + 3), (x0 + 6, y1 - 3), (x0 + 4, y1 - 3)], fill=(40, 46, 48))   # блик на стекле
    d.rectangle([x0 + 10, y1 + 10, x1 - 10, y1 + 15], fill=GREY_VD)                                           # прорезь
    d.rectangle([x0 + 14, y1 + 8, x1 - 14, y1 + 10], fill=GREY_L)                                             # язычок
d.rectangle([W - 22, int(4.6 * U), W - 14, int(5.4 * U)], fill=GREY_VD)                                       # защёлка
save(scratches(im), "ep_meter")

# --- правая высокая дверь: круглая ручка слева
W, H = int(7 * U), int(16 * U)
im = plate(W, H, 11); d = ImageDraw.Draw(im)
hx, hy = int(1.3 * U), H // 2
d.ellipse([hx - 17, hy - 17, hx + 17, hy + 17], fill=GREY_VD); d.ellipse([hx - 13, hy - 13, hx + 13, hy + 13], fill=GREY_D)
save(scratches(im, 20), "ep_door_right")

# --- обратная сторона открытой дверцы: схема на бумаге
W, H = int(9 * U), int(6.8 * U)
im = plate(W, H, 9, False); d = ImageDraw.Draw(im)
d.rectangle([26, 26, W - 26, H - 26], fill=(226, 224, 212))
for i in range(7):
    d.line([32, 34 + i * 12, W - 32, 34 + i * 12], fill=(92, 92, 98), width=1)
for i in range(7):
    d.line([32 + i * 22, 34, 32 + i * 22, H - 32], fill=(132, 132, 138), width=1)
d.rectangle([34, 36, 66, 46], fill=(40, 40, 46))
save(im, "ep_door_back")

# --- задняя плата проёма
W, H = int(7.2 * U), int(6.0 * U)
im = Image.new("RGB", (W, H), (26, 28, 30)); d = ImageDraw.Draw(im)
d.rectangle([2, int(2.6 * U), W - 2, int(2.6 * U) + 9], fill=(150, 154, 158))           # DIN-рейка
d.rectangle([2, int(4.4 * U), W - 2, int(4.4 * U) + 9], fill=(130, 134, 138))
save(im, "ep_interior", dirty=False)

# --- автомат (1.4 x 3.6): белый корпус с экранами и надписями
W, H = int(1.4 * U), int(3.6 * U)
im = Image.new("RGB", (W, H), WHITE); d = ImageDraw.Draw(im)
d.rectangle([0, 0, W - 1, H - 1], outline=(140, 140, 140), width=2)
d.rectangle([W // 2 - 4, 8, W // 2 + 4, 15], fill=GREY_VD)
d.rectangle([W // 2 - 4, H - 15, W // 2 + 4, H - 8], fill=GREY_VD)
d.rectangle([W // 2 - 5, 22, W // 2 + 5, H - 22], fill=(206, 206, 200))                 # паз под рычажок
d.line([4, 18, W - 4, 18], fill=(120, 120, 124), width=1); d.line([4, H - 19, W - 4, H - 19], fill=(120, 120, 124), width=1)
save(im, "ep_breaker", dirty=False)

# --- жёлтый рычажок, ручка, провода, розетка
im = Image.new("RGB", (16, 16), YELLOW); d = ImageDraw.Draw(im); d.rectangle([0, 0, 15, 4], fill=(255, 226, 84)); d.rectangle([0, 12, 15, 15], fill=(198, 152, 10))
save(im, "ep_handle", dirty=False)
im = Image.new("RGB", (16, 16), GREY_L); d = ImageDraw.Draw(im); d.rectangle([0, 11, 15, 15], fill=GREY_D); d.rectangle([2, 2, 6, 6], fill=(156, 160, 154))
save(im, "ep_knob", dirty=False)
save(Image.new("RGB", (8, 8), (62, 96, 200)), "ep_wire_blue", dirty=False)
save(Image.new("RGB", (8, 8), (176, 178, 184)), "ep_wire_gray", dirty=False)
im = Image.new("RGB", (24, 24), WHITE); d = ImageDraw.Draw(im); d.rectangle([0, 0, 23, 23], outline=(150, 150, 150), width=2)
d.ellipse([5, 9, 9, 13], fill=GREY_VD); d.ellipse([14, 9, 18, 13], fill=GREY_VD); d.rectangle([10, 3, 13, 5], fill=GREY_D)
save(im, "ep_socket", dirty=False)

# ---------------------------------------------------------------- модели
TEXTURES = {"particle": "babushka:block/ep_body"}
for k in ("body", "rim", "cavity", "door_top", "meter", "door_right", "door_back", "interior",
          "breaker", "handle", "knob", "wire_blue", "wire_gray", "socket"):
    TEXTURES[k] = "babushka:block/ep_" + k

def face(tex, uv=(0, 0, 16, 16)):
    return {"uv": list(uv), "texture": tex}

def box(x0, x1, y0, y1, z0, z1, faces):
    return {"from": [round(x0, 3), round(y0, 3), round(z0, 3)], "to": [round(x1, 3), round(y1, 3), round(z1, 3)], "faces": faces}

def small(tex):
    return {k: face(tex, (0, 0, 4, 4)) for k in ("north", "south", "east", "west", "up", "down")}

def plate_el(u0, u1, y0, y1, tex):
    x0, x1 = X(u0, u1)
    f = {"north": face(tex)}
    f.update({"up": face("#body", (0, 0, round(x1 - x0, 2), 1)), "down": face("#body", (0, 0, round(x1 - x0, 2), 1)),
              "east": face("#body", (0, 0, 1, round(y1 - y0, 2))), "west": face("#body", (0, 0, 1, round(y1 - y0, 2)))})
    return box(x0, x1, y0, y1, 8, 9, f)

def rim_els(u0, u1, y0, y1, t=0.45, p=0.3):
    """выступающий ободок вокруг дверцы/панели (объём)"""
    E = []
    a0, a1 = u0 + 0.35, u1 - 0.35
    b0, b1 = y0 + 0.35, y1 - 0.35
    def strip(ua, ub, ya, yb):
        xa, xb = X(ua, ub)
        E.append(box(xa, xb, ya, yb, 8 - p, 8, {"north": face("#rim", (0, 0, 8, 2)), "up": face("#rim", (0, 0, 8, 1)),
                                                  "down": face("#rim", (0, 0, 8, 1)), "east": face("#rim", (0, 0, 1, 2)), "west": face("#rim", (0, 0, 1, 2))}))
    strip(a0, a1, b1 - t, b1); strip(a0, a1, b0, b0 + t)
    strip(a0, a0 + t, b0 + t, b1 - t); strip(a1 - t, a1, b0 + t, b1 - t)
    return E

def common():
    E = []
    # правая высокая дверь + ручка + петли
    E.append(plate_el(*RIGHT_U, 0, 16, "#door_right"))
    E += rim_els(*RIGHT_U, 0, 16)
    x0, x1 = X(9.7, 11.0)
    E.append(box(x0, x1, 7.4, 8.6, 7.2, 8, small("#knob")))
    for yy in (3.0, 11.8):
        xa, xb = X(15.4, 16.0)
        E.append(box(xa, xb, yy, yy + 1.2, 7.7, 8.3, small("#knob")))
    # панель счётчиков + ободок + защёлка
    E.append(plate_el(*DOOR_U, *METER_Y, "#meter"))
    E += rim_els(*DOOR_U, *METER_Y)
    xa, xb = X(8.6, 8.9)
    E.append(box(xa, xb, 10.0 - 4.2 + 0.0, 10.0 - 3.7, 7.5, 8, small("#knob")))
    return E

def body_closed():
    return [box(0, 16, 0, 16, 9, 16, {"east": face("#body", (0, 0, 7, 16)), "west": face("#body", (0, 0, 7, 16)),
                                      "south": face("#body"), "up": face("#body", (0, 0, 16, 7)), "down": face("#body", (0, 0, 16, 7))})]

def closed_model():
    E = body_closed() + common()
    E.append(plate_el(*DOOR_U, *DOOR_Y, "#door_top"))
    E += rim_els(*DOOR_U, *DOOR_Y)
    xa, xb = X(7.6, 8.6)
    E.append(box(xa, xb, 12.3, 13.3, 7.3, 8, small("#knob")))                         # замок
    for yy in (10.4, 14.2):                                                           # петли слева
        xa, xb = X(0.0, 0.6)
        E.append(box(xa, xb, yy, yy + 1.2, 7.7, 8.3, small("#knob")))
    return E

def open_model(powered):
    E = common()
    cx0, cx1 = X(*CAV_U)
    cy0, cy1 = CAV_Y
    # корпус с выемкой под автоматы
    E.append(box(0, 16, 0, cy0, 9, 16, {"east": face("#body", (0, 0, 7, 9)), "west": face("#body", (0, 0, 7, 9)), "south": face("#body"),
                                         "down": face("#body", (0, 0, 16, 7)), "up": face("#cavity")}))
    E.append(box(0, 16, cy1, 16, 9, 16, {"east": face("#body", (0, 0, 7, 1)), "west": face("#body", (0, 0, 7, 1)), "south": face("#body"),
                                          "up": face("#body", (0, 0, 16, 7)), "down": face("#cavity")}))
    E.append(box(cx1, 16, cy0, cy1, 9, 16, {"east": face("#body", (0, 0, 7, 6)), "south": face("#body"), "west": face("#cavity")}))
    E.append(box(0, cx0, cy0, cy1, 9, 16, {"west": face("#body", (0, 0, 7, 6)), "south": face("#body"), "east": face("#cavity")}))
    E.append(box(cx0, cx1, cy0, cy1, 13, 16, {"north": face("#interior"), "south": face("#body")}))
    # рамка проёма на уровне дверцы
    ua, ub = DOOR_U
    for (a, b, c, d_) in ((ua, ub, cy1, DOOR_Y[1]), (ua, ub, DOOR_Y[0], cy0), (ua, CAV_U[0], cy0, cy1), (CAV_U[1], ub, cy0, cy1)):
        xa, xb = X(a, b)
        E.append(box(xa, xb, c, d_, 8, 9, {"north": face("#rim", (0, 0, 8, 2)), "up": face("#body", (0, 0, 8, 1)), "down": face("#body", (0, 0, 8, 1)),
                                           "east": face("#body", (0, 0, 1, 2)), "west": face("#body", (0, 0, 1, 2))}))
    # розетка и провода
    xa, xb = X(6.2, 7.4)
    E.append(box(xa, xb, 14.2, 15.2, 11.4, 13, {"north": face("#socket"), "up": face("#socket", (0, 0, 4, 2)), "down": face("#socket", (0, 0, 4, 2)),
                                                "east": face("#socket", (0, 0, 2, 4)), "west": face("#socket", (0, 0, 2, 4))}))
    xa, xb = X(1.2, 6.0)
    E.append(box(xa, xb, 14.8, 15.1, 11.0, 11.3, small("#wire_gray")))
    xa, xb = X(1.0, 7.2)
    E.append(box(xa, xb, 9.55, 9.85, 11.0, 11.3, small("#wire_blue")))
    for u in (1.5, 4.6):
        xa, xb = X(u, u + 0.3)
        E.append(box(xa, xb, 13.6, 14.9, 11.0, 11.3, small("#wire_blue")))
    # 4 автомата; жёлтый рычажок двигается только у второго слева
    for (u0, u1), st in zip(MODULES, HANDLE_STATE):
        xa, xb = X(u0, u1)
        E.append(box(xa, xb, MOD_Y[0], MOD_Y[1], 9.3, 13, {
            "north": face("#breaker"), "up": face("#breaker", (0, 0, 2, 2)), "down": face("#breaker", (0, 0, 2, 2)),
            "east": face("#breaker", (0, 0, 2, 2)), "west": face("#breaker", (0, 0, 2, 2))}))
        if st == "move":
            y0, y1 = H_UP if powered else H_DOWN       # вкл — вверху, выкл — внизу
        elif st == "up":
            y0, y1 = H_UP
        else:
            y0, y1 = H_DOWN
        c = (u0 + u1) / 2
        ha, hb = X(c - 0.45, c + 0.45)
        E.append(box(ha, hb, y0, y1, 8.7, 9.3, {k: face("#handle") for k in ("north", "up", "down", "east", "west")}))
    # открытая дверца: повёрнута на 90° вокруг ЛЕВОЙ (для игрока) петли и торчит вперёд
    E.append(box(15.2, 16, DOOR_Y[0], DOOR_Y[1], 0, 9, {"east": face("#door_top"), "west": face("#door_back"),
                                                          "north": face("#body", (0, 0, 1, 7)), "up": face("#body", (0, 0, 1, 9))}))
    return E

def write_model(name, elements):
    json.dump({"parent": "minecraft:block/block", "textures": TEXTURES, "elements": elements},
              open(os.path.join(RES, "models/block", name + ".json"), "w"), indent=1)

models = {"electrical_panel_closed": closed_model(), "electrical_panel_open_off": open_model(False), "electrical_panel_open_on": open_model(True)}
for n, e in models.items():
    write_model(n, e)

rot = {"north": 0, "east": 90, "south": 180, "west": 270}
variants = {}
for f, y in rot.items():
    for o in ("false", "true"):
        for p in ("false", "true"):
            m = "electrical_panel_closed" if o == "false" else ("electrical_panel_open_on" if p == "true" else "electrical_panel_open_off")
            v = {"model": "babushka:block/" + m}
            if y: v["y"] = y
            variants["facing=%s,open=%s,powered=%s" % (f, o, p)] = v
json.dump({"variants": variants}, open(os.path.join(RES, "blockstates/electrical_panel.json"), "w"), indent=1)
json.dump({"model": {"type": "minecraft:model", "model": "babushka:block/electrical_panel_closed"}},
          open(os.path.join(RES, "items/electrical_panel.json"), "w"), indent=1)

# ---------------------------------------------------------------- превью-рендер JSON-моделей
def load_tex(n):
    return np.asarray(Image.open(os.path.join(TEX, n + ".png")).convert("RGBA"))
TEXCACHE = {}
def tex_of(var):
    n = TEXTURES[var.lstrip("#")].split("ep_")[-1]
    if n not in TEXCACHE: TEXCACHE[n] = load_tex("ep_" + n)
    return TEXCACHE[n]

def face_points(el, name, n):
    (x0, y0, z0), (x1, y1, z1) = el["from"], el["to"]
    a, b = np.meshgrid(np.linspace(0, 1, n), np.linspace(0, 1, n))   # a: вдоль u, b: вдоль v
    if name == "north": P = np.stack([x1 - a * (x1 - x0), y1 - b * (y1 - y0), np.full_like(a, z0)], -1)
    if name == "south": P = np.stack([x0 + a * (x1 - x0), y1 - b * (y1 - y0), np.full_like(a, z1)], -1)
    if name == "west":  P = np.stack([np.full_like(a, x0), y1 - b * (y1 - y0), z0 + a * (z1 - z0)], -1)
    if name == "east":  P = np.stack([np.full_like(a, x1), y1 - b * (y1 - y0), z1 - a * (z1 - z0)], -1)
    if name == "up":    P = np.stack([x0 + a * (x1 - x0), np.full_like(a, y1), z0 + b * (z1 - z0)], -1)
    if name == "down":  P = np.stack([x0 + a * (x1 - x0), np.full_like(a, y0), z1 - b * (z1 - z0)], -1)
    return P, a, b

def render(elements, yaw, pitch, S=22, path="/tmp/r.png"):
    PX, PY, DD, CC = [], [], [], []
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    for el in elements:
        for name, f in el["faces"].items():
            ext = max(el["to"][i] - el["from"][i] for i in range(3))
            n = max(6, int(ext * S * 1.5))
            P, a, b = face_points(el, name, n)
            tx = tex_of(f["texture"]); th, tw = tx.shape[:2]
            u0, v0, u1, v1 = f["uv"]
            uu = (u0 + a * (u1 - u0)) / 16 * tw; vv = (v0 + b * (v1 - v0)) / 16 * th
            col = tx[np.clip(vv.astype(int), 0, th - 1), np.clip(uu.astype(int), 0, tw - 1)]
            p = P.reshape(-1, 3) - 8.0
            # поворот вокруг Y (yaw), затем наклон (pitch); камера смотрит со стороны севера (z<0) на юг; игрок-слева = большой x
            x = p[:, 0] * cy + p[:, 2] * sy; z = -p[:, 0] * sy + p[:, 2] * cy
            y = p[:, 1] * cp - z * sp; zz = p[:, 1] * sp + z * cp
            PX.append((-x * S + 20 * S).astype(int)); PY.append((-y * S + 12 * S).astype(int)); DD.append(-zz.copy() * 0 + zz)
            CC.append(col.reshape(-1, 4))
    PX = np.concatenate(PX); PY = np.concatenate(PY); DD = np.concatenate(DD); CC = np.concatenate(CC)
    Wd, Hd = 40 * S, 26 * S
    img = np.zeros((Hd, Wd, 4), np.uint8); img[:] = (205, 210, 215, 255)
    ok = (PX >= 0) & (PX < Wd) & (PY >= 0) & (PY < Hd) & (CC[:, 3] > 0)
    o = np.argsort(DD[ok])[::-1]   # дальние первыми
    img[PY[ok][o], PX[ok][o]] = CC[ok][o]
    Image.fromarray(img).save(path)

views = []
for i, (n, e) in enumerate(models.items()):
    render(e, 0, 0, 16, "/tmp/pv_front_%d.png" % i)
    render(e, -35, 25, 16, "/tmp/pv_obl_%d.png" % i)
rowsW = 40 * 16
canvas = Image.new("RGB", (rowsW * 3, 26 * 16 * 2), (255, 255, 255))
for i in range(3):
    canvas.paste(Image.open("/tmp/pv_front_%d.png" % i).convert("RGB"), (i * rowsW, 0))
    canvas.paste(Image.open("/tmp/pv_obl_%d.png" % i).convert("RGB"), (i * rowsW, 26 * 16))
canvas = canvas.crop((0, 0, canvas.width, canvas.height))
canvas.save("/home/claude/panel_preview.png")
print("ok", sum(len(e) for e in models.values()) // 3, "элементов в среднем")
