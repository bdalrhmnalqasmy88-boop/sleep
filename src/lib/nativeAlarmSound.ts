import { registerPlugin } from '@capacitor/core';

export type PickedAudio = {
  uri: string;
  name: string;
};

export interface AlarmSoundPlugin {
  pickAudio(): Promise<PickedAudio>;

  configureChannel(options: {
    soundUri?: string;
    channelId: string;
  }): Promise<{
    channelId: string;
  }>;

  scheduleAlarm(options: {
    id: number;
    title: string;
    body: string;
    at: number;
    soundUri?: string;
  }): Promise<void>;

  cancelAlarm(options: {
    id: number;
  }): Promise<void>;

  openExactAlarmSettings(): Promise<void>;
}

export const NativeAlarmSound =
  registerPlugin<AlarmSoundPlugin>('AlarmSound');
