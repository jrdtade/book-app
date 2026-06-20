import type { NativeStackNavigationProp } from '@react-navigation/native-stack';

export type RootStackParamList = {
  Tabs: undefined;
  Detail: { id: string };
  Reader: { id: string };
};

// Tab screens also reach into the root stack (Detail/Reader) and sibling tabs,
// so we type navigation loosely as `any`-friendly composite for ergonomics.
export type TabNav = NativeStackNavigationProp<RootStackParamList> & {
  navigate: (screen: string, params?: any) => void;
};
