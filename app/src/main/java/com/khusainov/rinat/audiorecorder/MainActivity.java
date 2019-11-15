package com.khusainov.rinat.audiorecorder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.khusainov.rinat.audiorecorder.model.Record;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String RECORDS_FOLDER_NAME = "MyAudioRecords";
    public static final String RECORD_FORMAT = ".3gp";
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

    private BroadcastReceiver mBroadcastReceiver;

    private RecyclerView mRecordRecyclerView;
    private RecordAdapter mRecordAdapter;
    private Button mRecordButton;
    private List<Record> mRecords = new ArrayList<>();

    private MediaRecorder mRecorder;
    private String mFileName;

    /**
     * Проверяем разрешения
     *
     * @param context     - context приложения
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
            Intent intent = new Intent(MainActivity.this, RecordService.class);
            startService(intent);
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
            Intent intent = new Intent(MainActivity.this, RecordService.class);
            startService(intent);
            startRecord();
            Log.d(TAG, "onRequestPermissionsResult: REQUEST");
        } else {
            Log.d(TAG, "onRequestPermissionsResult: REQUEST_NO");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createFolder();

        initViews();
        initBroadCastReceiver();
    }

    private void initViews() {
        mRecordButton = findViewById(R.id.btn_record);
        mRecordRecyclerView = findViewById(R.id.record_recycler);
        mRecordRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecordAdapter = new RecordAdapter(mRecords);
        mRecordRecyclerView.setAdapter(mRecordAdapter);

        mRecordButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                startRecordingService();
            }
        });
    }


    /**
     * Обрабатываем полученные сообщения
     */
    private void initBroadCastReceiver() {

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(STOP_RECORD)) {
                    stopRecord();
                    Toast.makeText(context, "STOP", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onReceive: STOP");
                } else if (intent.getAction().equals(PAUSE_RECORD)) {
                    pauseRecord();
                    Toast.makeText(context, "PAUSE", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onReceive: PAUSE");
                } else if (intent.getAction().equals(RESUME_RECORD)) {
                    resumeRecord();
                    Toast.makeText(context, "RESUME", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "onReceive: RESUME");
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupReceiver();
    }

    /**
     * Создаем IntentFilter
     */
    private void setupReceiver() {
        IntentFilter stopFilter = new IntentFilter(STOP_RECORD);
        registerReceiver(mBroadcastReceiver, stopFilter);

        IntentFilter pauseFilter = new IntentFilter(PAUSE_RECORD);
        registerReceiver(mBroadcastReceiver, pauseFilter);

        IntentFilter resumeFilter = new IntentFilter(RESUME_RECORD);
        registerReceiver(mBroadcastReceiver, resumeFilter);
    }

    /**
     * Создаем директорию для наших записей
     */
    private void createFolder() {
        File folder = new File(Environment.getExternalStorageDirectory() +
                File.separator + RECORDS_FOLDER_NAME);
        if (!folder.exists()) {
            folder.mkdir();
        }
    }

    /**
     * Создаем путь для нового файла
     */
    private void createFilePath() {
        mFileName = Environment.getExternalStorageDirectory()
                + File.separator
                + RECORDS_FOLDER_NAME
                + File.separator
                + Calendar.getInstance().getTime()
                + RECORD_FORMAT;
    }

    /**
     * Начинаем запись
     */
    private void startRecord() {

        createFilePath();

        try {
            File outFile = new File(mFileName);
            if (outFile.exists()) {
                outFile.delete();
            }

            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile(mFileName);
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Останавливаем запись
     */
    private void stopRecord() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        }
    }


    /**
     * Ставим запись на паузу
     */
    private void pauseRecord() {
        if (mRecorder != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mRecorder.pause();
            }
        }
    }

    /**
     * Возобновляем запись
     */
    private void resumeRecord() {
        if (mRecorder != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mRecorder.resume();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }
}
