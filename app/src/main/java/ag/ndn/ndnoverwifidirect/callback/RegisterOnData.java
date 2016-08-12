package ag.ndn.ndnoverwifidirect.callback;

import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;

/**
 * For when a registration interest /ndn/wifid/register receives a Data packet.
 *
 * Created by allengong on 8/11/16.
 */
public class RegisterOnData implements NDNCallbackOnData {
    private final String TAG = "RegisterOnData";

    @Override
    public void doJob(Interest interest, Data data) {
        Log.d(TAG, "Received data for registration interest: " + interest.getName().toUri());
        Log.d(TAG, "data: " + data.getContent().toString());
    }
}
