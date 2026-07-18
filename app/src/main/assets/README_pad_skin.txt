Drop your realistic button skin here as: pad_skin.png  (PNG, or pad_skin.svg)

For correct compositing over each pad's colour:
- 32-bit PNG WITH ALPHA (transparency).
- Color-neutral (grays/whites), NOT a coloured button.
- Transparent CENTRE/face so the pad colour shows through the glass.
- Transparent OUTSIDE the rounded corners (no square background).
- Bake the relief into the alpha/luminance: bezel rim, top gloss highlight,
  bottom inner shadow, soft outer drop shadow.
- Square 1:1, high-res (1024x1024 min, 2048 better), top-down view.
The app renders it at runtime (Coil). Absent file = flat gradient pads.
