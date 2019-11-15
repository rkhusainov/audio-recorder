package com.khusainov.rinat.audiorecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class RecordService extends Service {

    public static final String CHANNEL_ID = "CHANNEL_1";
    public static final int NOTIFICATION_ID = 1;

    public static final String STOP_ACTION = "STOP_ACTION";
    private static final String PAUSE_ACTION = "PAUSE_ACTION";
    private static final String TAG = RecordService.class.getSimpleName();

    private boolean isPaused=false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    /**
     * Вызывается системой каждый раз, когда клиент непосредственно стартует
     * Service c помощью вызова Context.startService(Intent)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if (STOP_ACTION.equals(action)) {
            Intent stopIntent = new Intent(MainActivity.STOP_RECORD);
            sendBroadcast(stopIntent);
            stopSelf();
        } else if (PAUSE_ACTION.equals(action)) {
            if (!isPaused) {
                Intent pauseIntent = new Intent(MainActivity.PAUSE_RECORD);
                sendBroadcast(pauseIntent);
                isPaused = true;
                Log.d(TAG, "onStartCommand: PAUSE");
            } else {
                Intent resumeIntent = new Intent(MainActivity.RESUME_RECORD);
                sendBroadcast(resumeIntent);
                isPaused = false;
                Log.d(TAG, "onStartCommand: RESUME");
            }
        } else {
            startForeground(NOTIFICATION_ID, createNotification());
        }

        return START_NOT_STICKY;
    }

    /**
     * Создание кастомной нотификации через RemoteViews
     * Обработка кликов путем отправки PendingIntent
     **/
    private Notification createNotification() {

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.record_notification);
        remoteViews.setTextViewText(R.id.tv_name, "Record_1");
        remoteViews.setOnClickPendingIntent(R.id.root, getOpenActivityPendingIntent());
        remoteViews.setOnClickPendingIntent(R.id.btn_stop, getStopPendingIntent());
        remoteViews.setOnClickPendingIntent(R.id.btn_pause, getPausePendingIntent());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContent(remoteViews);
        return builder.build();

    }

    private PendingIntent getOpenActivityPendingIntent() {
        Intent openActivityIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openActivityIntent, 0);
        return pendingIntent;
    }

    private PendingIntent getStopPendingIntent() {
        Intent stopIntent = new Intent(this, RecordService.class);
        stopIntent.setAction(STOP_ACTION);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, stopIntent, 0);
        return pendingIntent;
    }

    private PendingIntent getPausePendingIntent() {
        Intent pauseIntent = new Intent(this, RecordService.class);
        pauseIntent.setAction(PAUSE_ACTION);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, pauseIntent, 0);
        return pendingIntent;
    }

    /**
     * Создание канала для Notification, требуется для API >= 26
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "Channel Name",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
