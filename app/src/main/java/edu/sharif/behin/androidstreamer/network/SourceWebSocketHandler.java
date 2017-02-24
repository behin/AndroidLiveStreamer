package edu.sharif.behin.androidstreamer.network;

import android.util.Log;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import edu.sharif.behin.androidstreamer.Constants;
import edu.sharif.behin.androidstreamer.multimedia.AudioEncoder;
import edu.sharif.behin.androidstreamer.multimedia.AudioPreview;
import edu.sharif.behin.androidstreamer.multimedia.CameraPreview;
import edu.sharif.behin.androidstreamer.multimedia.FrameHandler;
import edu.sharif.behin.androidstreamer.multimedia.VideoEncoder;

/**
 * Created by Behin on 2/24/2017.
 */

public class SourceWebSocketHandler implements ICommunicationHandler,Closeable {

    public interface ISourceWebSocketHandlerStateChangeListener {
        void onStateChanged(SourceState oldState,SourceState newState);
    }

    private FrameHandler frameHandler;

    public enum SourceState {
        WAITING_FOR_CONNECTION,
        STREAMING
    }
    private SourceState state=SourceState.WAITING_FOR_CONNECTION;
    private ServerWebSocketHandler serverWebSocketHandler;
    private UUID destUUID;
    private NetworkOutputStream networkOutputStream;
    private CameraPreview cameraPreview;
    private AudioPreview audioPreview;
    private AudioEncoder audioEncoder;
    private VideoEncoder videoEncoder;
    private ISourceWebSocketHandlerStateChangeListener stateChangeListener;

    public SourceWebSocketHandler(UUID uuid,String serverAddress,CameraPreview cameraPreview,AudioPreview audioPreview,ISourceWebSocketHandlerStateChangeListener stateChangeListener){
        serverWebSocketHandler = new ServerWebSocketHandler(uuid,this,serverAddress);
        this.stateChangeListener = stateChangeListener;
        this.cameraPreview = cameraPreview;
        this.audioPreview = audioPreview;
        serverWebSocketHandler.start();
    }
    public SourceWebSocketHandler(UUID uuid,CameraPreview cameraPreview,AudioPreview audioPreview,ISourceWebSocketHandlerStateChangeListener stateChangeListener){
        serverWebSocketHandler = new ServerWebSocketHandler(uuid,this);
        this.stateChangeListener = stateChangeListener;
        this.cameraPreview = cameraPreview;
        this.audioPreview = audioPreview;
        serverWebSocketHandler.start();
    }

    public SourceState getState(){
        return state;
    }

    public void changeState(SourceState newState){
        SourceState oldState = state;
        state = newState;
        if(stateChangeListener != null){
            stateChangeListener.onStateChanged(oldState,newState);
        }
    }

    public boolean sendFramesToDestination(ByteBuffer buffer){
        if(state == SourceState.STREAMING){
            serverWebSocketHandler.sendBinaryMessage(destUUID,buffer.array());
            return true;
        }else {
            return false;
        }
    }

    @Override
    public void handleBinaryMessage(UUID from,ByteBuffer message) {
        Log.e(SourceWebSocketHandler.class.getName(),"Source Cannot Handle Binary Messages");
        serverWebSocketHandler.sendStringMessage(from,Constants.FAULT_MESSAGE);
    }

    @Override
    public synchronized void handleTextMessage(UUID from,String message) {
        if(message.equals(Constants.START_STREAM_COMMAND)){
            if(state == SourceState.WAITING_FOR_CONNECTION){
                startStreaming(from);
            }else {
                Log.w(SourceWebSocketHandler.class.getName(),"Already Streaming cannot Stream to: "+from);
                serverWebSocketHandler.sendStringMessage(from, Constants.SOURCE_BUSY_MESSAGE);
            }
        }else if(message.equals(Constants.STOP_STREAM_COMMAND)){
            if(state == SourceState.STREAMING && destUUID.equals(from)){
                stopStreaming();
            }else if (state == SourceState.STREAMING && !destUUID.equals(from)) {
                Log.e(SourceWebSocketHandler.class.getName(),"Stream Stop requested from another device: "+from);
                serverWebSocketHandler.sendStringMessage(from, Constants.FAULT_MESSAGE);
            }else {
                Log.e(SourceWebSocketHandler.class.getName(),"Cannot Stop Streaming in WAITING State.");
                serverWebSocketHandler.sendStringMessage(from,Constants.FAULT_MESSAGE);
            }
        }
    }

    private void startStreaming(UUID to) {
        changeState(SourceState.STREAMING);
        destUUID = to;
        networkOutputStream = new NetworkOutputStream(this);
        frameHandler = new FrameHandler(networkOutputStream);

        try {
            videoEncoder = new VideoEncoder(cameraPreview, frameHandler);
            videoEncoder.start();
            audioEncoder = new AudioEncoder(audioPreview, frameHandler);
            audioEncoder.start();

        }catch (Exception e){
            changeState(SourceState.WAITING_FOR_CONNECTION);
            Log.e(SourceWebSocketHandler.class.getName(),"Cannot Initialize Encoders",e);
            try {
                networkOutputStream.close();
                frameHandler.close();
                networkOutputStream = null;
                frameHandler = null;
                if(videoEncoder != null){
                    videoEncoder.close();
                }
                videoEncoder = null;
                if(audioEncoder != null){
                    audioEncoder.close();
                }
                audioEncoder = null;
            }catch (IOException ex){
                Log.e(SourceWebSocketHandler.class.getName(),"Cannot Close",ex);
            }

        }
    }

    private void stopStreaming() {
        try {
            videoEncoder.close();
            audioEncoder.close();
            frameHandler.close();
            networkOutputStream.close();
            videoEncoder = null;
            audioEncoder = null;
            frameHandler = null;
            networkOutputStream = null;
            changeState(SourceState.WAITING_FOR_CONNECTION);
        }catch (Exception e){
            Log.e(SourceWebSocketHandler.class.getName(),"Cannot Stop Stream",e);
        }
    }

    @Override
    public void close() throws IOException {
        if(state == SourceState.STREAMING){
            stopStreaming();
        }
        serverWebSocketHandler.close();
    }
}
