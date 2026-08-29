package com.sleepcycle.alarm;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.PluginMethod;

import java.util.HashSet;
import java.util.Set;

@CapacitorPlugin(name = "AlarmSound")
public class AlarmSoundPlugin extends Plugin {

    private static final String DEFAULT_CHANNEL_ID = "alarm-channel";

    private final Set<Integer> scheduledAlarmIds = new HashSet<>();

    // ============================================================
    // إنشاء قناة المنبه
    // ============================================================

    @PluginMethod
    public void configureChannel(PluginCall call) {

        String channelId = call.getString("channelId");
        String soundUri = call.getString("soundUri");

        if (channelId == null || channelId.trim().isEmpty()) {
            channelId = DEFAULT_CHANNEL_ID;
        }

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                NotificationManager manager =
                        (NotificationManager)
                                getContext().getSystemService(
                                        Context.NOTIFICATION_SERVICE
                                );

                if (manager == null) {
                    call.reject("NotificationManager unavailable");
                    return;
                }

                NotificationChannel channel =
                        new NotificationChannel(
                                channelId,
                                "Sleep Alarm",
                                NotificationManager.IMPORTANCE_HIGH
                        );

                channel.setDescription("منبه دورة النوم");
                channel.enableVibration(true);

                AudioAttributes audioAttributes =
                        new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ALARM)
                                .setContentType(
                                        AudioAttributes.CONTENT_TYPE_SONIFICATION
                                )
                                .build();

                if (soundUri != null &&
                        !soundUri.trim().isEmpty()) {

                    try {

                        Uri uri = Uri.parse(soundUri);

                        channel.setSound(
                                uri,
                                audioAttributes
                        );

                    } catch (Exception ignored) {
                        // استخدام الصوت الافتراضي إذا فشل الصوت المخصص
                    }
                }

                manager.createNotificationChannel(channel);
            }

            JSObject result = new JSObject();

            result.put("channelId", channelId);

            call.resolve(result);

        } catch (Exception e) {

            call.reject(
                    "Could not configure alarm channel",
                    e
            );
        }
    }

    // ============================================================
    // جدولة المنبه
    // ============================================================

    @PluginMethod
    public void scheduleAlarm(PluginCall call) {

        int id = call.getInt("id", 10001);

        String title =
                call.getString(
                        "title",
                        "منبه دورة النوم"
                );

        String body =
                call.getString(
                        "body",
                        "حان وقت المنبه"
                );

        Long at = call.getLong("at");

        String soundUri = call.getString("soundUri");

        if (at == null) {

            call.reject("Alarm time is missing");

            return;
        }

        if (at <= System.currentTimeMillis()) {

            call.reject("Alarm time is in the past");

            return;
        }

        try {

            AlarmManager alarmManager =
                    (AlarmManager)
                            getContext().getSystemService(
                                    Context.ALARM_SERVICE
                            );

            if (alarmManager == null) {

                call.reject("AlarmManager unavailable");

                return;
            }

            // Android 12 وما بعده
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                if (!alarmManager.canScheduleExactAlarms()) {

                    call.reject(
                            "Exact alarm permission is not granted"
                    );

                    return;
                }
            }

            Intent receiverIntent =
                    new Intent(
                            getContext(),
                            AlarmReceiver.class
                    );

            receiverIntent.putExtra(
                    AlarmReceiver.EXTRA_ALARM_ID,
                    id
            );

            receiverIntent.putExtra(
                    AlarmReceiver.EXTRA_TITLE,
                    title
            );

            receiverIntent.putExtra(
                    AlarmReceiver.EXTRA_BODY,
                    body
            );

            if (soundUri != null &&
                    !soundUri.trim().isEmpty()) {

                receiverIntent.putExtra(
                        AlarmReceiver.EXTRA_SOUND_URI,
                        soundUri
                );
            }

            PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(
                            getContext(),
                            id,
                            receiverIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT |
                                    PendingIntent.FLAG_IMMUTABLE
                    );

            // إلغاء أي منبه سابق بنفس الرقم
            alarmManager.cancel(pendingIntent);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        at,
                        pendingIntent
                );

            } else {

                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        at,
                        pendingIntent
                );
            }

            scheduledAlarmIds.add(id);

            JSObject result = new JSObject();

            result.put("id", id);
            result.put("scheduledAt", at);
            result.put("success", true);

            call.resolve(result);

        } catch (SecurityException e) {

            call.reject(
                    "Exact alarm permission denied",
                    e
            );

        } catch (Exception e) {

            call.reject(
                    "Failed to schedule Android alarm",
                    e
            );
        }
    }

    // ============================================================
    // إلغاء المنبه
    // ============================================================

    @PluginMethod
    public void cancelAlarm(PluginCall call) {

        int id = call.getInt("id", 10001);

        try {

            AlarmManager alarmManager =
                    (AlarmManager)
                            getContext().getSystemService(
                                    Context.ALARM_SERVICE
                            );

            if (alarmManager != null) {

                Intent receiverIntent =
                        new Intent(
                                getContext(),
                                AlarmReceiver.class
                        );

                PendingIntent pendingIntent =
                        PendingIntent.getBroadcast(
                                getContext(),
                                id,
                                receiverIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT |
                                        PendingIntent.FLAG_IMMUTABLE
                        );

                alarmManager.cancel(pendingIntent);

                pendingIntent.cancel();
            }

            scheduledAlarmIds.remove(id);

            JSObject result = new JSObject();

            result.put("id", id);
            result.put("success", true);

            call.resolve(result);

        } catch (Exception e) {

            call.reject(
                    "Failed to cancel alarm",
                    e
            );
        }
    }

    // ============================================================
    // فتح إعدادات Exact Alarm
    // ============================================================

    @PluginMethod
    public void openExactAlarmSettings(PluginCall call) {

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                Intent intent =
                        new Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        );

                intent.setData(
                        Uri.parse(
                                "package:" +
                                        getContext().getPackageName()
                        )
                );

                try {

                    getContext().startActivity(intent);

                } catch (ActivityNotFoundException e) {

                    Intent fallback =
                            new Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            );

                    fallback.setData(
                            Uri.parse(
                                    "package:" +
                                            getContext().getPackageName()
                            )
                    );

                    getContext().startActivity(fallback);
                }
            }

            call.resolve();

        } catch (Exception e) {

            call.reject(
                    "Could not open exact alarm settings",
                    e
            );
        }
    }

    // ============================================================
    // اختيار ملف صوتي
    // ============================================================

    @PluginMethod
    public void pickAudio(PluginCall call) {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType("audio/*");

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(
                call,
                intent,
                "audioPickerResult"
        );
    }

    // ============================================================
    // نتيجة اختيار الصوت
    // ============================================================

    @ActivityCallback
    private void audioPickerResult(
            PluginCall call,
            androidx.activity.result.ActivityResult result
    ) {

        if (call == null) {
            return;
        }

        if (result == null ||
                result.getResultCode() !=
                        android.app.Activity.RESULT_OK ||
                result.getData() == null ||
                result.getData().getData() == null) {

            call.reject("لم يتم اختيار ملف صوتي");

            return;
        }

        Intent data = result.getData();

        Uri uri = data.getData();

        try {

            int takeFlags =
                    data.getFlags() &
                            (
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            );

            getContext()
                    .getContentResolver()
                    .takePersistableUriPermission(
                            uri,
                            takeFlags
                    );

        } catch (Exception ignored) {
            // بعض مديري الملفات لا يدعمون صلاحية التخزين الدائمة
        }

        String name = uri.getLastPathSegment();

        if (name == null ||
                name.trim().isEmpty()) {

            name = "custom-alarm-sound";
        }

        JSObject resultObject = new JSObject();

        resultObject.put(
                "uri",
                uri.toString()
        );

        resultObject.put(
                "name",
                name
        );

        call.resolve(resultObject);
    }
}
