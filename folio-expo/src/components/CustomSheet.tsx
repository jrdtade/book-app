import React from 'react';
import { Modal, Pressable, ScrollView, Text, View } from 'react-native';
import Slider from '@react-native-community/slider';
import { Icon } from './Icon';
import { C, F, READER_THEMES } from '../theme';
import { Prefs, usePrefs } from '../lib/prefs';

export function CustomSheet({ visible, onClose }: { visible: boolean; onClose: () => void }) {
  const { prefs, setPrefs } = usePrefs();
  const dark = prefs.theme === 'night' || prefs.theme === 'black';
  const up = (patch: Partial<Prefs>) => setPrefs(patch);

  const bg = dark ? '#26282D' : C.paper2;
  const fg = dark ? '#EDEAE4' : C.ink;
  const subColor = dark ? '#9C9892' : C.ink3;
  const segBg = dark ? '#34373D' : C.paper3;
  const segOn = dark ? '#4A4E55' : C.paper2;

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable style={{ flex: 1, backgroundColor: '#00000055' }} onPress={onClose} />
      <View
        style={{
          position: 'absolute',
          left: 0,
          right: 0,
          bottom: 0,
          maxHeight: '88%',
          backgroundColor: bg,
          borderTopLeftRadius: 24,
          borderTopRightRadius: 24,
          paddingBottom: 28,
        }}
      >
        <View style={{ alignItems: 'center', paddingTop: 10 }}>
          <View style={{ width: 38, height: 5, borderRadius: 3, backgroundColor: dark ? '#4A4E55' : C.line2 }} />
        </View>
        <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 20, paddingTop: 6, paddingBottom: 8 }}>
          <Text style={{ fontFamily: F.serif, fontSize: 21, fontWeight: '600', color: fg }}>Display</Text>
          <Pressable onPress={onClose} hitSlop={10}>
            <Icon name="x" size={22} color={subColor} />
          </Pressable>
        </View>

        <ScrollView showsVerticalScrollIndicator={false}>
          {/* brightness */}
          <Section>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 14 }}>
              <Icon name="sun" size={18} color={subColor} />
              <Slider
                style={{ flex: 1 }}
                minimumValue={0.3}
                maximumValue={1}
                value={prefs.brightness}
                onValueChange={(v) => up({ brightness: v })}
                minimumTrackTintColor={C.blue}
                maximumTrackTintColor={segBg}
                thumbTintColor={C.blue}
              />
              <Icon name="sun" size={24} color={subColor} />
            </View>
          </Section>

          {/* themes */}
          <Section>
            <Label color={subColor}>Page theme</Label>
            <View style={{ flexDirection: 'row', gap: 12 }}>
              {READER_THEMES.map((t) => {
                const on = prefs.theme === t.id;
                return (
                  <Pressable
                    key={t.id}
                    onPress={() => up({ theme: t.id })}
                    style={{
                      width: 52,
                      height: 52,
                      borderRadius: 14,
                      backgroundColor: t.bg,
                      alignItems: 'center',
                      justifyContent: 'center',
                      borderWidth: on ? 2.5 : 1,
                      borderColor: on ? C.blue : '#00000022',
                    }}
                  >
                    <Text style={{ fontFamily: F.serif, fontSize: 19, fontWeight: '600', color: t.fg }}>Aa</Text>
                  </Pressable>
                );
              })}
            </View>
          </Section>

          {/* warmth */}
          <Section>
            <Label color={subColor}>Warmth</Label>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 14 }}>
              <Icon name="droplet" size={18} color={subColor} />
              <Slider
                style={{ flex: 1 }}
                minimumValue={0}
                maximumValue={1}
                value={prefs.warmth}
                onValueChange={(v) => up({ warmth: v })}
                minimumTrackTintColor="#E0A340"
                maximumTrackTintColor={segBg}
                thumbTintColor="#E0A340"
              />
              <View style={{ width: 22, height: 22, borderRadius: 11, backgroundColor: '#ECA851' }} />
            </View>
          </Section>

          <Divider dark={dark} />

          {/* typeface */}
          <Section>
            <Label color={subColor}>Typeface</Label>
            <Seg
              dark={dark}
              segBg={segBg}
              segOn={segOn}
              fg={fg}
              options={[
                { key: 'serif', label: 'Serif' },
                { key: 'sans', label: 'Sans-serif' },
              ]}
              value={prefs.font}
              onChange={(v) => up({ font: v })}
            />
          </Section>

          {/* font size */}
          <Section>
            <Label color={subColor}>Font size · {prefs.size}px</Label>
            <View style={{ flexDirection: 'row', backgroundColor: segBg, borderRadius: 12, height: 56, alignItems: 'center' }}>
              <Pressable onPress={() => up({ size: Math.max(15, prefs.size - 1) })} style={{ flex: 1, alignItems: 'center' }}>
                <Text style={{ fontSize: 17, color: fg }}>A</Text>
              </Pressable>
              <View style={{ width: 1, height: '60%', backgroundColor: dark ? '#4A4E55' : C.line2 }} />
              <View style={{ flex: 2, alignItems: 'center' }}>
                <Text style={{ fontSize: Math.min(30, prefs.size), fontFamily: prefs.font === 'sans' ? F.sans : F.serif, color: fg }}>Aa</Text>
              </View>
              <View style={{ width: 1, height: '60%', backgroundColor: dark ? '#4A4E55' : C.line2 }} />
              <Pressable onPress={() => up({ size: Math.min(30, prefs.size + 1) })} style={{ flex: 1, alignItems: 'center' }}>
                <Text style={{ fontSize: 30, color: fg }}>A</Text>
              </Pressable>
            </View>
          </Section>

          {/* line spacing */}
          <Section>
            <Label color={subColor}>Line spacing</Label>
            <Seg
              dark={dark}
              segBg={segBg}
              segOn={segOn}
              fg={fg}
              options={[
                { key: '1.42', label: 'Tight' },
                { key: '1.62', label: 'Normal' },
                { key: '1.95', label: 'Loose' },
              ]}
              value={String(prefs.lh)}
              onChange={(v) => up({ lh: parseFloat(v) })}
            />
          </Section>

          {/* margins */}
          <Section>
            <Label color={subColor}>Margins</Label>
            <Seg
              dark={dark}
              segBg={segBg}
              segOn={segOn}
              fg={fg}
              options={[
                { key: '18', label: 'Narrow' },
                { key: '30', label: 'Medium' },
                { key: '48', label: 'Wide' },
              ]}
              value={String(prefs.mx)}
              onChange={(v) => up({ mx: parseInt(v, 10) })}
            />
          </Section>

          {/* alignment + bold */}
          <Section>
            <View style={{ flexDirection: 'row', gap: 14 }}>
              <View style={{ flex: 1 }}>
                <Label color={subColor}>Alignment</Label>
                <Seg
                  dark={dark}
                  segBg={segBg}
                  segOn={segOn}
                  fg={fg}
                  options={[
                    { key: 'left', icon: 'alignL' },
                    { key: 'justify', icon: 'align' },
                  ]}
                  value={prefs.align}
                  onChange={(v) => up({ align: v as any })}
                />
              </View>
              <View style={{ flex: 1 }}>
                <Label color={subColor}>Bold text</Label>
                <Seg
                  dark={dark}
                  segBg={segBg}
                  segOn={segOn}
                  fg={fg}
                  options={[
                    { key: '400', label: 'Off' },
                    { key: '600', label: 'On' },
                  ]}
                  value={String(prefs.weight)}
                  onChange={(v) => up({ weight: parseInt(v, 10) as 400 | 600 })}
                />
              </View>
            </View>
          </Section>

          <Divider dark={dark} />

          {/* layout */}
          <Section>
            <Label color={subColor}>Reading layout</Label>
            <Seg
              dark={dark}
              segBg={segBg}
              segOn={segOn}
              fg={fg}
              options={[
                { key: 'paged', label: 'Paged', icon: 'bookOpen' },
                { key: 'scroll', label: 'Scroll', icon: 'rows' },
              ]}
              value={prefs.scroll ? 'scroll' : 'paged'}
              onChange={(v) => up({ scroll: v === 'scroll' })}
            />
          </Section>

          {/* page flip */}
          <Section style={{ opacity: prefs.scroll ? 0.4 : 1 }}>
            <Label color={subColor}>Page-turn animation</Label>
            <Seg
              dark={dark}
              segBg={segBg}
              segOn={segOn}
              fg={fg}
              disabled={prefs.scroll}
              options={[
                { key: 'curl', label: 'Curl' },
                { key: 'slide', label: 'Slide' },
                { key: 'fade', label: 'Fade' },
                { key: 'none', label: 'None' },
              ]}
              value={prefs.flip}
              onChange={(v) => up({ flip: v as any })}
            />
          </Section>
          <View style={{ height: 6 }} />
        </ScrollView>
      </View>
    </Modal>
  );
}

function Section({ children, style }: { children: React.ReactNode; style?: any }) {
  return <View style={[{ paddingHorizontal: 20, paddingVertical: 10 }, style]}>{children}</View>;
}

function Label({ children, color }: { children: React.ReactNode; color: string }) {
  return <Text style={{ fontSize: 13, fontWeight: '600', color, marginBottom: 10, fontFamily: F.sans }}>{children}</Text>;
}

function Divider({ dark }: { dark: boolean }) {
  return <View style={{ height: 1, marginHorizontal: 22, marginVertical: 4, backgroundColor: dark ? '#3A3D43' : C.line }} />;
}

function Seg({
  options,
  value,
  onChange,
  segBg,
  segOn,
  fg,
  dark,
  disabled,
}: {
  options: { key: string; label?: string; icon?: string }[];
  value: string;
  onChange: (k: string) => void;
  segBg: string;
  segOn: string;
  fg: string;
  dark: boolean;
  disabled?: boolean;
}) {
  return (
    <View style={{ flexDirection: 'row', backgroundColor: segBg, borderRadius: 12, padding: 4, gap: 4 }}>
      {options.map((o) => {
        const on = value === o.key;
        return (
          <Pressable
            key={o.key}
            disabled={disabled}
            onPress={() => onChange(o.key)}
            style={{
              flex: 1,
              height: 40,
              borderRadius: 9,
              flexDirection: 'row',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 6,
              backgroundColor: on ? segOn : 'transparent',
            }}
          >
            {o.icon ? <Icon name={o.icon} size={17} color={fg} /> : null}
            {o.label ? <Text style={{ color: fg, fontWeight: '600', fontSize: 14, fontFamily: F.sans }}>{o.label}</Text> : null}
          </Pressable>
        );
      })}
    </View>
  );
}
