import React from 'react';
import { ScrollView, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ring } from '../components/Ring';
import { Icon } from '../components/Icon';
import { Card, Eyebrow, Mono, Serif } from '../components/ui';
import { HIGHLIGHTS, STATS } from '../data';
import { C, F, GENRE_COLORS, HEAT_COLORS } from '../theme';

export function StatsScreen() {
  const insets = useSafeAreaInsets();
  const S = STATS;
  const maxWeek = Math.max(...S.weekMinutes);
  const maxMonth = Math.max(...S.monthly);
  const goalPct = S.booksRead / S.goal;

  // heatmap is 18 cols x 7 rows; render as columns of weeks.
  const weeks: number[][] = [];
  for (let w = 0; w < 18; w++) weeks.push(S.heat.slice(w * 7, w * 7 + 7));

  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: C.paper }}
      contentContainerStyle={{ paddingTop: insets.top + 12, paddingBottom: 110 }}
      showsVerticalScrollIndicator={false}
    >
      <View style={{ paddingHorizontal: 20, marginBottom: 6 }}>
        <Mono style={{ fontSize: 11, letterSpacing: 1.6, color: C.ink3, marginBottom: 4 }}>
          YOUR YEAR IN BOOKS · {S.year}
        </Mono>
        <Serif style={{ fontSize: 34, fontWeight: '600', letterSpacing: -0.5 }}>Reading stats</Serif>
      </View>

      <View style={{ paddingHorizontal: 20, gap: 16, marginTop: 6 }}>
        {/* GOAL */}
        <Card style={{ padding: 20, flexDirection: 'row', alignItems: 'center', gap: 20 }}>
          <View style={{ width: 124, height: 124 }}>
            <Ring pct={goalPct} size={124} sw={11} />
            <View style={{ position: 'absolute', inset: 0, alignItems: 'center', justifyContent: 'center' } as any}>
              <Serif style={{ fontSize: 38, fontWeight: '600', lineHeight: 40 }}>{S.booksRead}</Serif>
              <Mono style={{ fontSize: 11, color: C.ink3, marginTop: 3 }}>of {S.goal}</Mono>
            </View>
          </View>
          <View style={{ flex: 1 }}>
            <Eyebrow>2026 Goal</Eyebrow>
            <Serif style={{ fontSize: 21, fontWeight: '600', marginVertical: 7, lineHeight: 24 }}>
              {S.goal - S.booksRead} books to go
            </Serif>
            <View
              style={{
                flexDirection: 'row',
                alignSelf: 'flex-start',
                alignItems: 'center',
                gap: 6,
                backgroundColor: C.blueSoft,
                paddingHorizontal: 11,
                paddingVertical: 5,
                borderRadius: 9,
              }}
            >
              <Icon name="check" size={14} color={C.blueInk} />
              <Text style={{ fontSize: 13, fontWeight: '600', color: C.blueInk, fontFamily: F.sans }}>Ahead of schedule</Text>
            </View>
          </View>
        </Card>

        {/* STREAK + HEATMAP */}
        <Card style={{ padding: 20 }}>
          <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
            <View style={{ flexDirection: 'row', alignItems: 'center', gap: 12 }}>
              <View style={{ width: 48, height: 48, borderRadius: 14, backgroundColor: '#FFE7CE', alignItems: 'center', justifyContent: 'center' }}>
                <Icon name="flame" size={26} color="#DD6F23" />
              </View>
              <View>
                <Serif style={{ fontSize: 26, fontWeight: '600', lineHeight: 28 }}>{S.streak} days</Serif>
                <Text style={{ fontSize: 13, color: C.ink3, marginTop: 2, fontFamily: F.sans }}>Current streak</Text>
              </View>
            </View>
            <View style={{ alignItems: 'flex-end' }}>
              <Serif style={{ fontSize: 20, fontWeight: '600' }}>{S.longestStreak}</Serif>
              <Text style={{ fontSize: 11.5, color: C.ink3, fontFamily: F.sans }}>longest</Text>
            </View>
          </View>
          <View style={{ flexDirection: 'row', gap: 4, justifyContent: 'space-between' }}>
            {weeks.map((wk, wi) => (
              <View key={wi} style={{ gap: 4, flex: 1 }}>
                {wk.map((v, di) => (
                  <View key={di} style={{ aspectRatio: 1, borderRadius: 3, backgroundColor: HEAT_COLORS[v] }} />
                ))}
              </View>
            ))}
          </View>
          <View style={{ flexDirection: 'row', alignItems: 'center', justifyContent: 'flex-end', gap: 5, marginTop: 10 }}>
            <Mono style={{ fontSize: 11, color: C.ink3 }}>Less</Mono>
            {HEAT_COLORS.map((c, i) => (
              <View key={i} style={{ width: 11, height: 11, borderRadius: 3, backgroundColor: c }} />
            ))}
            <Mono style={{ fontSize: 11, color: C.ink3 }}>More</Mono>
          </View>
        </Card>

        {/* TIME THIS WEEK */}
        <Card style={{ padding: 20 }}>
          <Eyebrow style={{ marginBottom: 4 }}>Time read · this week</Eyebrow>
          <View style={{ flexDirection: 'row', alignItems: 'baseline', gap: 8, marginBottom: 16 }}>
            <Serif style={{ fontSize: 32, fontWeight: '600' }}>{S.hoursThisWeek}h</Serif>
            <Text style={{ fontSize: 13.5, color: C.ink3, fontFamily: F.sans }}>· avg {S.avgSession}m / session</Text>
          </View>
          <View style={{ flexDirection: 'row', height: 110, gap: 8 }}>
            {S.weekMinutes.map((m, i) => (
              <View key={i} style={{ flex: 1, alignItems: 'center', gap: 8 }}>
                <View style={{ flex: 1, width: '100%', justifyContent: 'flex-end' }}>
                  <View style={{ height: `${(m / maxWeek) * 100}%`, width: '100%', backgroundColor: C.blue, borderRadius: 5, opacity: i === 5 ? 1 : 0.82 }} />
                </View>
                <Mono style={{ fontSize: 11, color: C.ink3 }}>{S.weekDays[i]}</Mono>
              </View>
            ))}
          </View>
        </Card>

        {/* TOTALS */}
        <View style={{ flexDirection: 'row', flexWrap: 'wrap', justifyContent: 'space-between', gap: 16 }}>
          <Totem icon="pages" value={S.pagesYear.toLocaleString()} label="Pages read" sub="this year" />
          <Totem icon="type" value={S.wordsYear} label="Words read" sub="≈ 9 novels" />
          <Totem icon="clock" value={`${S.hoursYear}h`} label="Hours read" sub={`${S.avgPace} pages / day`} />
          <Totem icon="trophy" value={`${S.booksRead}`} label="Books finished" sub="across 4 genres" />
        </View>

        {/* MONTHLY */}
        <Card style={{ padding: 20 }}>
          <Eyebrow style={{ marginBottom: 16 }}>Books finished by month</Eyebrow>
          <View style={{ flexDirection: 'row', height: 90, gap: 5, alignItems: 'flex-end' }}>
            {S.monthly.map((m, i) => (
              <View key={i} style={{ flex: 1, alignItems: 'center', gap: 7 }}>
                <View style={{ flex: 1, width: '100%', justifyContent: 'flex-end' }}>
                  <View
                    style={{
                      width: '100%',
                      height: m ? `${(m / maxMonth) * 100}%` : 3,
                      minHeight: 3,
                      borderRadius: 4,
                      backgroundColor: m ? C.blue : C.paper4,
                      opacity: i > 5 ? 1 : 0.55,
                    }}
                  />
                </View>
                <Mono style={{ fontSize: 9.5, color: C.ink3 }}>{S.monthLabels[i]}</Mono>
              </View>
            ))}
          </View>
        </Card>

        {/* GENRES */}
        <Card style={{ padding: 20 }}>
          <Eyebrow style={{ marginBottom: 14 }}>Genres</Eyebrow>
          <View style={{ flexDirection: 'row', height: 14, borderRadius: 7, overflow: 'hidden', marginBottom: 16 }}>
            {S.genres.map((g) => (
              <View key={g.name} style={{ width: `${g.pct}%`, backgroundColor: GENRE_COLORS[g.name] || C.ink3 }} />
            ))}
          </View>
          <View style={{ gap: 11 }}>
            {S.genres.map((g) => (
              <View key={g.name} style={{ flexDirection: 'row', alignItems: 'center', gap: 10 }}>
                <View style={{ width: 11, height: 11, borderRadius: 3, backgroundColor: GENRE_COLORS[g.name] || C.ink3 }} />
                <Text style={{ flex: 1, fontWeight: '500', fontSize: 14, color: C.ink, fontFamily: F.sans }}>{g.name}</Text>
                <Mono style={{ color: C.ink3, fontSize: 13 }}>{g.pct}%</Mono>
              </View>
            ))}
          </View>
        </Card>

        {/* TOP AUTHORS */}
        <Card style={{ overflow: 'hidden', padding: 0 }}>
          <Eyebrow style={{ paddingHorizontal: 18, paddingTop: 18, paddingBottom: 4 }}>Most read authors</Eyebrow>
          {S.topAuthors.map((a, i) => (
            <View
              key={a.name}
              style={{ flexDirection: 'row', alignItems: 'center', gap: 13, paddingHorizontal: 18, paddingVertical: 13, borderTopWidth: 1, borderTopColor: C.line }}
            >
              <Serif style={{ fontSize: 18, color: C.ink3, width: 18 }}>{i + 1}</Serif>
              <Text style={{ flex: 1, fontWeight: '600', fontSize: 15.5, color: C.ink, fontFamily: F.sans }}>{a.name}</Text>
              <Mono style={{ fontSize: 12.5, color: C.ink3 }}>{a.books} books</Mono>
            </View>
          ))}
        </Card>

        {/* HIGHLIGHTS */}
        <View>
          <Eyebrow style={{ marginBottom: 12, paddingLeft: 2 }}>Highlights & notes · {HIGHLIGHTS.length}</Eyebrow>
          <View style={{ gap: 12 }}>
            {HIGHLIGHTS.map((h) => (
              <Card key={h.id} style={{ padding: 16 }}>
                <Serif style={{ fontSize: 16.5, lineHeight: 25, color: C.ink }}>
                  “
                  <Serif style={{ backgroundColor: h.color === 'b' ? '#B9D2F6' : '#F4E3A8' }}>{h.text}</Serif>
                  ”
                </Serif>
                <View style={{ flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 11 }}>
                  <Icon name="bookOpen" size={14} color={C.ink3} />
                  <Text style={{ fontSize: 12.5, color: C.ink3, fontFamily: F.sans }}>
                    <Text style={{ fontWeight: '600', color: C.ink2 }}>{h.book}</Text> · {h.author}
                  </Text>
                </View>
              </Card>
            ))}
          </View>
        </View>
      </View>
    </ScrollView>
  );
}

function Totem({ icon, value, label, sub }: { icon: string; value: string; label: string; sub: string }) {
  return (
    <Card style={{ padding: 16, width: '47%' }}>
      <Icon name={icon} size={19} color={C.ink3} />
      <Serif style={{ fontSize: 28, fontWeight: '600', marginTop: 9, lineHeight: 30 }}>{value}</Serif>
      <Text style={{ fontSize: 13.5, fontWeight: '600', marginTop: 5, color: C.ink, fontFamily: F.sans }}>{label}</Text>
      <Mono style={{ fontSize: 11, color: C.ink3, marginTop: 2 }}>{sub}</Mono>
    </Card>
  );
}
