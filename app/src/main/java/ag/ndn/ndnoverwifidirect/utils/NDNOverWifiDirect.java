package ag.ndn.ndnoverwifidirect.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import ag.ndn.ndnoverwifidirect.services.WiFiDirectBroadcastReceiver;

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
    private static final String TAG = "NDNOverWifiDirect";

    public static NDNOverWifiDirect getInstance() {

        if (singleton == null) {
            singleton = new NDNOverWifiDirect();
        }

        return singleton;
    }

    public void discoverPeers(WifiP2pManager mManager,WifiP2pManager.Channel mChannel) throws Exception {
        //Log.d(TAG, generalStatus().getNfdVersion());
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

                Log.d(TAG, "Success on discovering peers");
            }

            @Override
            public void onFailure(int reasonCode) {

                Log.d(TAG, "Fail discover peers");
            }
        });
    }

}
