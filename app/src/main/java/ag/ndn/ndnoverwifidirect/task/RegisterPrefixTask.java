package ag.ndn.ndnoverwifidirect.task;

import android.os.AsyncTask;
import android.util.Log;

import com.intel.jndn.management.Nfdc;

import net.named_data.jndn.Face;
import net.named_data.jndn.ForwardingFlags;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnRegisterSuccess;

import ag.ndn.ndnoverwifidirect.utils.IPAddress;
import ag.ndn.ndnoverwifidirect.utils.NDNController;
import ag.ndn.ndnoverwifidirect.utils.WDBroadcastReceiver;

/**
 * Task that provides the ability to register a prefix to the specified face.
 * Given that multiple successive prefix registration calls can fail (NFD timeout),
 * This task will attempt to register prefixes 5 times or until success. Each
 * attempt is separated (e.g. 500ms) to increase chance of registration.
 *
 * As of 12/2016, this class has the added importance of letting NDNController
 * know if its own /localhop prefix is registered.
 *
 * Created by allengong on 7/31/16.
 */
public class RegisterPrefixTask extends AsyncTask<String, Void, Void> {

    private final String TAG = "RegisterPrefixTask";

    private Face mFace;
    private OnInterestCallback onInterestCallback;

    private String prefixToRegister;
    private boolean mStopProcessing, handleForever, isRegisteringOwnLocalhop = false;

    private long processEventsTimer = 500;  // by default, every half second, process events
    private int attemptNum = 1;             // for re-attempting to register a prefix (in case of timeout)
    private final int MAX_ATTEMPTS = 5;     // max retries for above

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

    private void onRegisterFailRetry(Name prefix, final ForwardingFlags flags) {
        try {
            mFace.registerPrefix(prefix, onInterestCallback,
                    new OnRegisterFailed() {
                        @Override
                        public void onRegisterFailed(Name prefix) {
                            try {
                                Thread.sleep(500);
                            } catch (Exception e) {
                                e.printStackTrace();
                                return;
                            }

                            if (attemptNum <= MAX_ATTEMPTS) {
                                Log.e(TAG, "Error on registering prefix task, attempt: " + attemptNum);
                                attemptNum++;
                                onRegisterFailRetry(prefix, flags);
                            }
                        }
                    }, new OnRegisterSuccess() {
                        @Override
                        public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
                            Log.d(TAG, "Prefix registered successfully: " + prefixToRegister);

                            // if it is the case that we are registering the localhop prefix
                            // for this device, tell Controller that it was successful
                            if (isRegisteringOwnLocalhop) {
                                NDNController.getInstance().setHasRegisteredOwnLocalhop(true);
                            }
                        }
                    },
                    flags);
        } catch (Exception e) {
            Log.e(TAG, "Error in retry " + attemptNum);
            e.printStackTrace();
        }

    }

    @Override
    protected Void doInBackground(String... params) {
        try {
            // precaution, only applicable if registering own /localhop
            if (prefixToRegister.startsWith(NDNController.PROBE_PREFIX)) {
                isRegisteringOwnLocalhop = true;

                // we must have a valid WiFiDirect IP to proceed, sometimes
                // there is a delay in setting it up framework-wise. This is an added precaution.
                int maxAttempts = 5;
                int attempt = 0;
                while(WDBroadcastReceiver.myAddress == null && attempt < maxAttempts) {
                    try {
                        Thread.sleep(1000);
                        WDBroadcastReceiver.myAddress = IPAddress.getLocalIPAddress();
                        Log.d(TAG, "[NULL IP] Registering own localhop attempt: " + (++attempt) +
                                " IP Address is: " + WDBroadcastReceiver.myAddress);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // if it is still null, DO NOT proceed to register
                if (WDBroadcastReceiver.myAddress == null) {
                    Log.e(TAG, "There was an issue with registering own localhop.");
                    return null;
                } else {
                    // we have a valid IP now, use it.
                    prefixToRegister = NDNController.PROBE_PREFIX + "/" +
                            WDBroadcastReceiver.myAddress;
                }
            }

            // allow child inherit
            final ForwardingFlags flags = new ForwardingFlags();
            flags.setChildInherit(true);

            Name prefix = new Name(prefixToRegister);

           mFace.registerPrefix(prefix, onInterestCallback,
                    new OnRegisterFailed() {
                        @Override
                        public void onRegisterFailed(Name prefix) {
                            Log.d(TAG, "Failed to register prefix: " + prefix.toString());
                            onRegisterFailRetry(prefix, flags);
                        }
                    }, new OnRegisterSuccess() {
                        @Override
                        public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
                            Log.d(TAG, "Prefix registered successfully: " + prefixToRegister);

                            // if it is the case that we are registering the localhop prefix
                            // for this device, tell Controller that it was successful
                            if (isRegisteringOwnLocalhop) {
                                NDNController.getInstance().setHasRegisteredOwnLocalhop(true);
                            }
                        }
                    },
                    flags);

            // Keep precessing events on the face (necessary)
            while (!mStopProcessing || handleForever) {  // should last forever
                mFace.processEvents();
                Thread.sleep(processEventsTimer);  // every x (e.g. 1500) milliseconds, modulate as needed
            }

            // unregister the prefix, no longer handled if logic gets to here
            Nfdc.unregister(NDNController.getInstance().getLocalHostFace(), prefix);
            Log.d(TAG, "No longer handling: " + prefixToRegister);


        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
