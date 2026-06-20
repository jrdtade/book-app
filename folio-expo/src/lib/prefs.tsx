import React, { createContext, useContext, useEffect, useState } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { ReaderThemeId } from '../theme';

export type FlipMode = 'curl' | 'slide' | 'fade' | 'none';
export type Align = 'left' | 'justify';

export interface Prefs {
  theme: ReaderThemeId;
  font: string; // serif | sans (family bucket)
  size: number;
  lh: number;
  mx: number;
  weight: 400 | 600;
  align: Align;
  brightness: number;
  warmth: number;
  flip: FlipMode;
  scroll: boolean;
}

export const DEFAULTS: Prefs = {
  theme: 'sepia',
  font: 'serif',
  size: 20,
  lh: 1.62,
  mx: 30,
  weight: 400,
  align: 'left',
  brightness: 1,
  warmth: 0,
  flip: 'curl',
  scroll: false,
};

const KEY = 'folio.prefs';

interface Ctx {
  prefs: Prefs;
  setPrefs: (p: Partial<Prefs>) => void;
}

const PrefsContext = createContext<Ctx>({ prefs: DEFAULTS, setPrefs: () => {} });

export function PrefsProvider({ children }: { children: React.ReactNode }) {
  const [prefs, setState] = useState<Prefs>(DEFAULTS);

  useEffect(() => {
    AsyncStorage.getItem(KEY).then((raw) => {
      if (raw) {
        try {
          setState({ ...DEFAULTS, ...JSON.parse(raw) });
        } catch {}
      }
    });
  }, []);

  const setPrefs = (patch: Partial<Prefs>) => {
    setState((prev) => {
      const next = { ...prev, ...patch };
      AsyncStorage.setItem(KEY, JSON.stringify(next)).catch(() => {});
      return next;
    });
  };

  return <PrefsContext.Provider value={{ prefs, setPrefs }}>{children}</PrefsContext.Provider>;
}

export const usePrefs = () => useContext(PrefsContext);

// Per-book last reading position.
export const posKey = (id: string) => `folio.pos.${id}`;
