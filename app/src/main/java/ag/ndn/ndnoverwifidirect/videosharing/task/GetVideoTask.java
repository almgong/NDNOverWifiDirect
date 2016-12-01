package ag.ndn.ndnoverwifidirect.videosharing.task;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.security.KeyChain;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import ag.ndn.ndnoverwifidirect.task.SendInterestTask;
import ag.ndn.ndnoverwifidirect.utils.NDNController;
import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;
import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayer;
import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayerBuffer;
import ag.ndn.ndnoverwifidirect.videosharing.callback.GetVideoOnData;

import static android.content.ContentValues.TAG;
import static android.os.Environment.getExternalStorageDirectory;

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

    private final String TAG = "GetVideoTask";

    private boolean stop = false;       // stops probing network for desired video bytes
    private int sequenceNumber = 0;

    private VideoPlayerBuffer buffer;
    private Context mActivity;

    private Face mFace = NDNController.getInstance().getLocalHostFace();

    private OnData onDataReceived;  // OnData callback when data is received
    private SendInterestTask currentSendInterestTask;

    public GetVideoTask(VideoPlayerBuffer buffer, Context activity) {
        this.buffer = buffer;
        this.mActivity = activity;
    }

    public void stop(boolean toStop) {
        stop = toStop;
    }

    // new method
    int windowSize = 3;     // this is a tuneable parameter, depends on number of cores on device
    TransmissionWindow window = new TransmissionWindow(windowSize, this.buffer); // window size of 10
    int taskNumber = window.endIndex;
    Face[] faces;

    // temp
    //FileInputStream fis = null;

    long start;

    @Override
    protected Void doInBackground(String... params) {
        final Face mFace =  new Face("localhost");

        /**
         * Method 1: Daisy Chaining
         *
         * This method is best used on weaker devices, wherein the overhead of
         * creating more threads is NOT ideal.
         */


        /**
         * Data should be in form:
         * FLAG-1-byte | DATA-(SIZE-1)-bytes
         * FLAG is a 1 byte value in the set: VideoPlayer.X_FLAG
         *
         * current seq num could be encoded in the name?? for use with skipping -- actually, we
         * have a flag for seek, header should be extended and have a length (4 byte) using ByteBuffer
         * that says the sequence number? Not sure if this will actually work to seek an mp4...
         */
        // send out interest with sequence number, e.g. /ndn/wifid/movie/[sequenceNumber], seqNum starts at 1
        final String prefix = params[0];
        //final Face mFace = new Face("localhost");
        final int processEventsTimer = 50;
        onDataReceived = new OnData() {

            @Override
            public void onData(Interest interest, Data data) {

                // end processing on prev task (optimization)
                //currentSendInterestTask.setStopProcessing(true);

                byte[] payload = data.getContent().getImmutableArray();

                if (payload[0] == VideoPlayer.EOF_FLAG || stop) { // no more data

                    // there was no data in resp, meaning all data sent for resource
                    // looping stops here
                    buffer.notifyEofReached(); // probably should be done a different way
                    buffer.addToBuffer(VideoPlayerBuffer.EOF_MARKER);    //
                    Log.d(TAG, String.format("All data from %s has been processed from peer, or stop was set.", prefix));

                } else if (payload[0] == VideoPlayer.DATA_FLAG) { // packet contains data
                    System.err.println("Took " + (System.currentTimeMillis()-start + " ms to get packet"));
                    //System.out.println("Size of entire payload is: " + payload.length);

                    // parse custom header
                    byte[] dataToBuffer = Arrays.copyOfRange(payload, 1, payload.length);

                    boolean bufferFull = !(buffer.addToBuffer(dataToBuffer));
                    while (bufferFull) {
                        Log.d(TAG, "Buffer full, trying again...");
                        try {
                            Thread.sleep(VideoPlayerBuffer.POLITENESS_DELAY);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }

                        bufferFull = !(buffer.addToBuffer(dataToBuffer));
                    }

                    // if there was data in this resp, daisy chain for next
                    Log.d(TAG, "Daisy chaining for " + (sequenceNumber+1));

                    start = System.currentTimeMillis();

                    try {
                        final Interest nextInterest = new Interest(new Name(prefix + "/" + (++sequenceNumber)));
                        nextInterest.setMustBeFresh(false);
                        mFace.expressInterest(nextInterest, onDataReceived, new OnTimeout() {
                            @Override
                            public void onTimeout(Interest interest) {
                                Log.e(TAG, "timeout for interest, resending one more time: " + interest.toUri());
                                // try to resend one more time

                                try {
                                    mFace.expressInterest(nextInterest, onDataReceived);
                                } catch (IOException ioe) {
                                    ioe.printStackTrace();
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else if (payload[0] == VideoPlayer.PAUSE_FLAG) {
                    // pause video player
                } else if (payload[0] == VideoPlayer.RESUME_FLAG) {
                    // set video player to play
                } else if (payload[0] == VideoPlayer.SEEK_FLAG) {
                    // seek (or ask for interest with new seq num
                }
            }
        };

        Log.d(TAG, "Sending first interest for video data...");
        start = System.currentTimeMillis();
//        currentSendInterestTask = (SendInterestTask) mController.sendInterest(new Interest(new Name(prefix + "/" + sequenceNumber)),
//                mFace, onDataReceived, processEventsTimer);

        try {
            mFace.expressInterest(new Interest(new Name(prefix + "/" + sequenceNumber)), onDataReceived,
                    new OnTimeout() {
                        @Override
                        public void onTimeout(Interest interest) {
                            Log.e(TAG, "Timeout on interest: " + interest.getName().toString());
                        }
                    });

            while (!stop) {
                Thread.sleep(processEventsTimer);
                mFace.processEvents();
            }
        } catch (IOException ie) {
            ie.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mFace.shutdown();
        }


//        if (true) return null;
//
//        // new method using transmission window -- really only a good option if device is strong enough
//
//        // until either the calling activity, or this class wants to stop
//        sequenceNumber = 0;
//        faces = new Face[windowSize];               // array of faces to use with window
//        for (int i = 0; i < faces.length; i++) {
//            faces[i] = new Face("localhost");
//        }
//        //final String prefix = params[0];
//        while (!stop) {
//            if (buffer.isFull() || window.isFull()) {
//                try {
//
//                    if (buffer.isFull() ) {
//                        Log.d(TAG, "Buffer full, sleep");
//                    } else if (window.isFull()) {
//                        Log.d(TAG, "window is full, sleep");
//                    } else {
//                        Log.d(TAG, "Either video buffer is full or task queue is full, sleep...");
//                    }
//
//                    Thread.sleep(100);
//                    window.sendToBuffer();  // perhaps some delayed packets (or missing segment holding line up)
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    return null;
//                }
//
//                continue;
//            }
//
//            // fill free window spots with tasks (or rather, their "soon-to-be" data
//            for (int i = 0; i < window.numFreeSlots; i++) {
//                final int taskNum = window.endIndex;
//                System.err.println("Starting task: " + taskNum);
//                onDataReceived = new OnData() {
//                    @Override
//                    public void onData(Interest interest, Data data) {
//                        (new GetVideoOnData(taskNum, window, buffer)).doJob(interest, data);
//                    }
//                };
//
//                Log.d(TAG, "Sequence number: " + sequenceNumber);
//                mController.sendInterest(new Interest(new Name(prefix + "/" + sequenceNumber++)), faces[window.endIndex], onDataReceived, 50);
//
//                // update book keeping variables
//                window.endIndex = (window.endIndex + 1)%windowSize;
//                window.numFreeSlots--;
//            }
//
//            System.err.println("Current endIndex: " + window.endIndex + " free: " + window.numFreeSlots);
//
//            try {
//                Thread.sleep(100);      // want a little for data to come back
//                window.sendToBuffer();  // send contiguous bytes to buffer
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//        }
//
        return null;
    }

    /**
     * Inner class representing a TCP-like transmission window, with
     * some specific modifications to be used along with GetVideoTask.
     *
     * The internal implementation somewhat resembles a circular queue, but because
     * we are allowing direct setting of index values, it is a little different.
     */
    public class TransmissionWindow {
        private int startIndex;         // points to first active slot
        private int endIndex;           // used in outer class for book keeping, points to index of first free slot
        private int numFreeSlots;       // number of openings (non-active slots), used primarily in outer class

        //private VideoPlayerBuffer buffer;
        private byte[][] window;

        public TransmissionWindow(int size, VideoPlayerBuffer buffer) {
            window = new byte[size][];
            for (int i = 0; i < window.length; i++) {
                window[i] = null;
            }
            startIndex = 0;
            endIndex = 0;
            numFreeSlots = size;
            //this.buffer = buffer;
        }

        // most likely only ever called in main thread
        public boolean isEmpty() {
            return numFreeSlots == window.length;
        }

        public boolean isFull() {
            return numFreeSlots == 0;
        }

        // only called in main thread (no multithreading support needed)
        public void sendToBuffer() {

            byte[] curr;
            int i = 0;
            while ((curr = window[startIndex]) != null) {
                buffer.addToBuffer(curr);                   // add to buffer
                window[startIndex] = null;                  // null out the data
                startIndex = (startIndex+1)%window.length;  // update start index
                numFreeSlots++;                             // one more free slot opened up

                i++;
            }

            System.err.println("Sent " + i + " packets to buffer");
        }

        // is called in multiple threads, BUT each thread in our case
        // has a unique index assigned to them, so no need to synchronize
        public void setSlot(int index, byte[] data) {
            window[index] = data;
        }

        // flushes remaining items to buffer
        public void flush() {
            while (!isEmpty()) {
                sendToBuffer();
                try {
                    Thread.sleep(500);      // give network some time to fill in remaining bytes
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }

    @Override
    protected void onPostExecute(Void v) {
        // for any UI updates
    }
}
