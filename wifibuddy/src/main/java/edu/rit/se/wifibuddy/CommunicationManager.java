package edu.rit.se.wifibuddy;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InterfaceAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
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
            byte[] messageSizeBuffer = new byte[Integer.SIZE/Byte.SIZE];
            int messageSize;
            byte[] buffer;// = new byte[1024];
            int bytes;
            int totalBytes;
            handler.obtainMessage(WifiDirectHandler.MY_HANDLE, this).sendToTarget();

            while (true) {
                try {
                    // Read from the InputStream
                    bytes = inputStream.read(messageSizeBuffer);
                    if (bytes == -1) { break; }
                    messageSize = ByteBuffer.wrap(messageSizeBuffer).getInt();
                    Log.i(TAG, "message size: " + messageSize);

                    buffer = new byte[messageSize];
                    bytes = inputStream.read(buffer);
                    totalBytes = bytes;
                    while (bytes != -1 && totalBytes < messageSize) {
                        bytes = inputStream.read(buffer, totalBytes, messageSize - totalBytes);
                        totalBytes += bytes;
                    }
                    if (bytes == -1) { break; }

                    // Send the obtained bytes to the UI Activity
                    Log.i(TAG, "Rec:" + Arrays.toString(buffer));
                    handler.obtainMessage(WifiDirectHandler.MESSAGE_READ,
                            bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    // Sends a message to WifiDirectHandler to handle the disconnect
                    handler.obtainMessage(WifiDirectHandler.COMMUNICATION_DISCONNECTED, this).sendToTarget();
                    Log.i(TAG, "Communication disconnected");
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

    public void write(byte[] message) {
        try {
            ByteBuffer sizeBuffer = ByteBuffer.allocate(Integer.SIZE/Byte.SIZE);
            byte[] sizeArray = sizeBuffer.putInt(message.length).array();
            byte[] completeMessage = new byte[sizeArray.length + message.length];
            System.arraycopy(sizeArray, 0, completeMessage, 0, sizeArray.length);
            System.arraycopy(message, 0, completeMessage, sizeArray.length, message.length);
            outputStream.write(completeMessage);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }
}
