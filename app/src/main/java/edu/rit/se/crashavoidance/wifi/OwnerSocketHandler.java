package edu.rit.se.crashavoidance.wifi;

import android.os.Handler;
import android.util.Log;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The implementation of a ServerSocket handler. This is used by the Wi-Fi P2P group owner.
 */
public class OwnerSocketHandler extends Thread {

    private ServerSocket serverSocket = null;
    private final int THREAD_COUNT = 10;
    private Handler handler;
    private static final String TAG = "GroupOwnerSocketHandler";

    public OwnerSocketHandler(Handler handler) throws IOException {
        try {
            serverSocket = new ServerSocket(WifiDirectHandler.SERVER_PORT);
            this.handler = handler;
            Log.i(TAG, "Group owner server socket started");
        } catch (IOException e) {
            Log.e(TAG, "Error starting server socket");
            Log.e(TAG, e.getMessage());
            pool.shutdownNow();
            throw e;
        }
    }

    /**
     * A ThreadPool for client sockets.
     */
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            THREAD_COUNT, THREAD_COUNT, 10, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

    @Override
    public void run() {
        Log.i(TAG, "Group owner server socket thread running");
        while (true) {
            try {
                // A blocking operation. Initiate a CommunicationManager instance when
                // there is a new connection
                pool.execute(new CommunicationManager(serverSocket.accept(), handler));
                Log.i(TAG, "Launching the I/O handler");
            } catch (IOException e) {
                Log.e(TAG, "Error launching the I/O handler");
                Log.e(TAG, e.getMessage());
                try {
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        serverSocket.close();
                        Log.i(TAG, "Server socket closed");
                    }
                } catch (IOException ioe) {
                    Log.e(TAG, "Error closing socket");
                    Log.e(TAG, ioe.getMessage());
                }
                pool.shutdownNow();
                break;
            }
        }
    }
}
