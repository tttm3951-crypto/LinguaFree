#!/usr/bin/env python3
"""LinguaFree's own icon set. Single source of truth: SVG strings below.
Run:  python3 scripts/icons/icons.py OUT_SVG_DIR
then convert with svg2vectordrawable (see scripts/icons/README.md). All icons are original,
flat, 24x24 viewport, multi-colour (not tinted) except those whose name ends in `_mono`.
"""
import os, sys

FERN, FERN_D = '#4CC417', '#3A9A0F'
EMBER, EMBER_D, EMBER_L = '#FF8A1F', '#E0650A', '#FFC44D'
ICE, ICE_D, ICE_L = '#2EB8F2', '#1C8FC4', '#9BE3FF'
HEART, HEART_D = '#FF4B6E', '#D93356'
HONEY, HONEY_D = '#FFC21F', '#D99A00'
PLUM, PLUM_D = '#A56BF7', '#7F48D1'
GREY, GREY_D = '#C9CED1', '#9CA4A8'
BROWN, BROWN_D = '#B8743A', '#8A5225'
WHITE = '#FFFFFF'
NAVY = '#2D4A8A'
RED = '#E8414F'

def svg(body):
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24">{body}</svg>'

ICONS = {
    # ---- bottom navigation ----
    'ic_tab_learn': svg(
        f'<path d="M3 11.2 12 3.5l9 7.7V20a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 20z" fill="{EMBER}"/>'
        f'<path d="M1.6 11.6 12 2.7l10.4 8.9-1.2 1.4L12 5.1 2.8 13z" fill="{EMBER_D}"/>'
        f'<rect x="9.3" y="13.5" width="5.4" height="8" rx="1.2" fill="{BROWN}"/>'
        f'<circle cx="13.3" cy="17.6" r=".7" fill="{EMBER_L}"/>'),
    'ic_tab_practice': svg(
        f'<rect x="1.5" y="8" width="3.4" height="8" rx="1.2" fill="{ICE_D}"/>'
        f'<rect x="19.1" y="8" width="3.4" height="8" rx="1.2" fill="{ICE_D}"/>'
        f'<rect x="4.2" y="6" width="3.6" height="12" rx="1.3" fill="{ICE}"/>'
        f'<rect x="16.2" y="6" width="3.6" height="12" rx="1.3" fill="{ICE}"/>'
        f'<rect x="7.6" y="10.6" width="8.8" height="2.8" rx="1" fill="{GREY_D}"/>'),
    'ic_tab_leagues': svg(
        f'<path d="M6 3h12v5.5a6 6 0 0 1-12 0z" fill="{HONEY}"/>'
        f'<path d="M6 5H3.2v2.2A3.6 3.6 0 0 0 6.6 11M18 5h2.8v2.2a3.6 3.6 0 0 1-3.4 3.8" fill="none" stroke="{HONEY_D}" stroke-width="1.6"/>'
        f'<path d="M10.4 13.8h3.2v3.4h-3.2z" fill="{HONEY_D}"/>'
        f'<rect x="7" y="17" width="10" height="4" rx="1.2" fill="{BROWN}"/>'
        f'<path d="M12 5.2l.9 1.9 2 .3-1.5 1.4.4 2-1.8-1-1.8 1 .4-2-1.5-1.4 2-.3z" fill="{WHITE}"/>'),
    'ic_tab_shop': svg(
        f'<rect x="2.5" y="9" width="19" height="12.5" rx="2" fill="{PLUM}"/>'
        f'<path d="M2.5 11a2 2 0 0 1 2-2h15a2 2 0 0 1 2 2v2h-19z" fill="{PLUM_D}"/>'
        f'<rect x="10.3" y="9" width="3.4" height="12.5" fill="{HONEY}"/>'
        f'<path d="M12 9C10 4.5 6.5 5 7 7.2 7.4 9 12 9 12 9zm0 0c2-4.5 5.5-4 5-1.8C16.6 9 12 9 12 9z" fill="{HONEY}" stroke="{HONEY_D}" stroke-width=".8"/>'),
    'ic_tab_profile': svg(
        f'<path d="M3 3.5 8 8h8l5-4.5-1 9.5c0 4.2-3.6 8.5-8 8.5s-8-4.3-8-8.5z" fill="{EMBER}"/>'
        f'<path d="M5.4 7.6 4.6 4.8 7.2 7z M18.6 7.6l.8-2.8L16.8 7z" fill="#5A3418"/>'
        f'<path d="M12 21.5c-2.9 0-5.4-2.3-5.4-4.4 0-1.5 2.4-2.6 5.4-2.6s5.4 1.1 5.4 2.6c0 2.1-2.5 4.4-5.4 4.4z" fill="{WHITE}"/>'
        f'<circle cx="8.8" cy="12.2" r="1.3" fill="#3A2412"/><circle cx="15.2" cy="12.2" r="1.3" fill="#3A2412"/>'
        f'<ellipse cx="12" cy="15.3" rx="1.2" ry=".85" fill="#3A2412"/>'),
    # ---- stats ----
    'ic_flame': svg(
        f'<path d="M12 2.2c.8 3.2 5.8 5.6 5.8 11.4A5.8 5.8 0 0 1 12 19.4a5.8 5.8 0 0 1-5.8-5.8c0-2.7 1.4-4.4 2.6-5.6.2 1.7.9 2.7 1.9 3.1C10.6 7.6 11 4.8 12 2.2z" fill="{EMBER}"/>'
        f'<path d="M12 10.2c.6 1.9 3.1 3 3.1 5.6A3.1 3.1 0 0 1 12 19a3.1 3.1 0 0 1-3.1-3.2c0-1.4.8-2.4 1.6-3 .1.8.4 1.4 1 1.6-.2-1.4.1-3 .5-4.2z" fill="{EMBER_L}"/>'
        f'<rect x="6.2" y="20.2" width="11.6" height="1.6" rx=".8" fill="{EMBER_D}" fill-opacity=".35"/>'),
    'ic_flame_off': svg(
        f'<path d="M12 2.2c.8 3.2 5.8 5.6 5.8 11.4A5.8 5.8 0 0 1 12 19.4a5.8 5.8 0 0 1-5.8-5.8c0-2.7 1.4-4.4 2.6-5.6.2 1.7.9 2.7 1.9 3.1C10.6 7.6 11 4.8 12 2.2z" fill="{GREY}"/>'
        f'<path d="M12 10.2c.6 1.9 3.1 3 3.1 5.6A3.1 3.1 0 0 1 12 19a3.1 3.1 0 0 1-3.1-3.2c0-1.4.8-2.4 1.6-3 .1.8.4 1.4 1 1.6-.2-1.4.1-3 .5-4.2z" fill="#E4E7E9"/>'),
    'ic_gem': svg(
        f'<path d="M6.5 3.5h11l4 5.5L12 21.2 2.5 9z" fill="{ICE}"/>'
        f'<path d="M2.5 9h19L12 21.2z" fill="{ICE_D}"/>'
        f'<path d="M6.5 3.5 9 9h6l2.5-5.5z" fill="{ICE_L}"/>'
        f'<path d="M9 9l3 12.2L15 9z" fill="{ICE}"/>'),
    'ic_heart': svg(
        f'<path d="M12 21.2S2.2 15.3 2.2 8.6A5 5 0 0 1 12 6.4a5 5 0 0 1 9.8 2.2c0 6.7-9.8 12.6-9.8 12.6z" fill="{HEART}"/>'
        f'<path d="M6.4 6.6a2.6 2.6 0 0 0-2.1 2.6" stroke="{WHITE}" stroke-opacity=".7" stroke-width="1.6" stroke-linecap="round" fill="none"/>'),
    'ic_heart_empty': svg(
        f'<path d="M12 21.2S2.2 15.3 2.2 8.6A5 5 0 0 1 12 6.4a5 5 0 0 1 9.8 2.2c0 6.7-9.8 12.6-9.8 12.6z" fill="{GREY}"/>'),
    'ic_heart_infinite': svg(
        f'<path d="M12 21.2S2.2 15.3 2.2 8.6A5 5 0 0 1 12 6.4a5 5 0 0 1 9.8 2.2c0 6.7-9.8 12.6-9.8 12.6z" fill="{HEART}"/>'
        f'<path d="M7.4 11.6c0-1.2 1-2 2-2 1.7 0 3.5 4 5.2 4 1 0 2-.8 2-2s-1-2-2-2c-1.7 0-3.5 4-5.2 4-1 0-2-.8-2-2z" fill="none" stroke="{WHITE}" stroke-width="1.5"/>'),
    'ic_xp': svg(
        f'<path d="M13.6 1.8 4.4 13.4h6l-1.6 8.8 10.8-12.6h-6.4z" fill="{HONEY}" stroke="{HONEY_D}" stroke-width="1" stroke-linejoin="round"/>'),
    'ic_flag_en': svg(
        f'<rect x="1.5" y="4.5" width="21" height="15" rx="3" fill="{WHITE}"/>'
        f'<path d="M1.5 7.5a3 3 0 0 1 3-3h15a3 3 0 0 1 3 3v.5h-21zM1.5 11h21v2.2h-21zM1.5 16h21v.5a3 3 0 0 1-3 3h-15a3 3 0 0 1-3-3z" fill="{RED}"/>'
        f'<path d="M1.5 7.5a3 3 0 0 1 3-3H11v8.5H1.5z" fill="{NAVY}"/>'
        f'<circle cx="4.2" cy="7.4" r=".7" fill="{WHITE}"/><circle cx="6.8" cy="7.4" r=".7" fill="{WHITE}"/><circle cx="9.3" cy="7.4" r=".7" fill="{WHITE}"/>'
        f'<circle cx="5.5" cy="10" r=".7" fill="{WHITE}"/><circle cx="8.1" cy="10" r=".7" fill="{WHITE}"/>'
        f'<rect x="1.5" y="4.5" width="21" height="15" rx="3" fill="none" stroke="#00000022" stroke-width="1"/>'),
    'ic_snowflake': svg(
        f'<circle cx="12" cy="12" r="10.3" fill="{ICE_L}"/>'
        f'<path d="M12 4.5v15M5.5 8.2l13 7.6M5.5 15.8l13-7.6M10 5.8l2 1.8 2-1.8M10 18.2l2-1.8 2 1.8" stroke="{ICE_D}" stroke-width="1.6" stroke-linecap="round" fill="none"/>'),
    'ic_double_xp': svg(
        f'<circle cx="12" cy="12" r="10.3" fill="{HONEY}"/>'
        f'<path d="M10.6 4.2 5.4 11.6h3.5l-.9 6.3 5.8-8h-3.8z" fill="{WHITE}"/>'
        f'<path d="M16.4 6.2 11.2 13.6h3.5l-.9 6.3 5.8-8h-3.8z" fill="{EMBER_D}"/>'),
    'ic_lock': svg(
        f'<rect x="4.5" y="10" width="15" height="11" rx="2.5" fill="{GREY_D}"/>'
        f'<path d="M7.8 10V7.6a4.2 4.2 0 0 1 8.4 0V10" fill="none" stroke="{GREY_D}" stroke-width="2.2"/>'
        f'<circle cx="12" cy="15" r="1.6" fill="{WHITE}"/><rect x="11.3" y="15" width="1.4" height="3" rx=".6" fill="{WHITE}"/>'),
    'ic_star': svg(
        f'<path d="M12 2.4l2.9 6 6.5.9-4.7 4.6 1.1 6.5L12 17.3l-5.8 3.1 1.1-6.5-4.7-4.6 6.5-.9z" fill="{WHITE}"/>'),
    'ic_check': svg(
        f'<path d="M4.5 12.5 9.8 17.6 19.6 6.8" fill="none" stroke="{WHITE}" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"/>'),
    'ic_chest': svg(
        f'<path d="M3 10.5h18V20a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 20z" fill="{BROWN}"/>'
        f'<path d="M3 10.5V8a5 5 0 0 1 5-5h8a5 5 0 0 1 5 5v2.5z" fill="{BROWN_D}"/>'
        f'<path d="M3 10.5h18v2.3H3zM6.2 3.4v18M17.8 3.4v18" stroke="{HONEY}" stroke-width="1.6" fill="none"/>'
        f'<rect x="10" y="9.2" width="4" height="5.2" rx="1" fill="{HONEY}"/><circle cx="12" cy="11.6" r=".8" fill="{BROWN_D}"/>'),
    'ic_chest_open': svg(
        f'<path d="M3 11.5h18V20a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 20z" fill="{BROWN}"/>'
        f'<path d="M4 9.8 5.4 3.6l13.2 0 1.4 6.2z" fill="{BROWN_D}"/>'
        f'<path d="M7 11.5l2-4h6l2 4z" fill="{ICE}"/><circle cx="12" cy="8.6" r="1.4" fill="{ICE_L}"/>'
        f'<path d="M3 11.5h18v2.3H3z" fill="{HONEY}"/>'),
    'ic_book': svg(
        f'<path d="M2.5 5.2c3.4-1.4 6.6-1 9.5.9v14.4c-2.9-1.9-6.1-2.3-9.5-.9z" fill="{ICE}"/>'
        f'<path d="M21.5 5.2c-3.4-1.4-6.6-1-9.5.9v14.4c2.9-1.9 6.1-2.3 9.5-.9z" fill="{ICE_D}"/>'
        f'<path d="M5 8.6c1.6-.4 3.3-.2 4.8.6M5 11.6c1.6-.4 3.3-.2 4.8.6M14.2 9.2c1.5-.8 3.2-1 4.8-.6M14.2 12.2c1.5-.8 3.2-1 4.8-.6" stroke="{WHITE}" stroke-width="1.1" stroke-linecap="round" fill="none"/>'),
    'ic_story': svg(
        f'<path d="M3 4.5A2.5 2.5 0 0 1 5.5 2H15l6 6v11.5a2.5 2.5 0 0 1-2.5 2.5h-13A2.5 2.5 0 0 1 3 19.5z" fill="{PLUM}"/>'
        f'<path d="M15 2v4a2 2 0 0 0 2 2h4z" fill="{PLUM_D}"/>'
        f'<path d="M7 12h10M7 15.5h10M7 19h6" stroke="{WHITE}" stroke-width="1.6" stroke-linecap="round"/>'),
    'ic_dumbbell': svg(
        f'<rect x="2" y="8.5" width="3" height="7" rx="1" fill="{WHITE}"/><rect x="19" y="8.5" width="3" height="7" rx="1" fill="{WHITE}"/>'
        f'<rect x="4.5" y="6.5" width="3.4" height="11" rx="1.2" fill="{WHITE}"/><rect x="16.1" y="6.5" width="3.4" height="11" rx="1.2" fill="{WHITE}"/>'
        f'<rect x="7.6" y="10.8" width="8.8" height="2.4" rx="1" fill="{WHITE}"/>'),
    'ic_trophy': svg(
        f'<path d="M6 3h12v5.5a6 6 0 0 1-12 0z" fill="{WHITE}"/>'
        f'<path d="M6 5H3.4v2.2A3.6 3.6 0 0 0 6.6 11M18 5h2.6v2.2a3.6 3.6 0 0 1-3.2 3.8" fill="none" stroke="{WHITE}" stroke-width="1.6"/>'
        f'<path d="M10.4 13.8h3.2v3.4h-3.2zM7 17h10v4H7z" fill="{WHITE}"/>'),
    'ic_crown': svg(
        f'<path d="M3 8l4.5 4L12 4.5 16.5 12 21 8l-2 11H5z" fill="{HONEY}" stroke="{HONEY_D}" stroke-width="1" stroke-linejoin="round"/>'
        f'<circle cx="12" cy="14.5" r="1.4" fill="{HEART}"/><rect x="5" y="19" width="14" height="2.4" rx="1" fill="{HONEY_D}"/>'),
    'ic_speaker': svg(
        f'<path d="M3 9.2h3.8L12 4.6v14.8L6.8 14.8H3z" fill="{WHITE}"/>'
        f'<path d="M15 8.6a4.8 4.8 0 0 1 0 6.8M17.6 6a8.4 8.4 0 0 1 0 12" fill="none" stroke="{WHITE}" stroke-width="2" stroke-linecap="round"/>'),
    'ic_turtle': svg(
        f'<path d="M4 15.5a8 6.5 0 0 1 16 0z" fill="{WHITE}"/>'
        f'<circle cx="20.3" cy="13.6" r="2.2" fill="{WHITE}"/>'
        f'<path d="M5.2 15.5l-.6 3M9.5 15.5l-.3 3M14.5 15.5l.3 3M18.8 15.5l.6 3" stroke="{WHITE}" stroke-width="2.2" stroke-linecap="round"/>'),
    'ic_mic': svg(
        f'<rect x="8.5" y="2.5" width="7" height="12" rx="3.5" fill="{WHITE}"/>'
        f'<path d="M5.5 11.5a6.5 6.5 0 0 0 13 0M12 18v3.5M8.5 21.5h7" fill="none" stroke="{WHITE}" stroke-width="2" stroke-linecap="round"/>'),
    'ic_target': svg(
        f'<circle cx="12" cy="12" r="10" fill="{HEART}"/><circle cx="12" cy="12" r="7" fill="{WHITE}"/>'
        f'<circle cx="12" cy="12" r="4.2" fill="{HEART}"/><circle cx="12" cy="12" r="1.6" fill="{WHITE}"/>'),
    'ic_medal': svg(
        f'<path d="M7 2h4l2 6H9zM13 2h4l-2 6h-4z" fill="{ICE}"/>'
        f'<circle cx="12" cy="15" r="7" fill="{HONEY}" stroke="{HONEY_D}" stroke-width="1.2"/>'
        f'<path d="M12 11.4l1.1 2.2 2.4.3-1.7 1.7.4 2.4-2.2-1.2-2.2 1.2.4-2.4-1.7-1.7 2.4-.3z" fill="{WHITE}"/>'),
    'ic_calendar': svg(
        f'<rect x="3" y="4.5" width="18" height="17" rx="3" fill="{WHITE}" stroke="{GREY_D}" stroke-width="1.2"/>'
        f'<path d="M3 7.5a3 3 0 0 1 3-3h12a3 3 0 0 1 3 3V9H3z" fill="{HEART}"/>'
        f'<rect x="7" y="2.5" width="2" height="4" rx="1" fill="{GREY_D}"/><rect x="15" y="2.5" width="2" height="4" rx="1" fill="{GREY_D}"/>'
        f'<circle cx="8" cy="13" r="1.2" fill="{EMBER}"/><circle cx="12" cy="13" r="1.2" fill="{EMBER}"/><circle cx="16" cy="13" r="1.2" fill="{GREY}"/>'
        f'<circle cx="8" cy="17" r="1.2" fill="{GREY}"/><circle cx="12" cy="17" r="1.2" fill="{GREY}"/>'),
    'ic_settings_mono': svg(
        '<path d="M12 8.2a3.8 3.8 0 1 0 0 7.6 3.8 3.8 0 0 0 0-7.6zm8.4 5.1-1.9-.4a6.7 6.7 0 0 1-.6 1.5l1.1 1.6-1.9 1.9-1.6-1.1a6.7 6.7 0 0 1-1.5.6l-.4 1.9h-2.7l-.4-1.9a6.7 6.7 0 0 1-1.5-.6l-1.6 1.1-1.9-1.9 1.1-1.6a6.7 6.7 0 0 1-.6-1.5l-1.9-.4v-2.7l1.9-.4c.1-.5.3-1 .6-1.5L4.7 6.4l1.9-1.9 1.6 1.1c.5-.3 1-.5 1.5-.6l.4-1.9h2.7l.4 1.9c.5.1 1 .3 1.5.6l1.6-1.1 1.9 1.9-1.1 1.6c.3.5.5 1 .6 1.5l1.9.4z" fill="#000"/>'),
    'ic_close_mono': svg('<path d="M6 6l12 12M18 6 6 18" stroke="#000" stroke-width="2.6" stroke-linecap="round"/>'),
    'ic_arrow_down_mono': svg('<path d="M12 4v15M5.5 12.5 12 19l6.5-6.5" fill="none" stroke="#000" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/>'),
    'ic_back_mono': svg('<path d="M15 4.5 7.5 12l7.5 7.5" fill="none" stroke="#000" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/>'),
}

def _circles_to_paths(body):
    # svg2vectordrawable drops <circle>/<ellipse>; emit equivalent arc paths instead.
    import re
    def attrs(tag):
        return dict(re.findall(r'([\w-]+)="([^"]*)"', tag))
    def ellipse(tag, kind):
        a = attrs(tag)
        cx, cy = float(a.pop('cx')), float(a.pop('cy'))
        if kind == 'circle':
            rx = ry = float(a.pop('r'))
        else:
            rx, ry = float(a.pop('rx')), float(a.pop('ry'))
        d = (f'M{cx - rx:g} {cy:g}a{rx:g} {ry:g} 0 1 0 {2 * rx:g} 0'
             f'a{rx:g} {ry:g} 0 1 0 {-2 * rx:g} 0z')
        rest = ' '.join(f'{k}="{v}"' for k, v in a.items())
        return f'<path d="{d}" {rest}/>'
    body = re.sub(r'<circle\b[^>]*/>', lambda m: ellipse(m.group(0), 'circle'), body)
    body = re.sub(r'<ellipse\b[^>]*/>', lambda m: ellipse(m.group(0), 'ellipse'), body)
    return body

if __name__ == '__main__':
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    for name, body in ICONS.items():
        with open(os.path.join(out, name + '.svg'), 'w') as f:
            f.write(_circles_to_paths(body))
    print(len(ICONS), 'icons')
