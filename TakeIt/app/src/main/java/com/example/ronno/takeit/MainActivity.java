package com.example.ronno.takeit;

import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private Button recordingButton;
    private Button stopButton;
    private Button playButton;
    private Button matchButton;

    private MediaRecorder mediaRecorder;
    String voiceStoragePath;

    static final String AB = "abcdefghijklmnopqrstuvwxyz";
    static Random rnd = new Random();

    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // load here

        recordingButton = (Button)findViewById(R.id.recording_button);
        stopButton = (Button)findViewById(R.id.stop_button);
        playButton = (Button)findViewById(R.id.play_button);

        stopButton.setEnabled(false);
        playButton.setEnabled(false);
        matchButton.setEnabled(false);
    }
}
