import React, { useState } from 'react';
import { Pressable, ScrollView, Switch, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Icon } from '../components/Icon';
import { Card, Eyebrow, Mono, Serif } from '../components/ui';
import { usePrefs } from '../lib/prefs';
import { C, F, SETTINGS_TINTS, shadow } from '../theme';

export function SettingsScreen() {
  const insets = useSafeAreaInsets();
  const { prefs } = usePrefs();
  const [reminder, setReminder] = useState(true);
  const [autoNight, setAutoNight] = useState(true);
  const [brightness, setBrightness] = useState(true);
  const themeName = ({ paper: 'Paper', sepia: 'Sepia', quartz: 'Quartz', night: 'Night', black: 'Black' } as Record<string, string>)[prefs.theme] || 'Sepia';

  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: C.paper }}
      contentContainerStyle={{ paddingTop: insets.top + 12, paddingBottom: 110 }}
      showsVerticalScrollIndicator={false}
    >
      <View style={{ paddingHorizontal: 20, marginBottom: 4 }}>
        <Serif style={{ fontSize: 34, fontWeight: '600', letterSpacing: -0.5 }}>Settings</Serif>
      </View>

      <View style={{ paddingHorizontal: 20, gap: 22, marginTop: 4 }}>
        {/* profile */}
        <Card style={{ padding: 18, flexDirection: 'row', alignItems: 'center', gap: 15 }}>
          <LinearGradient
            colors={['#4C80CD', '#005AD2']}
            start={{ x: 0.1, y: 0 }}
            end={{ x: 0.9, y: 1 }}
            style={{ width: 58, height: 58, borderRadius: 999, alignItems: 'center', justifyContent: 'center', ...shadow.sm }}
          >
            <Serif style={{ color: '#fff', fontSize: 24, fontWeight: '600' }}>EV</Serif>
          </LinearGradient>
          <View style={{ flex: 1 }}>
            <Serif style={{ fontSize: 20, fontWeight: '600' }}>Eleanor Vance</Serif>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 2 }}>
              <Icon name="cloudOff" size={14} color={C.ink3} />
              <Text style={{ fontSize: 13, color: C.ink3, fontFamily: F.sans }}>Offline library · 14 books</Text>
            </View>
          </View>
        </Card>

        {/* offline banner */}
        <View style={{ flexDirection: 'row', gap: 13, padding: 16, borderRadius: 16, backgroundColor: C.blueSoft, borderWidth: 1, borderColor: C.blueSoftBorder }}>
          <Icon name="cloudOff" size={22} color={C.blueInk} />
          <Text style={{ flex: 1, fontSize: 13.5, lineHeight: 20, color: C.blueInk, fontFamily: F.sans }}>
            <Text style={{ fontWeight: '700' }}>Everything stays on this device.</Text> Folio never sends your books or reading data
            anywhere — no account required.
          </Text>
        </View>

        <Group title="Reading">
          <SRow icon="aa" tint={SETTINGS_TINTS[258]} name="Default appearance" detail={themeName} />
          <SRow icon="type" tint={SETTINGS_TINTS[30]} name="Default font" detail="Newsreader" />
          <SRow icon="sun" tint={SETTINGS_TINTS[80]} name="Match system brightness" toggle on={brightness} onToggle={() => setBrightness((v) => !v)} />
          <SRow icon="moon" tint={SETTINGS_TINTS[260]} name="Auto-night at sunset" toggle on={autoNight} onToggle={() => setAutoNight((v) => !v)} last />
        </Group>

        <Group title="Library">
          <SRow icon="download" tint={SETTINGS_TINTS[150]} name="Import EPUB…" detail="from Files" />
          <SRow icon="library" tint={SETTINGS_TINTS[280]} name="Manage collections" detail="3" />
          <SRow icon="sliders" tint={SETTINGS_TINTS[220]} name="Sort & display" last />
        </Group>

        <Group title="Goals">
          <SRow icon="target" tint={SETTINGS_TINTS[258]} name="Annual reading goal" detail="36 books" />
          <SRow icon="calendar" tint={SETTINGS_TINTS[350]} name="Daily reminder" toggle on={reminder} onToggle={() => setReminder((v) => !v)} last />
        </Group>

        <Group title="Data & storage">
          <SRow icon="chart" tint={SETTINGS_TINTS[200]} name="Storage used" detail="18.4 MB" />
          <SRow icon="share" tint={SETTINGS_TINTS[160]} name="Export library & stats" detail=".json" />
          <SRow icon="bookmark" tint={SETTINGS_TINTS[60]} name="Backup to Files" last />
        </Group>

        <Group title="About">
          <SRow icon="feather" tint={SETTINGS_TINTS[258]} name="Version" detail="Folio 2.4 (118)" chevron={false} />
          <SRow icon="heart" tint={SETTINGS_TINTS[20]} name="Acknowledgements" last />
        </Group>

        <View style={{ alignItems: 'center', paddingTop: 4 }}>
          <Serif style={{ fontSize: 17, color: C.ink3 }}>Folio</Serif>
          <Mono style={{ fontSize: 11, color: C.ink3, marginTop: 3, letterSpacing: 1.2 }}>READ · TRACK · KEEP</Mono>
        </View>
      </View>
    </ScrollView>
  );
}

function Group({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <View>
      <Eyebrow style={{ marginBottom: 8, paddingLeft: 4 }}>{title}</Eyebrow>
      <Card style={{ overflow: 'hidden', padding: 0 }}>{children}</Card>
    </View>
  );
}

function SRow({
  icon,
  tint,
  name,
  detail,
  last,
  chevron = true,
  toggle,
  on,
  onToggle,
}: {
  icon: string;
  tint: string;
  name: string;
  detail?: string;
  last?: boolean;
  chevron?: boolean;
  toggle?: boolean;
  on?: boolean;
  onToggle?: () => void;
}) {
  return (
    <Pressable
      onPress={toggle ? onToggle : undefined}
      style={{
        flexDirection: 'row',
        alignItems: 'center',
        gap: 13,
        paddingHorizontal: 15,
        paddingVertical: 11,
        borderBottomWidth: last ? 0 : 1,
        borderBottomColor: C.line,
      }}
    >
      <View style={{ width: 30, height: 30, borderRadius: 9, backgroundColor: tint, alignItems: 'center', justifyContent: 'center' }}>
        <Icon name={icon} size={17} color={C.ink} />
      </View>
      <Text style={{ flex: 1, fontSize: 15.5, fontWeight: '500', color: C.ink, fontFamily: F.sans }}>{name}</Text>
      {detail ? <Text style={{ fontSize: 14.5, color: C.ink3, fontFamily: F.sans }}>{detail}</Text> : null}
      {toggle ? (
        <Switch value={on} onValueChange={onToggle} trackColor={{ true: C.blue, false: C.paper4 }} thumbColor="#fff" />
      ) : chevron ? (
        <Icon name="chevR" size={17} color={C.ink3} />
      ) : null}
    </Pressable>
  );
}
