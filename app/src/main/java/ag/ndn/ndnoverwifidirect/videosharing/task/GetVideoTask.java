package ag.ndn.ndnoverwifidirect.videosharing.task;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.FileInputStream;

import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayerBuffer;

/**
 * Asynchronous task that will repeatedly query network
 * for video bytes identified by the input ndn prefix.
 *
 * Instantiate as a new GetVideoTask(buffer);
 * And stop any time by calling the stop(True) method.
 *
 * Created by allengong on 9/29/16.
 */

public class GetVideoTask extends AsyncTask<String, Void, Void> {

    private boolean stop = false;
    private VideoPlayerBuffer buffer;

    public GetVideoTask(VideoPlayerBuffer buffer) {
        this.buffer = buffer;
    }

    public void stop(boolean toStop) {
        stop = toStop;
    }

    @Override
    protected Void doInBackground(String... params) {
        boolean bufferFull = false;
        int sequenceNumber = 0;     // sequence number of packet, starts at 0
        int offset = 0, bytesRead = 0;

        while (!stop) {

            // send out interest with sequence number 0, e.g. /ndn/wifid/movie/[sequenceNumber]

            // process and store content into a byte[]
            try {
                //Thread.sleep(500); // sleep before trying to ask network again
                int size = VideoPlayerBuffer.MAX_ITEM_SIZE;
                FileInputStream fis = new FileInputStream(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_MOVIES + "/big_buck_bunny.mp4");
                byte[] bytes = new byte[size];

                while (!bufferFull && ((bytesRead = (fis.read(bytes, 0, size))) != -1)) {
                    bufferFull = !(buffer.addToBuffer(bytes));
                    bytes = new byte[size];

                    // simulate network
                    System.err.println("network delay...");
                    Thread.sleep(new Double(Math.random()*750).intValue());

                    offset += bytesRead;
                }

                if (bytesRead == -1) {
                    buffer.notifyEofReached();
                }
            } catch(Exception e) {
                e.printStackTrace();
                return null;
            }

            // attempt to add byte to VideoPlayerBuffer

            // if byte was not added, loop until it is, sleeping for 500 ms each try

            // once it is added, stop loop and go back to while condition
        }


        return null;
    }
}
