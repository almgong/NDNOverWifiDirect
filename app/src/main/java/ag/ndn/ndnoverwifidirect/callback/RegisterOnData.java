package ag.ndn.ndnoverwifidirect.callback;

import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;

import java.util.ArrayList;

import ag.ndn.ndnoverwifidirect.task.FaceCreateTask;
import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;
import ag.ndn.ndnoverwifidirect.utils.WiFiDirectBroadcastReceiver;

/**
 * For when a registration interest /ndn/wifid/register receives a Data packet.
 *
 * Created by allengong on 8/11/16.
 */
public class RegisterOnData implements NDNCallbackOnData {
    private final String TAG = "RegisterOnData";
    private NDNOverWifiDirect mController = NDNOverWifiDirect.getInstance();

    @Override
    public void doJob(Interest interest, Data data) {
        Log.d(TAG, "Received data for registration interest: " + interest.getName().toUri());
        Log.d(TAG, "data: " + data.getContent().toString());

        String[] responseArray = data.getContent().toString().split("\n");
        int numPeers = Integer.parseInt(responseArray[1]);
        int numPrefixes = Integer.parseInt(responseArray[numPeers + 2]);

        // logic for adding peers that group owner knows about, but this devices may not
        for (int i = 0; i < numPeers; i++) {
            Face face = new Face(responseArray[i+2]);
            if(mController.logFace(responseArray[i+2], face)) {

                // NOTE: lazy registration, don't spam registration interests
                // to the other peers right now, leave it up to the upper level application
                // to decide.
                Log.d(TAG, "Logged peer: " + responseArray[i+2]);
            } else {
                // else the face already exists, do nothing
                Log.d(TAG, "Peer is already logged, or is this device, skipping...");
            }
        }

        // logic for registering prefixes of the group owner
        ArrayList<String> prefixesToLog = new ArrayList<String>(numPrefixes);
        for (int i = 0; i < numPrefixes; i++) {
            String currPrefix = responseArray[numPeers+ 2 + 1 + i];
            Log.d(TAG, "adding prefix: " + currPrefix);
            prefixesToLog.add(currPrefix);
        }

        mController.createFace(WiFiDirectBroadcastReceiver.groupOwnerAddress,
                prefixesToLog.toArray(new String[numPrefixes]));

    }
}
