package edu.sharif.behin.androidstreamer.multimedia;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.AttributeSet;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * Created by Behin on 1/31/2017.
 */

public class AudioPreview extends WaveformView implements Closeable {
    private AudioRecord audioRecord;
    private short[] buffer;
    private int bufferSize;
    AudioEncoder encoder;
    Thread runningThread;

    public AudioPreview(Context context) {
        super(context);
        setFields();
    }

    public AudioPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFields();
    }

    public AudioPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFields();
    }

    private void setFields() {
        createChart();

        bufferSize = AudioRecord.getMinBufferSize(AudioEncoder.AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        buffer = new short[bufferSize/2];
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,AudioEncoder.AUDIO_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,bufferSize);
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e("Behin", "Audio Record can't initialize!");
            return;
        }
        audioRecord.startRecording();
        runningThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    try{
                        int readSize = audioRecord.read(buffer, 0, buffer.length);
                        setSamples(buffer);
                        if(encoder!=null){
                            ByteBuffer byteBuf = ByteBuffer.allocate(2*readSize);
                            byteBuf.order(ByteOrder.nativeOrder());
                            for(int i=0;i<readSize;i++){
                                byteBuf.putShort(buffer[i]);
                            }
                            encoder.offerEncoder(byteBuf.array());
                        }
                    }catch (Exception e){
                        Log.e(AudioPreview.class.getName(), "error occurred while running thread", e);
                        break;
                    }
                }
            }
        });
        runningThread.setDaemon(true);
        runningThread.start();
    }

    private void createChart() {
        setSampleRate(AudioEncoder.AUDIO_SAMPLE_RATE);
        setChannels(1);

    }

    public void setEncoder(AudioEncoder encoder) {
        this.encoder = encoder;
    }

    public void restart(){
        if(audioRecord==null)
            setFields();
    }

    public void stop() {
        if(runningThread != null){
            runningThread.interrupt();
        }
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    @Override
    public void close() throws IOException {
        stop();
    }
}
