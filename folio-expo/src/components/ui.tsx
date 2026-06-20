import React from 'react';
import { Text, TextProps, View, ViewProps } from 'react-native';
import { C, F, shadow } from '../theme';

export function Card({ style, children, ...rest }: ViewProps) {
  return (
    <View
      {...rest}
      style={[
        { backgroundColor: C.paper2, borderRadius: 18, borderWidth: 1, borderColor: C.line },
        shadow.sm,
        style,
      ]}
    >
      {children}
    </View>
  );
}

export function Eyebrow({ children, style }: { children: React.ReactNode; style?: any }) {
  return (
    <Text
      style={[
        {
          fontFamily: F.mono,
          fontSize: 11,
          letterSpacing: 1.4,
          textTransform: 'uppercase',
          color: C.ink3,
          fontWeight: '600',
        },
        style,
      ]}
    >
      {children}
    </Text>
  );
}

export function Serif({ style, children, ...rest }: TextProps) {
  return (
    <Text {...rest} style={[{ fontFamily: F.serif, color: C.ink }, style]}>
      {children}
    </Text>
  );
}

export function Mono({ style, children, ...rest }: TextProps) {
  return (
    <Text {...rest} style={[{ fontFamily: F.mono, color: C.ink }, style]}>
      {children}
    </Text>
  );
}

// Thin progress track with blue fill.
export function Track({ pct, style }: { pct: number; style?: any }) {
  return (
    <View style={[{ height: 6, borderRadius: 3, backgroundColor: C.paper3, overflow: 'hidden' }, style]}>
      <View style={{ width: `${Math.max(0, Math.min(1, pct)) * 100}%`, height: '100%', backgroundColor: C.blue, borderRadius: 3 }} />
    </View>
  );
}

// Section header used across Home.
export function ScreenTitle({ kicker, title }: { kicker?: string; title: string }) {
  return (
    <View style={{ paddingHorizontal: 20, paddingTop: 8, paddingBottom: 4 }}>
      {kicker ? <Eyebrow style={{ marginBottom: 6 }}>{kicker}</Eyebrow> : null}
      <Serif style={{ fontSize: 34, fontWeight: '600', letterSpacing: -0.5, lineHeight: 38 }}>{title}</Serif>
    </View>
  );
}
