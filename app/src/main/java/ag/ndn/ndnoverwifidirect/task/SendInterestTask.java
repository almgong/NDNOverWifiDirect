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
 * May need to create a thread in case the async task takes too long (waiting for data)
 * Created by allengong on 7/29/16.
 */
public class SendInterestTask extends AsyncTask<Void, Void, Void> {
    private final String TAG = "SendInterestTask";

    private Interest interest;
    private Face mFace;
    private OnData onDataCallback;

    private boolean mStopProcessing = false;

    // minimal constructor
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

    // constructor with ability to customize callback - probably will need to create a custom callback object/class
    public SendInterestTask(Interest interest, Face face, OnData onData) {
        this.interest = interest;
        this.interest.setInterestLifetimeMilliseconds(10000);
        this.mFace = face;
        this.onDataCallback = onData;
    }

    // used to stop processing on a given face+interest
    public void setStopProcessing(boolean flag) {
        this.mStopProcessing = flag;
    }

    protected Void doInBackground(Void... stuff) {

        Log.d(TAG, "attempting to send out interest " + interest.getName().toUri());

        //Face mFace = new Face("localhost"); // may or may not be correct

        try {

            KeyChain keyChain = NDNOverWifiDirect.getInstance().getKeyChain();
            mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());

            // will want to take as input, perhaps in constructor, a callback to pass
            // into expressInterest() -- for now just print that you got data back
            mFace.expressInterest(this.interest, this.onDataCallback);

            // Keep precessing events on the face (necessary)
            while (!mStopProcessing) {  // should last until the response comes back
                mFace.processEvents();
                Thread.sleep(100);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
