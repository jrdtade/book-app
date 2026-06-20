# Folio — Expo (React Native)

A React Native + Expo port of the Folio EPUB-reader design. This is the
cross-platform (iOS / Android) sibling of the native Kotlin app in `../app`.

It faithfully recreates the prototype's five surfaces — Reading (home),
Library, Book detail, Stats, and Settings — plus an in-book **Reader** with a
live customization sheet (theme, typeface, size, line spacing, margins,
brightness, warmth, paged/scroll layout, and page-turn animation). Reader
preferences persist via AsyncStorage, as does each book's last page.

## Design fidelity

- The prototype's `oklch(...)` design tokens were converted to sRGB hex
  (`src/theme.ts`), including the typographic cover gradients, heatmap ramp,
  genre colors, and per-row settings tints.
- Type hierarchy (serif / sans / mono) maps to platform fonts — no webfonts
  are bundled, so it works offline in Expo Go out of the box.
- Sample library, stats, and the Pride and Prejudice chapter are the same
  public-domain mock data the prototype shipped with (`src/data.ts`).

## Run it

```bash
cd folio-expo
npm install
npm start          # then press i / a, or scan the QR with Expo Go
```

Requires Node 18+ and the Expo Go app (SDK 51) on a device, or an
iOS Simulator / Android emulator.

## Layout

```
App.tsx                      Navigation root (stack: Tabs + Detail + Reader)
src/theme.ts                 Converted color tokens, shadows, reader themes
src/data.ts                  Books, stats, highlights, sample chapter
src/lib/prefs.tsx            Reader prefs context + AsyncStorage persistence
src/components/              Cover, Ring, Stars, Icon, ChapterBody, CustomSheet, ui
src/screens/                 Home, Library, Detail, Stats, Settings, Reader
src/navigation/              Bottom tabs + route types
```

## Reader notes

Paged mode measures the chapter once (hidden column) and slices it into
fixed-height pages, swept horizontally with an `Animated` translate. `none`
turns instantly; `curl` / `slide` / `fade` currently share an animated slide
(a true page-curl would need a GL/skia surface). Scroll mode is a plain
vertical `ScrollView` with live progress.
