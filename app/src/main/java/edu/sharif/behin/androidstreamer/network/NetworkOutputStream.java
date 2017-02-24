package edu.sharif.behin.androidstreamer.network;

import android.util.Log;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import edu.sharif.behin.androidstreamer.multimedia.VideoEncoder;

public class NetworkOutputStream extends OutputStream {

    public enum NetworkStreamState{
        OPEN,
        CLOSED,
        ;
    }

    public interface StatusChangeListener{
        void onStatusChanged(NetworkStreamState oldState, NetworkStreamState newState);
    }

    public static final int BUFFER_THRESHOLD = VideoEncoder.VIDEO_BITRATE / (8*4);
    public static final int MAX_BUFFER_SIZE = BUFFER_THRESHOLD * 16;
    private byte[] buffer = new byte[MAX_BUFFER_SIZE];
    private int bufferIndex = 0;
    private int sequenceNumber = 0;

    private NetworkStreamState state = NetworkStreamState.OPEN;

    private SourceWebSocketHandler webSocketHandler;

    private Thread runningThread = new Thread(new Runnable() {
        @Override
        public void run() {
            boolean end = false;
            while(!end){
                end = NetworkOutputStream.this.drainAndWait();
            }
        }
    });

    public NetworkOutputStream(SourceWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
        runningThread.setDaemon(true);
        runningThread.start();
    }

    @Override
    public synchronized void write(int i) throws IOException {
        if(state == NetworkStreamState.CLOSED){
            throw new IOException("connection closed");
        }
        if(bufferIndex+1 >= MAX_BUFFER_SIZE){
            throw new IOException("buffer is full.");
        }
        buffer[bufferIndex++] = (byte) i;
        if(bufferIndex >= BUFFER_THRESHOLD){
            notify();
        }
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        if(state == NetworkStreamState.CLOSED){
            throw new IOException("connection closed");
        }
        if(bufferIndex+len >= MAX_BUFFER_SIZE){
            throw new IOException("buffer is full.");
        }
        for(int i=off;i<off+len;i++){
            buffer[bufferIndex++] = b[i];
        }
        if(bufferIndex >= BUFFER_THRESHOLD){
            notify();
        }
    }

    private boolean drainAndWait() {
        if(bufferIndex>BUFFER_THRESHOLD){
            flush();
        }
        synchronized (this){
            try {
                wait();
                return false;
            } catch (InterruptedException e) {
                return true;
            }
        }
    }

    @Override
    public void flush() {
        ByteBuffer byteBuffer;
        synchronized (this){
            byteBuffer = ByteBuffer.allocate(bufferIndex + Integer.SIZE / 8);
            byteBuffer.putInt(sequenceNumber++);
            byteBuffer.put(buffer, 0, bufferIndex);
            bufferIndex = 0;
        }

        if(!webSocketHandler.sendFramesToDestination(byteBuffer)){
            try {
                close();
            }catch (IOException e){
                Log.e(NetworkOutputStream.class.getName(),"Cannot Close Stream",e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        state = NetworkStreamState.CLOSED;
        runningThread.interrupt();


    }

}
