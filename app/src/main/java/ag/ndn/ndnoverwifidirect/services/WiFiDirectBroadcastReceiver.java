package ag.ndn.ndnoverwifidirect.services;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;

/**
 * Created by allengong on 7/5/16.
 *
 * Interface to initialize defaults and handle WifiP2P events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "WifiP2PBR";

    private WifiP2pManager mManager;
    private Channel mChannel;
    private Activity mActivity;
    private PeerListListener myPeerListListener;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
                                       Activity activity) { // was MyWifiActivity
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity

            Log.d(TAG, "wifi enabled check");
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled

                Log.d(TAG, "WIFI IS ENABLED");
            } else {
                // Wi-Fi P2P is not enabled

                Log.d(TAG, "WIFI IS NOT ENABLED");
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers

            Log.d(TAG, "peers changed check");

            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (mManager != null) {
                mManager.requestPeers(mChannel, new PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        Log.d(TAG,
                                String.format("woo peers available: %d", peers.getDeviceList().size()));

                    }
                });
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections

            Log.d(TAG, "p2pconnection changed check");
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing

            Log.d(TAG, "wifi state changed check");
        }
    }
}


