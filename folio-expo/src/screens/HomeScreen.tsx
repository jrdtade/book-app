import React from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Cover } from '../components/Cover';
import { Ring } from '../components/Ring';
import { Stars } from '../components/Stars';
import { Icon } from '../components/Icon';
import { Card, Eyebrow, Mono, Serif, Track } from '../components/ui';
import { finished, pctOf, reading, timeLeft } from '../data';
import { C, F } from '../theme';
import type { TabNav } from '../navigation/types';

export function HomeScreen({ navigation }: { navigation: TabNav }) {
  const insets = useSafeAreaInsets();
  const hero = reading[0];
  const moreReading = reading.slice(1);
  const recent = finished.slice(0, 6);
  const openBook = (id: string) => navigation.navigate('Detail', { id });
  const openReader = (id: string) => navigation.navigate('Reader', { id });

  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: C.paper }}
      contentContainerStyle={{ paddingTop: insets.top + 12, paddingBottom: 110 }}
      showsVerticalScrollIndicator={false}
    >
      <View style={{ paddingHorizontal: 20, paddingBottom: 6 }}>
        <View style={{ flexDirection: 'row', alignItems: 'center', gap: 7, marginBottom: 8 }}>
          <Icon name="feather" size={15} color={C.ink2} />
          <Mono style={{ fontSize: 11, letterSpacing: 1.6, color: C.ink3 }}>FOLIO · FRI 20 JUN</Mono>
        </View>
        <Serif style={{ fontSize: 32, fontWeight: '600', lineHeight: 36, letterSpacing: -0.5 }}>
          Good evening,{'\n'}Eleanor.
        </Serif>
      </View>

      {/* hero — continue reading */}
      <View style={{ paddingHorizontal: 20, marginTop: 6 }}>
        <Card style={{ padding: 16 }}>
          <Eyebrow style={{ marginBottom: 12 }}>Continue reading</Eyebrow>
          <View style={{ flexDirection: 'row', gap: 16 }}>
            <Cover book={hero} w={96} onPress={() => openBook(hero.id)} />
            <View style={{ flex: 1, justifyContent: 'space-between' }}>
              <View>
                <Serif style={{ fontSize: 22, fontWeight: '600', lineHeight: 24, letterSpacing: -0.2 }}>
                  {hero.title.replace('\n', ' ')}
                </Serif>
                <Text style={{ color: C.ink2, fontSize: 14, marginTop: 3, fontFamily: F.sans }}>{hero.author}</Text>
              </View>
              <View style={{ marginTop: 14 }}>
                <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginBottom: 6 }}>
                  <Mono style={{ fontSize: 12.5, color: C.ink3 }}>{Math.round(pctOf(hero) * 100)}%</Mono>
                  <Mono style={{ fontSize: 12.5, color: C.ink3 }}>{timeLeft(hero)}</Mono>
                </View>
                <Track pct={pctOf(hero)} />
              </View>
            </View>
          </View>
          <Pressable
            onPress={() => openReader(hero.id)}
            style={({ pressed }) => ({
              marginTop: 16,
              height: 50,
              borderRadius: 14,
              backgroundColor: pressed ? C.blue2 : C.blue,
              flexDirection: 'row',
              alignItems: 'center',
              justifyContent: 'center',
              gap: 8,
            })}
          >
            <Icon name="bookOpen" size={19} color="#fff" />
            <Text style={{ color: '#fff', fontWeight: '600', fontSize: 16, fontFamily: F.sans }}>
              Continue · page {hero.page}
            </Text>
          </Pressable>
        </Card>
      </View>

      {/* also reading */}
      {moreReading.length > 0 && (
        <Section title="Also reading">
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={{ paddingHorizontal: 20, gap: 16 }}
          >
            {moreReading.map((b) => (
              <Pressable key={b.id} style={{ width: 104 }} onPress={() => openBook(b.id)}>
                <View>
                  <Cover book={b} w={104} />
                  <View
                    style={{
                      position: 'absolute',
                      right: 6,
                      bottom: 6,
                      backgroundColor: '#FDFCFAEB',
                      borderRadius: 999,
                      padding: 3,
                    }}
                  >
                    <Ring pct={pctOf(b)} size={30} sw={3.5} />
                  </View>
                </View>
                <Serif style={{ fontSize: 15, fontWeight: '600', marginTop: 8, lineHeight: 17 }}>
                  {b.title.replace('\n', ' ')}
                </Serif>
                <Mono style={{ fontSize: 12, color: C.ink3, marginTop: 1 }}>{Math.round(pctOf(b) * 100)}% read</Mono>
              </Pressable>
            ))}
          </ScrollView>
        </Section>
      )}

      {/* this week */}
      <Section title="This week" action="See all" onAction={() => navigation.navigate('Stats')}>
        <View style={{ paddingHorizontal: 20 }}>
          <Card style={{ flexDirection: 'row', padding: 4 }}>
            <MiniStat icon="clock" value="7.1h" label="Read" />
            <Divider />
            <MiniStat icon="pages" value="186" label="Pages" />
            <Divider />
            <MiniStat icon="flame" value="24" label="Day streak" accent />
          </Card>
        </View>
      </Section>

      {/* finished recently */}
      <Section title="Finished recently" action="Library" onAction={() => navigation.navigate('Library')}>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ paddingHorizontal: 20, gap: 14 }}>
          {recent.map((b) => (
            <Pressable key={b.id} style={{ width: 96 }} onPress={() => openBook(b.id)}>
              <Cover book={b} w={96} />
              <View style={{ marginTop: 7 }}>
                <Stars n={b.rating} size={11} />
              </View>
            </Pressable>
          ))}
        </ScrollView>
      </Section>
    </ScrollView>
  );
}

function Section({
  title,
  action,
  onAction,
  children,
}: {
  title: string;
  action?: string;
  onAction?: () => void;
  children: React.ReactNode;
}) {
  return (
    <View style={{ marginTop: 24 }}>
      <View
        style={{
          paddingHorizontal: 20,
          flexDirection: 'row',
          alignItems: 'baseline',
          justifyContent: 'space-between',
          marginBottom: 12,
        }}
      >
        <Serif style={{ fontSize: 21, fontWeight: '600', letterSpacing: -0.2 }}>{title}</Serif>
        {action ? (
          <Pressable onPress={onAction}>
            <Text style={{ fontSize: 14, fontWeight: '600', color: C.blue, fontFamily: F.sans }}>{action}</Text>
          </Pressable>
        ) : null}
      </View>
      {children}
    </View>
  );
}

function MiniStat({ icon, value, label, accent }: { icon: string; value: string; label: string; accent?: boolean }) {
  return (
    <View style={{ flex: 1, paddingVertical: 14, alignItems: 'center', gap: 4 }}>
      <Icon name={icon} size={20} color={accent ? C.blue : C.ink2} />
      <Serif style={{ fontSize: 24, fontWeight: '600', color: accent ? C.blue : C.ink }}>{value}</Serif>
      <Text style={{ fontSize: 11.5, color: C.ink3, fontWeight: '500', fontFamily: F.sans }}>{label}</Text>
    </View>
  );
}

const Divider = () => <View style={{ width: 1, alignSelf: 'stretch', marginVertical: 12, backgroundColor: C.line }} />;
