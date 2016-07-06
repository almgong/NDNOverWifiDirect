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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "Init WifiP2P");
        initWifiP2p();

        Log.d(TAG, "Enumerate intent filer");
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        Log.d(TAG, "Discover peers");
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

    /* initialize manager and receiver for activity */
    private void initWifiP2p() {
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
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
