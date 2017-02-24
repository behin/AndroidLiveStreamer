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

public class LocalLoopBackActivity extends AppCompatActivity implements SourceWebSocketHandler.ISourceWebSocketHandlerStateChangeListener {

    private CameraPreview cameraPreview;
    private AudioPreview audioPreview;

    private LocalWebSocketServer localWebSocketServer;

    private SourceWebSocketHandler sourceWebSocketHandler;
    private ViewerWebSocketHandler viewerWebSocketHandler;

    private AppCompatTextView sourceStateTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_loop_back);
        setTitle("Local Loop Back Demo");

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

        localWebSocketServer = new LocalWebSocketServer();

        sourceWebSocketHandler = new SourceWebSocketHandler(Constants.DEFAULT_SOURCE_UUID,cameraPreview,audioPreview,this);
        view.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                viewerWebSocketHandler = new ViewerWebSocketHandler(Constants.DEFAULT_VIEWER_UUID,view.getHolder().getSurface());
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {}
        });

        final Button playStopButton = (Button) findViewById(R.id.play_stop_button);
        playStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(viewerWebSocketHandler.getState() == ViewerWebSocketHandler.ViewerState.STOPPED){
                    if(viewerWebSocketHandler.startPlaying(Constants.DEFAULT_SOURCE_UUID)) {
                        playStopButton.setText("Stop");
                    }
                }else {
                    if(viewerWebSocketHandler.stopPlaying()){
                        playStopButton.setText("Play");
                    }
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            sourceWebSocketHandler.close();
            viewerWebSocketHandler.close();
            localWebSocketServer.close();
        }catch (IOException e){
            Log.e(LocalWebSocketServer.class.getName(),"Cannot Close Handlers",e);
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
