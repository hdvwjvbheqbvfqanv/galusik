"""Вырезает голову из фото (без торса и фона) для текстуры лица."""
import numpy as np
from PIL import Image, ImageFilter

import os
SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), "face_source.png")
CROP = (230, 130, 950, 1138)      # 720 x 1008  (5:7)

def _blur(a, r):
    im = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))
    return np.asarray(im.filter(ImageFilter.GaussianBlur(r))).astype(np.float32)

def face_arrays():
    """Возвращает (лицо RGB float, hair_col, skin_col, hair_patch)."""
    a = np.asarray(Image.open(SRC).convert("RGB").crop(CROP)).astype(np.float32)
    h, w, _ = a.shape
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    # цвета для заливки вокруг головы
    hair_patch = a[40:150, 200:520]
    hair_col = np.median(hair_patch.reshape(-1, 3), axis=0)
    skin_col = np.median(a[640:820, 90:190].reshape(-1, 3), axis=0)
    skin_col = skin_col * 0.75 + np.array([140, 105, 85], np.float32) * 0.25
    # суперэллипс головы
    cx, cy, rx, ry, n = w / 2 + 5, h * 0.50, w * 0.50, h * 0.50, 2.7
    d = (np.abs((xx - cx) / rx) ** n + np.abs((yy - cy) / ry) ** n) ** (1 / n)
    inside = np.clip((1.0 - d) / 0.07, 0, 1)            # мягкий край
    # заливка: сверху — цвет волос, снизу — цвет кожи/шеи (плавный переход)
    t = np.clip((yy - h * 0.40) / (h * 0.25), 0, 1)[..., None]
    fill = hair_col * (1 - t) + skin_col * t
    rng = np.random.default_rng(3)
    noise = _blur(rng.normal(0, 14, (h, w, 3)).astype(np.float32) + 128, 3) - 128
    fill = fill + noise * 0.8
    out = a * inside[..., None] + fill * (1 - inside[..., None])
    return out, hair_col, skin_col, hair_patch

def face_image():
    out, hair_col, skin_col, patch = face_arrays()
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8)), hair_col, skin_col

if __name__ == "__main__":
    img, hc, sc = face_image()
    img.save("/home/claude/face_crop_preview.png")
    print(img.size, hc, sc)
