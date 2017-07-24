package admu.raintransmitter.main;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class RecorderThread extends Thread {
    private static final String TAG = "RecorderThread";
    AudioRecord audioRecord;
    private int recBufSize = 0;
    RecorderThread(){
        recBufSize = AudioRecord.getMinBufferSize(
                Constants.sampleRate,
                Constants.channelConfiguration,
                Constants.audioEncoding); // need to be larger than size of a frame

        Log.d(TAG, "min. buffer size: " + recBufSize);
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                Constants.sampleRate,
                Constants.channelConfiguration,
                Constants.audioEncoding,
                recBufSize);
    }

    void startRecording(){
        try{
            audioRecord.startRecording();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void stopRecording(){
        try{
            audioRecord.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        startRecording();
    }

    public int getRecBufSize() {
        return recBufSize;
    }
}