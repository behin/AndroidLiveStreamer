package edu.sharif.behin.androidstreamer.multimedia;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class AudioDecoder implements Closeable {
    private FrameHandler handler;
    private Thread decodeThread;
    private Thread playThread;
    private AudioTrack audioTrack;
    private boolean isRunning = false;

    private MediaCodec mediaCodec;

    public AudioDecoder(FrameHandler frameHandler) {
        this.handler = frameHandler;
        initCodec();
    }

    public void start(){
        isRunning = true;
        decodeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(isRunning){
                    try {
                        byte[] frame = handler.getAudioFrame();
                        if(frame == null){
                            continue;
                        }

                        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                        int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {
                            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                            inputBuffer.clear();
                            inputBuffer.put(frame, 0, frame.length);
                            mediaCodec.queueInputBuffer(inputBufferIndex, 0, frame.length, 0, 0);
                        }else{
                            Log.e(AudioDecoder.class.getName(), "input buffer index is negative");
                        }

                    } catch (IllegalStateException ise) {
                        if(isRunning){
                            Log.e(AudioDecoder.class.getName(), "illegal state on reading input stream", ise);
                        }
                    } catch (Exception e) {
                        Log.e(AudioDecoder.class.getName(), "error on reading input stream", e);
                    }
                }
            }
        });
        decodeThread.setDaemon(true);
        decodeThread.start();
        playThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(isRunning){
                    try {
                        try {
                            Thread.sleep(1);
                        }catch (InterruptedException e){
                            if(!isRunning){
                                break;
                            }
                        }
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
                        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,0);
                        while(outputBufferIndex>=0){
                            ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

                            final byte[] outputData = new byte[bufferInfo.size];
                            outputBuffer.position(bufferInfo.offset);
                            outputBuffer.get(outputData);

                            if (outputData.length > 0) {
                                audioTrack.write(outputData, 0, outputData.length);
                            }
                            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                        }
                        if(Thread.currentThread().isInterrupted()){
                            break;
                        }
                    }catch (IllegalStateException ise) {
                        if(isRunning){
                            Log.e(AudioDecoder.class.getName(), "illegal state on reading input stream", ise);
                        }
                    }catch (Exception e) {
                        Log.e(AudioDecoder.class.getName(), "error on reading input stream", e);
                    }
                }
            }
        });
        playThread.setDaemon(true);
        playThread.start();
    }

    private void initCodec() {
        try {
            mediaCodec = MediaCodec.createDecoderByType(AudioEncoder.MIME_TYPE);
            MediaFormat mediaFormat = MediaFormat.createAudioFormat(AudioEncoder.MIME_TYPE, AudioEncoder.AUDIO_SAMPLE_RATE, AudioEncoder.AUDIO_CHANNELS);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, AudioEncoder.AUDIO_BITRATE);
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            mediaCodec.configure(mediaFormat, null, null, 0);
            mediaCodec.start();
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, AudioEncoder.AUDIO_SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    AudioTrack.getMinBufferSize(AudioEncoder.AUDIO_SAMPLE_RATE,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
            audioTrack.play();

        }catch (IOException e){
            Log.e(AudioDecoder.class.getName(), "error in initializing Audio Decoder", e);
        }
    }

    @Override
    public void close() {
        isRunning = false;
        try {
            decodeThread.interrupt();
            playThread.interrupt();
            mediaCodec.stop();
            mediaCodec.release();
            audioTrack.stop();
            audioTrack.release();
        } catch (Exception e){
            Log.e(AudioDecoder.class.getName(), "error on closing audio decoder", e);
        }
    }
}
