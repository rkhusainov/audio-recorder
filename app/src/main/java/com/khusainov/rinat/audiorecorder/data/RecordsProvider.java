package com.khusainov.rinat.audiorecorder.data;

import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.khusainov.rinat.audiorecorder.ui.MainActivity.RECORDS_FOLDER_NAME;

public class RecordsProvider {

    public static List<File> getRecords() {
        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator
                + RECORDS_FOLDER_NAME);
        List<File> files = new ArrayList<>(Arrays.asList(folder.listFiles()));
        return files;
    }
}
