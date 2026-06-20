import React from 'react';
import { View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { C } from '../theme';

export function Stars({ n, size = 14, gap = 2 }: { n: number; size?: number; gap?: number }) {
  return (
    <View style={{ flexDirection: 'row', gap }}>
      {[1, 2, 3, 4, 5].map((i) => (
        <Ionicons
          key={i}
          name={i <= n ? 'star' : 'star-outline'}
          size={size}
          color={i <= n ? C.gold : C.paper4}
        />
      ))}
    </View>
  );
}
