package ag.ndn.ndnoverwifidirect.callback;

import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;

import ag.ndn.ndnoverwifidirect.utils.NDNController;

/**
 * Handle OnData events for probe interests.
 *
 * Created by allengong on 11/12/16.
 */
public class ProbeOnData implements NDNCallbackOnData {

    private static final String TAG = "ProbeOnData";
    private NDNController mController = NDNController.getInstance();

    @Override
    public void doJob(Interest interest, Data data) {
        Log.d(TAG, "Got data for interest: " + interest.toString());
        Log.d(TAG, "Data is in form: " + data.getContent().toString());
        // parse the data, update controller prefix map

        // registerPrefixTask is ready to use, via mController.registerPrefix(...)

    }
}
