package com.khusainov.rinat.audiorecorder.recorder;

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

    private final IBinder binder = new LocalBinder();
    private RecordHelper mRecordHelper;
    private NotificationActionListener mActionListener;
    private RecorderNotificationHelper mRecorderNotificationHelper;

    private boolean isPaused = false;

    public void setNotificationActionListener(NotificationActionListener listener) {
        mActionListener = listener;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mRecorderNotificationHelper = new RecorderNotificationHelper(this);
        mRecorderNotificationHelper.createNotificationChannel();
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
        startForeground(NOTIFICATION_ID, mRecorderNotificationHelper.createNotification(false));
    }

    public void pauseRecord() {
        mRecordHelper.pauseRecord();
        mRecorderNotificationHelper.updateNotification(true);
    }

    public void resumeRecord() {
        mRecordHelper.resumeRecord();
        mRecorderNotificationHelper.updateNotification(false);
    }

    public void stopRecord() {
        mRecordHelper.stopRecord();
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public RecordService getRecorderService() {
            return RecordService.this;
        }
    }
}
