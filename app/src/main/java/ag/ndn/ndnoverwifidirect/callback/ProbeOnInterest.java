package ag.ndn.ndnoverwifidirect.callback;

import android.util.Log;

import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;

/**
 * Created by allengong on 11/12/16.
 */
public class ProbeOnInterest implements NDNCallBackOnInterest {

    private static final String TAG = "ProbeOnInterest";

    @Override
    public void doJob(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
        Log.d(TAG, "Got an interest for: " + prefix.toString());

        // if not logged (a face created for this probing peer), should then create a face (mainly for GO)

        // enumerate RIB, look for all /ndn/wifidirect/* data prefixes, return to user as described in slides
    }
}
