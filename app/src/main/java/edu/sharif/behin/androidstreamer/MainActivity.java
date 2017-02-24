package edu.sharif.behin.androidstreamer;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button bufferLoopBackButton = (Button) findViewById(R.id.buffer_loop_back_button);
        bufferLoopBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BufferLoopBackActivity.class);
                startActivity(intent);
            }
        });

        Button localLoopBackButton = (Button) findViewById(R.id.local_loop_back_button);
        localLoopBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LocalLoopBackActivity.class);
                startActivity(intent);
            }
        });

        Button relayLoopBackButton = (Button) findViewById(R.id.relay_loop_back_button);
        relayLoopBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RelayLoopBackActivity.class);
                startActivity(intent);
            }
        });

        Button videoStreamerButton = (Button) findViewById(R.id.video_streamer);
        videoStreamerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, VideoStreamerActivity.class);
                startActivity(intent);
            }
        });

        Button videoPlayerButton = (Button) findViewById(R.id.video_player);
        videoPlayerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, VideoPlayerActivity.class);
                startActivity(intent);
            }
        });
    }
}
