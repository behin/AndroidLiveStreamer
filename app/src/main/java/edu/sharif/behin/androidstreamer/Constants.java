package edu.sharif.behin.androidstreamer;

import java.util.UUID;

/**
 * Created by Behin on 2/23/2017.
 */

public class Constants {
    public static final String SERVER_ADDRESS = "192.168.1.100:8080";
    public static final String LOCAL_SERVER_ADDRESS = "127.0.0.1:9987";

    public static final String START_STREAM_COMMAND = "START_STREAM";
    public static final String STOP_STREAM_COMMAND = "STOP_STREAM";

    public static final String SOURCE_BUSY_MESSAGE = "SOURCE_BUSY";
    public static final String FAULT_MESSAGE = "FAULT";
    public static final String NOT_FOUND_MESSAGE = "NOT_FOUND";


    public static final UUID SERVER_UUID = UUID.fromString("98024d16-fa65-11e6-bc64-92361f002671");

    public static final UUID DEFAULT_SOURCE_UUID = UUID.fromString("544d01ec-fa60-11e6-bc64-92361f002671");
    public static final UUID DEFAULT_VIEWER_UUID = UUID.fromString("544d0458-fa60-11e6-bc64-92361f002671");



}
