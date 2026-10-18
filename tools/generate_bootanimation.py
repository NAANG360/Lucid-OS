#!/usr/bin/env python3
"""Generate a lightweight LucidOS bootanimation from the selected Figma wallpaper."""
import math
import os
import sys
import zipfile
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageOps

if len(sys.argv) != 3:
    raise SystemExit("usage: generate_bootanimation.py WALLPAPER_JPG OUTPUT_ZIP")

source_path, output_zip = sys.argv[1:]
WIDTH, HEIGHT = 540, 1170
FPS = 24
FRAME_COUNT = 36
BLUE = (8, 102, 255)
LAVENDER = (143, 132, 232)
CYAN = (118, 217, 235)
INK = (13, 12, 31)

source = Image.open(source_path).convert("RGB")
font_path = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
small_font_path = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
font = ImageFont.truetype(font_path, 35)
small_font = ImageFont.truetype(small_font_path, 12)

def ease(t):
    return t * t * (3 - 2 * t)

def make_background(t):
    zoom = 1.0 + 0.035 * ease(t)
    scaled = ImageOps.fit(
        source,
        (int(WIDTH * zoom), int(HEIGHT * zoom)),
        method=Image.Resampling.LANCZOS,
        centering=(0.5, 0.42),
    )
    left = (scaled.width - WIDTH) // 2
    top = (scaled.height - HEIGHT) // 2
    frame = scaled.crop((left, top, left + WIDTH, top + HEIGHT)).convert("RGBA")

    shade = Image.new("RGBA", (WIDTH, HEIGHT), (INK[0], INK[1], INK[2], 48))
    frame = Image.alpha_composite(frame, shade)

    # Subtle top/bottom vignette keeps the wordmark readable without hiding the art.
    gradient = Image.new("RGBA", (1, HEIGHT), (0, 0, 0, 0))
    pixels = []
    for y in range(HEIGHT):
        distance = min(y / HEIGHT, 1 - y / HEIGHT)
        alpha = int(88 * (1 - min(distance / 0.28, 1)))
        pixels.append((8, 7, 25, alpha))
    gradient.putdata(pixels)
    gradient = gradient.resize((WIDTH, HEIGHT))
    return Image.alpha_composite(frame, gradient)

def draw_centered(draw, y, text, font_obj, fill, letter_spacing=0):
    widths = [draw.textlength(ch, font=font_obj) for ch in text]
    total = sum(widths) + max(0, len(text)-1) * letter_spacing
    x = (WIDTH - total) / 2
    for ch, width in zip(text, widths):
        draw.text((int(x), y), ch, font=font_obj, fill=fill, stroke_width=0)
        x += width + letter_spacing

frames = []
for i in range(FRAME_COUNT):
    t = i / max(1, FRAME_COUNT - 1)
    image = make_background(t).convert("RGBA")
    overlay = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)

    fade = min(1.0, t / 0.16, (1.0 - t) / 0.16)
    fade = max(0.0, fade)
    alpha = int(255 * ease(fade))

    # Quiet orbital mark.
    cx, cy = WIDTH // 2, int(HEIGHT * 0.48)
    pulse = 1.0 + 0.06 * math.sin(t * math.pi * 2)
    radius = int(29 * pulse)
    draw.ellipse((cx-radius, cy-radius, cx+radius, cy+radius),
                 outline=(*LAVENDER, int(alpha * 0.92)), width=2)
    draw.arc((cx-radius-10, cy-radius-10, cx+radius+10, cy+radius+10),
             start=205 + int(t*80), end=320 + int(t*80),
             fill=(*CYAN, int(alpha * 0.95)), width=3)
    dot_angle = t * math.pi * 2
    dot_x = cx + int((radius + 10) * math.cos(dot_angle))
    dot_y = cy + int((radius + 10) * math.sin(dot_angle))
    draw.ellipse((dot_x-3, dot_y-3, dot_x+3, dot_y+3), fill=(*BLUE, alpha))

    draw_centered(draw, int(HEIGHT * 0.53), "LUCID OS", font, (255,255,255,alpha), 2)
    draw_centered(draw, int(HEIGHT * 0.53) + 52, "A CLEARER KIND OF ANDROID",
                  small_font, (224,224,246,int(alpha*0.88)), 2)

    image = Image.alpha_composite(image, overlay).convert("RGB")
    frames.append(image)

os.makedirs(os.path.dirname(os.path.abspath(output_zip)), exist_ok=True)
with zipfile.ZipFile(output_zip, "w", compression=zipfile.ZIP_STORED) as z:
    z.writestr("desc.txt", f"{WIDTH} {HEIGHT} {FPS}\np 1 0 part0\n")
    for i, frame in enumerate(frames):
        import io
        buffer = io.BytesIO()
        frame.save(buffer, format="PNG", optimize=True)
        z.writestr(f"part0/{i:05d}.png", buffer.getvalue(), compress_type=zipfile.ZIP_STORED)
print(f"Created {output_zip}: {WIDTH}x{HEIGHT}, {FRAME_COUNT} frames at {FPS} fps.")
