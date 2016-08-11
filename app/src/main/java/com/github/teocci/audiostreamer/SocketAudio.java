package com.github.teocci.audiostreamer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by teocci on 8/11/16.
 */
public class SocketAudio extends Thread
{

    private static final String TAG = "SocketAudio";

    private Socket socket;
    private AudioRecord recorder;

    private String remoteIP;
    private int remotePort;

    private int rate = 44100;                   // 44100 for music
    private int packageSize = rate / 10 * 2;     // 0.1 seconds buffer size

    private int channel = AudioFormat.CHANNEL_IN_MONO;
    private int encoding = AudioFormat.ENCODING_PCM_16BIT;
    private int source = MediaRecorder.AudioSource.MIC;
    private int minBufSize = AudioRecord.getMinBufferSize(rate, channel, encoding);

    private boolean stopped = true;

    public SocketAudio()
    {
        start();
    }

    public SocketAudio(String ip, int port)
    {
        remoteIP = ip;
        remotePort = port;
        start();
    }

    public boolean isStopped()
    {
        return stopped;
    }

    public void setStopped(boolean stopped)
    {
        this.stopped = stopped;
    }

    @Override
    public void run()
    {
        // TODO Auto-generated method stub
        super.run();

        try {
            int timeOut = 10000; // in milliseconds

            socket = new Socket();
            socket.connect(new InetSocketAddress(remoteIP, remotePort), timeOut);

            Log.d(TAG, "Socket Created");

            BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
            BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());

            JsonObject jsonObj = new JsonObject();
            jsonObj.addProperty("type", "data");
            jsonObj.addProperty("length", minBufSize);
            jsonObj.addProperty("channel", channel);
            jsonObj.addProperty("encoding", encoding);
            jsonObj.addProperty("rate", rate);

            outputStream.write(jsonObj.toString().getBytes());
            outputStream.flush();

            byte[] buff = new byte[256];
            int len;
            String msg;

            while ((len = inputStream.read(buff)) != -1) {
                msg = new String(buff, 0, len);

                // JSON analysis
                JsonParser parser = new JsonParser();
                boolean isJSON = true;
                JsonElement element = null;
                try {
                    element = parser.parse(msg);
                } catch (JsonParseException e) {
                    Log.e(TAG, "exception: " + e);
                    isJSON = false;
                }
                if (isJSON && element != null) {
                    JsonObject obj = element.getAsJsonObject();
                    element = obj.get("state");
                    if (element != null && element.getAsString().equals("ok")) {

                        byte[] buffer = initBuffer();
                        Log.d(TAG, "Buffer created of size " + packageSize);

                        recorder = new AudioRecord(source, rate, channel, encoding, packageSize);
                        Log.d(TAG, "Recorder initialized");

                        recorder.startRecording();
                        stopped = false;

                        // send data
                        while (!stopped) {

                            //reading data from MIC into buffer
                            minBufSize = recorder.read(buffer, 0, buffer.length);

                            outputStream.write(buffer);
                            outputStream.flush();

                            System.out.println("MinBufferSize: " + minBufSize);
                            if (Thread.currentThread().isInterrupted())
                                break;
                        }

                        break;
                    }
                } else {
                    break;
                }
            }

            if (recorder != null)
                recorder.stop();
            outputStream.close();
            inputStream.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            stopped = true;
            Log.e(TAG, e.toString());
        } finally {
            try {
                socket.close();
                socket = null;
                stopped = true;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void close()
    {
        if (socket != null) {
            try {
                if (recorder != null)
                    recorder.stop();
                stopped = true;
                socket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private byte[] initBuffer()
    {
        Log.e(TAG, String.format("minBufferSize is %d bytes", minBufSize));
        if (this.packageSize < minBufSize)
            this.packageSize = minBufSize;

        Log.e(TAG, String.format("Audio packageSize is %d bytes", packageSize));
        return new byte[packageSize];
    }
}