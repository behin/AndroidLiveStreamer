package edu.sharif.behin.androidstreamer.multimedia;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class AudioEncoder {

    private MediaCodec mediaCodec;
    public static String MIME_TYPE = "audio/mp4a-latm";
    public static final int AUDIO_BITRATE = 64*1024;
    public static final int AUDIO_SAMPLE_RATE = 44100;
    public static final int AUDIO_CHANNELS = 1;
    private boolean isRunning = false;

    private FrameHandler handler;
    private AudioPreview audioPreview;


    public AudioEncoder(AudioPreview audioPreview,FrameHandler frameHandler) {
        this.audioPreview = audioPreview;
        this.handler = frameHandler;
        initCodec();
    }

    private void initCodec() {
        try {
            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            MediaFormat mediaFormat = MediaFormat.createAudioFormat(MIME_TYPE,AUDIO_SAMPLE_RATE,AUDIO_CHANNELS);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BITRATE);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();

        }catch (IOException e){
            Log.e(this.getClass().getName(), "Cannot Create AAC Encoder", e);
        }
    }

    public void close() {
        isRunning = false;
        try {
            audioPreview.setEncoder(null);
            mediaCodec.stop();
            mediaCodec.release();
            handler.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void start(){
        isRunning = true;
        audioPreview.setEncoder(this);
    }

    public synchronized void offerEncoder(byte[] input) {
        try {
            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(input);
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, System.currentTimeMillis()*1000, 0);
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,0);

            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.position(bufferInfo.offset);
                outputBuffer.get(outData);
                handler.addAudioFrame(outData);

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
            }
        } catch (Throwable t) {
            if(isRunning || !t.getClass().equals(IOException.class)) {
                Log.e(AudioEncoder.class.getName(), "Error Occurred : ", t);
            }
        }

    }

}