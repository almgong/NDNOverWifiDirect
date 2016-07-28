package ag.ndn.ndnoverwifidirect.task;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Task to await IP negotiation with the other peer (you are the group owner).
 * Created by allengong on 7/26/16.
 */
public class GroupOwnerNegotiationTask extends AsyncTask<Void, Void, String> {

    private Context context;
    private TextView statusText;
    private static final String TAG = "GroupOwnerNegotiationTask";

    public GroupOwnerNegotiationTask(Context context, View statusText) {
        this.context = context;
        this.statusText = (TextView) statusText;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {

            /**
             * Create a server socket and wait for client connections. This
             * call blocks until a connection is accepted from a client
             */
            ServerSocket serverSocket = new ServerSocket(9000);
            Socket client = serverSocket.accept();

            InputStream inputstream = client.getInputStream();
            byte[] buffer =  new byte[1024];                    // 1KiB
            int bytesRead = inputstream.read(buffer, 0, buffer.length);
            serverSocket.close();
            System.out.println( "Got something from client: " + buffer);
            return "Success!";
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
            return "Failed.";
        }
    }

    /**
     * Start activity that can handle the JPEG image
     */
    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            Log.d(TAG, "task: " + result);
        }
    }
}

