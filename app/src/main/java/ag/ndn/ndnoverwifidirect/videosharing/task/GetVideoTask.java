package ag.ndn.ndnoverwifidirect.videosharing.task;

import android.os.AsyncTask;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.security.KeyChain;

import java.util.Arrays;

import ag.ndn.ndnoverwifidirect.task.SendInterestTask;
import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;
import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayer;
import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayerBuffer;

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

    private boolean stop = false;       // stops probing network for desired video bytes
    private int sequenceNumber = 0;

    private VideoPlayerBuffer buffer;
    private NDNOverWifiDirect mController = NDNOverWifiDirect.getInstance();
    private OnData onDataReceived;  // OnData callback when data is received
    private SendInterestTask currentSendInterestTask;

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

        try {
            KeyChain keyChain = NDNOverWifiDirect.getInstance().getKeyChain();
            mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
        } catch (Exception e) {
            e.printStackTrace();
        }

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
        onDataReceived = new OnData() {

            @Override
            public void onData(Interest interest, Data data) {

                // end processing on prev task (optimization)
                currentSendInterestTask.setStopProcessing(true);

                byte[] payload = data.getContent().getImmutableArray();

                if (payload[0] == VideoPlayer.EOF_FLAG || stop) { // no more data

                    // there was no data in resp, meaning all data sent for resource
                    // looping stops here
                    buffer.notifyEofReached(); // probably should be done a different way TODO maybe have a EOF byte[] added to buffer
                    Log.d(TAG, String.format("All data from %s has been processed from peer, or stop was set.", prefix));

                } else if (payload[0] == VideoPlayer.DATA_FLAG) { // packet contains data

                    System.out.println("Size of entire payload is: " + payload.length);

                    // parse custom header
                    byte[] dataToBuffer = Arrays.copyOfRange(payload, 1, payload.length);
                    while ((buffer.addToBuffer(dataToBuffer)) == false) {
                        Log.d(TAG, "Could not add to buffer");
                        try {
                            Thread.sleep(VideoPlayerBuffer.POLITENESS_DELAY);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
                    }

                    // if there was data in this resp, daisy chain for next
                    Log.d(TAG, "Daisy chaining for " + (sequenceNumber+1));
                    currentSendInterestTask = (SendInterestTask) mController.sendInterest(new Interest(new Name(prefix + "/" + (++sequenceNumber))), mFace, onDataReceived, false);

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
        currentSendInterestTask = (SendInterestTask) mController.sendInterest(new Interest(new Name(prefix + "/" + sequenceNumber)), mFace, onDataReceived, false);

        return null;
    }
}
