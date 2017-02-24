package edu.sharif.behin.androidstreamer.network;


import android.support.annotation.NonNull;
import android.util.Log;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;



public class NetworkInputStream extends InputStream{

    public static final int MAX_BUFFER_SIZE = 1310720;
    public static final int CHUNK_WINDOW_SIZE = 5;

    private NetworkOutputStream.NetworkStreamState state = NetworkOutputStream.NetworkStreamState.OPEN;
    private Queue<Byte> buffer = new CircularFifoQueue<>(MAX_BUFFER_SIZE);
    private int sequenceNumber = 0;
    private Map<Integer, byte[]> waitingChunks = new ConcurrentHashMap<>();

    public NetworkInputStream() {
    }

    public void addDataToBuffer(ByteBuffer byteBuffer) {
        int seq = byteBuffer.getInt();
        byte[] body = new byte[byteBuffer.remaining()];
        byteBuffer.get(body);
        if(seq<sequenceNumber + CHUNK_WINDOW_SIZE && seq >= sequenceNumber){
            waitingChunks.put(seq, body);
        }else{
            state = NetworkOutputStream.NetworkStreamState.CLOSED;
        }
        synchronized (this){
            while(waitingChunks.containsKey(sequenceNumber)){
                buffer.addAll(Arrays.asList(ArrayUtils.toObject(waitingChunks.get(sequenceNumber))));
                sequenceNumber++;
            }
            if(buffer.size()>0){
                notify();
            }
        }
    }

    @Override
    public synchronized int read() throws IOException {
        if(buffer.isEmpty()){
            try {
                if(state == NetworkOutputStream.NetworkStreamState.CLOSED){
                    throw new IOException("Stream closed.");
                }
                wait();
            } catch (InterruptedException e) {
                throw new IOException("Stream Interrupted.",e);
            }
        }
        return buffer.poll();
    }

    @Override
    public synchronized int read(@NonNull byte[] b) throws IOException {
        while(buffer.size()<b.length){
            if(state == NetworkOutputStream.NetworkStreamState.CLOSED){
                throw new IOException("Stream closed.");
            }
            try {
                wait();
            } catch (InterruptedException e) {
                throw new IOException("Stream Interrupted.",e);
            }
        }
        int index;
        for(index=0;index<b.length;index++){
            b[index]=buffer.poll();
        }
        return index;
    }

    @Override
    public synchronized void close() {
        try {
            super.close();
        } catch (IOException e) {
            Log.e(NetworkInputStream.class.getName(), "can not close stream", e);
        }
        state = NetworkOutputStream.NetworkStreamState.CLOSED;
        notify();
    }
}
