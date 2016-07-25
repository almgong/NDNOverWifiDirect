package ag.ndn.ndnoverwifidirect.utils;

import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.intel.jndn.management.ManagementException;

import net.named_data.jndn_xx.util.FaceUri;

import java.util.HashSet;
import java.util.Set;

/**
 * Interface specification for the family of classes that
 * encapsulate WifiDirect logic under the familiar NFD
 * interface.
 *
 * Created by allengong on 7/11/16.
 */
public class NDNOverWifiDirect extends NfdcHelper {

    // prevents instantiation
    private NDNOverWifiDirect() {}

    // singleton
    private static NDNOverWifiDirect singleton;

    // members
    private static final String TAG = "NDNOverWifiDirect";
    private Set<String> activeFaceUris = new HashSet<String>(); // O(1) access to registered faces

    public static NDNOverWifiDirect getInstance() {

        if (singleton == null) {
            singleton = new NDNOverWifiDirect();
        }

        return singleton;
    }

    // checks for peers, if new peers then broadcast will be sent for PEERS_CHANGED
    public void discoverPeers(WifiP2pManager mManager,WifiP2pManager.Channel mChannel) throws Exception {
        //Log.d(TAG, generalStatus().getNfdVersion());
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

                Log.d(TAG, "Success on discovering peers");
            }

            @Override
            public void onFailure(int reasonCode) {

                Log.d(TAG, "Fail discover peers, reasoncode: " + reasonCode);
            }
        });
    }


    // overrides

    @Override
    public int
    faceCreate(String faceUri) throws ManagementException, FaceUri.Error, FaceUri.CanonizeError {

        if (!activeFaceUris.contains(faceUri)) {
            activeFaceUris.add(faceUri);
        }

        return super.faceCreate(faceUri);
    }

}
