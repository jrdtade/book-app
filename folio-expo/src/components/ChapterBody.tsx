import React from 'react';
import { Text, View } from 'react-native';
import { CHAPTER } from '../data';
import { F } from '../theme';
import { Prefs } from '../lib/prefs';

// Highlight injections matching the prototype: para 0 (yellow) and para 32 (blue).
const HL0 = 'single man in possession of a good fortune';
const HL32 = 'quick parts, sarcastic humour, reserve, and caprice';

function family(prefs: Prefs) {
  return prefs.font === 'sans' ? F.sans : F.serif;
}

function ParaText({ text, prefs, fg }: { text: string; prefs: Prefs; fg: string }) {
  const base = {
    fontFamily: family(prefs),
    fontSize: prefs.size,
    lineHeight: prefs.size * prefs.lh,
    color: fg,
    fontWeight: (prefs.weight === 600 ? '600' : '400') as '400' | '600',
    textAlign: prefs.align,
    marginBottom: prefs.size,
  };

  // split for highlight if needed
  let segments: { t: string; hl?: string }[] = [{ t: text }];
  const split = (needle: string, color: string) => {
    const idx = text.indexOf(needle);
    if (idx >= 0) {
      segments = [
        { t: text.slice(0, idx) },
        { t: needle, hl: color },
        { t: text.slice(idx + needle.length) },
      ];
    }
  };
  if (text.includes(HL0)) split(HL0, '#F4E3A8');
  else if (text.includes(HL32)) split(HL32, '#B9D2F6');

  return (
    <Text style={base}>
      {segments.map((s, i) =>
        s.hl ? (
          <Text key={i} style={{ backgroundColor: s.hl }}>
            {s.t}
          </Text>
        ) : (
          <Text key={i}>{s.t}</Text>
        ),
      )}
    </Text>
  );
}

export function ChapterBody({ prefs, fg }: { prefs: Prefs; fg: string }) {
  return (
    <View>
      <Text
        style={{
          fontFamily: F.mono,
          fontSize: 12,
          letterSpacing: 2,
          textTransform: 'uppercase',
          color: fg,
          opacity: 0.6,
          marginBottom: 6,
        }}
      >
        {CHAPTER.no}
      </Text>
      <Text
        style={{
          fontFamily: family(prefs),
          fontSize: prefs.size * 1.5,
          lineHeight: prefs.size * 1.5 * 1.15,
          fontWeight: '600',
          color: fg,
          marginBottom: prefs.size * 1.2,
        }}
      >
        {CHAPTER.title}
      </Text>
      {CHAPTER.paras.map((p, i) => (
        <ParaText key={i} text={p} prefs={prefs} fg={fg} />
      ))}
    </View>
  );
}
