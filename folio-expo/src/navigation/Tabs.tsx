import React from 'react';
import { Platform, Text, View } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Icon } from '../components/Icon';
import { HomeScreen } from '../screens/HomeScreen';
import { LibraryScreen } from '../screens/LibraryScreen';
import { StatsScreen } from '../screens/StatsScreen';
import { SettingsScreen } from '../screens/SettingsScreen';
import { C, F } from '../theme';

const Tab = createBottomTabNavigator();

const ICONS: Record<string, string> = {
  Reading: 'home',
  Library: 'library',
  Stats: 'chart',
  Settings: 'gear',
};

export function Tabs() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: C.blue,
        tabBarInactiveTintColor: C.ink3,
        tabBarStyle: {
          backgroundColor: '#FDFAF5F2',
          borderTopColor: C.line,
          height: Platform.OS === 'ios' ? 84 : 64,
          paddingTop: 8,
          paddingBottom: Platform.OS === 'ios' ? 28 : 10,
          position: 'absolute',
        },
        tabBarLabel: ({ focused, color }) => (
          <Text style={{ fontSize: 11, fontWeight: '600', color, fontFamily: F.sans }}>{route.name}</Text>
        ),
        tabBarIcon: ({ color }) => (
          <View>
            <Icon name={ICONS[route.name]} size={23} color={color} />
          </View>
        ),
      })}
    >
      <Tab.Screen name="Reading" component={HomeScreen} />
      <Tab.Screen name="Library" component={LibraryScreen} />
      <Tab.Screen name="Stats" component={StatsScreen} />
      <Tab.Screen name="Settings" component={SettingsScreen} />
    </Tab.Navigator>
  );
}
