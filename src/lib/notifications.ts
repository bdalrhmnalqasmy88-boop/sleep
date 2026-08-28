import type { SleepSession } from './storage';
import { NativeAlarmSound } from './nativeAlarmSound';

type AlarmConfig = {
  id: number;
  title: string;
  body: string;
  at: Date;
  sound: string;
  volume: number;
  soundUri?: string;
};

const isCapacitor =
  typeof window !== 'undefined' &&
  typeof window.Capacitor !== 'undefined';

async function getLocalNotifications() {
  if (!isCapacitor) return null;

  try {
    const mod = await import('@capacitor/local-notifications');
    return mod.LocalNotifications;
  } catch (error) {
    console.error(
      'Failed to load LocalNotifications:',
      error
    );
    return null;
  }
}

export async function initializeNotifications(): Promise<void> {
  if (!isCapacitor) return;

  const LocalNotifications =
    await getLocalNotifications();

  if (LocalNotifications) {
    try {
      const permission =
        await LocalNotifications.checkPermissions();

      if (permission.display !== 'granted') {
        await LocalNotifications.requestPermissions();
      }
    } catch (error) {
      console.warn(
        'Notification permission error:',
        error
      );
    }
  }

  try {
    await NativeAlarmSound.configureChannel({
      channelId: 'alarm-channel',
    });
  } catch (error) {
    console.warn(
      'Could not configure native alarm channel:',
      error
    );
  }
}

export async function configureAlarmChannel(
  soundUri?: string
): Promise<string> {
  const channelId = soundUri
    ? `alarm-channel-${Array.from(soundUri)
        .reduce(
          (hash, char) =>
            ((hash * 31 + char.charCodeAt(0)) | 0),
          0
        )
        .toString(36)
        .replace('-', 'n')}`
    : 'alarm-channel';

  try {
    const result =
      await NativeAlarmSound.configureChannel({
        channelId,
        soundUri,
      });

    return result.channelId;
  } catch (error) {
    console.warn(
      'Could not configure alarm channel:',
      error
    );

    return 'alarm-channel';
  }
}

export async function scheduleAlarm(
  config: AlarmConfig
): Promise<void> {
  if (!isCapacitor) {
    console.warn(
      'Native Android alarm is not available.'
    );
    return;
  }

  const alarmTime = config.at.getTime();

  if (!Number.isFinite(alarmTime)) {
    throw new Error('Invalid alarm date');
  }

  if (alarmTime <= Date.now()) {
    throw new Error(
      'Alarm time is already in the past'
    );
  }

  const channelId =
    await configureAlarmChannel(
      config.soundUri
    );

  console.log(
    'Scheduling native Android alarm:',
    {
      id: config.id,
      time: config.at.toISOString(),
      timestamp: alarmTime,
    }
  );

  try {
    try {
      await NativeAlarmSound.cancelAlarm({
        id: config.id,
      });
    } catch {
      // لا يوجد منبه سابق، وهذا طبيعي.
    }

    await NativeAlarmSound.scheduleAlarm({
      id: config.id,
      title: config.title,
      body: config.body,
      at: alarmTime,
      soundUri: config.soundUri,
    });

    console.log(
      'Native Android alarm scheduled successfully:',
      config.id,
      config.at.toISOString(),
      channelId
    );
  } catch (error) {
    console.error(
      'FAILED TO SCHEDULE NATIVE ANDROID ALARM:',
      config.id,
      error
    );

    throw error;
  }
}

export async function cancelAlarm(
  id: number
): Promise<void> {
  if (!isCapacitor) return;

  try {
    await NativeAlarmSound.cancelAlarm({
      id,
    });

    console.log(
      'Native Android alarm cancelled:',
      id
    );
  } catch (error) {
    console.error(
      'Failed to cancel native alarm:',
      id,
      error
    );
  }
}

export async function cancelAllAlarms(): Promise<void> {
  if (!isCapacitor) return;

  const alarmIds = [
    10001,
    10002,
  ];

  for (const id of alarmIds) {
    try {
      await NativeAlarmSound.cancelAlarm({
        id,
      });
    } catch (error) {
      console.warn(
        'Could not cancel alarm:',
        id,
        error
      );
    }
  }

  console.log(
    'Native Android alarms cancelled.'
  );
}

export async function openExactAlarmSettings(): Promise<void> {
  if (!isCapacitor) return;

  try {
    await NativeAlarmSound.openExactAlarmSettings();
  } catch (error) {
    console.error(
      'Could not open exact alarm settings:',
      error
    );
  }
}

export function makeSessionFromAlarm(
  wakeTime: string,
  bedtime: Date,
  wakeDate: Date,
  cycles: number,
  durationMin: number
): SleepSession {
  return {
    id:
      Date.now().toString(36) +
      Math.random().toString(36).substring(2, 7),

    wake_time: wakeTime,
    bedtime: bedtime.toISOString(),
    final_wake: wakeDate.toISOString(),

    cycles,

    duration_min: durationMin,

    completed: false,

    created_at:
      new Date().toISOString(),
  };
}
