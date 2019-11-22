package com.khusainov.rinat.audiorecorder;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.khusainov.rinat.audiorecorder.PlayerService.MESSAGE_START;
import static com.khusainov.rinat.audiorecorder.PlayerService.PLAY_RECORD;

public class MainActivity extends AppCompatActivity implements OnItemClickListener, NotificationActionListener {

    public static final String RECORDS_FOLDER_NAME = "MyAudioRecords";
    private static final int REQUEST_CODE = 1;
    private static final int REQUEST_CODE2 = 2;

    public static final int MESSAGE_PREVIOUS = 1;
    public static final int MESSAGE_NEXT = 2;
    public static final int MESSAGE_PAUSE = 3;
    public static final int MESSAGE_RESUME = 4;
    public static final int MESSAGE_STOP = 5;

    private static final String TAG = MainActivity.class.getSimpleName();

    private static String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };

    private RecyclerView mRecordRecyclerView;
    private RecordAdapter mRecordAdapter;
    private Button mRecordButton;
    private Button mPlayButton;
    private TextView mCurrentRecordTextView;
    private List<File> mRecords = new ArrayList<>();

    private RecordService mRecordService;
    private boolean mBoundRecorder = false;
    private boolean mBoundPlayer = false;

    private Messenger mPlayerServiceMessenger = null;
    private Messenger mMainActivityMessenger = new Messenger(new PlayerIncomingHandlerMainActivity());

    private int mCurrentPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
    }

    private void initViews() {
        mRecordButton = findViewById(R.id.btn_record);
        mPlayButton = findViewById(R.id.btn_play);
        mRecordRecyclerView = findViewById(R.id.record_recycler);
        mRecordRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecordAdapter = new RecordAdapter(mRecords, this);
        mRecordRecyclerView.setAdapter(mRecordAdapter);
        mCurrentRecordTextView = findViewById(R.id.current_record);

        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecordingService();
            }
        });

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        // Проверяем разрешения при открытии приложения, нужно для чтения записей
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_CODE);
        } else {
            createFolder();
            updateRecords();
        }
    }

    /**
     * В методе onStart() привязываем сервисы
     */
    @Override
    protected void onStart() {
        super.onStart();
        Intent recorderIntent = new Intent(this, RecordService.class);
        bindService(recorderIntent, mRecorderConnection, Context.BIND_AUTO_CREATE);

        Intent playerIntent = new Intent(this, PlayerService.class);
        bindService(playerIntent, mPlayerConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * В методе onStop() отвязываем сервисы
     */
    @Override
    protected void onStop() {
        super.onStop();
        if (mBoundRecorder) {
            unbindService(mRecorderConnection);
            mBoundRecorder = false;
        }

        if (mBoundPlayer) {
            unbindService(mPlayerConnection);
            mBoundPlayer = false;
        }
    }

    /**
     * Проверяем разрешения
     *
     * @param context     - context activity
     * @param permissions - массив разрешений
     */
    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Если нет разрешений, запрашиваем
     * Если есть, запускаем сервис
     */
    private void startRecordingService() {
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_CODE2);
        } else {
            startRecord();
            mRecordService.setNotificationActionListener(this);
        }
    }

    /**
     * Здесь получим решение пользователя на запрос разрешений
     * Данный метод запустится после получения решения пользователя на запрос разрешений
     *
     * @param requestCode  - код запроса, проверяем, что requestСode тот же, что мы указывали в requestPermissions
     * @param permissions  - в массиве permissions придут названия разрешений, которые мы запрашивали
     * @param grantResults - в массиве grantResults придут ответы пользователя на запросы разрешений
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            createFolder();
        } else if (requestCode == REQUEST_CODE2 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startRecord();
            mRecordService.setNotificationActionListener(this);
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Recorder ServiceConnection
     */
    private ServiceConnection mRecorderConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            RecordService.LocalBinder binder = (RecordService.LocalBinder) service;
            mRecordService = binder.getRecorderService();
            mBoundRecorder = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBoundRecorder = false;
        }
    };

    @Override
    public void startRecord() {
        mRecordService.startRecord();
    }

    @Override
    public void pauseRecord() {
        mRecordService.pauseRecord();
    }

    @Override
    public void resumeRecord() {
        mRecordService.resumeRecord();
    }

    @Override
    public void stopRecord() {
        mRecordService.stopRecord();
        updateRecords();
    }

    /**
     * Player ServiceConnection
     */
    private ServiceConnection mPlayerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPlayerServiceMessenger = new Messenger(service);
//            sendMessageToPlayerService();
            mBoundPlayer = true;
            Log.d(TAG, "onServiceConnected: PLAYER_SERVICE_BIND");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPlayerServiceMessenger = null;
            mBoundPlayer = false;
        }
    };

    private void prevPlay() {
        int currentIndex = 0;
        if (mCurrentPosition > 0) {
            currentIndex = mCurrentPosition - 1;
            mCurrentPosition = currentIndex;
        }

        Message msg = Message.obtain(null, PLAY_RECORD, currentIndex, 0);
        try {
            mPlayerServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void nextPlay() {
        int currentIndex = RecordsProvider.getInstance().getRecords().size() - 1;
        if (mCurrentPosition < currentIndex) {
            currentIndex = mCurrentPosition + 1;
            mCurrentPosition = currentIndex;
        }

        Message msg = Message.obtain(null, PLAY_RECORD, currentIndex, 0);
        try {
            mPlayerServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void pausePlay() {
        Message msg = Message.obtain(null, PlayerService.PAUSE_RECORD);
        try {
            mPlayerServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void resumePlay() {
        Message msg = Message.obtain(null, PlayerService.RESUME_RECORD);
        try {
            mPlayerServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void stop() {
        Message msg = Message.obtain(null, PlayerService.STOP_RECORD);
        try {
            mPlayerServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Создаем директорию для наших записей
     */
    private void createFolder() {
        File folder = new File(Environment.getExternalStorageDirectory()
                + File.separator
                + RECORDS_FOLDER_NAME);
        if (!folder.exists()) {
            folder.mkdir();
        }
    }

    @Override
    public void onClick(int position) {
        Log.d(TAG, "onClick: " + mBoundPlayer);
        sendMessageToPlayerService();
    }

    /**
     * Отправляем message сервису "PlayerService"
     * */
    private void sendMessageToPlayerService() {
        if (mBoundPlayer) {
            Message message = Message.obtain(null, MESSAGE_START, 0, 0);
            message.replyTo = mMainActivityMessenger;
            try {
                mPlayerServiceMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Обновляем список записей в RecyclerView
     * */
    void updateRecords() {
        List<File> filesList = RecordsProvider.getInstance().getRecords();
        mRecordAdapter.addData(filesList);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Получаем ответ от сервиса "PlayerService"
     * */
    class PlayerIncomingHandlerMainActivity extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MESSAGE_NEXT: {
                    nextPlay();
                    break;
                }
                case MESSAGE_PREVIOUS: {
                    prevPlay();
                    break;
                }
                case MESSAGE_PAUSE: {
                    pausePlay();
                    break;
                }
                case MESSAGE_RESUME: {
                    resumePlay();
                    break;
                }
                case MESSAGE_STOP: {
                    stop();
                    break;
                }
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
