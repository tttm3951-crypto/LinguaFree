#!/usr/bin/env python3
"""Outfits for the fox (shop cosmetics). Drawn for LinguaFree. Usage: accessories.py OUT_SVG_DIR"""
import os, sys
sys.path.insert(0, os.path.dirname(__file__))
from icons import _circles_to_paths  # noqa: E402

ACC = {
    # 120×80 cap, peak to the right
    'acc_cap': '<svg xmlns="http://www.w3.org/2000/svg" width="120" height="80" viewBox="0 0 120 80">'
               '<path d="M14 58c0-26 18-44 44-44s44 18 44 44z" fill="#2EB8F2"/>'
               '<path d="M58 14c-6 10-8 26-8 44h16c0-18-2-34-8-44z" fill="#1C8FC4"/>'
               '<path d="M96 52c10 0 20 3 22 9H58z" fill="#1C8FC4"/>'
               '<rect x="12" y="54" width="92" height="9" rx="4" fill="#1C8FC4"/>'
               '<circle cx="58" cy="13" r="5" fill="#FFC21F"/></svg>',
    'acc_crown': '<svg xmlns="http://www.w3.org/2000/svg" width="100" height="70" viewBox="0 0 100 70">'
                 '<path d="M8 22l20 18L50 6l22 34 20-18-8 40H16z" fill="#FFC21F" stroke="#D99A00" stroke-width="3" stroke-linejoin="round"/>'
                 '<rect x="16" y="56" width="68" height="10" rx="4" fill="#D99A00"/>'
                 '<circle cx="50" cy="44" r="6" fill="#FF4B6E"/><circle cx="30" cy="48" r="4" fill="#2EB8F2"/><circle cx="70" cy="48" r="4" fill="#4CC417"/></svg>',
    'acc_bow': '<svg xmlns="http://www.w3.org/2000/svg" width="80" height="50" viewBox="0 0 80 50">'
               '<path d="M40 25 8 6c-6 6-6 32 0 38z" fill="#FF4B6E"/><path d="M40 25 72 6c6 6 6 32 0 38z" fill="#FF4B6E"/>'
               '<path d="M40 25 14 14c-2 6-2 16 0 22z" fill="#D93356"/><path d="M40 25 66 14c2 6 2 16 0 22z" fill="#D93356"/>'
               '<circle cx="40" cy="25" r="9" fill="#FF7A95"/></svg>',
    'acc_scarf': '<svg xmlns="http://www.w3.org/2000/svg" width="160" height="90" viewBox="0 0 160 90">'
                 '<path d="M6 20c40 18 108 18 148 0l2 20c-44 20-108 20-152 0z" fill="#F2464B"/>'
                 '<path d="M30 30l4 22M60 36v22M92 36v22M124 30l-4 22" stroke="#FFFFFF" stroke-width="7"/>'
                 '<path d="M104 44l18 2-6 42-20-4z" fill="#F2464B"/><path d="M104 60l16 3M100 76l15 3" stroke="#FFFFFF" stroke-width="6"/></svg>',
}

if __name__ == '__main__':
    out = sys.argv[1]
    os.makedirs(out, exist_ok=True)
    for k, v in ACC.items():
        open(os.path.join(out, k + '.svg'), 'w').write(_circles_to_paths(v))
    print(len(ACC))
