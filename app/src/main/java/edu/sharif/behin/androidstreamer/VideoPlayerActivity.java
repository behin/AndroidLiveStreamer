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
import java.util.Timer;
import java.util.TimerTask;

import edu.sharif.behin.androidstreamer.local.LocalWebSocketServer;
import edu.sharif.behin.androidstreamer.multimedia.AudioPreview;
import edu.sharif.behin.androidstreamer.multimedia.CameraPreview;
import edu.sharif.behin.androidstreamer.network.SourceWebSocketHandler;
import edu.sharif.behin.androidstreamer.network.ViewerWebSocketHandler;

public class VideoPlayerActivity extends AppCompatActivity {


    private ViewerWebSocketHandler viewerWebSocketHandler;
    private Timer relayStatusUpdateTimer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        setTitle("Video Player Demo");

        final SurfaceView view = (SurfaceView) findViewById(R.id.decodedView);



        final AppCompatTextView relayServerTextView = (AppCompatTextView) findViewById(R.id.relay_server);

        relayStatusUpdateTimer = new Timer();
        relayStatusUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(viewerWebSocketHandler.isConnected()) {
                            relayServerTextView.setText("Relay Server : "+Constants.SERVER_ADDRESS + "(Connected)");
                            relayServerTextView.setBackgroundColor(getResources().getColor(R.color.green_state));
                        }else {
                            relayServerTextView.setText("Relay Server : " + Constants.SERVER_ADDRESS + " (Not Connected)");
                            relayServerTextView.setBackgroundColor(getResources().getColor(R.color.yellow_state));
                        }
                    }
                });

            }
        },500,2000);


        view.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                viewerWebSocketHandler = new ViewerWebSocketHandler(Constants.DEFAULT_VIEWER_UUID,Constants.SERVER_ADDRESS,view.getHolder().getSurface());
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
            viewerWebSocketHandler.close();
        }catch (IOException e){
            Log.e(LocalWebSocketServer.class.getName(),"Cannot Close Handlers",e);
        }

        relayStatusUpdateTimer.cancel();
        relayStatusUpdateTimer.purge();

    }

}
