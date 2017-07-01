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

    public RecorderThread(){
        int recBufSize = AudioRecord.getMinBufferSize(
                Constants.sampleRate,
                Constants.channelConfiguration,
                Constants.audioEncoding); // need to be larger than size of a frame

        Log.d(TAG, "min. buffer size: " + recBufSize);
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                Constants.sampleRate,
                Constants.channelConfiguration,
                Constants.audioEncoding, recBufSize);
        buffer = new byte[Constants.frameByteSize];
    }

    public boolean isRecording(){
        return this.isAlive() && isRecording;
    }

    public void startRecording(){
        try{
            audioRecord.startRecording();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopRecording(){
        try{
            audioRecord.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double getPower(){
        audioRecord.read(buffer, 0, Constants.frameByteSize);
        /*
         * Noise level meter begins here
         */
        // Compute the RMS value. (Note that this does not remove DC).
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
}