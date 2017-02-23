package edu.sharif.behin.androidstreamer.multimedia;

import java.nio.ByteBuffer;

public class Utils {
    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE/8);
        buffer.putLong(x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE/8);
        buffer.put(bytes);
        buffer.flip();//need flip
        return buffer.getLong();
    }

    public static byte[] intToBytes(int x) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.SIZE/8);
        buffer.putInt(x);
        return buffer.array();
    }

    public static int bytesToInt(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.SIZE/8);
        buffer.put(bytes);
        buffer.flip();//need flip
        return buffer.getInt();
    }
}
