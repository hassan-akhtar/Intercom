package com.android.newintercom.Utils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class BroadcastCall {

    private static final String LOG_TAG = "BroadcastCall";
    private static final int SAMPLE_RATE = 8000; // Hertz
    private static final int SAMPLE_INTERVAL = 200; // Milliseconds
    private static final int SAMPLE_SIZE = 2; // Bytes
    //private static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2; //Bytes
    private InetAddress address; // Address to call
    private int port = 50666; // Port the packets are addressed to
    private boolean mic = false; // Enable mic?
    private boolean speakers = false; // Enable speakers?
    private String ownAddress = "";
    Context mContext;
    SharedPreferencesManager sharedPreferencesManager;
    AudioManager am ;
    AcousticEchoCanceler acousticEchoCanceler;
    NoiseSuppressor noiseSuppressor;
    AutomaticGainControl automaticGainControl;

    public BroadcastCall(Context context, InetAddress address, String ownAddress) {

        this.address = address;
        this.ownAddress = ownAddress;
        this.mContext = context;
        sharedPreferencesManager = new SharedPreferencesManager(mContext);
        am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    public void startCall() {

        startMic();
        startSpeakers();
    }

    public void endCall() {

        Log.e(LOG_TAG, "Ending call!");
        muteMic();
        muteSpeakers();
    }

    public void muteMic() {

        mic = false;
    }

    public void muteSpeakers() {

        speakers = false;
    }


    public void startMic() {
        // Creates the thread for capturing and transmitting audio

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                // Create an instance of the AudioRecord class
                //  android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
                mic = true;
/*                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);*/

                int bufferSize = 4096;
                int playBufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);

                Log.e(LOG_TAG, "Send thread started. Thread id: " + Thread.currentThread().getId());

                AudioRecord audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);


                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        if(AcousticEchoCanceler.isAvailable()) {
                            acousticEchoCanceler = AcousticEchoCanceler.create(audioRecorder.getAudioSessionId());
                            if (null!=acousticEchoCanceler) {
                                acousticEchoCanceler.setEnabled(true);
                            }
                        }

                        if(NoiseSuppressor.isAvailable()) {
                            noiseSuppressor = NoiseSuppressor.create(audioRecorder.getAudioSessionId());
                            if (null!=noiseSuppressor) {
                                noiseSuppressor.setEnabled(true);
                            }
                        }

                        if(AutomaticGainControl.isAvailable()) {
                            automaticGainControl = AutomaticGainControl.create(audioRecorder.getAudioSessionId());
                            if (null!=automaticGainControl) {
                                automaticGainControl.setEnabled(true);
                            }
                        }
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }


                am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                audioRecorder.startRecording();

                int bytes_read = 0;
                int bytes_sent = 0;
                byte[] buf = new byte[bufferSize];
                try {
                    // Create a socket and start recording
                    Log.e(LOG_TAG, "Packet destination: " + address.toString());
                    DatagramSocket socket = new DatagramSocket();
                    socket.setBroadcast(true);
                    while (mic) {
                        // Capture audio from the mic and transmit it
                        bytes_read = audioRecorder.read(buf, 0, buf.length);
                        if (0 < bytes_read ) {
                            if (!sharedPreferencesManager.getBoolean(SharedPreferencesManager.IS_DND)) {
                            DatagramPacket packet = new DatagramPacket(buf, bytes_read, address, port);
                            Log.e("Mic:", "Addressss  " + packet.getAddress());
                            Log.e("Mic:", "bufferSize  " + bufferSize);
                            Log.e("Mic:", "buf.length  " + buf.length);
                            Log.e("Mic:", "bytes_read  " + bytes_read);
                            Log.e("Mic:", "packet.getLength()  " + packet.getLength());
                            socket.send(packet);
                            bytes_sent += bytes_read;
                            Log.e(LOG_TAG, "Total bytes sent: " + bytes_sent);
                            Thread.sleep(SAMPLE_INTERVAL, 0);
                            } else {
                                Log.e("Speaker:", "Dnd on nigga  ");
                            }
                        }
                    }
                    // Stop recording and release resources
                    am.setMode(AudioManager.MODE_NORMAL);
                    audioRecorder.stop();
                    audioRecorder.release();
                    socket.disconnect();
                    socket.close();
                    mic = false;
                    return;
                } catch (SocketException e) {

                    Log.e(LOG_TAG, "SocketException: " + e.toString());
                    mic = false;
                } catch (UnknownHostException e) {

                    Log.e(LOG_TAG, "UnknownHostException: " + e.toString());
                    mic = false;
                } catch (IOException e) {

                    Log.e(LOG_TAG, "IOException: " + e.toString());
                    mic = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    public void startSpeakers() {
        // Creates the thread for receiving and playing back audio
        if (!speakers) {

            speakers = true;

            Thread receiveThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    //android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
/*                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);*/

                    int bufferSize = 4096;
                    int playBufSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);

                    // Create an instance of AudioTrack, used for playing back audio
                    Log.e(LOG_TAG, "Receive thread started. Thread id: " + Thread.currentThread().getId());
                    AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT, playBufSize, AudioTrack.MODE_STREAM);
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        AcousticEchoCanceler.create(track.getAudioSessionId());
                        NoiseSuppressor.create(track.getAudioSessionId());
                        AutomaticGainControl.create(track.getAudioSessionId());
                    }
                    am.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    track.play();
                    try {
                        // Define a socket to receive the audio
                        DatagramSocket socket = new DatagramSocket(port);
                        byte[] buf = new byte[bufferSize];
                        while (speakers) {
                            // Play back the audio received from packets
                            DatagramPacket packet = new DatagramPacket(buf, bufferSize);
                            socket.receive(packet);
                            if (!packet.getAddress().toString().equals("/" + ownAddress) && !sharedPreferencesManager.getBoolean(SharedPreferencesManager.IS_DND)) {
                                Log.e(LOG_TAG, "Packet received: " + packet.getLength());
                                track.write(packet.getData(), 0, packet.getLength());
                                Log.e("Speaker:", "Addressss  " + packet.getAddress());
                                Log.e("Speaker:", "playBufSize  " + playBufSize);
                                Log.e("Speaker:", "bufferSize  " + bufferSize);
                                Log.e("Speaker:", "buf.length  " + buf.length);
                                Log.e("Speaker:", "packet.getLength()  " + packet.getLength());
                            } else {
                                Log.e("Speaker:", "Itss mee nigga  " + packet.getAddress());
                            }


                        }
                        // Stop playing back and release resources
                        am.setMode(AudioManager.MODE_NORMAL);
                        socket.disconnect();
                        socket.close();
                        track.stop();
                        track.flush();
                        track.release();
                        speakers = false;
                        return;
                    } catch (SocketException e) {

                        Log.e(LOG_TAG, "SocketException: " + e.toString());
                        speakers = false;
                    } catch (IOException e) {

                        Log.e(LOG_TAG, "IOException: " + e.toString());
                        speakers = false;
                    }
                }
            });


            receiveThread.start();
        }
    }
}

