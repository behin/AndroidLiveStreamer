package edu.sharif.behin.androidstreamer.multimedia;


import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class VideoEncoder implements Closeable {

    public static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    public static final int VIDEO_PIXEL_COUNT = CameraPreview.previewHeight*CameraPreview.previewWidth;

    public static final int VIDEO_BITRATE=512*1024;
    public static final int FRAME_RATE = 25;               // 25fps
    public static final int IFRAME_INTERVAL = 1;           // 1 seconds between I-frames


    private FrameHandler handler;
    private CameraPreview cameraPreview;

    private MediaCodec mediaCodec;

    public VideoEncoder(CameraPreview cameraPreview,FrameHandler frameHandler){
        this.handler = frameHandler;
        this.cameraPreview = cameraPreview;
        initCodec();
    }

    public void start(){
        cameraPreview.setEncoder(this);
    }



    private void initCodec() {
        try {
            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,CameraPreview.previewHeight,CameraPreview.previewWidth);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BITRATE);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,0);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            //mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();

        }catch (IOException e){
            Log.e(this.getClass().getName(), "Cannot Create AVC Encoder", e);
        }
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
                outputBuffer.get(outData);
                //Stream Data
                handler.addVideoFrame(outData);

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);

            }
        } catch (Throwable t) {
            Log.e("AvcEncoder", "error occurred while encoding video", t);
        }

    }

    public void close() {
        try {
            cameraPreview.setEncoder(null);
            mediaCodec.stop();
            mediaCodec.release();
            handler.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }


}
