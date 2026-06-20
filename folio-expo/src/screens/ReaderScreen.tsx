import React, { useEffect, useRef, useState } from 'react';
import { Animated, LayoutChangeEvent, Pressable, ScrollView, Text, View } from 'react-native';
import Slider from '@react-native-community/slider';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { Icon } from '../components/Icon';
import { ChapterBody } from '../components/ChapterBody';
import { CustomSheet } from '../components/CustomSheet';
import { byId } from '../data';
import { C, F, READER_THEMES } from '../theme';
import { posKey, usePrefs } from '../lib/prefs';
import type { RootStackParamList } from '../navigation/types';

type Props = NativeStackScreenProps<RootStackParamList, 'Reader'>;

export function ReaderScreen({ route, navigation }: Props) {
  const insets = useSafeAreaInsets();
  const { prefs } = usePrefs();
  const b = byId(route.params.id);
  const theme = READER_THEMES.find((t) => t.id === prefs.theme)!;
  const dark = prefs.theme === 'night' || prefs.theme === 'black';

  const [stage, setStage] = useState({ w: 0, h: 0 });
  const [contentH, setContentH] = useState(0);
  const [page, setPage] = useState(0);
  const [chrome, setChrome] = useState(true);
  const [sheet, setSheet] = useState(false);
  const [marked, setMarked] = useState(false);
  const [scrollPct, setScrollPct] = useState(0);
  const tx = useRef(new Animated.Value(0)).current;
  const initRef = useRef(false);

  const padTop = insets.top + 56;
  const padBottom = 64;
  const pageH = Math.max(1, stage.h - padTop - padBottom);
  const textW = Math.max(1, stage.w - 2 * prefs.mx);
  const total = Math.max(1, Math.ceil(contentH / pageH));

  // restore saved page once we know pagination
  useEffect(() => {
    if (prefs.scroll || initRef.current || contentH === 0 || stage.h === 0) return;
    initRef.current = true;
    AsyncStorage.getItem(posKey(b.id)).then((raw) => {
      const stored = raw ? parseInt(raw, 10) : NaN;
      const start = !isNaN(stored) ? stored : Math.round((b.page ? b.page / b.pages : 0) * (total - 1));
      goTo(Math.max(0, Math.min(total - 1, start)), false);
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [contentH, stage.h]);

  // clamp current page when pagination changes (font/size/margins)
  useEffect(() => {
    const p = Math.max(0, Math.min(total - 1, page));
    goTo(p, false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [total]);

  function goTo(p: number, animate: boolean) {
    p = Math.max(0, Math.min(total - 1, p));
    setPage(p);
    AsyncStorage.setItem(posKey(b.id), String(p)).catch(() => {});
    const toValue = -p * stage.w;
    if (!animate || prefs.flip === 'none') {
      tx.setValue(toValue);
    } else {
      const duration = prefs.flip === 'curl' ? 420 : prefs.flip === 'fade' ? 320 : 280;
      Animated.timing(tx, { toValue, duration, useNativeDriver: true }).start();
    }
  }

  const go = (d: number) => {
    if (prefs.scroll) return;
    goTo(page + d, true);
  };

  const onStageLayout = (e: LayoutChangeEvent) => {
    const { width, height } = e.nativeEvent.layout;
    if (width !== stage.w || height !== stage.h) setStage({ w: width, h: height });
  };

  const progress = prefs.scroll ? scrollPct : total > 1 ? page / (total - 1) : 0;
  const minsLeft = Math.round((1 - progress) * b.pages * (b.minPerPage || 0.7));

  const warmthOpacity = prefs.warmth * 0.22;
  const dimOpacity = (1 - prefs.brightness) * 0.85;

  return (
    <View style={{ flex: 1, backgroundColor: theme.bg }}>
      {/* STAGE */}
      <View style={{ flex: 1 }} onLayout={onStageLayout}>
        {stage.w > 0 && (
          <>
            {/* hidden measuring column */}
            <View
              style={{ position: 'absolute', left: prefs.mx, top: 0, width: textW, opacity: 0 }}
              pointerEvents="none"
              onLayout={(e) => setContentH(e.nativeEvent.layout.height)}
            >
              <ChapterBody prefs={prefs} fg={theme.fg} />
            </View>

            {prefs.scroll ? (
              <ScrollView
                contentContainerStyle={{ paddingTop: padTop, paddingHorizontal: prefs.mx, paddingBottom: 100 }}
                showsVerticalScrollIndicator={false}
                scrollEventThrottle={16}
                onScroll={(e) => {
                  const { contentOffset, contentSize, layoutMeasurement } = e.nativeEvent;
                  setScrollPct(contentOffset.y / Math.max(1, contentSize.height - layoutMeasurement.height));
                }}
              >
                <ChapterBody prefs={prefs} fg={theme.fg} />
              </ScrollView>
            ) : (
              <Animated.View
                style={{
                  flex: 1,
                  flexDirection: 'row',
                  width: stage.w * total,
                  transform: [{ translateX: tx }],
                }}
              >
                {Array.from({ length: total }).map((_, i) => (
                  <View key={i} style={{ width: stage.w, height: stage.h, overflow: 'hidden' }}>
                    <View style={{ position: 'absolute', top: padTop - i * pageH, left: prefs.mx, width: textW }}>
                      <ChapterBody prefs={prefs} fg={theme.fg} />
                    </View>
                  </View>
                ))}
              </Animated.View>
            )}

            {/* warmth + dim veils */}
            {warmthOpacity > 0 && (
              <View pointerEvents="none" style={{ position: 'absolute', inset: 0, backgroundColor: '#C98A2E', opacity: warmthOpacity } as any} />
            )}
            {dimOpacity > 0 && (
              <View pointerEvents="none" style={{ position: 'absolute', inset: 0, backgroundColor: '#000', opacity: dimOpacity } as any} />
            )}

            {/* tap zones */}
            <View style={{ position: 'absolute', inset: 0, flexDirection: 'row' } as any}>
              {!prefs.scroll && <Pressable style={{ width: '30%' }} onPress={() => go(-1)} />}
              <Pressable style={{ flex: 1 }} onPress={() => setChrome((c) => !c)} />
              {!prefs.scroll && <Pressable style={{ width: '30%' }} onPress={() => go(1)} />}
            </View>
          </>
        )}
      </View>

      {/* TOP CHROME */}
      {chrome && (
        <View
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            paddingTop: insets.top + 6,
            paddingBottom: 10,
            paddingHorizontal: 6,
            flexDirection: 'row',
            alignItems: 'center',
            backgroundColor: theme.chrome,
          }}
        >
          <RdIcon name="back" theme={theme} onPress={() => navigation.goBack()} />
          <View style={{ flex: 1, alignItems: 'center' }}>
            <Text numberOfLines={1} style={{ fontFamily: F.serif, fontSize: 15, fontWeight: '600', color: theme.fg }}>
              {b.title.replace('\n', ' ')}
            </Text>
          </View>
          <RdIcon name="search" theme={theme} onPress={() => {}} />
          <RdIcon name="bookmark" theme={theme} onPress={() => setMarked((m) => !m)} color={marked ? C.blue : theme.icon} />
          <RdIcon name="list" theme={theme} onPress={() => {}} />
          <RdIcon name="aa" theme={theme} onPress={() => setSheet(true)} />
        </View>
      )}

      {/* BOTTOM CHROME */}
      {chrome && (
        <View
          style={{
            position: 'absolute',
            bottom: 0,
            left: 0,
            right: 0,
            paddingHorizontal: 20,
            paddingTop: 10,
            paddingBottom: insets.bottom + 12,
            backgroundColor: theme.chrome,
          }}
        >
          {!prefs.scroll && (
            <Slider
              style={{ marginBottom: 6 }}
              minimumValue={0}
              maximumValue={Math.max(1, total - 1)}
              value={page}
              step={1}
              onValueChange={(v) => goTo(Math.round(v), false)}
              minimumTrackTintColor={theme.icon}
              maximumTrackTintColor={dark ? '#ffffff33' : '#00000022'}
              thumbTintColor={theme.fg}
            />
          )}
          <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
            <Text style={{ fontFamily: F.mono, fontSize: 12, color: theme.fg, opacity: 0.7 }}>
              {prefs.scroll ? `${Math.round(progress * 100)}%` : `page ${page + 1} of ${total}`}
            </Text>
            <Text style={{ fontFamily: F.mono, fontSize: 12, color: theme.fg, opacity: 0.7 }}>{minsLeft} min left in book</Text>
          </View>
        </View>
      )}

      <CustomSheet visible={sheet} onClose={() => setSheet(false)} />
    </View>
  );
}

function RdIcon({
  name,
  theme,
  onPress,
  color,
}: {
  name: string;
  theme: { icon: string };
  onPress: () => void;
  color?: string;
}) {
  return (
    <Pressable onPress={onPress} style={{ width: 42, height: 42, alignItems: 'center', justifyContent: 'center' }} hitSlop={4}>
      <Icon name={name} size={21} color={color || theme.icon} />
    </Pressable>
  );
}
