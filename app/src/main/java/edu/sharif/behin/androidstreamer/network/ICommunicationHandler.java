package edu.sharif.behin.androidstreamer.network;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by Behin on 2/23/2017.
 */

public interface ICommunicationHandler {
    void handleBinaryMessage(UUID from,ByteBuffer message);
    void handleTextMessage(UUID from,String message);
}
