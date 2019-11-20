package com.khusainov.rinat.audiorecorder;

import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.io.IOException;

public class RecordHelper {

    public static final String RECORD_NAME_PREFIX = "Record_";
    public static final String RECORD_FORMAT = ".3gp";

    private MediaRecorder mRecorder;
    private String mPath;

    /**
     * Создаем путь для нового файла
     */
    public void createFilePath() {
        mPath = Environment.getExternalStorageDirectory()
                + File.separator
                + MainActivity.RECORDS_FOLDER_NAME
                + File.separator
                + RECORD_NAME_PREFIX
                + System.currentTimeMillis()
                + RECORD_FORMAT;
    }

    /**
     * Начинаем запись
     */
    public void startRecord() {

        createFilePath();

        try {
            File outFile = new File(mPath);
            if (outFile.exists()) {
                outFile.delete();
            }

            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile(mPath);
            mRecorder.prepare();
            mRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Останавливаем запись
     */
    public void stopRecord() {
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
    public void pauseRecord() {
        if (mRecorder != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mRecorder.pause();
            }
        }
    }

    /**
     * Возобновляем запись
     */
    public void resumeRecord() {
        if (mRecorder != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mRecorder.resume();
            }
        }
    }
}
