import React from 'react';
import Svg, { Circle } from 'react-native-svg';
import { C } from '../theme';

export function Ring({
  pct,
  size = 44,
  sw = 4,
  color = C.blue,
  track = C.paper4,
}: {
  pct: number;
  size?: number;
  sw?: number;
  color?: string;
  track?: string;
}) {
  const r = (size - sw) / 2;
  const c = 2 * Math.PI * r;
  return (
    <Svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
      <Circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke={track} strokeWidth={sw} />
      <Circle
        cx={size / 2}
        cy={size / 2}
        r={r}
        fill="none"
        stroke={color}
        strokeWidth={sw}
        strokeLinecap="round"
        strokeDasharray={c}
        strokeDashoffset={c * (1 - pct)}
        transform={`rotate(-90 ${size / 2} ${size / 2})`}
      />
    </Svg>
  );
}
