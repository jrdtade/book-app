import React from 'react';
import { Pressable, Text, View, ViewStyle } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Book } from '../data';
import { F, shadow } from '../theme';

// Typographic book cover: diagonal gradient + author kicker + serif title,
// mirroring the prototype's <Cover> (covers carry no artwork).
export function Cover({
  book,
  w,
  onPress,
  style,
}: {
  book: Book;
  w: number;
  onPress?: () => void;
  style?: ViewStyle;
}) {
  const h = w * 1.5;
  const scale = w / 154;
  const titleSize = 19 * book.titleSize * scale;
  const lines = book.title.split('\n');

  const inner = (
    <LinearGradient
      colors={book.cover}
      start={{ x: 0.1, y: 0 }}
      end={{ x: 0.9, y: 1 }}
      style={[
        {
          width: w,
          height: h,
          borderRadius: Math.max(4, 6 * scale),
          padding: 14 * scale,
          justifyContent: 'space-between',
        },
        shadow.book,
        style,
      ]}
    >
      <Text
        style={{
          color: book.fg,
          opacity: 0.78,
          fontSize: Math.max(7, 9 * scale),
          letterSpacing: 1.2,
          textTransform: 'uppercase',
          fontFamily: F.sans,
          fontWeight: '700',
        }}
        numberOfLines={1}
      >
        {book.author}
      </Text>
      <View>
        {lines.map((ln, i) => (
          <Text
            key={i}
            style={{
              color: book.fg,
              fontSize: titleSize,
              lineHeight: titleSize * 1.04,
              fontFamily: F.serif,
              fontWeight: '600',
            }}
          >
            {ln}
          </Text>
        ))}
      </View>
      <View
        style={{
          height: 2 * scale,
          width: w * 0.34,
          backgroundColor: book.fg,
          opacity: 0.5,
          borderRadius: 2,
        }}
      />
    </LinearGradient>
  );

  if (onPress) {
    return (
      <Pressable onPress={onPress} style={({ pressed }) => ({ opacity: pressed ? 0.85 : 1 })}>
        {inner}
      </Pressable>
    );
  }
  return inner;
}
