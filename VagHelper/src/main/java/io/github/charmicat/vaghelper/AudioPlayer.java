package io.github.charmicat.vaghelper;

import android.content.Context;
import android.media.MediaPlayer;

public class AudioPlayer {

    private MediaPlayer mMediaPlayer;

    public void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    public boolean play(Context context, int rid) {
        stop();

        mMediaPlayer = MediaPlayer.create(context, rid); //already calls prepare() on success
        if (mMediaPlayer != null) {
            mMediaPlayer.setOnCompletionListener(mediaPlayer -> stop());

            mMediaPlayer.start();
            stop();
            return true;
        }
        return false;
    }

}
