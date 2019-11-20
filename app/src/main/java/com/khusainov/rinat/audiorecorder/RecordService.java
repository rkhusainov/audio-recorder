package com.khusainov.rinat.audiorecorder;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.Nullable;

public class RecordService extends Service {

    public static final int NOTIFICATION_ID = 1;
    public static final String STOP_ACTION = "STOP_ACTION";
    public static final String PAUSE_ACTION = "PAUSE_ACTION";
    private static final String TAG = RecordService.class.getSimpleName();

    private final IBinder binder = new LocalBinder();
    private RecordHelper mRecordHelper;
    private NotificationActionListener mActionListener;
    private NotificationHelper mNotificationHelper;

    private boolean isPaused = false;

    public void setNotificationActionListener(NotificationActionListener listener) {
        mActionListener = listener;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationHelper = new NotificationHelper(this);
        mNotificationHelper.createNotificationChannel();
    }

    /**
     * Вызывается системой каждый раз, когда клиент непосредственно стартует
     * Service c помощью вызова Context.startService(Intent)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && !TextUtils.isEmpty(intent.getAction())) {
            if (STOP_ACTION.equals(intent.getAction())) {
                mActionListener.stopRecord();
            } else if (PAUSE_ACTION.equals(intent.getAction())) {
                if (!isPaused) {
                    mActionListener.pauseRecord();
                    isPaused = true;
                } else {
                    mActionListener.resumeRecord();
                    isPaused = false;
                }
            }
        }
        return START_NOT_STICKY;
    }

    public void startRecord() {
        mRecordHelper = new RecordHelper();
        mRecordHelper.startRecord();
        startForeground(NOTIFICATION_ID, mNotificationHelper.createNotification(false));
    }

    public void pauseRecord() {
        mRecordHelper.pauseRecord();
        mNotificationHelper.updateNotification(true);
    }

    public void resumeRecord() {
        mRecordHelper.resumeRecord();
        mNotificationHelper.updateNotification(false);
    }

    public void stopRecord() {
        mRecordHelper.stopRecord();
        stopForeground(true);
    }

    class LocalBinder extends Binder {
        RecordService getRecorderService() {
            return RecordService.this;
        }
    }
}
