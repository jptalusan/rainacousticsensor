package admu.raintransmitter.main;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class RecorderThread extends Thread {
    private static final String TAG = "RecorderThread";
    public AudioRecord audioRecord;
    private boolean isRecording;
    private byte[] buffer;
    private double mRmsSmoothed = 0;
    private FileOutputStream os = null;
    private String audioFileName = "";
    public int recBufSize = 0;
    public RecorderThread(){
        try {
            Thread.sleep(200);
            if (audioRecord != null)
                audioRecord.release();
            audioRecord = findAudioRecord();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        buffer = new byte[Constants.frameByteSize];
    }

    public boolean isRecording(){
        return this.isAlive() && isRecording;
    }

    public void startRecording(){
        if (audioRecord != null) {
            audioRecord.startRecording();
        } else {
            audioRecord = findAudioRecord();
            try {
                Thread.sleep(200);
                audioRecord.startRecording();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopRecording(){
        Log.d(TAG, "Stopping and releasing recorder thread.");
        if (audioRecord != null && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.stop();
            audioRecord.release();
        }
    }

    //Problem with emulator must set framerate to 8000, https://stackoverflow.com/questions/13583827/audiorecord-writing-pcm-file
    //TODO: Rename, this is not dB yet.
    public double getPower() {
        audioRecord.read(buffer, 0, Constants.frameByteSize);
        double rms = 0;
        for (byte b : buffer) {
            rms += b * b;
        }
        double out = Math.sqrt(rms / buffer.length);
        Log.d("EXTRA", "Power: " + out);
        return out;
    }

    public void run() {
        startRecording();
    }

    public AudioRecord findAudioRecord() {
        int sampleRate = 0;
        int channelConfiguration = 0;
        int audioEncoding = 0;
        recBufSize = 0;
//        int mSampleRates[] = new int[] { 8000, 11025, 16000, 22050,
//                32000, 37800, 44056, 44100, 47250, 4800, 50000, 50400, 88200,
//                96000, 176400, 192000, 352800, 2822400, 5644800 };
        int mSampleRates[] = new int[] { Constants.sampleRate };
        for (int rate : mSampleRates) {
            for (short audioFormat : new short[]{AudioFormat.ENCODING_PCM_16BIT}) {
                for (short channelConfig : new short[]{AudioFormat.CHANNEL_IN_MONO}) {
                    try {
                        //Log.d("audioSetup", "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
                        Log.d(TAG, "buffersize: " + bufferSize);
                        if (bufferSize > 0 && bufferSize <= 256){
                            bufferSize = 256;
                        }else if (bufferSize > 256 && bufferSize <= 512){
                            bufferSize = 512;
                        }else if (bufferSize > 512 && bufferSize <= 1024){
                            bufferSize = 1024;
                        }else if (bufferSize > 1024 && bufferSize <= 2048){
                            bufferSize = 2048;
                        }else if (bufferSize > 2048 && bufferSize <= 4096){
                            bufferSize = 4096;
                        }else if (bufferSize > 4096 && bufferSize <= 8192){
                            bufferSize = 8192;
                        }else if (bufferSize > 8192 && bufferSize <= 16384){
                            bufferSize = 16384;
                        }else{
                            bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
                        }

                        bufferSize = Constants.frameByteSize; //TODO Remove
                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE && bufferSize != AudioRecord.ERROR) {
                            // check if we can instantiate and have a success
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, channelConfig, audioFormat, bufferSize);

                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                Log.d(TAG, "rate: " + rate + " channelConfig: " + channelConfig + " bufferSize: " + bufferSize + " audioFormat: " + audioFormat);
                                sampleRate = rate;
                                channelConfiguration = channelConfig;
                                audioEncoding = audioFormat;
                                recBufSize = bufferSize;
                                return recorder;
                            }
                        }
                    } catch (Exception e) {
                        Log.d(TAG, rate + "Exception, keep trying.", e);
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }
}