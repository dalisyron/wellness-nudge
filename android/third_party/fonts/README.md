# Bundled fonts

The app ships its typefaces in `app/src/main/res/font/` instead of using
downloadable fonts, so text renders identically offline, on first launch and
in JVM snapshot tests.

| Font | Files | Source | License |
|------|-------|--------|---------|
| Manrope (Regular, Medium, SemiBold, Bold) | `manrope_*.ttf` | [googlefonts/manrope](https://github.com/googlefonts/manrope) | SIL Open Font License 1.1, see [`OFL_Manrope.txt`](OFL_Manrope.txt) |
| Newsreader (Regular and Italic at the 16 pt text size, Regular at the 36 pt display size) | `newsreader_*.ttf` | [productiontype/Newsreader](https://github.com/productiontype/Newsreader) | SIL Open Font License 1.1, see [`OFL_Newsreader.txt`](OFL_Newsreader.txt) |

All are static instances served by Google Fonts. Newsreader has an optical-size
axis: `newsreader_display.ttf` is the `opsz 36` instance, with finer hairlines
and tighter spacing for titles and the nudge itself, while the 16 pt files set
the 18 sp quotes and previews. The license texts live here because Android
resource directories only accept resource files.
