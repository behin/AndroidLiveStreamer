package edu.sharif.behin.androidstreamer.network;

import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import edu.sharif.behin.androidstreamer.Constants;

public class ServerWebSocketHandler implements Closeable {

    private  Date lastRun;
    private  boolean isConnected;
    private  boolean interrupted = false;

    private ICommunicationHandler communicationHandler;
    private WebSocketClient webSocketClient;
    private UUID myUUID;
    private String serverAddress;

    @Override
    public void close() throws IOException {
        interrupted = true;
        webSocketClient.close();
    }

    private enum WebSocketType {
        LOCAL,RELAY
    }

    private WebSocketType type;


    public ServerWebSocketHandler(UUID uuid,ICommunicationHandler communicationHandler,String serverAddress){
        this.myUUID = uuid;
        this.serverAddress = serverAddress;
        type = WebSocketType.RELAY;
        this.communicationHandler = communicationHandler;
    }

    public ServerWebSocketHandler(UUID uuid,ICommunicationHandler communicationHandler){
        this.myUUID = uuid;

        type = WebSocketType.LOCAL;
        this.communicationHandler = communicationHandler;
    }

    private void createWebSocketClient(){
        if(type == WebSocketType.RELAY){
            createWebSocketClientToRelayServer();
        }else {
            createWebSocketClientToLocalServer();
        }
    }

    private void createWebSocketClientToRelayServer() {
        webSocketClient = new WebSocketClient(getUri(this.serverAddress, myUUID), new Draft_17()) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                ServerWebSocketHandler.this.onOpen(handshakedata);
            }

            @Override
            public void onMessage(String message) {
                ServerWebSocketHandler.this.onMessage(message);
            }

            @Override
            public void onMessage(ByteBuffer message) {
                ServerWebSocketHandler.this.onMessage(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                ServerWebSocketHandler.this.onClose(code,reason,remote);
            }

            @Override
            public void onError(Exception ex) {
                ServerWebSocketHandler.this.onError(ex);
            }
        };
    }
    private void createWebSocketClientToLocalServer() {
        webSocketClient = new WebSocketClient(getLocalUri(), new Draft_17(),Collections.singletonMap("uuid", myUUID.toString()), 0) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                ServerWebSocketHandler.this.onOpen(handshakedata);
            }

            @Override
            public void onMessage(String message) {
                ServerWebSocketHandler.this.onMessage(message);
            }

            @Override
            public void onMessage(ByteBuffer message) {
                ServerWebSocketHandler.this.onMessage(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                ServerWebSocketHandler.this.onClose(code,reason,remote);
            }

            @Override
            public void onError(Exception ex) {
                ServerWebSocketHandler.this.onError(ex);

            }
        };
    }

    public synchronized void start(){
        isConnected = false;
        if(interrupted){
            return;
        }
        Date now = new Date();
        if(lastRun!= null && now.getTime() - lastRun.getTime() < 2000){
            new Timer(true).schedule(new TimerTask() {
                @Override
                public void run() {
                    start();
                }
            }, 2000);
            return;
        }
        lastRun = new Date();
        createWebSocketClient();
        webSocketClient.connect();
    }

    public void stop() {
        interrupted = true;
        webSocketClient.close();
    }

    public synchronized void sendStringMessage(UUID to,String message){
        if(isConnected){
            StringMessage stringMessage=new StringMessage();
            stringMessage.message = message;
            stringMessage.uuid = to;
            try {
                String result = new ObjectMapper().writeValueAsString(stringMessage);
                send(result);
            }catch (Exception e){
                Log.e(ServerWebSocketHandler.class.getName(),"Cannot Parse String Message to Json",e);
            }
        }else{
            Log.e(ServerWebSocketHandler.class.getName(), "Web socket not connected");
        }
    }

    public synchronized void sendBinaryMessage(UUID to,byte[] message){
        if(isConnected){
            ByteBuffer buffer = ByteBuffer.allocate(message.length+2*(Long.SIZE/8));
            buffer.putLong(to.getMostSignificantBits());
            buffer.putLong(to.getLeastSignificantBits());
            buffer.put(message);
            send(buffer.array());
        }else{
            Log.e(ServerWebSocketHandler.class.getName(), "Web socket not connected");
        }
    }

    public static URI getUri(String serverAddress,UUID uuid){
        String url = "ws://" + serverAddress+"/WebSocket/"+uuid;
        return URI.create(url);
    }
    private static URI getLocalUri() {
        String uri = "http://" + Constants.LOCAL_SERVER_ADDRESS + "/";
        return URI.create(uri);
    }

    public void onOpen(ServerHandshake handshakedata) {
        Log.i(ServerWebSocketHandler.class.getName(), "Web socket connection opened.");
        isConnected = true;
    }

    public void onMessage(String message){
        try {
            StringMessage stringMessage = new ObjectMapper().readValue(message,StringMessage.class);
            communicationHandler.handleTextMessage(stringMessage.uuid,stringMessage.message);
        }catch (Exception e){
            Log.e(ServerWebSocketHandler.class.getName(),"Cannot Parse Message ",e);
        }
    }

    public void onMessage(ByteBuffer message){
        if(message.remaining()<(Long.SIZE/8)*3){
            Log.e(ServerWebSocketHandler.class.getName(),"Bad message format : "+message);
            return;
        }
        long msb = message.getLong();
        long lsb = message.getLong();
        UUID fromUUID = new UUID(msb,lsb);
        byte[] newMessagePayload = new byte[message.remaining()];
        message.get(newMessagePayload);
        communicationHandler.handleBinaryMessage(fromUUID,ByteBuffer.wrap(newMessagePayload));
    }

    public void onClose(int code, String reason, boolean remote) {
        Log.e(ServerWebSocketHandler.class.getName(), "Web Socket Disconnected: "+code+" reason: "+reason);
        start();
    }

    public void onError(Exception ex) {
        // TODO: change log level
        Log.d(ServerWebSocketHandler.class.getName(), "Error occurred: ", ex);
    }


    public void send(String message){
        webSocketClient.send(message);
    }

    public void send(byte[] message){
        webSocketClient.send(message);
    }

}
