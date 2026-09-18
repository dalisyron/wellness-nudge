# Bundled fonts

The app ships its typefaces in `app/src/main/res/font/` instead of using
downloadable fonts, so text renders identically offline, on first launch and
in JVM snapshot tests.

| Font | Files | Source | License |
|------|-------|--------|---------|
| Manrope (Regular, Medium, SemiBold, Bold, ExtraBold) | `manrope_*.ttf` | [googlefonts/manrope](https://github.com/googlefonts/manrope) | SIL Open Font License 1.1, see [`OFL_Manrope.txt`](OFL_Manrope.txt) |
| Newsreader (Regular, Medium, Italic) | `newsreader_*.ttf` | [productiontype/Newsreader](https://github.com/productiontype/Newsreader) | SIL Open Font License 1.1, see [`OFL_Newsreader.txt`](OFL_Newsreader.txt) |

Both are static instances served by Google Fonts. The license texts live here
because Android resource directories only accept resource files.
