package com.example.ronno.takeit;

import android.content.pm.PackageManager;
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
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class RecordingActivity extends AppCompatActivity {

    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.wav";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int[] RECORDER_SAMPLERATES = new int[] {44100, 8000, 11025, 16000, 22050};
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private boolean isRecorded = false;
    private boolean isPlayback = false;

    private MediaRecorder mediaRecorder = null;
    private MediaPlayer playbackPlayer = null;

    private String lastFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        setButtonHandlers();
        enableButtons(false, isRecorded);
        bufferSize = AudioRecord.getMinBufferSize(8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
    }

    private void setButtonHandlers() {
        ((Button)findViewById(R.id.recording_button)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.stop_button)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.play_button)).setOnClickListener(btnClick);

    }

    private void enableButton(int id, boolean isEnable) {
        ((Button)findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording, boolean isRecorded) {
        enableButton(R.id.recording_button, !isRecording);
        enableButton(R.id.stop_button, isRecording || isPlayback);
        enableButton(R.id.play_button, !isRecording && isRecorded);
    }

    private String getFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);
        if (!file.exists()) {
            file.mkdir();
        }

        lastFile = file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV;
        return (lastFile);
    }

    private String getTempFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);
        if (!file.exists()) {
            file.mkdir();
        }

        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);
        if (tempFile.exists())
            tempFile.delete();
        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private void startRecording() {
        for (int rate : RECORDER_SAMPLERATES) {
            bufferSize = AudioRecord.getMinBufferSize(rate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0) {
                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        rate, AudioFormat.CHANNEL_IN_MONO, RECORDER_AUDIO_ENCODING, bufferSize);

                int i = recorder.getState();
                if (i == 1) {
                    recorder.startRecording();
                    toast("Recording");
                    break;
                }
            }
        }
        isRecording = true;
        recordingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
/*
        isRecording = true;
        // File path of recorded audio
        String mFileName;
        final String LOG_TAG = "AudioRecordTest";
        // Verify that the device has a mic first
        PackageManager pmanager = this.getPackageManager();
        if (pmanager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
            // Set the file location for the audio

            mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFileName += "/audiorecordtest.3gp";
            // Create the recorder

            mediaRecorder = new MediaRecorder();
            // Set the audio format and encoder
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            // Setup the output location
            mediaRecorder.setOutputFile(mFileName);
            // Start the recording
            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                toast("mediaRecorder started");
            } catch (IOException e) {
                toast("catchedd?");
                e.printStackTrace();
            }
        } else { // no mic on device
            Toast.makeText(this, "This device doesn't have a mic!", Toast.LENGTH_LONG).show();
        }*/
    }

    private void writeAudioDataToFile() {
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

         try {
             os = new FileOutputStream(filename);
         } catch (FileNotFoundException e) {
             e.printStackTrace();
         }

         int read = 0;
        if (null != os) {
            while (isRecording) {
                read = recorder.read(data, 0, bufferSize);

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

        if(null != recorder) {
            isRecording = false;

            int i = recorder.getState();
            if(i==1)
                recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }

        copyWaveFile(getTempFilename(), getFilename());
        deleteTempFile();

        isRecorded = true;
/*
        isRecorded = true;
        isRecording = false;

        mediaRecorder.stop();
        mediaRecorder.reset();
        mediaRecorder.release();
*/
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;
        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            AppLog.logString("File size: " + totalDataLen);
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
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

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException{

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (totalDataLen & 0xff);
        header[25] = (byte) ((totalDataLen >> 8) & 0xff);
        header[26] = (byte) ((totalDataLen >> 16) & 0xff);
        header[27] = (byte) ((totalDataLen >> 24) & 0xff);
        header[28] = (byte) (totalDataLen & 0xff);
        header[29] = (byte) ((totalDataLen >> 8) & 0xff);
        header[30] = (byte) ((totalDataLen >> 16) & 0xff);
        header[31] = (byte) ((totalDataLen >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);
        header[33] = 0;
        header[34] = RECORDER_BPP;
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalDataLen & 0xff);
        header[41] = (byte) ((totalDataLen >> 8) & 0xff);
        header[42] = (byte) ((totalDataLen >> 16) & 0xff);
        header[43] = (byte) ((totalDataLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    private void startPlayback() {

        playbackPlayer = new MediaPlayer();
        playbackPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            //File filePath = new File(lastFile);
            //FileInputStream is = new FileInputStream(filePath);
            playbackPlayer.setDataSource(lastFile);//getFilename());
            //toast(lastFile);
            //playbackPlayer.prepare();
            //playbackPlayer.start();
            //is.close();
        } catch (IOException e) {
            playbackPlayer.reset();
            toast("Error");
            e.printStackTrace();

        }

    }

    private void stopPlayback() {
        if (playbackPlayer != null) {
            playbackPlayer.stop();
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
                    AppLog.logString("Start Recording");
                    enableButtons(true, isRecorded);
                    startRecording();

                    break;
                }

                case R.id.stop_button:{
                    AppLog.logString("Start Recording");

                    if (isPlayback)
                        stopPlayback();
                    else
                        stopRecording();

                    enableButtons(false, isRecorded);

                    break;
                }

                case R.id.play_button:{
                    AppLog.logString("Start Recording");
                    enableButtons(true, isRecorded);
                    startPlayback();
                }
            }
        }
    };

    private void toast(String text) {
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }
}
