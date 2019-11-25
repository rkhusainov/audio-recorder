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
import android.widget.ImageView;

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

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnItemClickListener, NotificationActionListener {

    private static String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };

    public static final String RECORDS_FOLDER_NAME = "MyAudioRecords";
    private static final int REQUEST_CODE = 1;
    private static final int REQUEST_CODE2 = 2;

    public static final int MESSAGE_PREVIOUS = 1;
    public static final int MESSAGE_NEXT = 2;
    public static final int MESSAGE_PAUSE = 3;
    public static final int MESSAGE_RESUME = 4;
    public static final int MESSAGE_STOP = 5;

    private ImageView mRecordButton;
    private ImageView mPlayButton;
    private ImageView mPlayPrevButton;
    private ImageView mPlayNextButton;
    private ImageView mDoneButton;
    private ImageView mCancelButton;

    private RecyclerView mRecordRecyclerView;
    private RecordAdapter mRecordAdapter;
    private List<File> mRecords = new ArrayList<>();

    private RecordService mRecordService;
    private boolean mBoundRecorder = false;
    private boolean mBoundPlayer = false;

    private Messenger mPlayerServiceMessenger = null;
    private Messenger mMainActivityMessenger = new Messenger(new PlayerIncomingHandlerMainActivity());

    private int mCurrentPosition;
    private boolean isPlaying = false;
    private boolean isPausing = false;
    private boolean isRecording = false;
    private boolean isRecordWasPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        // Проверяем разрешения при открытии приложения, нужно для чтения записей
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_CODE);
        } else {
            createFolder();
            updateRecords();
        }
    }

    private void initViews() {
        mRecordButton = findViewById(R.id.iv_record);
        mPlayButton = findViewById(R.id.iv_play);
        mPlayPrevButton = findViewById(R.id.iv_prev);
        mPlayNextButton = findViewById(R.id.iv_next);
        mDoneButton = findViewById(R.id.iv_done);
        mCancelButton = findViewById(R.id.iv_cancel);
        mRecordButton.setOnClickListener(this);
        mPlayButton.setOnClickListener(this);
        mPlayPrevButton.setOnClickListener(this);
        mPlayNextButton.setOnClickListener(this);
        mDoneButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);

        mRecordRecyclerView = findViewById(R.id.record_recycler);
        mRecordRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        mRecordAdapter = new RecordAdapter(mRecords, this);
        mRecordRecyclerView.setAdapter(mRecordAdapter);
        SimpleDividerItemDecoration dividerItemDecoration = new SimpleDividerItemDecoration(this, getResources().getColor(R.color.colorGray), 1);
        mRecordRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_record:
                if (!isRecording) {
                    if (!isRecordWasPaused) {
                        startRecordingService();
                    } else {
                        resumeRecord();
                    }
                } else {
                    pauseRecord();
                }
                mPlayButton.setEnabled(false);
                mPlayPrevButton.setEnabled(false);
                mPlayNextButton.setEnabled(false);
                break;
            case R.id.iv_play:
                if (isPlaying) {
                    pausePlay();
                    isPausing = true;
                } else if (isPausing) {
                    resumePlay();
                } else {
                    sendMessageToPlayerService(mCurrentPosition);
                }
                break;
            case R.id.iv_prev:
                if (!mRecords.isEmpty()) {
                    prevPlay();
                }
                break;
            case R.id.iv_next:
                if (!mRecords.isEmpty()) {
                    nextPlay();
                }
                break;
            case R.id.iv_done:
                stopRecord();
                break;
            case R.id.iv_cancel:
                // TODO: Cancel recording
                break;
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
            setRecordState(true);
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
            setRecordState(true);
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
        setRecordState(true);
        isRecordWasPaused = false;
        mDoneButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void pauseRecord() {
        mRecordService.pauseRecord();
        setRecordState(false);
        isRecordWasPaused = true;
    }

    @Override
    public void resumeRecord() {
        mRecordService.resumeRecord();
        setRecordState(true);
    }

    @Override
    public void stopRecord() {
        mRecordService.stopRecord();
        updateRecords();
        setRecordState(false);
        isRecordWasPaused = false;
        mDoneButton.setVisibility(View.INVISIBLE);
        mPlayButton.setEnabled(true);
        mPlayPrevButton.setEnabled(false);
        mPlayNextButton.setEnabled(false);
    }

    /**
     * Player ServiceConnection
     */
    private ServiceConnection mPlayerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPlayerServiceMessenger = new Messenger(service);
            mBoundPlayer = true;

            Message message = Message.obtain(null, MESSAGE_START, 0, 0);
            message.replyTo = mMainActivityMessenger;
            try {
                mPlayerServiceMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
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

        setPlayerState(true);
        mRecordButton.setEnabled(false);

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

        setPlayerState(true);
        mRecordButton.setEnabled(false);

        Message msg = Message.obtain(null, PLAY_RECORD, currentIndex, 0);
        try {
            mPlayerServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void pausePlay() {

        setPlayerState(false);

        Message msg = Message.obtain(null, PlayerService.PAUSE_RECORD);
        try {
            mPlayerServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void resumePlay() {

        setPlayerState(true);

        Message msg = Message.obtain(null, PlayerService.RESUME_RECORD);
        try {
            mPlayerServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void stopPlay() {

        setPlayerState(false);
        mRecordButton.setEnabled(true);

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

    /**
     * Клик по элементу RecyclerView
     */
    @Override
    public void onClick(int position) {
        sendMessageToPlayerService(position);
    }

    /**
     * Отправляем message сервису "PlayerService"
     */
    private void sendMessageToPlayerService(int position) {
        if (mBoundPlayer && !mRecords.isEmpty()) {
            Message message = Message.obtain(null, PLAY_RECORD, position, 0);
            message.replyTo = mMainActivityMessenger;
            setPlayerState(true);
            mRecordButton.setEnabled(false);
            try {
                mPlayerServiceMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Обновляем список записей в RecyclerView
     */
    void updateRecords() {
        List<File> filesList = RecordsProvider.getInstance().getRecords();
        mRecordAdapter.addData(filesList);
    }

    private void setPlayerState(boolean isPlaying) {
        this.isPlaying = isPlaying;
        if (isPlaying) {
            mPlayButton.setImageResource(R.drawable.ic_pause);
        } else {
            mPlayButton.setImageResource(R.drawable.ic_play);
        }
    }

    private void setRecordState(boolean isRecording) {
        this.isRecording = isRecording;
        if (isRecording) {
            mRecordButton.setImageResource(R.drawable.ic_pause);
        } else {
            mRecordButton.setImageResource(R.drawable.ic_mic);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Получаем ответ от сервиса "PlayerService"
     */
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
                    stopPlay();
                    break;
                }
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
