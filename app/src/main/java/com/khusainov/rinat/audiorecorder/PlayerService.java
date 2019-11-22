package com.khusainov.rinat.audiorecorder;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.khusainov.rinat.audiorecorder.MainActivity.MESSAGE_NEXT;
import static com.khusainov.rinat.audiorecorder.MainActivity.MESSAGE_PAUSE;
import static com.khusainov.rinat.audiorecorder.MainActivity.MESSAGE_PREVIOUS;
import static com.khusainov.rinat.audiorecorder.MainActivity.MESSAGE_RESUME;
import static com.khusainov.rinat.audiorecorder.MainActivity.MESSAGE_STOP;

public class PlayerService extends Service {

    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_RESUME = "ACTION_RESUME";
    public static final String ACTION_STOP = "ACTION_STOP";

    public static final int RESUME_RECORD = 1;
    public static final int PAUSE_RECORD = 2;
    public static final int PLAY_RECORD = 3;
    public static final int STOP_RECORD = 4;
    public static final int MESSAGE_START = 5;

    private final Messenger mPlayerIncomingMessenger = new Messenger(new PlayerIncomingHandler());

    private PlayerHelper mPlayerHelper;
    private PlayerNotificationHelper mPlayerNotificationHelper;

    private Messenger activityMessenger;

    @Override
    public void onCreate() {
        super.onCreate();
        mPlayerHelper = new PlayerHelper();
        mPlayerNotificationHelper = new PlayerNotificationHelper(this);
        mPlayerNotificationHelper.createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_PREVIOUS.equals(intent.getAction())) {
            sendMessageToActivity(MESSAGE_PREVIOUS);
        }
        if (intent != null && ACTION_NEXT.equals(intent.getAction())) {
            sendMessageToActivity(MESSAGE_NEXT);
        }
        if (intent != null && ACTION_PAUSE.equals(intent.getAction())) {
            sendMessageToActivity(MESSAGE_PAUSE);
        }
        if (intent != null && ACTION_RESUME.equals(intent.getAction())) {
            sendMessageToActivity(MESSAGE_RESUME);
        }
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            sendMessageToActivity(MESSAGE_STOP);
        }
        return START_NOT_STICKY;
    }

    /**
     * Отправляем message в MainActivity
     */
    private void sendMessageToActivity(int what) {
        Message message = Message.obtain(null, what, 0, 0);
        try {
            activityMessenger.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mPlayerIncomingMessenger.getBinder();
    }

    /**
     * Получаем ответ от MainActivity
     */
    class PlayerIncomingHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MESSAGE_START: {
                    activityMessenger = msg.replyTo;
                    break;
                }
                case PLAY_RECORD:
                    startForeground(NOTIFICATION_ID, mPlayerNotificationHelper.createNotification(false));
                    mPlayerHelper.stopPlay();
                    mPlayerHelper.startPlay(PlayerService.this, msg.arg1, new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            sendMessageToActivity(MESSAGE_STOP);
                        }
                    });
                    break;
                case PAUSE_RECORD:
                    mPlayerHelper.pausePlay();
                    mPlayerNotificationHelper.updateNotification(true);
                    break;
                case RESUME_RECORD:
                    mPlayerHelper.resumePlay();
                    mPlayerNotificationHelper.updateNotification(false);
                    break;
                case STOP_RECORD:
                    mPlayerHelper.stopPlay();
                    stopForeground(true);
                    break;
            }
            super.handleMessage(msg);
        }
    }
}
