# Иконки LinguaFree

Все иконки нарисованы для LinguaFree и описаны SVG-строками в `icons.py` (единый источник).

Обновление:

```bash
python3 scripts/icons/icons.py /tmp/lf-svg          # SVG (круги переводятся в пути)
npx svg2vectordrawable -p 3 -f /tmp/lf-svg -o core/ui/src/main/res/drawable
```

Иконки с суффиксом `_mono` — одноцветные, их цвет задаётся в Compose через `tint`.
