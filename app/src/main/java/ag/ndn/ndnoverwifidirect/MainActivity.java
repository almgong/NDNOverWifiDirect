package ag.ndn.ndnoverwifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import ag.ndn.ndnoverwifidirect.services.WiFiDirectBroadcastReceiver;
import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;

/**
 * Logic flow:
 *
 * init() wifidirect and register this activity with it -- test to see if device changes cause a
 * event to be caught
 *
 * Create a new Interface that bridges the gap between wifidirect and JNDN (NFDC) -- interface
 * should be analogous to the regular NFDC, but has changes that deals with wifidirect.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private WifiP2pManager mManager;
    private Channel mChannel;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;

    private NDNOverWifiDirect mController;    // handler for ndn over wifidirect

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "Init WifiP2P for this app");
        initWifiP2p();

        Log.d(TAG, "Enumerate intent filer");
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        Log.d(TAG, "Start using NDNOverWifiDirect interface...");
        Log.d(TAG, "Discover peers");
        try {
            mController.discoverPeers(mManager, mChannel);

            // call init() that will run in the background discover peers every so often
            // discoverPeers() should update FIB entries with the correct information
        } catch (Exception e) {
            //
        }

    }

    /* initialize manager and receiver for activity */
    private void initWifiP2p() {
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        mController = NDNOverWifiDirect.getInstance();
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }
}
