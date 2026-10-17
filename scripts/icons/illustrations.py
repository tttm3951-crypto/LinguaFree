#!/usr/bin/env python3
"""Original flat illustrations for "select the picture" exercises (64×64 viewport).
Run: python3 scripts/icons/illustrations.py OUT_SVG_DIR  (then svg2vectordrawable, see README).
"""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from icons import _circles_to_paths  # noqa: E402

SKIN, SKIN_D = '#FFD2B0', '#F0B48C'
SKIN2 = '#E8A97F'
OUT = '#00000000'

def svg(body):
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="64" height="64" viewBox="0 0 64 64">{body}</svg>'

def face(cx=32, cy=30, r=13, skin=SKIN, smile=True, cheeks=True, eyes_y=None, glasses=False):
    ey = eyes_y if eyes_y is not None else cy - 1
    out = f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{skin}"/>'
    out += f'<circle cx="{cx - r * 0.38:.1f}" cy="{ey}" r="1.7" fill="#3A2412"/><circle cx="{cx + r * 0.38:.1f}" cy="{ey}" r="1.7" fill="#3A2412"/>'
    if cheeks:
        out += f'<circle cx="{cx - r * 0.6:.1f}" cy="{cy + 3.5}" r="2.2" fill="#FF9F9F" fill-opacity=".55"/><circle cx="{cx + r * 0.6:.1f}" cy="{cy + 3.5}" r="2.2" fill="#FF9F9F" fill-opacity=".55"/>'
    if smile:
        out += f'<path d="M{cx - 4} {cy + 4.5}q4 4 8 0" fill="none" stroke="#3A2412" stroke-width="1.8" stroke-linecap="round"/>'
    if glasses:
        out += (f'<circle cx="{cx - r * 0.38:.1f}" cy="{ey}" r="3.6" fill="none" stroke="#5B4636" stroke-width="1.3"/>'
                f'<circle cx="{cx + r * 0.38:.1f}" cy="{ey}" r="3.6" fill="none" stroke="#5B4636" stroke-width="1.3"/>'
                f'<path d="M{cx - 1.4} {ey}h2.8" stroke="#5B4636" stroke-width="1.3"/>')
    return out

def body(color, cx=32, w=40):
    return f'<path d="M{cx - w / 2} 64v-6a{w / 2} 13 0 0 1 {w} 0v6z" fill="{color}"/>'

PEOPLE = {
    'img_mother': svg(
        '<path d="M15 34c0-14 7-21 17-21s17 7 17 21v14H15z" fill="#7A4A2A"/>' + body('#FF6F91') + face() +
        '<path d="M19 27c2-9 8-12 13-12s11 3 13 12c-5-4-9-5-13-6-4 1-8 2-13 6z" fill="#7A4A2A"/>'),
    'img_father': svg(
        body('#3E8EDE') + face() +
        '<path d="M19 26c0-9 6-13 13-13s13 4 13 13c-3-3-7-5-13-5s-10 2-13 5z" fill="#4A3020"/>'
        '<path d="M22 34c2 7 6 9 10 9s8-2 10-9c-3 3-6 3-10 3s-7 0-10-3z" fill="#4A3020"/>'),
    'img_sister': svg(
        '<circle cx="16" cy="30" r="5" fill="#C0602B"/><circle cx="48" cy="30" r="5" fill="#C0602B"/>' +
        body('#FFC21F', w=34) + face(r=12) +
        '<path d="M20 28c1-8 6-12 12-12s11 4 12 12c-4-5-8-6-12-6s-8 1-12 6z" fill="#C0602B"/>'
        '<circle cx="21" cy="21" r="2.4" fill="#FF4B6E"/>'),
    'img_brother': svg(
        body('#4CC417', w=34) + face(r=12) +
        '<path d="M20 26l2-8 3 4 3-6 3 5 3-6 2 6 4-3 0 8c-4-3-8-4-12-4s-8 1-12 4z" fill="#2B2B2B"/>'),
    'img_grandmother': svg(
        '<circle cx="32" cy="14" r="6" fill="#D6D6D6"/>' + body('#A56BF7') + face(glasses=True) +
        '<path d="M19 28c1-8 7-12 13-12s12 4 13 12c-4-4-8-6-13-6s-9 2-13 6z" fill="#D6D6D6"/>'),
    'img_grandfather': svg(
        body('#8A6B4E') + face(glasses=True, smile=False) +
        '<path d="M19 30c-2-3-2-7 0-9M45 30c2-3 2-7 0-9" stroke="#D6D6D6" stroke-width="3" stroke-linecap="round"/>'
        '<path d="M26 35c3-2 4-2 6-1 2-1 3-1 6 1-2 2-4 2-6 1-2 1-4 1-6-1z" fill="#D6D6D6"/>'),
    'img_baby': svg(
        '<path d="M18 64v-4a14 9 0 0 1 28 0v4z" fill="#BDE7FF"/>' + face(cy=36, r=15, smile=False) +
        '<path d="M31 21c-3-3 1-6 4-4" fill="none" stroke="#E0A060" stroke-width="2.4" stroke-linecap="round"/>'
        '<circle cx="32" cy="42.5" r="3.2" fill="#FF8AB0"/><circle cx="32" cy="42.5" r="1.4" fill="#FFFFFF"/>'),
    'img_boy': svg(
        body('#FF8A1F', w=32) + face(r=12) +
        '<path d="M20 26c0-8 5-11 12-11s12 3 12 11c-3-2-6-3-9-3l-3-3-3 3c-3 0-6 1-9 3z" fill="#8A5225"/>'),
    'img_girl': svg(
        '<path d="M17 36c0-14 6-21 15-21s15 7 15 21v10H17z" fill="#E8B04A"/>' + body('#2EB8F2', w=32) + face(r=12) +
        '<path d="M20 28c1-8 6-12 12-12s11 4 12 12c-3-4-7-6-12-6s-9 2-12 6z" fill="#E8B04A"/>'),
    'img_man': svg(
        body('#5C6BC0') + face() + '<path d="M19 25c0-8 6-12 13-12s13 4 13 12c-4-3-8-4-13-4s-9 1-13 4z" fill="#2B2B2B"/>'),
    'img_woman': svg(
        '<path d="M16 35c0-14 7-21 16-21s16 7 16 21v12H16z" fill="#2B2B2B"/>' + body('#34D1A2') + face() +
        '<path d="M19 27c2-8 7-12 13-12s11 4 13 12c-6-6-12-7-26 0z" fill="#2B2B2B"/>'),
    'img_family': svg(
        '<path d="M2 64v-5a11 8 0 0 1 22 0v5z" fill="#3E8EDE"/><circle cx="13" cy="38" r="9" fill="' + SKIN + '"/>'
        '<path d="M5 35c0-6 4-9 8-9s8 3 8 9c-3-2-5-3-8-3s-5 1-8 3z" fill="#4A3020"/>'
        '<path d="M40 64v-5a11 8 0 0 1 22 0v5z" fill="#FF6F91"/><circle cx="51" cy="38" r="9" fill="' + SKIN + '"/>'
        '<path d="M42 40c0-9 4-14 9-14s9 5 9 14v8H42z" fill="#7A4A2A" fill-opacity="0"/><path d="M43 36c0-6 4-10 8-10s8 4 8 10c-4-3-8-3-16 0z" fill="#7A4A2A"/>'
        '<path d="M22 64v-4a10 7 0 0 1 20 0v4z" fill="#FFC21F"/><circle cx="32" cy="45" r="7.5" fill="' + SKIN + '"/>'
        '<path d="M25 43c0-5 3-7 7-7s7 2 7 7c-2-2-4-2-7-2s-5 0-7 2z" fill="#C0602B"/>'
        '<circle cx="10" cy="38" r="1.2" fill="#3A2412"/><circle cx="16" cy="38" r="1.2" fill="#3A2412"/>'
        '<circle cx="48" cy="38" r="1.2" fill="#3A2412"/><circle cx="54" cy="38" r="1.2" fill="#3A2412"/>'
        '<circle cx="29.5" cy="45" r="1.1" fill="#3A2412"/><circle cx="34.5" cy="45" r="1.1" fill="#3A2412"/>'
        '<path d="M32 12c-3-4-9-2-8 3 1 4 8 8 8 8s7-4 8-8c1-5-5-7-8-3z" fill="#FF4B6E"/>'),
}

FOOD = {
    'img_water': svg(
        '<path d="M18 12h28l-4 44a4 4 0 0 1-4 4H26a4 4 0 0 1-4-4z" fill="#DFF4FF" stroke="#9BD7F2" stroke-width="2"/>'
        '<path d="M20.5 26h23l-2.6 30a3 3 0 0 1-3 3H26.1a3 3 0 0 1-3-3z" fill="#5CC8F5"/>'
        '<path d="M26 32c0 4-1 8 0 14" stroke="#FFFFFF" stroke-opacity=".8" stroke-width="2.5" stroke-linecap="round"/>'),
    'img_tea': svg(
        '<path d="M10 30h36v8c0 10-8 18-18 18s-18-8-18-18z" fill="#FFFFFF" stroke="#D0D0D0" stroke-width="2"/>'
        '<path d="M46 33c6 0 9 3 9 7s-4 7-10 7" fill="none" stroke="#D0D0D0" stroke-width="3.5"/>'
        '<path d="M13 32h30v3H13z" fill="#C8763A"/>'
        '<path d="M34 31V16" stroke="#8A8A8A" stroke-width="1.2"/><rect x="30" y="10" width="8" height="8" rx="1.5" fill="#4CC417"/>'
        '<ellipse cx="28" cy="60" rx="20" ry="2.5" fill="#000000" fill-opacity=".08"/>'),
    'img_coffee': svg(
        '<path d="M12 26h34v20c0 7-6 12-13 12h-8c-7 0-13-5-13-12z" fill="#8A5225"/>'
        '<path d="M46 31c6 0 9 3 9 7s-4 7-10 7" fill="none" stroke="#8A5225" stroke-width="4"/>'
        '<ellipse cx="29" cy="26" rx="17" ry="3.5" fill="#4A2A14"/>'
        '<path d="M22 20c-3-4 3-6 0-11M30 20c-3-4 3-6 0-11M38 20c-3-4 3-6 0-11" fill="none" stroke="#C9CED1" stroke-width="2.4" stroke-linecap="round"/>'),
    'img_milk': svg(
        '<path d="M20 22l6-10h12l6 10v36a3 3 0 0 1-3 3H23a3 3 0 0 1-3-3z" fill="#FFFFFF" stroke="#C9D6E0" stroke-width="2"/>'
        '<path d="M26 12h12v-4H26z" fill="#2EB8F2"/>'
        '<path d="M20 34h24v14H20z" fill="#2EB8F2"/>'
        '<circle cx="32" cy="41" r="4.5" fill="#FFFFFF"/>'),
    'img_juice': svg(
        '<path d="M18 18h28l-4 40a4 4 0 0 1-4 4H26a4 4 0 0 1-4-4z" fill="#FFF3D6" stroke="#FFD48A" stroke-width="2"/>'
        '<path d="M20 28h24l-3 28a3 3 0 0 1-3 3H26a3 3 0 0 1-3-3z" fill="#FF9F1C"/>'
        '<path d="M36 34 44 4" stroke="#FF4B6E" stroke-width="3" stroke-linecap="round"/>'
        '<circle cx="46" cy="18" r="8" fill="#FFB347"/><circle cx="46" cy="18" r="5.5" fill="#FFD27A"/>'
        '<path d="M46 12.5v11M40.5 18h11" stroke="#FFB347" stroke-width="1.2"/>'),
    'img_bread': svg(
        '<path d="M8 36c0-12 10-20 24-20s24 8 24 20c0 4-2 6-5 6v12a4 4 0 0 1-4 4H17a4 4 0 0 1-4-4V42c-3 0-5-2-5-6z" fill="#E0A060"/>'
        '<path d="M8 36c0-12 10-20 24-20s24 8 24 20c0 4-2 6-5 6H13c-3 0-5-2-5-6z" fill="#C8783A"/>'
        '<path d="M22 24l4 8M31 22l3 9M40 24l2 8" stroke="#F2C08A" stroke-width="2.4" stroke-linecap="round"/>'),
    'img_apple': svg(
        '<path d="M32 20c-6-4-18-3-20 9-2 14 8 30 15 30 3 0 3-2 5-2s2 2 5 2c7 0 17-16 15-30-2-12-14-13-20-9z" fill="#F2464B"/>'
        '<path d="M32 20c0-5 1-9 4-12" stroke="#8A5225" stroke-width="2.6" stroke-linecap="round" fill="none"/>'
        '<path d="M34 14c3-6 10-7 14-5-2 5-8 8-14 5z" fill="#4CC417"/>'
        '<path d="M20 30c-1 4 0 8 2 11" stroke="#FFFFFF" stroke-opacity=".6" stroke-width="3" stroke-linecap="round"/>'),
    'img_cheese': svg(
        '<path d="M6 40 46 18l12 10v20H6z" fill="#FFD23F"/>'
        '<path d="M6 40 46 18l12 10z" fill="#FFE680"/>'
        '<path d="M6 40h52v8H6z" fill="#F2B705"/>'
        '<circle cx="20" cy="44" r="2.5" fill="#E0A300"/><circle cx="36" cy="43" r="3.2" fill="#E0A300"/><circle cx="50" cy="45" r="2" fill="#E0A300"/>'
        '<circle cx="38" cy="29" r="2.4" fill="#F2C94C"/><circle cx="48" cy="25.5" r="1.8" fill="#F2C94C"/>'),
    'img_egg': svg(
        '<ellipse cx="32" cy="36" rx="24" ry="20" fill="#FFFFFF" stroke="#E0E0E0" stroke-width="2"/>'
        '<circle cx="32" cy="36" r="10" fill="#FFC21F"/><circle cx="29" cy="33" r="3" fill="#FFE08A"/>'),
    'img_soup': svg(
        '<path d="M6 32h52c0 14-11 24-26 24S6 46 6 32z" fill="#FF7A45"/>'
        '<ellipse cx="32" cy="32" rx="26" ry="5" fill="#FFB347"/>'
        '<circle cx="22" cy="31" r="2.6" fill="#4CC417"/><circle cx="33" cy="33" r="2.2" fill="#F2464B"/><circle cx="42" cy="30.5" r="2.4" fill="#4CC417"/>'
        '<path d="M44 26 58 8" stroke="#9CA4A8" stroke-width="3.2" stroke-linecap="round"/>'
        '<path d="M22 22c-3-4 3-6 0-10M32 22c-3-4 3-6 0-10" fill="none" stroke="#C9CED1" stroke-width="2.2" stroke-linecap="round"/>'),
}

GREET = {
    'img_hello': svg(
        '<path d="M22 58c-8-4-12-12-12-20V22a4 4 0 0 1 8 0v10V12a4 4 0 0 1 8 0v18V8a4 4 0 0 1 8 0v22V12a4 4 0 0 1 8 0v26l4-8a4 4 0 0 1 7 3l-7 15c-3 7-9 11-16 11z" fill="' + SKIN + '" stroke="' + SKIN_D + '" stroke-width="1.5" stroke-linejoin="round"/>'
        '<path d="M50 10c4 2 6 5 7 9M54 4c5 3 8 7 9 12" fill="none" stroke="#FFC21F" stroke-width="2.6" stroke-linecap="round"/>'),
    'img_morning': svg(
        '<path d="M0 46h64v18H0z" fill="#7FCF5A"/><circle cx="32" cy="46" r="15" fill="#FFC21F"/>'
        '<path d="M0 46c10-4 20-6 32-6s22 2 32 6v18H0z" fill="#4CC417"/>'
        '<path d="M32 22v-8M15 29l-5-5M49 29l5-5M10 40H3M61 40h-7" stroke="#FFC21F" stroke-width="3" stroke-linecap="round"/>'),
    'img_evening': svg(
        '<rect x="0" y="0" width="64" height="64" rx="14" fill="#2D3A6E"/>'
        '<path d="M40 14a16 16 0 1 0 10 28A18 18 0 0 1 40 14z" fill="#FFE27A"/>'
        '<path d="M14 16l1.4 3 3 .4-2.2 2 .6 3-2.8-1.6-2.8 1.6.6-3-2.2-2 3-.4zM20 40l1 2 2 .3-1.5 1.4.4 2-1.9-1-1.9 1 .4-2-1.5-1.4 2-.3zM52 50l1 2 2 .3-1.5 1.4.4 2-1.9-1-1.9 1 .4-2-1.5-1.4 2-.3z" fill="#FFFFFF"/>'),
    'img_friend': svg(
        '<path d="M2 64v-6a13 10 0 0 1 26 0v6z" fill="#2EB8F2"/><circle cx="15" cy="34" r="11" fill="' + SKIN + '"/>'
        '<path d="M5 31c0-7 5-10 10-10s10 3 10 10c-3-2-6-3-10-3s-7 1-10 3z" fill="#4A3020"/>'
        '<path d="M36 64v-6a13 10 0 0 1 26 0v6z" fill="#FF8A1F"/><circle cx="49" cy="34" r="11" fill="' + SKIN2 + '"/>'
        '<path d="M38 33c0-8 5-12 11-12s11 4 11 12c-4-4-8-5-11-5s-7 1-11 5z" fill="#2B2B2B"/>'
        '<circle cx="11" cy="34" r="1.4" fill="#3A2412"/><circle cx="19" cy="34" r="1.4" fill="#3A2412"/>'
        '<circle cx="45" cy="34" r="1.4" fill="#3A2412"/><circle cx="53" cy="34" r="1.4" fill="#3A2412"/>'
        '<path d="M11 39q4 3 8 0M45 39q4 3 8 0" fill="none" stroke="#3A2412" stroke-width="1.6" stroke-linecap="round"/>'
        '<path d="M32 14c-2-3-7-2-6 2 1 3 6 6 6 6s5-3 6-6c1-4-4-5-6-2z" fill="#FF4B6E"/>'),
    'img_goodbye': svg(
        '<path d="M8 58V14a4 4 0 0 1 4-4h22v52H12a4 4 0 0 1-4-4z" fill="#B8743A"/>'
        '<path d="M34 10l18 6v42l-18 4z" fill="#8A5225"/><circle cx="40" cy="38" r="2" fill="#FFC21F"/>'
        '<path d="M20 30l-8 0M16 26l-4 4 4 4" fill="none" stroke="#FFFFFF" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/>'),
    'img_house': svg(
        '<path d="M10 30 32 12l22 18v26a3 3 0 0 1-3 3H13a3 3 0 0 1-3-3z" fill="#FF8A1F"/>'
        '<path d="M6 32 32 9l26 23-3 3L32 15 9 35z" fill="#E0650A"/>'
        '<rect x="26" y="40" width="12" height="19" rx="2" fill="#8A5225"/><rect x="15" y="34" width="8" height="8" rx="1.5" fill="#9BE3FF"/><rect x="41" y="34" width="8" height="8" rx="1.5" fill="#9BE3FF"/>'),
}

ALL = {**PEOPLE, **FOOD, **GREET}

if __name__ == '__main__':
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    for name, body_ in ALL.items():
        with open(os.path.join(out, name + '.svg'), 'w') as f:
            f.write(_circles_to_paths(body_))
    print(len(ALL), 'illustrations')
