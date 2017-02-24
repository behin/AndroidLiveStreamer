package edu.sharif.behin.androidstreamer.local;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by Behin on 1/30/2017.
 */

public class BufferLoopBackStream {
    private final Queue<byte[]> buffer=new ConcurrentLinkedQueue<>();

    public OutputStream getOutputStream(){
        return new OutputStream() {
            @Override
            public void write(int i) throws IOException {
                throw new RuntimeException("Write int");
            }

            @Override
            public void write(byte [] bytes) throws IOException {
                synchronized (buffer) {
                    buffer.add(bytes);
                    buffer.notify();
                }
            }
        };
    }

    public InputStream getInputStream(){
        return new InputStream() {
            @Override
            public int read() throws IOException {
                throw new RuntimeException("Read Int");
            }
            public int read(byte[] bytes) throws IOException {
                synchronized (buffer) {
                    if(buffer.size()==0) {
                        try {
                            buffer.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    byte[] item = buffer.poll();
                    System.arraycopy(item,0,bytes,0,bytes.length);
                    return bytes.length;
                }

            }
        };
    }
}
