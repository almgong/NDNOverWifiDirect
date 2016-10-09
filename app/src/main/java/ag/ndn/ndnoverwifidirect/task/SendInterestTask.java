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
    private boolean setKeyChain = true;         // by default, we will set the keychain using defaults

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

    public void setGenerateKeyChain(boolean setKeyChain) {
        this.setKeyChain = setKeyChain;
    }

    // used to stop processing on a given face+interest
    public void setStopProcessing(boolean flag) {
        this.mStopProcessing = flag;
    }

    protected Void doInBackground(Void... stuff) {

        Log.d(TAG, "attempting to send out interest " + interest.getName().toUri());

        //Face mFace = new Face("localhost"); // may or may not be correct

        try {

//            if (setKeyChain) {
//                KeyChain keyChain = NDNOverWifiDirect.getInstance().getKeyChain();
//                mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
//            }

            // simple
            mFace.expressInterest(this.interest, this.onDataCallback);

            // Keep precessing events on the face (necessary) - 10 times max
            int counter = 0;
            int numTries = 10000/1000 - 1; // TODO make this customizable by cstr
            while (!mStopProcessing) {  // should last until the response comes back
                mFace.processEvents();
                if (counter++ > numTries) {
                    //Log.d(TAG, "Stop processing on face listening for interest: " + interest.getName().toUri());
                    break;
                }
                Thread.sleep(500);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
