package admu.raintransmitter.main;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class RecorderThread extends Thread {

    private AudioRecord audioRecord;
    private boolean isRecording;
    private byte[] buffer;
    private double mRmsSmoothed = 0;

    public RecorderThread(){
        int recBufSize = AudioRecord.getMinBufferSize(
                Constants.sampleRate,
                Constants.channelConfiguration,
                Constants.audioEncoding); // need to be larger than size of a frame
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
        for (int i = 0; i < buffer.length; i++) {
            rms += buffer[i] * buffer[i];
        }
        rms = Math.sqrt(rms / buffer.length);
        double mAlpha = 0.9;   double mGain = 0.0044;
        /*Compute a smoothed version for less flickering of the
        // display.*/
        mRmsSmoothed = mRmsSmoothed * mAlpha + (1 - mAlpha) * rms;
//		Log.w("rain316", "RMS Smoothed: " + Double.toString(mRmsSmoothed));
        double rmsdB = -1;
        if (mGain * mRmsSmoothed > 0.0f)
            rmsdB = 20.0 * Math.log10(mGain * mRmsSmoothed);
        else rmsdB = -30.0;
        Log.w("rain316", "RMS dB: " + Double.toString(rmsdB));
        return rmsdB;
    }

    public void run() {
        startRecording();
    }
}