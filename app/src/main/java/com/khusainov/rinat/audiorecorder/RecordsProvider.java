package com.khusainov.rinat.audiorecorder;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.khusainov.rinat.audiorecorder.MainActivity.RECORDS_FOLDER_NAME;

public class RecordsProvider {

    private static RecordsProvider instance;

    public static RecordsProvider getInstance() {
        if (instance == null) {
            instance = new RecordsProvider();
        }
        return instance;
    }

    List<File> getRecords() {
        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator
                + RECORDS_FOLDER_NAME);
        List<File> files = new ArrayList<>(Arrays.asList(folder.listFiles()));
        return files;
    }
}
