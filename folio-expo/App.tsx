import 'react-native-gesture-handler';
import React from 'react';
import { StatusBar } from 'expo-status-bar';
import { DefaultTheme, NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { Tabs } from './src/navigation/Tabs';
import { DetailScreen } from './src/screens/DetailScreen';
import { ReaderScreen } from './src/screens/ReaderScreen';
import { PrefsProvider } from './src/lib/prefs';
import { RootStackParamList } from './src/navigation/types';
import { C } from './src/theme';

const Stack = createNativeStackNavigator<RootStackParamList>();

const navTheme = {
  ...DefaultTheme,
  colors: { ...DefaultTheme.colors, background: C.paper, card: C.paper, primary: C.blue },
};

export default function App() {
  return (
    <SafeAreaProvider>
      <PrefsProvider>
        <NavigationContainer theme={navTheme}>
          <StatusBar style="dark" />
          <Stack.Navigator screenOptions={{ headerShown: false }}>
            <Stack.Screen name="Tabs" component={Tabs} />
            <Stack.Screen name="Detail" component={DetailScreen} />
            <Stack.Screen
              name="Reader"
              component={ReaderScreen}
              options={{ animation: 'fade', presentation: 'fullScreenModal' }}
            />
          </Stack.Navigator>
        </NavigationContainer>
      </PrefsProvider>
    </SafeAreaProvider>
  );
}
