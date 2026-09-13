"""Generate the Neko GPS launcher icon set.

Design: Mysteria Purple (#1b1938) rounded square with a Lavender Glow (#cbb7fb)
cat-paw print — on brand for a private GPS app named Neko.

Outputs:
  - mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png      legacy full-bleed
  - mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_round.png legacy round
  - drawable/ic_launcher_foreground.xml                          adaptive foreground (vector)
  - values/colors.xml entry ic_launcher_background               adaptive background
"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # repo root
RES = os.path.join(ROOT, "app", "src", "main", "res")

BG = (27, 25, 56, 255)        # #1b1938 Mysteria Purple
PAW = (203, 183, 251, 255)    # #cbb7fb Lavender Glow

DENSITIES = {
    "mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192,
}


def draw_paw(d: ImageDraw, size: int, cx: float, cy: float, scale: float) -> None:
    """Draw a paw (main pad + 4 toes) centred near (cx, cy)."""
    s = size / 108.0 * scale
    # Main pad
    pad_w, pad_h = 30 * s, 24 * s
    d.ellipse([cx - pad_w / 2, cy - pad_h / 2, cx + pad_w / 2, cy + pad_h / 2], fill=PAW)
    # Toes
    toes = [(-22, -26), (-8, -34), (8, -34), (22, -26)]
    r = 7.5 * s
    for dx, dy in toes:
        d.ellipse([cx + dx * s - r, cy + dy * s - r, cx + dx * s + r, cy + dy * s + r], fill=PAW)


def rounded_mask(size: int, radius_ratio: float = 0.22) -> Image:
    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    r = int(size * radius_ratio)
    d.rounded_rectangle([0, 0, size - 1, size - 1], radius=r, fill=255)
    return mask


def legacy_icon(size: int, round_: bool) -> Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if round_:
        d.ellipse([0, 0, size - 1, size - 1], fill=BG)
        mask = None
    else:
        d.rounded_rectangle([0, 0, size - 1, size - 1], radius=int(size * 0.22), fill=BG)
        mask = rounded_mask(size)
    draw_paw(d, size, size * 0.5, size * 0.60, scale=1.0)
    if mask is not None:
        img.putalpha(mask)
    return img


def main() -> None:
    for folder, size in DENSITIES.items():
        out_dir = os.path.join(RES, f"mipmap-{folder}")
        os.makedirs(out_dir, exist_ok=True)
        legacy_icon(size, round_=False).save(os.path.join(out_dir, "ic_launcher.png"))
        legacy_icon(size, round_=True).save(os.path.join(out_dir, "ic_launcher_round.png"))
        print(f"  wrote mipmap-{folder}/ (ic_launcher.png + ic_launcher_round.png @ {size}px)")

    # Adaptive icon XML (API 26+)
    anydpi = os.path.join(RES, "mipmap-anydpi-v26")
    os.makedirs(anydpi, exist_ok=True)
    adaptive = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
"""
    with open(os.path.join(anydpi, "ic_launcher.xml"), "w") as f:
        f.write(adaptive)
    with open(os.path.join(anydpi, "ic_launcher_round.xml"), "w") as f:
        f.write(adaptive)
    print("  wrote mipmap-anydpi-v26/ic_launcher.xml + ic_launcher_round.xml")

    # Vector foreground: paw in the 66dp safe zone (centre 54,54, radius 33)
    fg = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- main pad -->
    <path
        android:fillColor="#cbb7fb"
        android:pathData="M54,74 C44,74 37,66 37,58 C37,48 45,42 54,42 C63,42 71,48 71,58 C71,66 64,74 54,74 Z" />
    <!-- toes -->
    <path android:fillColor="#cbb7fb" android:pathData="M30,44 m-7,0 a7,7 0 1,0 14,0 a7,7 0 1,0 -14,0" />
    <path android:fillColor="#cbb7fb" android:pathData="M45,36 m-7,0 a7,7 0 1,0 14,0 a7,7 0 1,0 -14,0" />
    <path android:fillColor="#cbb7fb" android:pathData="M63,36 m-7,0 a7,7 0 1,0 14,0 a7,7 0 1,0 -14,0" />
    <path android:fillColor="#cbb7fb" android:pathData="M78,44 m-7,0 a7,7 0 1,0 14,0 a7,7 0 1,0 -14,0" />
</vector>
"""
    with open(os.path.join(RES, "drawable", "ic_launcher_foreground.xml"), "w") as f:
        f.write(fg)
    print("  wrote drawable/ic_launcher_foreground.xml")

    # Background colour
    colors_path = os.path.join(RES, "values", "colors.xml")
    if os.path.exists(colors_path):
        with open(colors_path) as f:
            colors = f.read()
        if "ic_launcher_background" not in colors:
            colors = colors.replace("</resources>",
                                    '    <color name="ic_launcher_background">#1b1938</color>\n</resources>')
            with open(colors_path, "w") as f:
                f.write(colors)
            print("  added ic_launcher_background to colors.xml")
    else:
        with open(colors_path, "w") as f:
            f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
                    '    <color name="ic_launcher_background">#1b1938</color>\n</resources>\n')
        print("  created colors.xml with ic_launcher_background")

    print("\nIcon set generated.")


if __name__ == "__main__":
    main()
