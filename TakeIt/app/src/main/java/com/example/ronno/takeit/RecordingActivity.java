package com.example.ronno.takeit;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.view.View.GONE;

public class RecordingActivity extends AppCompatActivity {

    private static final int AUDIO_BPP = 16;
    private static final String AUDIO_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_FOLDER = "RecordedAudio";
    private static final String AUDIO_TEMP_FILE = "audio_temp.wav";
    private static final int AUDIO_SAMPLERATE = 44100;
    private static final int[] AUDIO_SAMPLERATES = new int[] {44100, 8000, 11025, 16000, 22050};
    private static final int AUDIO_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isStartRecording = false;
    private boolean isRecorded = false;
    private boolean isPlayback = false;

    private MediaRecorder mediaRecorder = null;
    private MediaPlayer playbackPlayer = null;

    private String lastFile;

    private ImageView micImage;
    private ImageView recordingImage;
    private ImageView playingImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        setAllImageReferences();
        enableImageSwitcher(0);
        setAllButtonHandlers();
        enableButtonsSwitcher(false);
        bufferSize = AudioRecord.getMinBufferSize(AUDIO_SAMPLERATE,
                AUDIO_CHANNELS,
                AUDIO_ENCODING);
    }

    private void setAllImageReferences() {
        micImage = (ImageView)findViewById(R.id.micImage);
        recordingImage = (ImageView)findViewById(R.id.recordingImage);
        playingImage = (ImageView)findViewById(R.id.playingImage);
    }

    private void enableImageSwitcher(int state) { // 0 = ready, 1 = recording, 2 = playing
        switch (state) {
            case 0:{
                micImage.setVisibility(View.VISIBLE);
                recordingImage.setVisibility(GONE);
                playingImage.setVisibility(GONE);
                break;
            }
            case 1:{
                micImage.setVisibility(GONE);
                recordingImage.setVisibility(View.VISIBLE);
                playingImage.setVisibility(GONE);
                break;
            }
            case 2:{
                micImage.setVisibility(GONE);
                recordingImage.setVisibility(GONE);
                playingImage.setVisibility(View.VISIBLE);
                break;
            }
        }
    }

    private void setAllButtonHandlers() {
        ((Button)findViewById(R.id.recording_button)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.stop_button)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.play_button)).setOnClickListener(btnClick);

    }

    private void enableButton(int id, boolean isEnable) {
        ((Button)findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtonsSwitcher(boolean isRecord) {
        enableButton(R.id.recording_button, !isRecord);
        enableButton(R.id.stop_button, isRecord || isPlayback);
        enableButton(R.id.play_button, !isRecord && isRecorded);
    }

    private String getFilepath() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_FOLDER);
        if (!file.exists()) {
            file.mkdir();
        }

        lastFile = file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_FILE_EXT_WAV;
        return (lastFile);
    }

    private String getTemporaryFilepath() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_FOLDER);
        if (!file.exists()) {
            file.mkdir();
        }

        File tempFile = new File(filepath, AUDIO_TEMP_FILE);
        if (tempFile.exists())
            tempFile.delete();
        return (file.getAbsolutePath() + "/" + AUDIO_TEMP_FILE);
    }

    private void startRecording() {
        for (int rate : AUDIO_SAMPLERATES) {
            bufferSize = AudioRecord.getMinBufferSize(rate,
                    AUDIO_CHANNELS,
                    AUDIO_ENCODING);
            if (bufferSize > 0) {
                audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        rate, AUDIO_CHANNELS, AUDIO_ENCODING, bufferSize);

                int i = audioRecorder.getState();
                if (i == 1) { // 1 = Active, so if successful...
                    audioRecorder.startRecording();
                    toast("Recording");
                    break;
                }
            }
        }
        isStartRecording = true;
        recordingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                writeAudioDataToTemporaryFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void writeAudioDataToTemporaryFile() {
        byte data[] = new byte[bufferSize];
        String filepath = getTemporaryFilepath();
        FileOutputStream os = null;

         try {
             os = new FileOutputStream(filepath);
         } catch (FileNotFoundException e) {
             e.printStackTrace();
         }

         int read = 0;
        if (null != os) {
            while (isStartRecording) {
                read = audioRecorder.read(data, 0, bufferSize);

                if(AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {

        if(null != audioRecorder) {
            isStartRecording = false;

            int i = audioRecorder.getState();
            if(i==1)
                audioRecorder.stop();
            audioRecorder.release();
            audioRecorder = null;
            recordingThread = null;
        }

        copyWavFile(getTemporaryFilepath(), getFilepath());
        deleteTemporaryFile();

        isRecorded = true;
    }

    private void deleteTemporaryFile() {
        File file = new File(getTemporaryFilepath());
        file.delete();
    }

    private void copyWavFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = AUDIO_SAMPLERATE;
        int channels = 2;
        long byteRate = AUDIO_BPP * AUDIO_SAMPLERATE * channels/8;
        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            AppLog.logString("File size: " + totalDataLen);
            WriteWavFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);
            while(in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Note that this is the standard .wav file header format: http://soundfile.sapp.org/doc/WaveFormat/
    // Some examples were used to complete this
    private void WriteWavFileHeader(
            FileOutputStream out, long audioLength,
            long dataLength, long sampleRate, int channels,
            long byteRate) throws IOException{

        byte[] fileHeader = new byte[44];

        fileHeader[0] = 'R'; // RIFF/WAVE header
        fileHeader[1] = 'I';
        fileHeader[2] = 'F';
        fileHeader[3] = 'F';
        fileHeader[4] = (byte) (dataLength & 0xff);
        fileHeader[5] = (byte) ((dataLength >> 8) & 0xff);
        fileHeader[6] = (byte) ((dataLength >> 16) & 0xff);
        fileHeader[7] = (byte) ((dataLength >> 24) & 0xff);
        fileHeader[8] = 'W';
        fileHeader[9] = 'A';
        fileHeader[10] = 'V';
        fileHeader[11] = 'E';
        fileHeader[12] = 'f'; // 'fmt ' chunk
        fileHeader[13] = 'm';
        fileHeader[14] = 't';
        fileHeader[15] = ' ';
        fileHeader[16] = 16; // 4 bytes: size of 'fmt ' chunk
        fileHeader[17] = 0;
        fileHeader[18] = 0;
        fileHeader[19] = 0;
        fileHeader[20] = 1; // format = 1
        fileHeader[21] = 0;
        fileHeader[22] = (byte) channels;
        fileHeader[23] = 0;
        fileHeader[24] = (byte) (sampleRate & 0xff);
        fileHeader[25] = (byte) ((sampleRate >> 8) & 0xff);
        fileHeader[26] = (byte) ((sampleRate >> 16) & 0xff);
        fileHeader[27] = (byte) ((sampleRate >> 24) & 0xff);
        fileHeader[28] = (byte) (byteRate & 0xff);
        fileHeader[29] = (byte) ((byteRate >> 8) & 0xff);
        fileHeader[30] = (byte) ((byteRate >> 16) & 0xff);
        fileHeader[31] = (byte) ((byteRate >> 24) & 0xff);
        fileHeader[32] = (byte) (2 * 16 / 8);
        fileHeader[33] = 0;
        fileHeader[34] = AUDIO_BPP;
        fileHeader[35] = 0;
        fileHeader[36] = 'd';
        fileHeader[37] = 'a';
        fileHeader[38] = 't';
        fileHeader[39] = 'a';
        fileHeader[40] = (byte) (audioLength & 0xff);
        fileHeader[41] = (byte) ((audioLength >> 8) & 0xff);
        fileHeader[42] = (byte) ((audioLength >> 16) & 0xff);
        fileHeader[43] = (byte) ((audioLength >> 24) & 0xff);

        out.write(fileHeader, 0, 44);
    }

    private void startPlayback() {

        playbackPlayer = new MediaPlayer();
        playbackPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            playbackPlayer.setDataSource(lastFile);
            toast("Playing");
            playbackPlayer.prepare();
            playbackPlayer.start();
        } catch (IOException e) {
            playbackPlayer.reset();
            toast("Error: IOException");
            e.printStackTrace();

        } catch (IllegalArgumentException e) {
            playbackPlayer.reset();
            toast("Error: IllegalArgumentException");
            e.printStackTrace();
        }

    }

    private void stopPlayback() {
        if (playbackPlayer != null) {
            playbackPlayer.stop();
            playbackPlayer.reset();
            playbackPlayer = null;
        } else {
            toast("playbackPlayer=null");
        }
        if (isPlayback) isPlayback = !isPlayback;
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.recording_button:{
                    enableImageSwitcher(1);
                    enableButtonsSwitcher(true);
                    startRecording();
                    break;
                }

                case R.id.stop_button:{
                    enableImageSwitcher(0);
                    if (isPlayback)
                        stopPlayback();
                    else
                        stopRecording();
                    enableButtonsSwitcher(false);
                    break;
                }

                case R.id.play_button:{
                    enableImageSwitcher(2);
                    enableButtonsSwitcher(true);
                    startPlayback();
                }
            }
        }
    };

    // Used for a quick way to make a toast
    private void toast(String text) {
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }
}
