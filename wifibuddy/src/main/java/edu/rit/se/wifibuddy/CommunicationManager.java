package edu.rit.se.wifibuddy;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

/**
 * Handles reading and writing of messages with socket buffers. Uses a Handler
 * to post messages to UI thread for UI updates.
 */
public class CommunicationManager implements Runnable {

    private Socket socket = null;
    private Handler handler;
    private OutputStream outputStream;
    private static final String TAG = WifiDirectHandler.TAG + "CommManager";

    public CommunicationManager(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    // TODO: Add JavaDoc
    @Override
    public void run() {
        try {
            InputStream inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            byte[] buffer = new byte[1024];
            int bytes;
            handler.obtainMessage(WifiDirectHandler.MY_HANDLE, this).sendToTarget();

            while (true) {
                try {
                    // Read from the InputStream
                    bytes = inputStream.read(buffer);
                    if (bytes == -1) {
                        break;
                    }

                    // Send the obtained bytes to the UI Activity
                    Log.i(TAG, "Rec:" + Arrays.toString(buffer));
                    handler.obtainMessage(WifiDirectHandler.MESSAGE_READ,
                            bytes, -1, buffer.clone()).sendToTarget();
                    buffer = new byte[1024];
                } catch (IOException e) {
                    Log.e(TAG, "Communication disconnected", e);
                    // TODO: Handle disconnect
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error closing socket");
            }
        }
    }

    public void write(byte[] buffer) {
        try {
            outputStream.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }
}
