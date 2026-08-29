package com.sleepcycle.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {

    public static final String EXTRA_SOUND_URI = "soundUri";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_BODY = "body";
    public static final String EXTRA_ALARM_ID = "alarmId";

    @Override
    public void onReceive(Context context, Intent intent) {

        Intent serviceIntent =
                new Intent(context, AlarmService.class);

        if (intent != null) {

            String soundUri =
                    intent.getStringExtra(EXTRA_SOUND_URI);

            String title =
                    intent.getStringExtra(EXTRA_TITLE);

            String body =
                    intent.getStringExtra(EXTRA_BODY);

            int alarmId =
                    intent.getIntExtra(
                            EXTRA_ALARM_ID,
                            10001
                    );

            if (soundUri != null &&
                    !soundUri.isEmpty()) {

                serviceIntent.putExtra(
                        EXTRA_SOUND_URI,
                        soundUri
                );
            }

            if (title != null) {

                serviceIntent.putExtra(
                        EXTRA_TITLE,
                        title
                );
            }

            if (body != null) {

                serviceIntent.putExtra(
                        EXTRA_BODY,
                        body
                );
            }

            serviceIntent.putExtra(
                    EXTRA_ALARM_ID,
                    alarmId
            );
        }

        /*
         * تشغيل خدمة المنبه عند وصول الموعد،
         * حتى لو كانت واجهة التطبيق مغلقة.
         */
        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.O) {

            context.startForegroundService(
                    serviceIntent
            );

        } else {

            context.startService(
                    serviceIntent
            );
        }
    }
}
