package com.github.teocci.audiostreamer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class ASActivity extends AppCompatActivity
{
    private static String TAG = "ASActivity";
    private Button startButton, stopButton;

    private boolean started = true;
    private static final Object recordingLock = new Object();

    private SocketAudio threadAudio;
    private String remoteIP = "192.168.1.160";
    private int remotePortAudio = 9990;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_as);

        startButton = (Button) findViewById(R.id.start_button);
        stopButton = (Button) findViewById(R.id.stop_button);

        startButton.setOnClickListener(startListener);
        stopButton.setOnClickListener(stopListener);

        Toast.makeText(this, "New address: " + remoteIP + ":" + remotePortAudio, Toast
                .LENGTH_LONG).show();
    }

    @Override
    protected void onResume()
    {
        // TODO Auto-generated method stub
        super.onResume();
        reset();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        closeSocketClient();
        reset();
    }

    private void reset()
    {
        synchronized (recordingLock) {
            started = false;
        }
        updateUI();
    }

    private final View.OnClickListener stopListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {
            synchronized (recordingLock) {
                started = false;
            }
            updateUI();
            closeSocketClient();
            Log.e(TAG, "Recorder Released");
        }
    };

    private final View.OnClickListener startListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View arg0)
        {
            synchronized (recordingLock) {
                started = true;
            }
            updateUI();
            startStreaming();
            Log.e(TAG, "Recorder Started");
        }
    };

    private void updateUI() {
        synchronized (recordingLock) {
            if (started) {
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
            } else {
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
            }
        }
    }

    public void startStreaming()
    {
        threadAudio = new SocketAudio(remoteIP, remotePortAudio);

        while (true) {
            if (!threadAudio.getState().equals("WAITING"))
                break;
        }
        Toast.makeText(this, "Status: " + threadAudio.getState().toString(), Toast.LENGTH_LONG).show();
    }

    private void closeSocketClient()
    {
        if (threadAudio == null)
            return;

        threadAudio.interrupt();
        try {
            threadAudio.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        threadAudio = null;
    }
}
