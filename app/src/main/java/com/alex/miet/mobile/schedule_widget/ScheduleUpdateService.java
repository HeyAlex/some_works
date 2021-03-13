package com.alex.miet.mobile.schedule_widget;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.alex.miet.mobile.R;
import com.alex.miet.mobile.ScheduleApp;
import com.alex.miet.mobile.data.shared_interactor.ScheduleInteractor;
import com.alex.miet.mobile.schedule.ScheduleActivity;
import com.alex.miet.mobile.util.PrefUtils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Service for updating and building a widget
 */
public class ScheduleUpdateService extends IntentService {
    public static final String GROUPNAME_KEY = "group";

    public static final String TOMORROW_ACTION = "TOMORROW_ACTION";
    public static final String TODAY_ACTION = "TODAY_ACTION";
    public static final String PIN_ACTION = "PIN_ACTION";
    private static final int NOTIFICATION_ID = 100;

    @Inject
    ScheduleInteractor interactor;

    public ScheduleUpdateService() {
        super("ScheduleUpdateService");
    }

    public static PendingIntent getScheduleUpdateServicePendingIntent(Context context, String action,
                                                                      int widgetId, String group) {
        Intent resultValue = new Intent(context, ScheduleUpdateService.class);
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        resultValue.putExtra(GROUPNAME_KEY, group);
        resultValue.setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return PendingIntent.getForegroundService(context, 0, resultValue, PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            return PendingIntent.getService(context, 0, resultValue, PendingIntent.FLAG_CANCEL_CURRENT);
        }
    }

    public static PendingIntent getScheduleConfigurationPendingIntent(Context context,
                                                                      int widgetId) {
        Intent resultValue = new Intent(context, ScheduleAppWidgetConfigureActivity.class);
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        Uri data = Uri.parse(resultValue.toUri(Intent.URI_INTENT_SCHEME));
        resultValue.setData(data);
        return PendingIntent.getActivity(context, 0, resultValue, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    public static PendingIntent getScheduleConfigurationPendingIntentForPinning(Context context,
                                                                                String groupName) {
        Intent resultValue = new Intent(context, ScheduleUpdateService.class);
        resultValue.putExtra(GROUPNAME_KEY, groupName);
        resultValue.setAction(PIN_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return PendingIntent.getForegroundService(context, 0, resultValue, PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            return PendingIntent.getService(context, 0, resultValue, PendingIntent.FLAG_CANCEL_CURRENT);
        }
    }

    public static PendingIntent getAlarmIntent(Context context, String action, int widgetId,
                                               String group) {
        Intent resultValue = new Intent(context, ScheduleAlarmReceiver.class);
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        resultValue.putExtra(GROUPNAME_KEY, group);
        resultValue.setAction(action);
        return PendingIntent.getBroadcast(context, 0, resultValue, 0);
    }

    public static PendingIntent getSchedulePendingIntent(Context context, String group) {
        Intent resultValue = new Intent(context, ScheduleActivity.class);
        resultValue.putExtra(GROUPNAME_KEY, group);
        resultValue.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        Uri data = Uri.parse(resultValue.toUri(Intent.URI_INTENT_SCHEME));
        resultValue.setData(data);
        return PendingIntent.getActivity(context, 0, resultValue, 0);
    }

    public static Intent getScheduleIntent(Context context, String action, int widgetId,
                                           String group) {
        Intent resultValue = new Intent(context, ScheduleUpdateService.class);
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        resultValue.putExtra(GROUPNAME_KEY, group);
        resultValue.setAction(action);
        return resultValue;
    }

    public static void setupAlarm(Context context, int widgetId, String group) {
        final AlarmManager alarmManager = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = getAlarmIntent(context, ScheduleUpdateService.TODAY_ACTION
                + String.valueOf(widgetId), widgetId, group);
        alarmManager.cancel(pendingIntent);
        DateTime todayStart = new DateTime().withZone(DateTimeZone.forID("Europe/Moscow"));
        DateTime tomorrowStart = todayStart.plusDays(1).withTimeAtStartOfDay();
        long ml = new Duration(todayStart, tomorrowStart).getMillis();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Timber.i("Time for a next trigger of schedule widget update is %s", String.valueOf(ml));
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, tomorrowStart.getMillis(),
                    pendingIntent);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, tomorrowStart.getMillis(),
                        pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, tomorrowStart.getMillis(), pendingIntent);
            }

        }
    }

    public static void stopAlarm(Context context, int widgetId, String group) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(getScheduleUpdateServicePendingIntent(context, TODAY_ACTION
                + String.valueOf(widgetId), widgetId, group));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = getNotificationWithChannel();
        } else {
            notification = new Notification();
        }
        startForeground(NOTIFICATION_ID, notification);
        ScheduleApp.get(this)
                .getApplicationComponent()
                .inject(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification getNotificationWithChannel() {
        String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
        String channelName = "Widget Updater";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        return notificationBuilder.setOngoing(true)
                .setContentTitle("Обновляем виджет расписания")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
            int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);

            Timber.i("Schedule widget service update");
            if (intent.getAction() != null) {
                boolean isTomorrowAction = intent.getAction().startsWith(TOMORROW_ACTION);
                boolean isTodayAction = intent.getAction().startsWith(TODAY_ACTION);

                String groupName = intent.getStringExtra(GROUPNAME_KEY);
                if (intent.getAction().equals(PIN_ACTION)) {
                    isTomorrowAction = false;
                    isTodayAction = true;
                    Timber.i(String.valueOf(widgetId));
                    PrefUtils.saveToPrefs(this, String.valueOf(widgetId), groupName);
                    ScheduleUpdateService.setupAlarm(this, widgetId, groupName);
                }

                if (!interactor.isGroupInCache(groupName)) {
                    interactor.downloadGroup(groupName);
                }
                RemoteViews views = ScheduleRemoteViewBuilder.newBuilder(this, groupName, widgetId)
                        .setTomorrowHeader(isTomorrowAction)
                        .setTodayHeader(isTodayAction)
                        .setAdapterForLessons()
                        .build();
                appWidgetManager.updateAppWidget(widgetId, views);
                appWidgetManager.notifyAppWidgetViewDataChanged(widgetId, R.id.lessons);
            }
        }
    }
}