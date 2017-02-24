package edu.sharif.behin.androidstreamer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import java.io.IOException;

import edu.sharif.behin.androidstreamer.local.LocalWebSocketServer;
import edu.sharif.behin.androidstreamer.multimedia.AudioPreview;
import edu.sharif.behin.androidstreamer.multimedia.CameraPreview;
import edu.sharif.behin.androidstreamer.network.SourceWebSocketHandler;
import edu.sharif.behin.androidstreamer.network.ViewerWebSocketHandler;

public class VideoStreamerActivity extends AppCompatActivity implements SourceWebSocketHandler.ISourceWebSocketHandlerStateChangeListener{

    private CameraPreview cameraPreview;
    private AudioPreview audioPreview;

    private SourceWebSocketHandler sourceWebSocketHandler;
    private AppCompatTextView sourceStateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_streamer);
        setTitle("Video Streamer Demo");

        cameraPreview = (CameraPreview) findViewById(R.id.camera);
        audioPreview = (AudioPreview) findViewById(R.id.mic);
        sourceStateTextView = (AppCompatTextView) findViewById(R.id.source_state);

        final SurfaceView view = (SurfaceView) findViewById(R.id.decodedView);

        ImageButton swap = (ImageButton) findViewById(R.id.button);
        swap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraPreview.swapCamera();
            }
        });

        AppCompatTextView relayServerTextView = (AppCompatTextView) findViewById(R.id.relay_server);
        relayServerTextView.setText("Relay Server : "+Constants.SERVER_ADDRESS);

        sourceWebSocketHandler = new SourceWebSocketHandler(Constants.DEFAULT_SOURCE_UUID,Constants.SERVER_ADDRESS,cameraPreview,audioPreview,this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            sourceWebSocketHandler.close();
        }catch (IOException e){
            Log.e(VideoStreamerActivity.class.getName(),"Cannot Close Handlers",e);
        }

        cameraPreview.stop();
        audioPreview.stop();
    }

    @Override
    public void onStateChanged(SourceWebSocketHandler.SourceState oldState, SourceWebSocketHandler.SourceState newState) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(sourceWebSocketHandler.getState() == SourceWebSocketHandler.SourceState.STREAMING){
                    sourceStateTextView.setText("Streaming");
                }else {
                    sourceStateTextView.setText("Waiting For Connection");
                }
            }
        });
    }
}
