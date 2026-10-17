#!/usr/bin/env python3
"""Generates LinguaFree's own Lottie animations (no third-party assets):
  confetti.json — burst of confetti for the lesson summary
  streak_fire.json — gently flickering flame for the streak screen
Usage: python3 scripts/icons/lottie_anims.py core/ui/src/main/res/raw
"""
import json, math, os, random, sys

COLORS = [(0.30, 0.77, 0.09), (1.0, 0.76, 0.12), (0.18, 0.72, 0.95), (1.0, 0.29, 0.43), (0.65, 0.42, 0.97), (1.0, 0.54, 0.12)]


def kf(t, s, ease=True):
    k = {'t': t, 's': s if isinstance(s, list) else [s]}
    if ease:
        k['i'] = {'x': [0.4], 'y': [1]}
        k['o'] = {'x': [0.6], 'y': [0]}
    return k


def confetti(w=400, h=400, n=46, frames=84, seed=7):
    rnd = random.Random(seed)
    layers = []
    for i in range(n):
        c = COLORS[i % len(COLORS)]
        ang = rnd.uniform(-math.pi * 0.95, -math.pi * 0.05)
        speed = rnd.uniform(120, 260)
        x0, y0 = w / 2 + rnd.uniform(-20, 20), h * 0.62
        x1 = x0 + math.cos(ang) * speed
        y1 = y0 + math.sin(ang) * speed
        x2 = x1 + rnd.uniform(-40, 40)
        y2 = h + 40
        start = rnd.randint(0, 6)
        peak = start + rnd.randint(16, 24)
        end = frames
        shape = rnd.choice(['rect', 'rect', 'ellipse'])
        sw, sh = (rnd.uniform(8, 14), rnd.uniform(5, 8)) if shape == 'rect' else (rnd.uniform(7, 10),) * 2
        geo = {'ty': 'rc', 'p': {'a': 0, 'k': [0, 0]}, 's': {'a': 0, 'k': [sw, sh]}, 'r': {'a': 0, 'k': 2}} if shape == 'rect' else \
              {'ty': 'el', 'p': {'a': 0, 'k': [0, 0]}, 's': {'a': 0, 'k': [sw, sh]}}
        rot0 = rnd.uniform(0, 360)
        layers.append({
            'ddd': 0, 'ind': i + 1, 'ty': 4, 'nm': f'c{i}', 'sr': 1, 'ip': start, 'op': end, 'st': 0, 'bm': 0,
            'ks': {
                'o': {'a': 1, 'k': [kf(start, 100), kf(end - 16, 100), kf(end, 0, False)]},
                'r': {'a': 1, 'k': [kf(start, rot0), kf(end, rot0 + rnd.choice([-1, 1]) * rnd.uniform(360, 900), False)]},
                'p': {'a': 1, 'k': [
                    {'t': start, 's': [x0, y0, 0], 'i': {'x': 0.3, 'y': 1}, 'o': {'x': 0.2, 'y': 0.6}},
                    {'t': peak, 's': [x1, y1, 0], 'i': {'x': 0.5, 'y': 1}, 'o': {'x': 0.5, 'y': 0}},
                    {'t': end, 's': [x2, y2, 0]},
                ]},
                'a': {'a': 0, 'k': [0, 0, 0]},
                's': {'a': 1, 'k': [kf(start, [100, 100, 100]), kf(peak, [100, rnd.choice([30, 100]), 100]), kf(end, [100, 100, 100], False)]},
            },
            'ao': 0,
            'shapes': [geo, {'ty': 'fl', 'c': {'a': 0, 'k': [*c, 1]}, 'o': {'a': 0, 'k': 100}, 'r': 1}],
        })
    return {'v': '5.7.4', 'fr': 30, 'ip': 0, 'op': frames, 'w': w, 'h': h, 'nm': 'LinguaFree confetti', 'ddd': 0, 'assets': [], 'layers': layers}


def flame_path(scale, wobble):
    # Teardrop flame outline (closed bezier), centred at (0,0), tip up.
    s = scale
    v = [[0, -60 * s], [32 * s + wobble, 10 * s], [0, 46 * s], [-32 * s + wobble * 0.6, 10 * s]]
    i = [[-6 * s, 16 * s], [0, -26 * s], [20 * s, 0], [0, 22 * s]]
    o = [[6 * s, 16 * s], [0, 22 * s], [-20 * s, 0], [0, -26 * s]]
    return {'i': i, 'o': o, 'v': v, 'c': True}


def fire(w=200, h=200, frames=48):
    def layer(ind, scale, color, phase):
        ks = []
        for f in range(0, frames + 1, 8):
            wob = 6 * math.sin(2 * math.pi * (f / frames) + phase)
            ks.append({'t': f, 's': [flame_path(scale * (1 + 0.04 * math.sin(2 * math.pi * f / frames * 2 + phase)), wob)],
                       'i': {'x': 0.5, 'y': 1}, 'o': {'x': 0.5, 'y': 0}})
        return {'ddd': 0, 'ind': ind, 'ty': 4, 'nm': f'flame{ind}', 'sr': 1, 'ip': 0, 'op': frames, 'st': 0, 'bm': 0, 'ao': 0,
                'ks': {'o': {'a': 0, 'k': 100}, 'r': {'a': 0, 'k': 0}, 'p': {'a': 0, 'k': [w / 2, h * 0.6, 0]},
                       'a': {'a': 0, 'k': [0, 0, 0]}, 's': {'a': 0, 'k': [100, 100, 100]}},
                'shapes': [{'ty': 'sh', 'ks': {'a': 1, 'k': ks}}, {'ty': 'fl', 'c': {'a': 0, 'k': color}, 'o': {'a': 0, 'k': 100}, 'r': 1}]}
    return {'v': '5.7.4', 'fr': 24, 'ip': 0, 'op': frames, 'w': w, 'h': h, 'nm': 'LinguaFree streak fire', 'ddd': 0, 'assets': [],
            'layers': [layer(1, 0.45, [1, 0.88, 0.45, 1], 1.3), layer(2, 0.78, [1, 0.6, 0.15, 1], 0.6), layer(3, 1.05, [1, 0.45, 0.07, 1], 0.0)]}


if __name__ == '__main__':
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    for name, data in (('confetti', confetti()), ('streak_fire', fire())):
        with open(os.path.join(out, name + '.json'), 'w') as f:
            json.dump(data, f, separators=(',', ':'))
    print('ok')
