package ag.ndn.ndnoverwifidirect;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import ag.ndn.ndnoverwifidirect.fragment.PeerFragment;
import ag.ndn.ndnoverwifidirect.model.Peer;
import ag.ndn.ndnoverwifidirect.model.PeerList;
import ag.ndn.ndnoverwifidirect.utils.WiFiDirectBroadcastReceiver;
import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;

/**
 * Logic flow:
 *
 * init() wifidirect and register this activity with it -- test to see if device changes cause a
 * event to be caught
 *
 * Create a new Interface that bridges the gap between wifidirect and JNDN (NFDC) -- interface
 * should be analogous to the regular NFDC, but has changes that deals with wifidirect.
 *
 * TODO: Implement method of updating the list, and attach this logic to WifiDirect peers changed
 * event. And so forth.
 */
public class MainActivity extends AppCompatActivity implements PeerFragment.OnListFragmentInteractionListener {

    private static final String TAG = "MainActivity";

    private WifiP2pManager mManager;
    private Channel mChannel;
    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;

    private NDNOverWifiDirect mController;    // handler for ndn over wifidirect

    private Fragment peerFrag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // handle for the list fragment

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

    /* implement Fragment listener(s) to allow fragment communications */
    @Override
    public void onListFragmentInteraction(Peer peer) {
        // when an item is clicked, this is ran
        Log.d(TAG, "onListFragmentInteraction() called");
        Toast.makeText(this, peer.getId(),Toast.LENGTH_SHORT).show();

        Peer p = new Peer();
        p.setId("id2");
        p.setDeviceAddress("address2");
        PeerList.addPeer(p);
        PeerFragment.notifyDataChanged();
    }
}
