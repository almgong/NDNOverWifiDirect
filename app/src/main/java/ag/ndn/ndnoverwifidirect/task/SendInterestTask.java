package ag.ndn.ndnoverwifidirect.task;

import android.os.AsyncTask;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.security.KeyChain;

import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;

/**
 * May need to create a thread in case the async task takes too long (waiting for data). This
 * task should really be used for "one-off" interests, rather than sending multiple, time-dependent
 * interests.
 *
 * Created by allengong on 7/29/16.
 */
public class SendInterestTask extends AsyncTask<Void, Void, Void> {
    private final String TAG = "SendInterestTask";

    private Interest interest;
    private Face mFace;
    private OnData onDataCallback;

    private boolean mStopProcessing = false;
    private int processEventsTimer = 100;       // ms
    // minimal constructor, useful really for debugging or sending interests with no care about data
    public SendInterestTask(Interest interest, Face face) {
        this.interest = interest;
        this.interest.setInterestLifetimeMilliseconds(10000);
        this.mFace = face;
        this.onDataCallback = new OnData() {
            @Override
            public void onData(Interest interest, Data data) {
                Log.d(TAG,"Received data for interest: " + interest.getName().toUri());
                Log.d(TAG, "data: " + data.getContent().toString());
                mStopProcessing = true;
            }
        };
    }

    // constructor with ability to customize callback
    public SendInterestTask(Interest interest, Face face, OnData onData) {
        this.interest = interest;
        this.interest.setInterestLifetimeMilliseconds(10000);
        this.mFace = face;
        this.onDataCallback = onData;
    }

    public void setProcessEventsTimer(int timer) {
        this.processEventsTimer = timer;
    }

    // used to stop processing on a given face+interest
    public void setStopProcessing(boolean flag) {
        this.mStopProcessing = flag;
    }

    protected Void doInBackground(Void... stuff) {

        Log.d(TAG, "attempting to send out interest " + interest.getName().toUri());

        try {

            // simple
            mFace.expressInterest(this.interest, this.onDataCallback);

            // Keep precessing events on the face (necessary) - 10 times max
            int counter = 0;
            int numTries = 5000/processEventsTimer; // 5 seconds overall
            while (!mStopProcessing) {  // should last until the response comes back
                mFace.processEvents();
                if (counter++ > numTries) {
                    //Log.d(TAG, "Stop processing on face listening for interest: " + interest.getName().toUri());
                    break;
                }
                Thread.sleep(processEventsTimer);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
