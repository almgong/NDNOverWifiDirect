package ag.ndn.ndnoverwifidirect;

import android.app.Fragment;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.security.KeyChain;

import ag.ndn.ndnoverwifidirect.fragment.PeerFragment;
import ag.ndn.ndnoverwifidirect.model.Peer;
import ag.ndn.ndnoverwifidirect.utils.IPAddress;
import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;
import ag.ndn.ndnoverwifidirect.utils.WiFiDirectBroadcastReceiver;

/**
 * Logic flow:
 *
 * init() wifidirect and register this activity with it, attempt to discover peers, and allow
 * user to select via a list fragment, a peer to connect to.
 */
public class ConnectActivity extends AppCompatActivity implements PeerFragment.OnListFragmentInteractionListener {

    private static final String TAG = "ConnectActivity";

    private WifiP2pManager mManager;
    private Channel mChannel;
    private WiFiDirectBroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;

    private NDNOverWifiDirect mController;    // handler for ndn over wifidirect

    private Face testFace = new Face("localhost");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "Init WifiP2P...");
        initWifiP2p();

        Log.d(TAG, "Discovering peers...");
        try {
            mController.initialize();   // initializes this device for NDNOverWifid readiness
            mController.discoverPeers(mManager, mChannel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* initialize manager and receiver for activity */
    private void initWifiP2p() {
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /* implement Fragment listener(s) to allow fragment communications */
    @Override
    public void onListFragmentInteraction(Peer peer) {
        // when an item is clicked, this is ran
        Log.d(TAG, "onListFragmentInteraction() called");

        Toast.makeText(this, "My IP address: " + IPAddress.getLocalIPAddress()
                , Toast.LENGTH_SHORT).show();



        mReceiver.connectToPeer(peer);
        Log.d(TAG, "Sending registration interest again on click");

        Face mFace = new Face("localhost");
        Name n = new Name("/ndn/wifid/register/" + WiFiDirectBroadcastReceiver.groupOwnerAddress + "/" +
        WiFiDirectBroadcastReceiver.myAddress);
        try {
            KeyChain keyChain = mController.getKeyChain();
            mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
        } catch(Exception e) {
            e.printStackTrace();
            return;
        }
        Log.d(TAG, "manually sending interest");
        n = new Name("/ndn/wifid/register/192.168.49.142/192.168.49.1");
        mController.sendInterest(new Interest(n), mFace, new OnData() {
            @Override
            public void onData(Interest interest, Data data) {
                Log.d(TAG, "wooooo");
                System.out.println(data.toString());
            }
        });
    }
}