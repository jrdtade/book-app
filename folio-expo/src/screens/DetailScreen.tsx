import React from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Cover } from '../components/Cover';
import { Ring } from '../components/Ring';
import { Stars } from '../components/Stars';
import { Icon } from '../components/Icon';
import { Card, Eyebrow, Mono, Serif, Track } from '../components/ui';
import { byId, pctOf, timeLeft } from '../data';
import { C, F, shadow } from '../theme';
import type { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'Detail'>;

export function DetailScreen({ route, navigation }: Props) {
  const insets = useSafeAreaInsets();
  const b = byId(route.params.id);
  const isReading = b.status === 'reading';
  const isFinished = b.status === 'finished';
  const openReader = () => navigation.navigate('Reader', { id: b.id });

  return (
    <View style={{ flex: 1, backgroundColor: C.paper }}>
      {/* floating top bar */}
      <View
        style={{
          position: 'absolute',
          top: insets.top + 6,
          left: 0,
          right: 0,
          zIndex: 10,
          paddingHorizontal: 16,
          flexDirection: 'row',
          justifyContent: 'space-between',
        }}
      >
        <GlassButton icon="back" onPress={() => navigation.goBack()} />
        <GlassButton icon="more" onPress={() => {}} />
      </View>

      <ScrollView contentContainerStyle={{ paddingBottom: 50 }} showsVerticalScrollIndicator={false}>
        {/* hero */}
        <LinearGradient
          colors={[C.paper3, C.paper]}
          style={{ paddingTop: insets.top + 70, paddingHorizontal: 24, paddingBottom: 22, alignItems: 'center' }}
        >
          <Cover book={b} w={172} style={shadow.lg as any} />
          <Serif style={{ fontSize: 27, fontWeight: '600', lineHeight: 29, letterSpacing: -0.3, marginTop: 22, textAlign: 'center' }}>
            {b.title.replace('\n', ' ')}
          </Serif>
          <Text style={{ color: C.ink2, fontSize: 16, marginTop: 5, fontFamily: F.sans }}>{b.author}</Text>
          <View style={{ flexDirection: 'row', gap: 8, marginTop: 14, flexWrap: 'wrap', justifyContent: 'center' }}>
            <Meta>{b.genre}</Meta>
            <Meta>{b.year < 0 ? `${-b.year} BCE` : `${b.year}`}</Meta>
            <Meta>{b.pages} pages</Meta>
          </View>
        </LinearGradient>

        <View style={{ paddingHorizontal: 20 }}>
          {/* progress / action card */}
          <Card style={{ padding: 18, marginTop: 4 }}>
            {isReading && (
              <>
                <View style={{ flexDirection: 'row', alignItems: 'baseline', justifyContent: 'space-between' }}>
                  <View style={{ flexDirection: 'row', alignItems: 'baseline' }}>
                    <Serif style={{ fontSize: 34, fontWeight: '600' }}>{Math.round(pctOf(b) * 100)}</Serif>
                    <Serif style={{ fontSize: 20 }}>%</Serif>
                  </View>
                  <Mono style={{ fontSize: 13, color: C.ink3 }}>
                    page {b.page} of {b.pages}
                  </Mono>
                </View>
                <Track pct={pctOf(b)} style={{ marginVertical: 13 }} />
                <PrimaryButton label="Continue reading" onPress={openReader} />
                <Mono style={{ textAlign: 'center', fontSize: 13, color: C.ink3, marginTop: 10 }}>
                  {timeLeft(b)} · started {b.started}
                </Mono>
              </>
            )}
            {isFinished && (
              <>
                <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
                  <View>
                    <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                      <Icon name="check" size={18} color={C.blue} />
                      <Text style={{ fontWeight: '600', fontSize: 15, fontFamily: F.sans, color: C.ink }}>
                        Finished {b.finished}
                      </Text>
                    </View>
                    <View style={{ marginTop: 8 }}>
                      <Stars n={b.rating} size={16} />
                    </View>
                  </View>
                  <Ring pct={1} size={48} sw={4.5} />
                </View>
                <GhostButton label="Read again" onPress={openReader} />
              </>
            )}
            {b.status === 'want' && (
              <>
                <PrimaryButton label="Start reading" onPress={openReader} />
                <View style={{ height: 10 }} />
                <GhostButton label="On “Want to read”" icon="bookmark" onPress={() => {}} />
              </>
            )}
          </Card>

          {/* description */}
          <Serif style={{ fontSize: 17, lineHeight: 26, color: C.ink, marginTop: 22 }}>{b.desc}</Serif>

          {/* highlights teaser */}
          {(isReading || isFinished) && (
            <Card style={{ flexDirection: 'row', alignItems: 'center', gap: 13, padding: 14, marginTop: 18 }}>
              <View style={{ width: 38, height: 38, borderRadius: 11, backgroundColor: C.blueSoft, alignItems: 'center', justifyContent: 'center' }}>
                <Icon name="highlighter" size={20} color={C.blueInk} />
              </View>
              <View style={{ flex: 1 }}>
                <Text style={{ fontWeight: '600', fontSize: 15, fontFamily: F.sans, color: C.ink }}>Highlights & notes</Text>
                <Text style={{ fontSize: 13, color: C.ink3, fontFamily: F.sans }}>
                  {isFinished ? 9 : 3} highlights · {isFinished ? 2 : 1} note
                </Text>
              </View>
              <Icon name="chevR" size={18} color={C.ink3} />
            </Card>
          )}

          {/* details */}
          <View style={{ marginTop: 24 }}>
            <Eyebrow style={{ marginBottom: 8 }}>Details</Eyebrow>
            <Card style={{ overflow: 'hidden', padding: 0 }}>
              <DRow k="Published" v={b.year < 0 ? `${-b.year} BCE` : `${b.year}`} />
              <DRow k="Genre" v={b.genre} />
              <DRow k="Length" v={`${b.pages} pages`} />
              <DRow k="Format" v="EPUB · 1.2 MB" />
              <DRow k="Added" v={b.started || 'Jun 14'} last />
            </Card>
          </View>
        </View>
      </ScrollView>
    </View>
  );
}

function GlassButton({ icon, onPress }: { icon: string; onPress: () => void }) {
  return (
    <Pressable
      onPress={onPress}
      style={{
        width: 40,
        height: 40,
        borderRadius: 999,
        backgroundColor: '#FDFAF5E0',
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 1,
        borderColor: C.line,
        ...shadow.sm,
      }}
    >
      <Icon name={icon} size={20} color={C.ink} />
    </Pressable>
  );
}

function PrimaryButton({ label, onPress }: { label: string; onPress: () => void }) {
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => ({
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
      <Text style={{ color: '#fff', fontWeight: '600', fontSize: 16, fontFamily: F.sans }}>{label}</Text>
    </Pressable>
  );
}

function GhostButton({ label, icon = 'bookOpen', onPress }: { label: string; icon?: string; onPress: () => void }) {
  return (
    <Pressable
      onPress={onPress}
      style={({ pressed }) => ({
        height: 50,
        borderRadius: 14,
        backgroundColor: pressed ? C.paper3 : C.paper2,
        borderWidth: 1,
        borderColor: C.line2,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 8,
      })}
    >
      <Icon name={icon} size={18} color={C.ink} />
      <Text style={{ color: C.ink, fontWeight: '600', fontSize: 16, fontFamily: F.sans }}>{label}</Text>
    </Pressable>
  );
}

const Meta = ({ children }: { children: React.ReactNode }) => (
  <Text style={{ fontSize: 12.5, fontWeight: '600', color: C.ink2, backgroundColor: C.paper3, paddingHorizontal: 11, paddingVertical: 5, borderRadius: 9, overflow: 'hidden', fontFamily: F.sans }}>
    {children}
  </Text>
);

function DRow({ k, v, last }: { k: string; v: string; last?: boolean }) {
  return (
    <View
      style={{
        flexDirection: 'row',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        paddingVertical: 13,
        borderBottomWidth: last ? 0 : 1,
        borderBottomColor: C.line,
      }}
    >
      <Text style={{ color: C.ink3, fontSize: 15, fontFamily: F.sans }}>{k}</Text>
      <Text style={{ fontWeight: '500', fontSize: 15, color: C.ink, fontFamily: F.sans }}>{v}</Text>
    </View>
  );
}
