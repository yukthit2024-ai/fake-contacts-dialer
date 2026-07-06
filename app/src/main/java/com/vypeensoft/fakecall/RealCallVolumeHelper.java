package com.vypeensoft.fakecall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.telephony.TelephonyManager;

public class RealCallVolumeHelper {
    private Context context;
    private MediaPlayer mediaPlayer;
    private AudioPlayerHelper audioPlayerHelper;
    private AudioManager audioManager;
    private int originalRingVolume = -1;
    private boolean isRegistered = false;

    private BroadcastReceiver phoneStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                if (TelephonyManager.EXTRA_STATE_RINGING.equals(state) ||
                    TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                    setVolume(0.0f);
                } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                    setVolume(1.0f);
                }
            }
        }
    };

    public RealCallVolumeHelper(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void bindMediaPlayer(MediaPlayer player) {
        this.mediaPlayer = player;
        // set initial volume based on current state? (Optional, let's keep it simple)
    }

    public void bindAudioPlayerHelper(AudioPlayerHelper helper) {
        this.audioPlayerHelper = helper;
    }

    public void register() {
        if (!isRegistered) {
            IntentFilter filter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
            context.registerReceiver(phoneStateReceiver, filter);
            isRegistered = true;
        }
    }

    public void unregister() {
        if (isRegistered) {
            context.unregisterReceiver(phoneStateReceiver);
            isRegistered = false;
        }
        // Always restore the system volume if we are unregistering
        setVolume(1.0f);
    }

    private void setVolume(float volume) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.setVolume(volume, volume);
            } catch (Exception e) {}
        }
        if (audioPlayerHelper != null) {
            audioPlayerHelper.setVolume(volume);
        }

        if (audioManager != null) {
            if (volume < 1.0f) {
                if (originalRingVolume == -1) {
                    originalRingVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                }
                int maxRingVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
                int targetRingVolume = (int) Math.ceil(maxRingVolume * volume);
                try {
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, targetRingVolume, 0);
                } catch (SecurityException e) {
                    // Ignore if DND prevents volume changes
                }
            } else {
                if (originalRingVolume != -1) {
                    try {
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, originalRingVolume, 0);
                    } catch (SecurityException e) {
                        // Ignore
                    }
                    originalRingVolume = -1;
                }
            }
        }
    }
}
