package admu.raintransmitter.main;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class RecorderThread extends Thread {
	
	private AudioRecord audioRecord;
	private boolean isRecording;
	private int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
	private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private int sampleRate = 44100; //44100;
	private int frameByteSize = 2048; // for 1024 fft size (16bit sample size)
	private byte[] buffer;
	private double mRmsSmoothed = 0;
	
	public RecorderThread(){
		int recBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfiguration, audioEncoding); // need to be larger than size of a frame
		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfiguration, audioEncoding, recBufSize);
		buffer = new byte[frameByteSize];
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
		audioRecord.read(buffer, 0, frameByteSize);
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