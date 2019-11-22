package com.khusainov.rinat.audiorecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static com.khusainov.rinat.audiorecorder.PlayerService.ACTION_NEXT;
import static com.khusainov.rinat.audiorecorder.PlayerService.ACTION_PAUSE;
import static com.khusainov.rinat.audiorecorder.PlayerService.ACTION_PREVIOUS;
import static com.khusainov.rinat.audiorecorder.PlayerService.ACTION_STOP;
import static com.khusainov.rinat.audiorecorder.RecordService.NOTIFICATION_ID;

public class PlayerNotificationHelper {
    public static final String CHANNEL_ID = "PLAYER_CHANNEL";

    private Context mContext;

    public PlayerNotificationHelper(Context context) {
        mContext = context;
    }

    /**
     * Создание кастомной нотификации через RemoteViews
     * Обработка кликов путем отправки PendingIntent
     **/
    public Notification createNotification(boolean isPaused) {

        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.player_notification);
        remoteViews.setTextViewText(R.id.tv_name, "Record_1");
        remoteViews.setOnClickPendingIntent(R.id.player_notification_root, getOpenActivityPendingIntent());
        remoteViews.setOnClickPendingIntent(R.id.iv_prev, getPreviousPendingIntent());
        remoteViews.setOnClickPendingIntent(R.id.iv_next, getNextPendingIntent());
        remoteViews.setOnClickPendingIntent(R.id.iv_stop, getStopPendingIntent());
        remoteViews.setOnClickPendingIntent(R.id.iv_pause, getPausePendingIntent());

        if (isPaused) {
            remoteViews.setImageViewResource(R.id.iv_pause, R.drawable.ic_play);
        } else {
            remoteViews.setImageViewResource(R.id.iv_pause, R.drawable.ic_pause);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContent(remoteViews);
        return builder.build();
    }

    public void updateNotification(boolean isPause) {
        Notification notification = createNotification(isPause);
        NotificationManagerCompat.from(mContext).notify(NOTIFICATION_ID, notification);
    }

    private PendingIntent getOpenActivityPendingIntent() {
        Intent openActivityIntent = new Intent(mContext, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, openActivityIntent, 0);
        return pendingIntent;
    }

    private PendingIntent getPreviousPendingIntent() {
        Intent previousIntent = new Intent(mContext, PlayerService.class);
        previousIntent.setAction(ACTION_PREVIOUS);
        PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, previousIntent, 0);
        return pendingIntent;
    }

    private PendingIntent getNextPendingIntent() {
        Intent nextIntent = new Intent(mContext, PlayerService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, nextIntent, 0);
        return pendingIntent;
    }

    private PendingIntent getStopPendingIntent() {
        Intent stopIntent = new Intent(mContext, PlayerService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, stopIntent, 0);
        return pendingIntent;
    }

    private PendingIntent getPausePendingIntent() {
        Intent pauseIntent = new Intent(mContext, PlayerService.class);
        pauseIntent.setAction(ACTION_PAUSE);
        PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, pauseIntent, 0);
        return pendingIntent;
    }

    /**
     * Создание канала для Notification, требуется для API >= 26
     */
    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "Player Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = mContext.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }
}
