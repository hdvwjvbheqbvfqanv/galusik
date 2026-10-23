"""
Генерирует электрощиток: текстуры, JSON-модели (дверца закрыта / открыта, автоматы выкл/вкл),
blockstate и модель предмета. Запуск: python3 tools/make_panel.py  (нужны Pillow и numpy)
Размеры блока: 16 x 16 пикселей по лицевой стороне, глубина 8 (как основание рычага).
"""
import json, os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "../src/main/resources/assets/babushka")
TEX = os.path.join(RES, "textures/block")
os.makedirs(TEX, exist_ok=True)
os.makedirs(os.path.join(RES, "models/block"), exist_ok=True)
os.makedirs(os.path.join(RES, "blockstates"), exist_ok=True)
rng = np.random.default_rng(5)
U = 20  # пикселей на юнит модели

GREY = (84, 88, 82); GREY_D = (58, 62, 58); GREY_L = (104, 108, 102); GREY_VD = (36, 38, 36)
YELLOW = (244, 196, 24); BLACK = (20, 20, 22); WHITE = (232, 232, 228)

def noisy(im, amp=5):
    a = np.asarray(im.convert("RGB")).astype(np.float32)
    n = rng.normal(0, amp, a.shape[:2])[..., None]
    return Image.fromarray(np.clip(a + n, 0, 255).astype(np.uint8))

def metal(w, h, rim=8):
    im = Image.new("RGB", (w, h), GREY_D)
    d = ImageDraw.Draw(im)
    d.rectangle([rim, rim, w - rim - 1, h - rim - 1], fill=GREY)
    d.rectangle([rim, rim, w - rim - 1, rim + 2], fill=GREY_L)            # блик сверху
    d.rectangle([rim, h - rim - 3, w - rim - 1, h - rim - 1], fill=GREY_D)  # тень снизу
    for (x, y) in ((rim // 2, rim // 2), (w - rim // 2, rim // 2), (rim // 2, h - rim // 2), (w - rim // 2, h - rim // 2)):
        d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=GREY_VD)             # заклёпки
    return im

def save(im, name):
    noisy(im).save(os.path.join(TEX, name + ".png"))

# --- корпус
im = Image.new("RGB", (64, 64), GREY); d = ImageDraw.Draw(im)
d.rectangle([0, 0, 63, 3], fill=GREY_L); d.rectangle([0, 60, 63, 63], fill=GREY_D)
save(im, "ep_body")

# --- верхняя дверца (8 x 7): наклейки, знак молнии, замок
W, H = 8 * U, 7 * U
im = metal(W, H); d = ImageDraw.Draw(im)
d.rectangle([14, 14, 50, 34], fill=(240, 200, 40));  [d.line([18, 20 + i * 5, 46, 20 + i * 5], fill=BLACK, width=2) for i in range(3)]
d.rectangle([14, 42, 50, 62], fill=(236, 220, 170)); d.rectangle([18, 46, 30, 58], fill=(190, 40, 40)); d.line([34, 48, 46, 48], fill=BLACK, width=2); d.line([34, 54, 46, 54], fill=BLACK, width=2)
d.rectangle([14, 70, 50, 96], fill=(236, 170, 40));  d.rectangle([20, 76, 44, 90], fill=(250, 240, 220))
d.polygon([(96, 24), (66, 80), (126, 80)], fill=BLACK)                      # знак опасности
d.polygon([(96, 32), (73, 75), (119, 75)], fill=(240, 200, 60))
d.polygon([(98, 40), (88, 60), (96, 60), (92, 74), (106, 54), (98, 54)], fill=BLACK)  # молния
d.ellipse([128, 56, 148, 76], fill=GREY_VD); d.ellipse([131, 59, 145, 73], fill=GREY_L); d.rectangle([136, 62, 140, 70], fill=GREY_VD)  # замок
save(im, "ep_door_top")

# --- счётчики (8 x 9): четыре окошка и прорези
W, H = 8 * U, 9 * U
im = metal(W, H); d = ImageDraw.Draw(im)
for (x, y) in ((1.0, 1.3), (4.5, 1.3), (1.0, 5.3), (4.5, 5.3)):
    X, Y = int(x * U), int(y * U)
    d.rectangle([X - 3, Y - 3, X + 2 * U + 3, Y + 2 * U + 3], fill=GREY_D)
    d.rectangle([X, Y, X + 2 * U, Y + 2 * U], fill=(18, 20, 22))
    d.rectangle([X + 4, Y + 4, X + 14, Y + 10], fill=(34, 38, 40))
    d.rectangle([X + 8, Y + 2 * U + 8, X + 2 * U - 8, Y + 2 * U + 12], fill=GREY_D)   # прорезь
d.rectangle([W - 18, int(4.3 * U), W - 12, int(4.9 * U)], fill=GREY_VD)             # защёлка
save(im, "ep_meter")

# --- правая высокая дверь (8 x 16)
W, H = 8 * U, 16 * U
im = metal(W, H, 10); d = ImageDraw.Draw(im)
d.ellipse([8, H // 2 - 14, 36, H // 2 + 14], fill=GREY_D); d.ellipse([12, H // 2 - 10, 32, H // 2 + 10], fill=GREY_L)
save(im, "ep_door_right")

# --- обратная сторона открытой дверцы (8 x 7): схема на бумаге
W, H = 8 * U, 7 * U
im = metal(W, H, 8); d = ImageDraw.Draw(im)
d.rectangle([22, 22, W - 22, H - 22], fill=(226, 224, 214))
for i in range(6):
    d.line([28, 30 + i * 14, W - 28, 30 + i * 14], fill=(90, 90, 96), width=1)
for i in range(5):
    d.line([28 + i * 22, 30, 28 + i * 22, H - 30], fill=(130, 130, 136), width=1)
save(im, "ep_door_back")

# --- задняя плата с проводами (7 x 6)
W, H = 7 * U, 6 * U
im = Image.new("RGB", (W, H), (28, 30, 32)); d = ImageDraw.Draw(im)
d.rectangle([4, 56, W - 4, 66], fill=(150, 154, 158))                         # DIN-рейка
for i in range(0, 40):
    pass
d.line([6, 20, 40, 8, 70, 30, 100, 12, 134, 22], fill=(190, 190, 196), width=3)     # серый провод
d.line([6, 100, 44, 114, 80, 96, 110, 112, 134, 100], fill=(60, 90, 190), width=3)  # синий провод
d.line([60, 6, 62, 30], fill=(60, 90, 190), width=3)
d.rounded_rectangle([108, 36, 134, 82], radius=6, fill=(232, 232, 228))           # розетка
d.ellipse([114, 52, 128, 66], fill=(90, 90, 94)); d.ellipse([118, 56, 124, 62], fill=(30, 30, 32))
save(im, "ep_interior")

# --- автомат (1.6 x 4): белый корпус
W, H = int(1.6 * U), 4 * U
im = Image.new("RGB", (W, H), WHITE); d = ImageDraw.Draw(im)
d.rectangle([0, 0, W - 1, H - 1], outline=(150, 150, 150), width=2)
d.rectangle([W // 2 - 3, 12, W // 2 + 3, 20], fill=GREY_VD)
d.rectangle([W // 2 - 3, H - 20, W // 2 + 3, H - 12], fill=GREY_VD)
d.rectangle([W // 2 - 4, 26, W // 2 + 4, H - 26], fill=(205, 205, 200))       # паз под рычажок
d.line([4, 8, W - 4, 8], fill=(120, 120, 124), width=1)
save(im, "ep_breaker")

# --- рычажок автомата (жёлтый) и ручка-замок
im = Image.new("RGB", (16, 16), YELLOW); d = ImageDraw.Draw(im); d.rectangle([0, 0, 15, 4], fill=(255, 224, 80)); d.rectangle([0, 12, 15, 15], fill=(196, 150, 10))
save(im, "ep_handle")
im = Image.new("RGB", (16, 16), GREY_L); d = ImageDraw.Draw(im); d.rectangle([0, 11, 15, 15], fill=GREY_D); d.rectangle([2, 2, 6, 6], fill=(150, 154, 148))
save(im, "ep_knob")

# ============================================================== модели
def face(tex, uv=(0, 0, 16, 16)):
    return {"uv": list(uv), "texture": tex}

def el(frm, to, faces, comment=None):
    e = {"from": list(frm), "to": list(to), "faces": faces}
    if comment: e["__comment"] = comment
    return e

TEXTURES = {k: "babushka:block/" + k for k in ("ep_body", "ep_door_top", "ep_meter", "ep_door_right", "ep_door_back",
                                                 "ep_interior", "ep_breaker", "ep_handle", "ep_knob")}
TEXTURES["particle"] = "babushka:block/ep_body"
for k in list(TEXTURES):
    if k != "particle":
        TEXTURES[k.replace("ep_", "")] = TEXTURES.pop(k)
# переменные: body, door_top, meter, door_right, door_back, interior, breaker, handle, knob

def common():
    E = []
    # корпус (сзади)
    E.append(el([0, 0, 9], [16, 16, 16], {
        "east": face("#body", (0, 0, 7, 16)), "west": face("#body", (0, 0, 7, 16)),
        "south": face("#body"), "up": face("#body", (0, 0, 16, 7)), "down": face("#body", (0, 0, 16, 7))}))
    # панель счётчиков (низ-лево)
    E.append(el([0, 0, 8], [8, 9, 9], {"north": face("#meter"), "west": face("#body", (0, 0, 1, 9)), "down": face("#body", (0, 0, 8, 1))}))
    # правая высокая дверь
    E.append(el([8, 0, 8], [16, 16, 9], {"north": face("#door_right"), "east": face("#body", (0, 0, 1, 16)),
                                         "up": face("#body", (0, 0, 8, 1)), "down": face("#body", (0, 0, 8, 1))}))
    # ручка правой двери
    E.append(el([8.7, 7.4, 7.2], [10.1, 8.8, 8], {"north": face("#knob", (0, 0, 4, 4)), "up": face("#knob", (0, 0, 4, 2)),
                                                  "down": face("#knob", (0, 0, 4, 2)), "east": face("#knob", (0, 0, 2, 4)), "west": face("#knob", (0, 0, 2, 4))}))
    return E

def closed_model():
    E = common()
    E.append(el([0, 9, 8], [8, 16, 9], {"north": face("#door_top"), "west": face("#body", (0, 0, 1, 7)), "up": face("#body", (0, 0, 8, 1))}))
    E.append(el([6.2, 12.2, 7.3], [7.4, 13.4, 8], {"north": face("#knob", (0, 0, 4, 4)), "up": face("#knob", (0, 0, 4, 2)),
                                                    "down": face("#knob", (0, 0, 4, 2)), "east": face("#knob", (0, 0, 2, 4)), "west": face("#knob", (0, 0, 2, 4))}))
    return E

def open_model(powered):
    E = common()
    # рамка проёма
    E.append(el([0, 15.5, 8], [8, 16, 9], {"north": face("#body", (0, 0, 8, 0.5)), "up": face("#body", (0, 0, 8, 1)), "west": face("#body", (0, 0, 1, 0.5))}))
    E.append(el([0, 9, 8], [8, 9.5, 9], {"north": face("#body", (0, 0, 8, 0.5)), "down": face("#body", (0, 0, 8, 1)), "west": face("#body", (0, 0, 1, 0.5))}))
    E.append(el([0, 9.5, 8], [0.5, 15.5, 9], {"north": face("#body", (0, 0, 0.5, 6)), "west": face("#body", (0, 0, 1, 6))}))
    E.append(el([7.5, 9.5, 8], [8, 15.5, 9], {"north": face("#body", (0, 0, 0.5, 6))}))
    # задняя плата с проводами и розеткой
    E.append(el([0.5, 9.5, 9], [7.5, 15.5, 9.2], {"north": face("#interior")}))
    # три автомата и их рычажки
    for x in (0.9, 2.8, 4.7):
        E.append(el([x, 10.2, 7.8], [x + 1.6, 14.2, 9], {
            "north": face("#breaker"), "up": face("#breaker", (0, 0, 2, 2)), "down": face("#breaker", (0, 0, 2, 2)),
            "east": face("#breaker", (0, 0, 2, 2)), "west": face("#breaker", (0, 0, 2, 2))}))
        y0, y1 = (12.5, 13.7) if powered else (10.8, 12.0)      # вкл — рычажок вверху, выкл — внизу
        E.append(el([x + 0.4, y0, 7.3], [x + 1.2, y1, 7.8], {k: face("#handle") for k in ("north", "up", "down", "east", "west")}))
    # открытая дверца: повёрнута на 90° вокруг левой петли и торчит вперёд
    E.append(el([0, 9, 0], [1, 16, 8], {"west": face("#door_top", (16, 0, 0, 16)), "east": face("#door_back"),
                                        "north": face("#body", (0, 0, 1, 7)), "up": face("#body", (0, 0, 1, 8))}))
    return E

def write_model(name, elements):
    m = {"parent": "minecraft:block/block", "textures": TEXTURES, "elements": elements}
    json.dump(m, open(os.path.join(RES, "models/block", name + ".json"), "w"), indent=1)

write_model("electrical_panel_closed", closed_model())
write_model("electrical_panel_open_off", open_model(False))
write_model("electrical_panel_open_on", open_model(True))

# blockstate: facing x open x powered  (модели нарисованы лицом на север)
rot = {"north": 0, "east": 90, "south": 180, "west": 270}
variants = {}
for f, y in rot.items():
    for o in ("false", "true"):
        for p in ("false", "true"):
            if o == "false":
                m = "electrical_panel_closed"
            else:
                m = "electrical_panel_open_on" if p == "true" else "electrical_panel_open_off"
            v = {"model": "babushka:block/" + m}
            if y: v["y"] = y
            variants["facing=%s,open=%s,powered=%s" % (f, o, p)] = v
json.dump({"variants": variants}, open(os.path.join(RES, "blockstates/electrical_panel.json"), "w"), indent=1)
json.dump({"model": {"type": "minecraft:model", "model": "babushka:block/electrical_panel_closed"}},
          open(os.path.join(RES, "items/electrical_panel.json"), "w"), indent=1)

# превью: вид спереди (закрыто / открыто выкл / открыто вкл)
def load(n): return Image.open(os.path.join(TEX, n + ".png")).convert("RGB")
def front(open_, powered):
    c = Image.new("RGB", (16 * U, 16 * U), (200, 205, 210))
    c.paste(load("ep_meter"), (0, 7 * U)); c.paste(load("ep_door_right"), (8 * U, 0))
    if not open_:
        c.paste(load("ep_door_top"), (0, 0))
    else:
        c.paste(Image.new("RGB", (8 * U, 7 * U), GREY_D), (0, 0))
        c.paste(load("ep_interior"), (int(0.5 * U), int(0.5 * U)))
        br = load("ep_breaker"); hd = load("ep_handle")
        for x in (0.9, 2.8, 4.7):
            c.paste(br, (int(x * U), int(1.8 * U)))
            hy = 3.2 if powered else 5.0
            c.paste(hd.resize((int(0.8 * U), int(1.2 * U))), (int((x + 0.4) * U), int((hy - 1.0) * U)))
    return c
pv = Image.new("RGB", (16 * U * 3 + 20, 16 * U), (255, 255, 255))
for i, (o, p) in enumerate(((False, False), (True, False), (True, True))):
    pv.paste(front(o, p), (i * (16 * U + 10), 0))
pv.save("/home/claude/panel_preview.png")
print("ok")
