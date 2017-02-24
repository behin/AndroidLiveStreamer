package edu.sharif.behin.androidstreamer.local;


import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import edu.sharif.behin.androidstreamer.Constants;
import edu.sharif.behin.androidstreamer.network.ServerWebSocketHandler;
import edu.sharif.behin.androidstreamer.network.StringMessage;

public class LocalWebSocketServer extends WebSocketServer implements Closeable {

    public static final int PORT = 9987;
    private final Map<UUID, WebSocket> uuidConnections = new ConcurrentHashMap<>();
    private final Map<WebSocket, UUID> webSocketConnections = new ConcurrentHashMap<>();

    public LocalWebSocketServer() {
        super(new InetSocketAddress(PORT), Collections.<Draft>singletonList(new Draft_17()));
        start();
    }

    @Override
    public synchronized void onOpen(WebSocket conn, ClientHandshake handshake) {
        try{
            UUID uuid = UUID.fromString(handshake.getFieldValue("uuid"));
            uuidConnections.put(uuid, conn);
            webSocketConnections.put(conn, uuid);
        }catch (Exception e){
            Log.e(LocalWebSocketServer.class.getName(), "can not open connection", e);
            conn.close();
        }
    }

    @Override
    public synchronized void onClose(WebSocket conn, int code, String reason, boolean remote) {
        UUID uuid = webSocketConnections.get(conn);
        Log.w(LocalWebSocketServer.class.getName(),"Close uuid:"+uuid+" code:"+code);
        webSocketConnections.remove(conn);
        uuidConnections.remove(uuid);
    }

    @Override
    public void onMessage(WebSocket session, ByteBuffer message) {
        UUID fromUUID = webSocketConnections.get(session);
        if(message.remaining()<(Long.SIZE/8)*3){
            StringMessage stringMessage=new StringMessage();
            stringMessage.message = Constants.FAULT_MESSAGE;
            stringMessage.uuid = Constants.SERVER_UUID;
            try {
                String result = new ObjectMapper().writeValueAsString(stringMessage);
                session.send(result);
            }catch (Exception e){
                Log.e(ServerWebSocketHandler.class.getName(),"Cannot Parse String Message to Json",e);
            }
            return;
        }
        ByteBuffer buffer = ByteBuffer.allocate(message.remaining());
        long msb = message.getLong();
        long lsb = message.getLong();
        UUID toUUID = new UUID(msb,lsb);
        byte[] newMessagePayload = new byte[message.remaining()];
        message.get(newMessagePayload);
        Log.i(LocalWebSocketServer.class.getName(),"message relay from: "+fromUUID +
                " to "+ toUUID+ " size: "+ buffer.remaining());
        WebSocket toSession = uuidConnections.get(toUUID);
        buffer.putLong(fromUUID.getMostSignificantBits());
        buffer.putLong(fromUUID.getLeastSignificantBits());
        buffer.put(newMessagePayload);
        if(toSession!=null){
            toSession.send(buffer.array());
        }else {
            StringMessage stringMessage=new StringMessage();
            stringMessage.message = Constants.NOT_FOUND_MESSAGE;
            stringMessage.uuid = Constants.SERVER_UUID;
            try {
                String result = new ObjectMapper().writeValueAsString(stringMessage);
                session.send(result);
            }catch (Exception e){
                Log.e(ServerWebSocketHandler.class.getName(),"Cannot Parse String Message to Json",e);
            }
        }
    }

    @Override
    public void onMessage(WebSocket session, String message) {
        UUID fromUUID = webSocketConnections.get(session);
        try {
            StringMessage stringMessage = new ObjectMapper().readValue(message,StringMessage.class);
            WebSocket toSession = uuidConnections.get(stringMessage.uuid);
            if(toSession!=null){
                stringMessage.uuid = fromUUID;
                toSession.send(new ObjectMapper().writeValueAsString(stringMessage));
            }else {
                stringMessage.message = Constants.NOT_FOUND_MESSAGE;
                stringMessage.uuid = Constants.SERVER_UUID;
                String result = new ObjectMapper().writeValueAsString(stringMessage);
                session.send(result);
            }
            stringMessage.uuid = fromUUID;
        }catch (Exception e){
            Log.e(LocalWebSocketServer.class.getName(),"Cannot Parse Message ",e);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        Log.e(LocalWebSocketServer.class.getName(), "error on web socket server",ex);
    }

    @Override
    public void close() throws IOException {
        try {
            stop();
        } catch (IOException | InterruptedException e) {
            Log.e(LocalWebSocketServer.class.getName(),"Cannot Stop Server :",e);
        }
    }
}
