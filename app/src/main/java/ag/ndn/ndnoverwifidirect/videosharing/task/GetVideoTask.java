package ag.ndn.ndnoverwifidirect.videosharing.task;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;

import java.io.FileInputStream;

import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;
import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayerBuffer;
import ag.ndn.ndnoverwifidirect.videosharing.callback.GetVideoOnData;

import static android.content.ContentValues.TAG;

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
    private int sequenceNumber = 0;

    private VideoPlayerBuffer buffer;
    private NDNOverWifiDirect mController = NDNOverWifiDirect.getInstance();
    private OnData onDataReceived;  // OnData callback when data is received

    public GetVideoTask(VideoPlayerBuffer buffer) {
        this.buffer = buffer;
    }

    public void stop(boolean toStop) {
        stop = toStop;
    }

    @Override
    protected Void doInBackground(String... params) {
        boolean bufferFull = false;
        int bytesRead = 0;
        final Face mFace = new Face("localhost");

        //while (!stop) {

//            // process and store content into a byte[]
//            try {
//                //Thread.sleep(500); // sleep before trying to ask network again
//                int size = VideoPlayerBuffer.MAX_ITEM_SIZE;
//                FileInputStream fis = new FileInputStream(Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_MOVIES + "/big_buck_bunny.mp4");
//                byte[] bytes = new byte[size];
//
//                while (!bufferFull && ((bytesRead = (fis.read(bytes, 0, size))) != -1)) {
//                    bufferFull = !(buffer.addToBuffer(bytes));
//                    bytes = new byte[size];
//
//                    // simulate network
//                    System.err.println("network delay...");
//                    Thread.sleep(new Double(Math.random()*750).intValue());
//                }
//
//                if (bytesRead == -1) {
//                    buffer.notifyEofReached();
//                }
//            } catch(Exception e) {
//                e.printStackTrace();
//                return null;
//            }


            /**
             * Data should be in form: first byte is 0 or 1, 0 means no more data, 1 means data present
             * then next 2 bytes is data size (max is 2^16) in bytes,
             * followed by the data
             *
             */
            // send out interest with sequence number, e.g. /ndn/wifid/movie/[sequenceNumber], seqNum starts at 1
            final String prefix = params[0];
            onDataReceived = new OnData() {

                @Override
                public void onData(Interest interest, Data data) {
                    // if there was data in this resp, daisy chain for next
                    if (data.getContent().getImmutableArray()[0] == (byte)1 && !stop) {
                        (new GetVideoOnData(buffer)).doJob(interest, data); // blocks here until data is added to buffer

                        Log.d(TAG, "Daisy chaining for " + (sequenceNumber+1));
                        mController.sendInterest(new Interest(new Name(prefix + "/" + (++sequenceNumber))), mFace, onDataReceived);
                    } else {
                        // there was no data in resp, meaning all data sent for resource
                        // looping stops here
                        Log.d(TAG, String.format("All data from %s has been processed from peer, or stop was set.", prefix));
                    }

                }
            };

            Log.d(TAG, "Sending first interest for video data...");
            mController.sendInterest(new Interest(new Name(prefix + "/" + sequenceNumber)), mFace, onDataReceived);

            // attempt to add byte to VideoPlayerBuffer

            // if byte was not added, loop until it is, sleeping for 500 ms each try

            // once it is added, stop loop and go back to while condition
        //}


        return null;
    }
}
