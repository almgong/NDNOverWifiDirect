package ag.ndn.ndnoverwifidirect.task;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by allengong on 7/26/16.
 */
public class NonGroupOwnerNegotiationTask extends AsyncTask<Void, Void, String> {

    Socket socket = new Socket();
    String host = "";
    int port = 9000;
    @Override
    protected String doInBackground(Void... params) {
        try {
            /**
             * Create a client socket with the host,
             * port, and timeout information.
             */
            socket.bind(null);
            socket.connect((new InetSocketAddress(host, port)), 500);

            // generate data to send
            InetAddress myInetAddress = socket.getInetAddress();
            byte[] myIp = myInetAddress.getAddress();

            /**
             * Create a byte stream from a JPEG file and pipe it to the output stream
             * of the socket. This data will be retrieved by the server device.
             */
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(myIp, 0, myIp.length);

            outputStream.close();

        } catch (FileNotFoundException e) {
            //catch logic
        } catch (IOException e) {
            //catch logic
        }

/**
 * Clean up any open sockets when done
 * transferring or if an exception occurred.
 */
        finally {
            if (socket != null) {
                if (socket.isConnected()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        //catch logic
                    }
                }
            }
        }

        return "OK";
    }

    /**
     * Start activity that can handle the JPEG image
     */
    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            // pass
        }
    }
}
