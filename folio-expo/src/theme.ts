// Folio design tokens — ported from the prototype's oklch CSS variables to sRGB hex.
export const C = {
  paper: '#F7F2E9', // app background
  paper2: '#FDFAF5', // raised cards
  paper3: '#EEE8DF', // sunken wells / tracks
  paper4: '#E7E0D5', // deeper well / hover
  ink: '#2D241E', // primary text — warm espresso
  ink2: '#5D544D', // secondary
  ink3: '#888079', // tertiary / muted
  line: '#DDD7CF', // hairlines
  line2: '#D0C9BF',
  blue: '#0068E4',
  blue2: '#0056D8',
  blueSoft: '#D5EAFF',
  blueSoftBorder: '#B5D0F5',
  blueInk: '#0040B7',
  gold: '#D1A255',
  white: '#FFFFFF',
};

// Card / elevation shadows (approximated for RN).
export const shadow = {
  sm: {
    shadowColor: '#3A322A',
    shadowOpacity: 0.08,
    shadowRadius: 4,
    shadowOffset: { width: 0, height: 2 },
    elevation: 2,
  },
  md: {
    shadowColor: '#3A322A',
    shadowOpacity: 0.12,
    shadowRadius: 14,
    shadowOffset: { width: 0, height: 6 },
    elevation: 5,
  },
  lg: {
    shadowColor: '#2E2820',
    shadowOpacity: 0.2,
    shadowRadius: 26,
    shadowOffset: { width: 0, height: 14 },
    elevation: 10,
  },
  book: {
    shadowColor: '#2A241C',
    shadowOpacity: 0.3,
    shadowRadius: 14,
    shadowOffset: { width: 0, height: 7 },
    elevation: 7,
  },
};

// Font families. JetBrains Mono / Newsreader etc. aren't bundled here; we map
// to platform serif/sans/mono so the type hierarchy survives without webfonts.
import { Platform } from 'react-native';
export const F = {
  serif: Platform.select({ ios: 'Georgia', android: 'serif', default: 'serif' })!,
  sans: Platform.select({ ios: 'System', android: 'sans-serif', default: 'System' })!,
  mono: Platform.select({ ios: 'Menlo', android: 'monospace', default: 'monospace' })!,
};

export type ReaderThemeId = 'paper' | 'sepia' | 'quartz' | 'night' | 'black';

export const READER_THEMES: { id: ReaderThemeId; label: string; bg: string; fg: string; chrome: string; icon: string }[] = [
  { id: 'paper', label: 'Paper', bg: '#FDFCFA', fg: '#27221D', chrome: '#FFFFFFCC', icon: '#5C564E' },
  { id: 'sepia', label: 'Sepia', bg: '#F3E5D0', fg: '#3F2E25', chrome: '#F3E6D2D0', icon: '#6B5848' },
  { id: 'quartz', label: 'Quartz', bg: '#C0C3C6', fg: '#202328', chrome: '#C0C3C6D0', icon: '#4A4E54' },
  { id: 'night', label: 'Night', bg: '#1C1E22', fg: '#C7C2BA', chrome: '#24262BCC', icon: '#BBB6AE' },
  { id: 'black', label: 'Black', bg: '#090909', fg: '#A7A49F', chrome: '#16171AD0', icon: '#9E9B96' },
];

export const HEAT_COLORS = ['#E7E0D5', '#A7C7F2', '#73A6EF', '#4087EE', '#006AE6'];

export const GENRE_COLORS: Record<string, string> = {
  Classics: '#006AE6',
  Literary: '#00939A',
  Gothic: '#7A5283',
  Adventure: '#519160',
  Poetry: '#BF9752',
};

export const SETTINGS_TINTS: Record<number, string> = {
  258: '#D0E6FF',
  30: '#FFD9D1',
  80: '#F6E2C0',
  260: '#D2E6FF',
  150: '#CEEFD3',
  280: '#DDE2FF',
  220: '#C1EDFC',
  350: '#FFD8E9',
  200: '#BFEFF2',
  160: '#C9EFD9',
  60: '#FEDEC4',
  20: '#FFD8D7',
};
