# AndroidLiveStreamer
A Live Video Streaming Library on Android Devices. An Android device with camera is the video source, and another will be the video player. This project uses a relay server to connect two devices.
Each client will connect to the relay server using websocket. Commands will be sent using the Text channel of the web socket and the video data will be sent using binary channel.

## Instalation
To use this project and stream live video between two android device you need to run a [RelayServer](https://github.com/behin/RelayServer) on an IP address that is visible by both sides. Generaly you need to run it on a cloud based server, in some applications the server can be local. Then the IP and Port of the Server must set in the Constants.java file.
After that you can test the connection using relay loop back demo. 

Each Client is identified by a uuid. It is your job to sync or pair these uuids. As a sample two uuid hardcoded in the application Constants.java. A client needs to know the other uuid to connect to them.

## Code Example

Source Camera Example Code :
```java
CameraPreview cameraPreview = (CameraPreview) findViewById(R.id.camera);
AudioPreview audioPreview = (AudioPreview) findViewById(R.id.mic);
SourceWebSocketHandler sourceWebSocketHandler = 
    new SourceWebSocketHandler(SOURCE_UUID,SERVER_ADDRESS,cameraPreview,audioPreview,null);
```

```xml
<edu.sharif.behin.androidstreamer.multimedia.CameraPreview
            android:layout_width="200dp"
            android:layout_height="150dp"
            android:id="@+id/camera" />
<edu.sharif.behin.androidstreamer.multimedia.AudioPreview
            android:layout_width="200dp"
            android:layout_height="40dp"
            android:background="#000000"
            android:id="@+id/mic"/>
```

* CameraPreview : A widget for camera viewfinder.
* AudioPreview : A widget for audio waveform viewer
* SourceWebSocketHandler : A Handler that connects to RelayServer and handle stream commands.

Viewer Example Code : 

```java
Surface surface = view.getHolder().getSurface();
ViewerWebSocketHandler viewerWebSocketHandler = 
        new ViewerWebSocketHandler(VIEWER_UUID,SERVER_ADDRESS,surface);
```

```xml
<SurfaceView
            android:layout_width="250dp"
            android:layout_height="200dp"
            android:id="@+id/decodedView" />
```

* Surface : Surface to show Video On
* ViewerWebSocketHandler : A Handler that connexts to RelayServer and handle play and stop commands.

To Start Playing in the Viewer : 
```java
viewerWebSocketHandler.startPlaying(SOURCE_UUID)
```

To Stop Playin in the viewer : 
```java
viewerWebSocketHandler.stopPlaying()
```

## Tests
If you compile and run the application, there are 5 demo tasks inside it.

* Buffer Loop Back Demo : This demo just use the camera , microphone , encoder and decoder and test them.
* Local Loop Back Demo : In this demo a simple local web socket server opens on the mobile and device emulates both streamer and viewer.
* Relay Loop Back Demo : In this demo a device acts as both streamer and viewer but the video is pushed from an external RelayServer.
* Streamer Demo : A Simple Streamer Demo.
* Viewer Demo : A Simple Viewer Demo.

## License 
Everything in this repo is licensed under an MIT license. See the LICENSE file for specifics.


