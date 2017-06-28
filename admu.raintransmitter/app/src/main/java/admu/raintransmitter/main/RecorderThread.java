package admu.raintransmitter.main;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class RecorderThread extends Thread {
    private static final String TAG = "RecorderThread";
    private AudioRecord audioRecord;
    private boolean isRecording;
    private byte[] buffer;
    private double mRmsSmoothed = 0;
    private FileOutputStream os = null;
    private String audioFileName = "";
    public RecorderThread(String fileName){
        audioFileName = fileName;
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

        String filePath = Environment.getExternalStorageDirectory().getPath() + "/" + audioFileName + "_pcm.pcm";
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void stopRecording(){
        try{
            if (audioRecord != null)
                audioRecord.stop();

            os.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double getPower(){
        audioRecord.read(buffer, 0, Constants.frameByteSize);
        try {
            os.write(buffer, 0, Constants.frameByteSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*
         * Noise level meter begins here
         */
        // Compute the RMS value. (Note that this does not remove DC).
        double rms = 0;
        for (byte b : buffer) {
            rms += b * b;
        }
        double out = Math.sqrt(rms / buffer.length);
        Log.d(TAG, "Power: " + out);
        return out;
    }

    public void run() {
        startRecording();
    }
}