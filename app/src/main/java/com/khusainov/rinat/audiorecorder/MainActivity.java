package com.khusainov.rinat.audiorecorder;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnItemClickListener, NotificationActionListener {

    public static final String RECORDS_FOLDER_NAME = "MyAudioRecords";
    private static final int REQUEST_CODE = 1;
    public static final String STOP_RECORD = "STOP_RECORD";
    public static final String PAUSE_RECORD = "PAUSE_RECORD";
    public static final String RESUME_RECORD = "RESUME_RECORD";
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

    private File mDir;
    private File mFile;

    private MediaPlayer mMediaPlayer;

    private RecordService mRecordService;
    private boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getRecordsFromDir();

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
                playRecord();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, RecordService.class);
        startService(intent);
        bindService(intent, mRecorderConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mRecorderConnection);
            mBound = false;
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
     * Если есть, запускаем сервис и запись
     */
    private void startRecordingService() {
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_CODE);
        } else {
            mRecordService.setNotificationActionListener(this);
            createFolder();
            startRecord();
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
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private ServiceConnection mRecorderConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            RecordService.LocalBinder binder = (RecordService.LocalBinder) service;
            mRecordService = binder.getRecorderService();
//            mRecordService = ((RecordService.LocalBinder) service).getRecorderService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
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
    }

    private void getRecordsFromDir() {
        mDir = new File(Environment.getExternalStorageDirectory()
                + File.separator
                + RECORDS_FOLDER_NAME
                + File.separator);
        if (mDir.exists()) {
            mRecords = getRecordNames(mDir);
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
     * Проигрываем запись
     */
    private void playRecord() {
        if (!mRecords.isEmpty()) {
            if (mFile == null) {
                mFile = mRecords.get(0);
                setCurrentRecordName();
            }
            createPlayer();
        } else {
            Toast.makeText(this, getResources().getString(R.string.no_records), Toast.LENGTH_SHORT).show();
        }
    }

    private void createPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
        }
        mMediaPlayer = MediaPlayer.create(this, Uri.parse(Environment.getExternalStorageDirectory()
                + File.separator
                + RECORDS_FOLDER_NAME
                + File.separator
                + mFile.getName()));
        mMediaPlayer.start();

        mCurrentRecordTextView.setText(format(R.string.playing_record, mFile.getName()));
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mCurrentRecordTextView.setText(getResources().getString(R.string.play_finish));
            }
        });
    }

    private String format(int res, String text) {
        return String.format((getString(res)), text);
    }

    private List<File> getRecordNames(File dir) {
        List<File> files = new ArrayList<>();
        for (File file : dir.listFiles()) {
            files.add(file);
        }
        return files;
    }

    private void updateRecords() {
        mRecords = getRecordNames(mDir);
        mRecordAdapter.addData(mRecords);
    }

    @Override
    public void onClick(File file) {
        mFile = file;
        setCurrentRecordName();
    }

    private void setCurrentRecordName() {
        if (mFile != null) {
            mCurrentRecordTextView.setText(mFile.getName());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
