import React, { useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Cover } from '../components/Cover';
import { Ring } from '../components/Ring';
import { Stars } from '../components/Stars';
import { Icon } from '../components/Icon';
import { Serif, Mono, Track } from '../components/ui';
import { BOOKS, BookStatus, pctOf } from '../data';
import { C, F } from '../theme';
import type { TabNav } from '../navigation/types';

const FILTERS = ['All', 'Reading', 'Finished', 'Want to read'] as const;
const MAP: Record<string, BookStatus> = { Reading: 'reading', Finished: 'finished', 'Want to read': 'want' };

export function LibraryScreen({ navigation }: { navigation: TabNav }) {
  const insets = useSafeAreaInsets();
  const [filter, setFilter] = useState<string>('All');
  const [view, setView] = useState<'grid' | 'list'>('grid');
  const openBook = (id: string) => navigation.navigate('Detail', { id });

  const list = filter === 'All' ? BOOKS : BOOKS.filter((b) => b.status === MAP[filter]);

  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: C.paper }}
      contentContainerStyle={{ paddingTop: insets.top + 12, paddingBottom: 110 }}
      showsVerticalScrollIndicator={false}
    >
      <View style={{ paddingHorizontal: 20, flexDirection: 'row', alignItems: 'flex-end', justifyContent: 'space-between' }}>
        <View>
          <Mono style={{ fontSize: 11, letterSpacing: 1.6, color: C.ink3, marginBottom: 4 }}>
            {BOOKS.length} BOOKS · 3 SHELVES
          </Mono>
          <Serif style={{ fontSize: 34, fontWeight: '600', letterSpacing: -0.5 }}>Library</Serif>
        </View>
        <Seg
          options={[
            { key: 'grid', icon: 'grid' },
            { key: 'list', icon: 'rows' },
          ]}
          value={view}
          onChange={(v) => setView(v as any)}
          iconOnly
        />
      </View>

      {/* search */}
      <View style={{ paddingHorizontal: 20, marginTop: 18, marginBottom: 12 }}>
        <View
          style={{
            flexDirection: 'row',
            alignItems: 'center',
            gap: 9,
            height: 42,
            paddingHorizontal: 14,
            backgroundColor: C.paper3,
            borderRadius: 13,
          }}
        >
          <Icon name="search" size={18} color={C.ink3} />
          <Text style={{ fontSize: 15.5, color: C.ink3, fontFamily: F.sans }}>Search title, author, highlight…</Text>
        </View>
      </View>

      {/* filter chips */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ paddingHorizontal: 20, gap: 8 }} style={{ marginBottom: 16 }}>
        {FILTERS.map((f) => {
          const active = filter === f;
          return (
            <Pressable
              key={f}
              onPress={() => setFilter(f)}
              style={{
                paddingHorizontal: 15,
                height: 36,
                borderRadius: 999,
                justifyContent: 'center',
                backgroundColor: active ? C.ink : C.paper2,
                borderWidth: 1,
                borderColor: active ? C.ink : C.line,
              }}
            >
              <Text style={{ color: active ? C.paper : C.ink2, fontWeight: '600', fontSize: 14, fontFamily: F.sans }}>{f}</Text>
            </Pressable>
          );
        })}
      </ScrollView>

      {view === 'grid' ? (
        <View style={{ flexDirection: 'row', flexWrap: 'wrap', paddingHorizontal: 20, justifyContent: 'space-between' }}>
          {list.map((b) => (
            <Pressable key={b.id} style={{ width: '47%', marginBottom: 26 }} onPress={() => openBook(b.id)}>
              <View style={{ alignSelf: 'center' }}>
                <Cover book={b} w={154} />
                {b.status === 'reading' && (
                  <View style={{ position: 'absolute', right: 8, bottom: 8, backgroundColor: '#FDFCFAEB', borderRadius: 999, padding: 3 }}>
                    <Ring pct={pctOf(b)} size={32} sw={4} />
                  </View>
                )}
                {b.status === 'finished' && (
                  <View
                    style={{
                      position: 'absolute',
                      right: 8,
                      bottom: 8,
                      width: 26,
                      height: 26,
                      borderRadius: 999,
                      backgroundColor: C.blue,
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    <Icon name="check" size={15} color="#fff" />
                  </View>
                )}
              </View>
              <Serif style={{ fontSize: 16, fontWeight: '600', marginTop: 11, lineHeight: 18, letterSpacing: -0.2 }}>
                {b.title.replace('\n', ' ')}
              </Serif>
              <Text style={{ fontSize: 12.5, color: C.ink3, marginTop: 2, fontFamily: F.sans }}>{b.author}</Text>
            </Pressable>
          ))}
        </View>
      ) : (
        <View style={{ paddingHorizontal: 20 }}>
          {list.map((b, i) => (
            <Pressable
              key={b.id}
              onPress={() => openBook(b.id)}
              style={{
                flexDirection: 'row',
                gap: 14,
                paddingVertical: 12,
                borderBottomWidth: i < list.length - 1 ? 1 : 0,
                borderBottomColor: C.line,
                alignItems: 'center',
              }}
            >
              <Cover book={b} w={48} />
              <View style={{ flex: 1 }}>
                <Serif style={{ fontSize: 17, fontWeight: '600', lineHeight: 20 }}>{b.title.replace('\n', ' ')}</Serif>
                <Text style={{ fontSize: 13, color: C.ink3, marginTop: 2, fontFamily: F.sans }}>
                  {b.author} · {b.genre}
                </Text>
                {b.status === 'reading' && <Track pct={pctOf(b)} style={{ marginTop: 8, maxWidth: 160 }} />}
                {b.status === 'finished' && (
                  <View style={{ marginTop: 6 }}>
                    <Stars n={b.rating} size={12} />
                  </View>
                )}
              </View>
              <Icon name="chevR" size={18} color={C.ink3} />
            </Pressable>
          ))}
        </View>
      )}
    </ScrollView>
  );
}

function Seg({
  options,
  value,
  onChange,
  iconOnly,
}: {
  options: { key: string; icon?: string; label?: string }[];
  value: string;
  onChange: (k: string) => void;
  iconOnly?: boolean;
}) {
  return (
    <View style={{ flexDirection: 'row', backgroundColor: C.paper3, borderRadius: 11, padding: 3, marginBottom: 4 }}>
      {options.map((o) => {
        const active = value === o.key;
        return (
          <Pressable
            key={o.key}
            onPress={() => onChange(o.key)}
            style={{
              paddingHorizontal: iconOnly ? 12 : 14,
              height: 34,
              borderRadius: 8,
              alignItems: 'center',
              justifyContent: 'center',
              backgroundColor: active ? C.paper2 : 'transparent',
            }}
          >
            {o.icon ? <Icon name={o.icon} size={17} color={active ? C.ink : C.ink3} /> : null}
            {o.label ? (
              <Text style={{ color: active ? C.ink : C.ink3, fontWeight: '600', fontFamily: F.sans }}>{o.label}</Text>
            ) : null}
          </Pressable>
        );
      })}
    </View>
  );
}
