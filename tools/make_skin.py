"""Рисует скин 64x64 (слим-руки): бабушка в красном платье с цветами, тапочки, седеющие волосы."""
from PIL import Image
import random

random.seed(7)
img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
px = img.load()

SKIN = (226, 176, 146, 255)
SKIN_D = (205, 150, 122, 255)      # морщинки / тени
HAIR = (116, 84, 62, 255)
HAIR_G = (168, 154, 142, 255)      # седина
RED = (190, 36, 34, 255)
RED_D = (150, 24, 26, 255)
CREAM = (232, 212, 196, 255)
PINK = (214, 150, 140, 255)
SLIP = (184, 150, 124, 255)
SLIP_D = (150, 118, 96, 255)
EYE = (74, 90, 120, 255)
WHITE = (240, 240, 240, 255)
BROW = (96, 70, 52, 255)
MOUTH = (110, 40, 40, 255)

def rect(x0, y0, x1, y1, c):
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[x, y] = c

def floral(x0, y0, x1, y1):
    """красная ткань + кремовые цветы (крестик с розовым центром)"""
    rect(x0, y0, x1, y1, RED)
    for y in range(y0, y1):
        for x in range(x0, x1):
            if random.random() < 0.10:
                px[x, y] = RED_D
    cy = y0 + 1
    row = 0
    while cy < y1:
        cx = x0 + (1 if row % 2 == 0 else 3)
        while cx < x1:
            for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
                X, Y = cx + dx, cy + dy
                if x0 <= X < x1 and y0 <= Y < y1:
                    px[X, Y] = CREAM
            if x0 <= cx < x1 and y0 <= cy < y1:
                px[cx, cy] = PINK
            cx += 4
        cy += 3
        row += 1

# ---------- ГОЛОВА ----------
rect(0, 8, 32, 16, SKIN)
rect(8, 0, 24, 8, HAIR)                       # макушка и низ головы
rect(16, 0, 24, 8, SKIN)                      # низ головы (подбородок)
rect(8, 0, 16, 8, HAIR)                       # верх головы - волосы
for (x0, x1) in ((0, 8), (16, 24)):           # боковые грани: волосы сверху, ухо/щека ниже
    rect(x0, 8, x1, 11, HAIR)
rect(24, 8, 32, 16, HAIR)                     # затылок
rect(24, 14, 32, 16, HAIR)
# передняя грань (8..16, 8..16)
rect(8, 8, 16, 16, SKIN)
rect(8, 8, 16, 10, HAIR)
px[8, 10] = HAIR; px[15, 10] = HAIR
# брови, глаза
for x in (9, 10, 13, 14):
    px[x, 11] = BROW
px[9, 12] = WHITE; px[10, 12] = EYE
px[13, 12] = EYE;  px[14, 12] = WHITE
# нос, щёки, морщинки
px[11, 13] = SKIN_D; px[12, 13] = SKIN_D
px[9, 14] = SKIN_D;  px[14, 14] = SKIN_D
px[9, 13] = SKIN_D
# рот "О" - удивлённое выражение
px[11, 14] = MOUTH; px[12, 14] = MOUTH
px[11, 15] = SKIN_D; px[12, 15] = SKIN_D
# седые пряди
for (x, y) in ((9, 8), (12, 9), (14, 8), (10, 9), (11, 8)):
    px[x, y] = HAIR_G
for _ in range(14):
    px[random.randint(8, 31), random.choice((8, 9))] = HAIR_G
for _ in range(10):
    px[random.randint(8, 15), random.randint(0, 7)] = HAIR_G
for _ in range(10):
    px[random.randint(24, 31), random.randint(8, 15)] = HAIR_G

# ---------- ТЕЛО (платье) ----------
floral(16, 20, 40, 32)
rect(16, 16, 36, 20, RED)                     # верх/низ
# декольте: кожа + красная окантовка (передняя грань 20..28, 20..32)
for y, (a, b) in enumerate(((22, 26), (22, 26), (23, 25))):
    for x in range(a, b):
        px[x, 20 + y] = SKIN
px[21, 20] = RED_D; px[26, 20] = RED_D
px[22, 23] = RED_D; px[25, 23] = RED_D; px[23, 24] = RED_D; px[24, 24] = RED_D
rect(22, 20, 26, 22, SKIN)
# подол - чуть темнее
for x in range(16, 40):
    px[x, 31] = RED_D if x % 2 else RED

# ---------- РУКИ (голая кожа, лямки платья сверху) ----------
rect(40, 16, 56, 32, SKIN)                    # правая рука
rect(32, 48, 48, 64, SKIN)                    # левая рука
for (ox, oy) in ((40, 16), (32, 48)):
    rect(ox + 4, oy, ox + 14, oy + 4, SKIN)
    for yy in (oy + 4, oy + 5):               # лямка платья на плече
        for xx in range(ox + 4, ox + 14):
            px[xx, yy] = RED
    for _ in range(6):
        px[random.randint(ox + 4, ox + 13), random.randint(oy + 6, oy + 15)] = SKIN_D

# ---------- НОГИ ----------
def leg(ox, oy):
    rect(ox, oy, ox + 16, oy + 16, SKIN)
    # верх ноги - край платья (3 пикселя вниз по граням)
    for yy in range(oy + 4, oy + 7):
        for xx in range(ox, ox + 16):
            px[xx, yy] = RED
    for xx in range(ox, ox + 16):
        px[xx, oy + 6] = RED_D
    # тапочки - низ 2 пикселя
    for yy in range(oy + 14, oy + 16):
        for xx in range(ox, ox + 16):
            px[xx, yy] = SLIP
    # цветочек на мыске (передняя грань ox+4..ox+8)
    px[ox + 5, oy + 14] = CREAM; px[ox + 6, oy + 14] = PINK
    for xx in range(ox + 4, ox + 8):
        px[xx, oy + 15] = SLIP_D
    # подошва (нижняя грань)
    rect(ox + 8, oy, ox + 12, oy + 4, SLIP_D)
    rect(ox + 4, oy, ox + 8, oy + 4, RED)
    for _ in range(4):
        px[random.randint(ox, ox + 15), random.randint(oy + 7, oy + 13)] = SKIN_D
leg(0, 16)
leg(16, 48)

img.save("/home/claude/babushka-mod/src/main/resources/assets/babushka/textures/entity/babushka.png")
img.resize((512, 512), Image.NEAREST).save("/home/claude/skin_preview.png")
print("ok")
