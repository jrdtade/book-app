import React from 'react';
import { Feather, Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { C } from '../theme';

// Maps the prototype's icon names onto vector-icon glyphs across a few sets.
type Set = 'feather' | 'ion' | 'mci';
const MAP: Record<string, [Set, string]> = {
  feather: ['feather', 'feather'],
  home: ['feather', 'home'],
  library: ['feather', 'book'],
  chart: ['feather', 'bar-chart-2'],
  gear: ['feather', 'settings'],
  bookOpen: ['feather', 'book-open'],
  star: ['feather', 'star'],
  clock: ['feather', 'clock'],
  pages: ['feather', 'file-text'],
  flame: ['ion', 'flame'],
  grid: ['feather', 'grid'],
  rows: ['feather', 'list'],
  search: ['feather', 'search'],
  check: ['feather', 'check'],
  chevR: ['feather', 'chevron-right'],
  back: ['feather', 'arrow-left'],
  more: ['feather', 'more-horizontal'],
  highlighter: ['mci', 'marker'],
  bookmark: ['feather', 'bookmark'],
  x: ['feather', 'x'],
  sun: ['feather', 'sun'],
  droplet: ['feather', 'droplet'],
  alignL: ['feather', 'align-left'],
  align: ['feather', 'align-justify'],
  list: ['feather', 'list'],
  aa: ['mci', 'format-size'],
  type: ['feather', 'type'],
  moon: ['feather', 'moon'],
  download: ['feather', 'download'],
  sliders: ['feather', 'sliders'],
  target: ['feather', 'target'],
  calendar: ['feather', 'calendar'],
  share: ['feather', 'share'],
  heart: ['feather', 'heart'],
  cloudOff: ['feather', 'cloud-off'],
  trophy: ['ion', 'trophy'],
};

export function Icon({
  name,
  size = 20,
  color = C.ink,
}: {
  name: string;
  size?: number;
  color?: string;
}) {
  const entry = MAP[name] || (['feather', 'circle'] as [Set, string]);
  const [set, glyph] = entry;
  if (set === 'ion') return <Ionicons name={glyph as any} size={size} color={color} />;
  if (set === 'mci') return <MaterialCommunityIcons name={glyph as any} size={size} color={color} />;
  return <Feather name={glyph as any} size={size} color={color} />;
}
