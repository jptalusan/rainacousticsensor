package admu.raintransmitter.main;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.Iterator;
import java.util.List;

/**
 * Created by jptalusan on 7/2/17.
 */

public class Utilities {
    private static final String TAG = "Utilities";

    static double getPower(byte[] buffer, int readSize){
        /*
         * Noise level meter begins here
         */
        // Compute the RMS value. (Note that this does not remove DC).
        double rms = 0;
        for (byte b : buffer) {
            rms += b * b;
        }
        if (readSize > 0) {
            double out = Math.sqrt(rms / readSize);
            Log.d("EXTRA", "Read Size/Power: " + readSize + "/" + out);
            return out;
        }
        return 0;
    }

    static double roundDown(double d, int places) {
        return Math.round(d * Math.pow(10, places)) / Math.pow(10, places);
    }

    static int computeNumberOfSamplesPerText(double sampleRate, double bufferSize) {
        Log.d(TAG, "computeNumberOfSamplesPerText()" + sampleRate  + "," + bufferSize);
        double temp = 0.0;
        bufferSize /= 2; //IF 16 BIT PCM
        temp = bufferSize / sampleRate * 1000;
        return (int)(1000 / temp);
    }

    public AudioRecord findAudioRecord() {
        int sampleRate = 0;
        int channelConfiguration = 0;
        int audioEncoding = 0;
        int recBufSize = 0;
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

    public static double[] convertDoubles(List<Double> doubles)
    {
        double[] ret = new double[doubles.size()];
        Iterator<Double> iterator = doubles.iterator();
        for (int i = 0; i < ret.length; i++)
        {
            ret[i] = iterator.next().intValue();
        }
        return ret;
    }
}
