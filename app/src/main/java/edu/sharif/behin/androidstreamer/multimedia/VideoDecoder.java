package edu.sharif.behin.androidstreamer.multimedia;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;


public class VideoDecoder implements Closeable{
    private FrameHandler handler;
    private Surface surface;
    private Thread decodeThread;
    private static final int BUFFER_SIZE = 128*1024;

    private MediaCodec mediaCodec;

    public VideoDecoder(FrameHandler frameHandler,Surface surface) {
        this.handler = frameHandler;
        this.surface = surface;
        initCodec();
    }

    public void start(){
        decodeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        byte[] frame = handler.getVideoFrame();

                        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                        int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {

                            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                            inputBuffer.clear();
                            inputBuffer.put(frame, 0, frame.length);
                            mediaCodec.queueInputBuffer(inputBufferIndex, 0, frame.length, 0, 0);
                        }else{
                            Log.e("Behin", "input buffer index is negative");
                        }
                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,0);
                        while(outputBufferIndex>=0){
                            mediaCodec.releaseOutputBuffer(outputBufferIndex, true);
                            outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                        }
                        if(Thread.currentThread().isInterrupted()){
                            break;
                        }

                    } catch (Exception e) {
                        Log.e(VideoDecoder.class.getName(), "error on reading input stream", e);
                        break;
                    }

                }
            }
        });
        decodeThread.setDaemon(true);
        decodeThread.start();

    }

    private void initCodec() {
        try {
            mediaCodec = MediaCodec.createDecoderByType(VideoEncoder.MIME_TYPE);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(VideoEncoder.MIME_TYPE, CameraPreview.previewHeight, CameraPreview.previewWidth);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, VideoEncoder.FRAME_RATE);
            mediaCodec.configure(mediaFormat, surface, null, 0);
            mediaCodec.start();
        }catch (IOException e){
            Log.e(VideoDecoder.class.getName(), "error in initializing Video Decoder", e);
        }
    }


    @Override
    public void close() {
        try {
            decodeThread.interrupt();
            mediaCodec.stop();
            mediaCodec.release();
        } catch (Exception e){
            Log.e(VideoDecoder.class.getName(), "error on closing video decoder", e);
        }
    }
}
