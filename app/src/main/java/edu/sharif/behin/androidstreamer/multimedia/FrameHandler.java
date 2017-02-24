package edu.sharif.behin.androidstreamer.multimedia;

import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FrameHandler implements Closeable{
    private InputStream is;
    private OutputStream os;
    private boolean isEncoderType;
    private Queue<byte[]> videoFrames;
    private Queue<byte[]> audioFrames;
    private Queue<Long> videoTimestamps;
    private Queue<Long> audioTimestamps;
    private Thread bufferThread;

    private boolean isRunning=true;

    private long remoteTimestamp;
    private long localTimestamp;

    enum BufferState {
        BUFFERING_STATE,
        PLAYING_STATE
    }

    enum FrameHandlerType{
        AUDIO,
        VIDEO,
        AUDIO_VIDEO,

    }

    private BufferState bufferState= BufferState.BUFFERING_STATE;

    private int bufferThresholdVideo = 10;
    private int bufferThresholdAudio = 10;
    private int bufferOverflow = 250;

    private static final int MAX_BUFFER_OVERFLOW=100*10;

    public FrameHandler(InputStream is){
        this(is, FrameHandlerType.AUDIO_VIDEO);
    }

    public FrameHandler(InputStream is, FrameHandlerType type){
        this.is = is;
        switch (type){
            case AUDIO:
                bufferThresholdVideo = 0;
                break;
            case VIDEO:
                bufferThresholdAudio = 0;
                break;
        }
        isEncoderType=false;
        videoFrames = new ConcurrentLinkedQueue<>();
        audioFrames = new ConcurrentLinkedQueue<>();
        audioTimestamps = new ConcurrentLinkedQueue<>();
        videoTimestamps = new ConcurrentLinkedQueue<>();
        bufferThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted() && isRunning){
                    try {
                        readFrame();
                    } catch (IOException e) {
                        if(isRunning) {
                            Log.e(FrameHandler.class.getName(), "Read Exception : ", e);
                        }

                    }
                }

            }
        });
        bufferThread.setDaemon(true);
        bufferThread.start();
    }

    public FrameHandler(OutputStream os){
        this.os = os;
        isEncoderType=true;
    }

    //With Odd Timestamp
    public synchronized void addVideoFrame(byte[] frame) throws IOException{
        if(isEncoderType) {
            long timeStamp = System.currentTimeMillis();
            if(timeStamp%2 != 0){
                timeStamp++;
            }
            os.write(Utils.longToBytes(timeStamp));
            os.write(Utils.intToBytes(frame.length));
            os.write(frame);
        }
    }

    //With Even TimeStamp
    public synchronized void addAudioFrame(byte[] frame) throws IOException{
        if(isEncoderType) {
            long timeStamp = System.currentTimeMillis()-40-100; //100+40ms Audio Delay
            if(timeStamp%2 == 0){
                timeStamp++;
            }
            os.write(Utils.longToBytes(timeStamp));
            os.write(Utils.intToBytes(frame.length));
            os.write(frame);
        }
    }

    private void readFrame() throws IOException{
        if(!isEncoderType) {
            byte[] ts = new byte[Long.SIZE / 8];
            is.read(ts);
            byte[] size = new byte[Integer.SIZE / 8];
            is.read(size);
            byte[] frame = new byte[Utils.bytesToInt(size)];
            is.read(frame);
            synchronized (this) {
                long timeStamp = Utils.bytesToLong(ts);
                if(timeStamp%2==0) {
                    videoFrames.add(frame);
                    videoTimestamps.add(timeStamp);
                }else {
                    audioFrames.add(frame);
                    audioTimestamps.add(timeStamp);
                }

                if (bufferState == BufferState.BUFFERING_STATE && videoFrames.size() >= bufferThresholdVideo && audioFrames.size() >= bufferThresholdAudio) {
                    bufferState = BufferState.PLAYING_STATE;
                    localTimestamp  = System.currentTimeMillis();
                    remoteTimestamp = videoTimestamps.peek();
                    if(audioTimestamps.peek()<remoteTimestamp){
                        remoteTimestamp = audioTimestamps.peek();
                    }

                    notifyAll();
                }
                if((videoFrames.size()+audioFrames.size()) >= bufferOverflow){
                    if(bufferOverflow<MAX_BUFFER_OVERFLOW)
                        bufferOverflow++;
                    localTimestamp-=10;
                }
            }
        }

    }

    public byte[] getVideoFrame()throws IOException{
        if(!isEncoderType) {
            synchronized (this) {
                while (bufferState == BufferState.BUFFERING_STATE) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Log.e(FrameHandler.class.getName(), "Thread Interrupted", e);
                        close();
                    }
                }
            }
            if(videoFrames.size()==1){//Make Buffering
                makeBuffering();
            }
            byte[] frame = videoFrames.poll();
            long timeStamp = videoTimestamps.poll();
            long frameTime = timeStamp-remoteTimestamp;
            long actualTime = System.currentTimeMillis()-localTimestamp;
            if(actualTime<frameTime){
                try {
                    long sleepTime = frameTime-actualTime;
                    if(sleepTime>(1000/VideoEncoder.FRAME_RATE)){
                        sleepTime = 1500/VideoEncoder.FRAME_RATE;
                    }
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    if(isRunning) {
                        Log.e(FrameHandler.class.getName(), "Sleep Interrupted", e);
                    }
                }
            }
            return frame;
        }
        return null;
    }

    private void makeBuffering() {
        if(bufferOverflow<MAX_BUFFER_OVERFLOW) {
            if(bufferThresholdVideo != 0){
                bufferThresholdVideo+=2;
            }
            if(bufferThresholdAudio != 0){
                bufferThresholdAudio+=2;
            }
            bufferOverflow+=4;
            Log.e(FrameHandler.class.getName(),"Network Connection is Poor. New Threshold: audio: "+bufferThresholdAudio+" video: "+bufferThresholdVideo+" frames, Overflow:"+bufferOverflow+" frames");
        }
        bufferState= BufferState.BUFFERING_STATE;
    }

    public byte[] getAudioFrame()throws IOException{
        if(!isEncoderType) {
            synchronized (this) {
                while (bufferState == BufferState.BUFFERING_STATE) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Log.e(FrameHandler.class.getName(), "Thread Interrupted", e);
                    }
                }
            }
            if(audioFrames.size()==1){//Make Buffering
                makeBuffering();
            }
            byte[] frame = audioFrames.poll();
            long timeStamp = audioTimestamps.poll();
            long frameTime = timeStamp-remoteTimestamp;
            long actualTime = System.currentTimeMillis()-localTimestamp;
            if(actualTime<frameTime){
                try {
                    long sleepTime = frameTime-actualTime;
                    if(sleepTime>(100)){
                        Log.e(FrameHandler.class.getName(),"Audio Timestamp Problem Sleep time : "+sleepTime);
                        sleepTime = 100;
                    }
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    return null;
                }
            }
            return frame;
        }
        return null;
    }


    @Override
    public void close() throws IOException {
        isRunning = false;
        if(bufferThread != null){
            bufferThread.interrupt();
        }

    }
}
