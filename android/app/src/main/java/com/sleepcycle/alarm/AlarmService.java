package com.sleepcycle.alarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class AlarmService extends Service {

    private static final String CHANNEL_ID = "sleep_alarm_service";
    private static final int NOTIFICATION_ID = 20001;

    private MediaPlayer mediaPlayer;

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();

        Notification notification =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("منبه دورة النوم")
                        .setContentText("المنبه يعمل الآن")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setOngoing(true)
                        .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String soundUri = null;

        if (intent != null) {
            soundUri = intent.getStringExtra(
                    AlarmReceiver.EXTRA_SOUND_URI
            );
        }

        startAlarmSound(soundUri);

        return START_NOT_STICKY;
    }

    private void startAlarmSound(String soundUri) {

        stopAlarmSound();

        try {
            Uri alarmUri;

            if (soundUri != null && !soundUri.isEmpty()) {
                alarmUri = Uri.parse(soundUri);
            } else {
                alarmUri = RingtoneManager.getDefaultUri(
                        RingtoneManager.TYPE_ALARM
                );
            }

            mediaPlayer = new MediaPlayer();

            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(
                                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                            )
                            .build()
            );

            mediaPlayer.setDataSource(
                    getApplicationContext(),
                    alarmUri
            );

            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();

        } catch (Exception e) {

            // إذا تعذر تشغيل الصوت المخصص،
            // استخدم صوت المنبه الافتراضي.
            try {

                Uri fallbackUri =
                        RingtoneManager.getDefaultUri(
                                RingtoneManager.TYPE_ALARM
                        );

                mediaPlayer = new MediaPlayer();

                mediaPlayer.setAudioAttributes(
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(
                                        AudioAttributes.CONTENT_TYPE_SONIFICATION
                                )
                                .build()
                );

                mediaPlayer.setDataSource(
                        getApplicationContext(),
                        fallbackUri
                );

                mediaPlayer.setLooping(true);
                mediaPlayer.prepare();
                mediaPlayer.start();

            } catch (Exception ignored) {
            }
        }
    }

    private void stopAlarmSound() {

        if (mediaPlayer != null) {

            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (Exception ignored) {
            }

            try {
                mediaPlayer.release();
            } catch (Exception ignored) {
            }

            mediaPlayer = null;
        }
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Sleep Alarm",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription(
                    "منبه دورة النوم"
            );

            channel.enableVibration(true);

            channel.setSound(
                    RingtoneManager.getDefaultUri(
                            RingtoneManager.TYPE_ALARM
                    ),
                    new AudioAttributes.Builder()
                            .setUsage(
                                    AudioAttributes.USAGE_ALARM
                            )
                            .setContentType(
                                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                            )
                            .build()
            );

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {

        stopAlarmSound();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
