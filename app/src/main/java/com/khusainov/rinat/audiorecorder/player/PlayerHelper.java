package com.khusainov.rinat.audiorecorder.player;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import com.khusainov.rinat.audiorecorder.data.RecordsProvider;

public class PlayerHelper {

    private MediaPlayer mMediaPlayer;

    void startPlay(Context context, int recordIndex, MediaPlayer.OnCompletionListener listener) {
        mMediaPlayer = MediaPlayer.create(context, Uri.fromFile(RecordsProvider.getRecords().get(recordIndex)));
        mMediaPlayer.start();
        mMediaPlayer.setOnCompletionListener(listener);
    }

    void pausePlay() {
        mMediaPlayer.pause();
    }

    void resumePlay() {
        mMediaPlayer.start();
    }

    void stopPlay() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
    }
}
