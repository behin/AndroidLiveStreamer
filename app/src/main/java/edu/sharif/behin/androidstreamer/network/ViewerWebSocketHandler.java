package edu.sharif.behin.androidstreamer.network;

import android.util.Log;
import android.view.Surface;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import edu.sharif.behin.androidstreamer.Constants;
import edu.sharif.behin.androidstreamer.multimedia.AudioDecoder;
import edu.sharif.behin.androidstreamer.multimedia.AudioEncoder;
import edu.sharif.behin.androidstreamer.multimedia.AudioPreview;
import edu.sharif.behin.androidstreamer.multimedia.CameraPreview;
import edu.sharif.behin.androidstreamer.multimedia.FrameHandler;
import edu.sharif.behin.androidstreamer.multimedia.VideoDecoder;
import edu.sharif.behin.androidstreamer.multimedia.VideoEncoder;

/**
 * Created by Behin on 2/24/2017.
 */

public class ViewerWebSocketHandler implements ICommunicationHandler,Closeable {


    public enum ViewerState {
        STOPPED,
        PLAYING
    }
    private ViewerState state;
    private ServerWebSocketHandler serverWebSocketHandler;
    private UUID sourceUUID;
    private NetworkInputStream networkInputStream;
    private FrameHandler frameHandler;
    private Surface videoSurface;
    private AudioDecoder audioDecoder;
    private VideoDecoder videoDecoder;

    public ViewerState getState(){
        return state;
    }


    public ViewerWebSocketHandler(UUID uuid,String serverAddress,Surface videoSurface){
        serverWebSocketHandler = new ServerWebSocketHandler(uuid,this,serverAddress);
        this.videoSurface = videoSurface;
        state = ViewerState.STOPPED;
        serverWebSocketHandler.start();
    }

    public ViewerWebSocketHandler(UUID uuid,Surface videoSurface){
        serverWebSocketHandler = new ServerWebSocketHandler(uuid,this);
        this.videoSurface = videoSurface;
        state = ViewerState.STOPPED;
        serverWebSocketHandler.start();
    }

    @Override
    public void handleBinaryMessage(UUID from,ByteBuffer message) {
        if(state == ViewerState.PLAYING){
            if(from.equals(sourceUUID)){
                networkInputStream.addDataToBuffer(message);
            }else {
                Log.e(ViewerWebSocketHandler.class.getName(),"Message Came from wrong source : "+sourceUUID);
                serverWebSocketHandler.sendStringMessage(from, Constants.FAULT_MESSAGE);
            }
        }else {
            Log.e(ViewerWebSocketHandler.class.getName(),"Viewer Cannot Handle Binary Messages in STOPPED State");
            serverWebSocketHandler.sendStringMessage(from, Constants.FAULT_MESSAGE);
        }

    }

    @Override
    public synchronized void handleTextMessage(UUID from,String message) {
        if(message.equals(Constants.FAULT_MESSAGE)){
            Log.e(ViewerWebSocketHandler.class.getName(),"Fault Message came from : "+from);
        }else if(message.equals(Constants.SOURCE_BUSY_MESSAGE)){
            if(from == sourceUUID) {
                Log.w(ViewerWebSocketHandler.class.getName(), "Source is Busy");
                stopPlaying();
            }else {
                Log.e(ViewerWebSocketHandler.class.getName(),"Source Busy came from wrong source :"+from);
                serverWebSocketHandler.sendStringMessage(from, Constants.FAULT_MESSAGE);
            }
        }
    }

    public boolean startPlaying(UUID source) {
        if(state == ViewerState.STOPPED) {
            state = ViewerState.PLAYING;
            sourceUUID = source;
            networkInputStream = new NetworkInputStream();
            frameHandler = new FrameHandler(networkInputStream);

            try {
                videoDecoder = new VideoDecoder(frameHandler, videoSurface);
                videoDecoder.start();
                audioDecoder = new AudioDecoder(frameHandler);
                audioDecoder.start();

            } catch (Exception e) {
                state = ViewerState.STOPPED;
                Log.e(ViewerWebSocketHandler.class.getName(), "Cannot Initialize Decoders", e);
                try {
                    networkInputStream.close();
                    frameHandler.close();
                    if (videoDecoder != null) {
                        videoDecoder.close();
                    }
                    if (audioDecoder != null) {
                        audioDecoder.close();
                    }
                } catch (IOException ex) {
                    Log.e(ViewerWebSocketHandler.class.getName(), "Cannot Close", ex);
                }
                return false;
            }
            serverWebSocketHandler.sendStringMessage(sourceUUID,Constants.START_STREAM_COMMAND);
            return true;
        }else {
            Log.e(SourceWebSocketHandler.class.getName(),"Bad State Cannot Start when PLAYING");
            return false;
        }
    }

    public boolean stopPlaying() {
        if(state == ViewerState.PLAYING) {
            serverWebSocketHandler.sendStringMessage(sourceUUID,Constants.STOP_STREAM_COMMAND);
            try {
                networkInputStream.close();
                frameHandler.close();
                videoDecoder.close();
                audioDecoder.close();
                videoDecoder = null;
                audioDecoder = null;
                frameHandler = null;
                networkInputStream = null;
                state = ViewerState.STOPPED;
            } catch (Exception e) {
                Log.e(SourceWebSocketHandler.class.getName(), "Cannot Stop Stream", e);
            }
            return true;
        }else {
            Log.e(SourceWebSocketHandler.class.getName(),"Bad State Cannot Stop when STOPPED");
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        if(state==ViewerState.PLAYING)
            stopPlaying();
        serverWebSocketHandler.close();
    }

    public boolean isConnected(){
        if(serverWebSocketHandler != null){
            return serverWebSocketHandler.isConnected();
        }
        return false;
    }
}
