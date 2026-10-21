#!/usr/bin/env python3
"""Generates the animated splash icon of Android 12+ (app/src/main/res/drawable-v31/ic_tada_splash.xml)
and sets windowSplashScreenAnimationDuration in app/src/main/res/values/themes.xml to match.

The logo (drawable/ic_tada, the katakana タ) is split into its strokes, the second one drawn as two
bars in the logo, so four parts. In the stroke order, each one slides in along its own axis, in the
direction it is written, lands with a small bounce along that axis, and the next one leaves once it
has landed; the logo pulses once whole.

Change the parameters below, then run from the repository root:  python scripts/splash_icon.py
It checks that a stroke is only visible once it is whole inside the circle the icon is clipped to.
With --preview DIR (needs Pillow) it also renders a frame every 50 ms.
"""
import math
import os
import re
import sys

# ---- parameters ------------------------------------------------------------------------------
LOGO_HEIGHT_DP = 140       # height of the assembled logo in the 288dp splash icon canvas
STROKE_MOVE_MS = 280       # flight of one stroke, bounce included
STROKE_PAUSE_MS = 50       # pause between a landing and the next stroke leaving
FADE_MS = 100              # fade-in of a stroke, once it is whole inside the circle
PULSE_UP_MS, PULSE_DOWN_MS, PULSE_SCALE = 90, 130, 1.025   # pulse of the whole logo at the end
# how far back along its axis each stroke starts, in ic_tada units (1 unit = LOGO_HEIGHT_DP / 308.77 dp).
# None: as far as the circle the icon is clipped to allows while the stroke stays whole inside it, so it
# is seen for the whole flight. A longer distance starts it outside, hidden until it is whole inside.
DISTANCE = {
    "stroke_left": None,
    "stroke_top": None,
    "stroke_diagonal": None,
    "stroke_dot": None,
}
# flight curve (res/interpolator/splash_stroke.xml): fast start, ~6% overshoot, one rebound, settle
BOUNCE_PATH = "M0,0 C0.35,0.45 0.45,1.06 0.62,1.06 C0.72,1.06 0.76,0.986 0.84,0.986 C0.91,0.986 0.95,1 1,1"

# ---- logo geometry (ic_tada units, the viewport of drawable/ic_tada) ---------------------------
LOGO_W, LOGO_H = 252.8, 308.77
CURVE = ("C", (111.18, 147.23), (129.84, 150.94), (148.5, 154.54))
# Neighbouring strokes overlap a little inside the logo, so no seam shows between them.
STROKES = [
    ("stroke_left", [(57.56, 0), (118.61, 9.22), (99.21, 62.64), (81.09, 109.71), (54.69, 180.93), (0, 170.76)]),
    ("stroke_top", [(91.2, 63.29), (229.27, 52.15), (252.8, 85.86), (177.12, 103.03), (73.1, 110.27)]),
    ("stroke_diagonal", [(252.8, 85.86), (127.83, 308.77), (76.95, 283.96), (126.87, 193.65), (148.5, 154.54),
                         (177.12, 103.03), (213.6, 88.5)]),
    ("stroke_dot", [(92.53, 143.41), CURVE, (156, 156), (134.5, 195.2), (126.87, 193.65), (78.22, 183.79)]),
]
# axis of each stroke: its two long edges, each from where the stroke is written from to where it ends
AXES = {
    "stroke_left": (((57.56, 0), (0, 170.76)), ((118.61, 9.22), (54.69, 180.93))),
    "stroke_top": (((99.21, 62.64), (229.27, 52.15)), ((81.09, 109.71), (177.12, 103.03))),
    "stroke_diagonal": (((252.8, 85.86), (127.83, 308.77)), ((177.12, 103.03), (76.95, 283.96))),
    "stroke_dot": (((92.53, 143.41), (148.5, 154.54)), ((78.22, 183.79), (126.87, 193.65))),
}

CANVAS, CLIP_RADIUS_DP, MARGIN_DP = 288, 96, 1
SCALE = LOGO_HEIGHT_DP / LOGO_H
TX, TY = (CANVAS - LOGO_W * SCALE) / 2, (CANVAS - LOGO_H * SCALE) / 2
CX, CY = LOGO_W / 2, LOGO_H / 2                       # canvas centre, in logo units
STEP = STROKE_MOVE_MS + STROKE_PAUSE_MS
MOVE_END = STEP * (len(STROKES) - 1) + STROKE_MOVE_MS
TOTAL = MOVE_END + PULSE_UP_MS + PULSE_DOWN_MS
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")


def flat(pts, steps=16):
    out, prev = [], None
    for p in pts:
        if p[0] == "C":
            c1, c2, e = p[1], p[2], p[3]
            for i in range(1, steps + 1):
                t = i / steps
                out.append(tuple((1 - t) ** 3 * prev[k] + 3 * (1 - t) ** 2 * t * c1[k] + 3 * (1 - t) * t * t * c2[k]
                                 + t ** 3 * e[k] for k in (0, 1)))
            prev = e
        else:
            out.append(p)
            prev = p
    return out


def path_data(pts):
    s = []
    for i, p in enumerate(pts):
        s.append("C%g,%g %g,%g %g,%g" % (*p[1], *p[2], *p[3]) if p[0] == "C" else ("M%g,%g" if i == 0 else "L%g,%g") % p)
    return " ".join(s) + " Z"


def bezier_curve(path):
    """Samples a path interpolator ("M0,0 C... C...") into (x, y) points."""
    nums = [float(v) for v in re.findall(r"-?\d*\.?\d+", path)]
    pts, cur, out = nums[2:], (nums[0], nums[1]), [(nums[0], nums[1])]
    for i in range(0, len(pts), 6):
        c1, c2, e = (pts[i], pts[i + 1]), (pts[i + 2], pts[i + 3]), (pts[i + 4], pts[i + 5])
        for j in range(1, 201):
            t = j / 200
            out.append(tuple((1 - t) ** 3 * cur[k] + 3 * (1 - t) ** 2 * t * c1[k] + 3 * (1 - t) * t * t * c2[k]
                             + t ** 3 * e[k] for k in (0, 1)))
        cur = e
    return out


BOUNCE = bezier_curve(BOUNCE_PATH)


def bounce(x):
    for (x0, y0), (x1, y1) in zip(BOUNCE, BOUNCE[1:]):
        if x <= x1:
            return y0 + (y1 - y0) * ((x - x0) / (x1 - x0) if x1 > x0 else 1)
    return 1.0


def ease(t):  # fast_out_slow_in, cubic-bezier(0.4, 0, 0.2, 1)
    lo, hi = 0.0, 1.0
    for _ in range(40):
        m = (lo + hi) / 2
        lo, hi = (m, hi) if 3 * (1 - m) ** 2 * m * 0.4 + 3 * (1 - m) * m * m * 0.2 + m ** 3 < t else (lo, m)
    m = (lo + hi) / 2
    return 3 * (1 - m) * m * m + m ** 3


def radius_dp(pts, dx, dy, ps=1.0):
    return max(math.hypot(x + dx - CX, y + dy - CY) for x, y in pts) * SCALE * ps


def direction(name):
    """Unit vector of the stroke's axis, in its writing direction: the mean of its two long edges."""
    ux = uy = 0
    for (x0, y0), (x1, y1) in AXES[name]:
        length = math.hypot(x1 - x0, y1 - y0)
        ux, uy = ux + (x1 - x0) / length, uy + (y1 - y0) / length
    length = math.hypot(ux, uy)
    return ux / length, uy / length


def distance(name, pts):
    if DISTANCE[name] is not None:
        return DISTANCE[name]
    ux, uy = direction(name)
    d = 0.0
    while radius_dp(pts, -ux * (d + 0.5), -uy * (d + 0.5)) <= CLIP_RADIUS_DP - MARGIN_DP:
        d += 0.5
    return d


DISTANCES = {n: distance(n, flat(p)) for n, p in STROKES}


def offset(name):
    """Start of the stroke, from its place: back along its axis."""
    ux, uy = direction(name)
    return -ux * DISTANCES[name], -uy * DISTANCES[name]


def fade_start(i, name, pts):
    """First time (ms, from the stroke's start) when the stroke is whole inside the circle."""
    ox, oy = offset(name)
    for t in range(0, STROKE_MOVE_MS + 1):
        f = bounce(t / STROKE_MOVE_MS)
        if radius_dp(pts, ox * (1 - f), oy * (1 - f)) <= CLIP_RADIUS_DP - MARGIN_DP:
            return t
    sys.exit("%s never gets inside the circle" % name)


FADES = {n: fade_start(i, n, flat(p)) for i, (n, p) in enumerate(STROKES)}


def state(i, name, t):
    d = i * STEP
    ox, oy = offset(name)
    f = 0 if t <= d else bounce(min(1, (t - d) / STROKE_MOVE_MS))
    a0 = d + FADES[name]
    a = 0 if t <= a0 else ease(min(1, (t - a0) / FADE_MS))
    return ox * (1 - f), oy * (1 - f), a


def pulse(t):
    if t <= MOVE_END:
        return 1
    if t <= MOVE_END + PULSE_UP_MS:
        return 1 + (PULSE_SCALE - 1) * ease((t - MOVE_END) / PULSE_UP_MS)
    return PULSE_SCALE - (PULSE_SCALE - 1) * ease(min(1, (t - MOVE_END - PULSE_UP_MS) / PULSE_DOWN_MS))


def check():
    worst = 0
    for t in range(0, TOTAL + 1):
        for i, (n, p) in enumerate(STROKES):
            dx, dy, a = state(i, n, t)
            if a > 0:
                worst = max(worst, radius_dp(flat(p), dx, dy, pulse(t)))
    if worst > CLIP_RADIUS_DP:
        sys.exit("a visible stroke reaches %.2fdp, past the %ddp circle" % (worst, CLIP_RADIUS_DP))
    return worst


def kf(fraction, value, interp=None):
    a = ' android:interpolator="%s"' % interp if interp else ""
    return '                    <keyframe android:fraction="%.4f" android:value="%g"%s />' % (fraction, round(value, 3), a)


def holder(prop, frames):
    return ('                <propertyValuesHolder\n                    android:propertyName="%s"\n'
            '                    android:valueType="floatType">\n%s\n                </propertyValuesHolder>'
            % (prop, "\n".join(kf(*f) for f in frames)))


def animator(target, duration, holders):
    return ('    <target android:name="%s">\n        <aapt:attr name="android:animation">\n            <objectAnimator\n'
            '                android:duration="%d"\n                android:interpolator="@android:anim/linear_interpolator">\n'
            '%s\n            </objectAnimator>\n        </aapt:attr>\n    </target>' % (target, duration, "\n".join(holders)))


def held(start_ms, duration, a, b, interp):
    """a until start_ms, then to b at the end: delayed starts are keyframes, so the start values
    show from the first frame (a start offset would show the end state until it starts)."""
    f = start_ms / duration
    return ([(0, a), (f, a)] if f > 0 else [(0, a)]) + [(1, b, interp)]


def avd():
    out = ['''<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by scripts/splash_icon.py, change its parameters rather than this file.
     Splash icon of Android 12+: drawable/ic_tada split into the strokes of its タ (the second one
     drawn as two bars, so four parts). In the stroke order, each one slides in along its own axis,
     in the direction it is written, fades in once whole inside the circle the icon is clipped to,
     lands with a small bounce along that axis, and the next one leaves once it has landed; the logo
     pulses once whole. The vector holds the end state, shown as is when animations are off. -->
<animated-vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt">
    <aapt:attr name="android:drawable">
        <vector
            android:width="%ddp"
            android:height="%ddp"
            android:viewportWidth="%d"
            android:viewportHeight="%d">
            <group
                android:name="logo"
                android:pivotX="%g"
                android:pivotY="%g">
                <!-- ic_tada, %ddp tall, in the centre -->
                <group
                    android:scaleX="%.6f"
                    android:scaleY="%.6f"
                    android:translateX="%.3f"
                    android:translateY="%.3f">''' % (CANVAS, CANVAS, CANVAS, CANVAS, CANVAS / 2, CANVAS / 2,
                                                   LOGO_HEIGHT_DP, SCALE, SCALE, TX, TY)]
    for n, p in STROKES:
        out.append('''                    <group android:name="%s">
                        <path
                            android:name="%s_path"
                            android:fillColor="#FFFFFF"
                            android:pathData="%s" />
                    </group>''' % (n, n, path_data(p)))
    out.append('''                </group>
            </group>
        </vector>
    </aapt:attr>''')
    for i, (n, p) in enumerate(STROKES):
        d = i * STEP
        ox, oy = offset(n)
        dur = d + STROKE_MOVE_MS
        out.append(animator(n, dur, [holder("translateX", held(d, dur, ox, 0, "@interpolator/splash_stroke")),
                                     holder("translateY", held(d, dur, oy, 0, "@interpolator/splash_stroke"))]))
        a0 = d + FADES[n]
        out.append(animator(n + "_path", a0 + FADE_MS,
                            [holder("fillAlpha", held(a0, a0 + FADE_MS, 0, 1, "@android:interpolator/fast_out_slow_in"))]))
    up, down = MOVE_END / TOTAL, (MOVE_END + PULSE_UP_MS) / TOTAL
    frames = [(0, 1), (up, 1), (down, PULSE_SCALE, "@android:interpolator/fast_out_slow_in"),
              (1, 1, "@android:interpolator/fast_out_slow_in")]
    out.append(animator("logo", TOTAL, [holder("scaleX", frames), holder("scaleY", frames)]))
    out.append("</animated-vector>\n")
    return "\n".join(out)


def interpolator():
    return '''<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by scripts/splash_icon.py: flight of a stroke of the splash logo, fast start, small
     overshoot and one rebound before it settles -->
<pathInterpolator xmlns:android="http://schemas.android.com/apk/res/android"
    android:pathData="%s" />
''' % BOUNCE_PATH


def preview(folder):
    from PIL import Image, ImageChops, ImageDraw
    os.makedirs(folder, exist_ok=True)
    k = 2
    for t in list(range(0, TOTAL + 1, 50)) + [TOTAL]:
        im = Image.new("RGB", (CANVAS * k, CANVAS * k), (32, 33, 37))
        ImageDraw.Draw(im).ellipse([(CANVAS / 2 - CLIP_RADIUS_DP) * k, (CANVAS / 2 - CLIP_RADIUS_DP) * k,
                                    (CANVAS / 2 + CLIP_RADIUS_DP) * k, (CANVAS / 2 + CLIP_RADIUS_DP) * k], outline=(90, 90, 90))
        mask, ps = Image.new("L", im.size, 0), pulse(t)
        for i, (n, p) in enumerate(STROKES):
            dx, dy, a = state(i, n, t)
            pts = [(((CX + (x + dx - CX) * ps) * SCALE + TX) * k, ((CY + (y + dy - CY) * ps) * SCALE + TY) * k) for x, y in flat(p)]
            layer = Image.new("L", im.size, 0)
            ImageDraw.Draw(layer).polygon(pts, fill=int(255 * a))
            mask = ImageChops.lighter(mask, layer)
        im.paste((255, 255, 255), (0, 0), mask)
        im.save(os.path.join(folder, "t%04d.png" % t))


def main():
    worst = check()
    with open(os.path.join(RES, "drawable-v31", "ic_tada_splash.xml"), "w", encoding="utf-8", newline="\n") as f:
        f.write(avd())
    with open(os.path.join(RES, "interpolator", "splash_stroke.xml"), "w", encoding="utf-8", newline="\n") as f:
        f.write(interpolator())
    themes = os.path.join(RES, "values", "themes.xml")
    with open(themes, encoding="utf-8", newline="") as f:
        s = f.read()
    s, n = re.subn(r'(<item name="windowSplashScreenAnimationDuration">)\d+(</item>)', r"\g<1>%d\g<2>" % TOTAL, s)
    if n != 1:
        sys.exit("windowSplashScreenAnimationDuration not found in themes.xml")
    with open(themes, "w", encoding="utf-8", newline="") as f:
        f.write(s)
    print("total %d ms, strokes leave at %s ms, visible from +%s ms, max radius when visible %.2fdp"
          % (TOTAL, [i * STEP for i in range(len(STROKES))], [FADES[n] for n, _ in STROKES], worst))
    for n, _ in STROKES:
        ux, uy = direction(n)
        print("  %s: moves along (%.3f, %.3f) (y down), %.1f units = %.1fdp" % (n, ux, uy, DISTANCES[n], DISTANCES[n] * SCALE))
    if len(sys.argv) > 2 and sys.argv[1] == "--preview":
        preview(sys.argv[2])


if __name__ == "__main__":
    main()
