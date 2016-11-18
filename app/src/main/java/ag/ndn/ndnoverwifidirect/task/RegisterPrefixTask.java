package ag.ndn.ndnoverwifidirect.task;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.intel.jndn.management.Nfdc;
import com.intel.jndn.management.types.FaceStatus;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.ForwardingFlags;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnRegisterSuccess;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.util.List;

import ag.ndn.ndnoverwifidirect.utils.NDNController;
import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;
import ag.ndn.ndnoverwifidirect.utils.NfdcHelper;

/**
 * Provides ability to register one or more prefixes to the specified face.
 * Created by allengong on 7/31/16.
 */
public class RegisterPrefixTask extends AsyncTask<String, Void, Integer> {

    private final String TAG = "RegisterPrefixTask";

    private NDNController mController = NDNController.getInstance();

    private Face mFace;
    private OnInterestCallback onInterestCallback;

    private String prefixToRegister;
    private boolean mStopProcessing, handleForever;

    private long processEventsTimer = 500;  // by default, every half second, process events

    public RegisterPrefixTask(Face f, String prefix, OnInterestCallback cb, boolean forever) {
        this.mFace = f;
        this.prefixToRegister = prefix;
        this.onInterestCallback = cb;
        this.handleForever = forever;
        this.mStopProcessing = false;   // default
    }

    public void setProcessEventsTimer(long repeat) {
        this.processEventsTimer = repeat;
    }
    public void setStopProcessing(boolean stop) { this.mStopProcessing = stop; }

    // hardcoded responses for now
    @Override
    protected Integer doInBackground(String... params) {
        try {
//            // keychain business
//            KeyChain keyChain = mController.getKeyChain();
//            mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());

            // allow child inherit
            ForwardingFlags flags = new ForwardingFlags();
            flags.setChildInherit(true);

           long prefixId = mFace.registerPrefix(new Name(this.prefixToRegister),
                    this.onInterestCallback,
                    new OnRegisterFailed() {
                        @Override
                        public void onRegisterFailed(Name prefix) {
                            Log.e(TAG, "Error on registering face task");
                        }
                    }, new OnRegisterSuccess() {
                        @Override
                        public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
                            Log.d(TAG, "Prefix registered successfully: " + prefixToRegister);
                        }
                    },
                    flags);

            // Keep precessing events on the face (necessary)
            while (!mStopProcessing || handleForever) {  // should last forever
                mFace.processEvents();
                Thread.sleep(processEventsTimer);  // every x (e.g. 1500) milliseconds, modulate as needed
            }

            // TODO might no longer be appropriate
//            mFace.removeRegisteredPrefix(prefixId);
//            mFace.shutdown();       // above does not work...

            Log.d(TAG, "No longer handling: " + prefixToRegister);


        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}
