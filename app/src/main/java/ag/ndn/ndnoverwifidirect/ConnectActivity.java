package ag.ndn.ndnoverwifidirect;

import android.app.Fragment;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.security.KeyChain;

import ag.ndn.ndnoverwifidirect.callback.RegisterOnData;
import ag.ndn.ndnoverwifidirect.fragment.PeerFragment;
import ag.ndn.ndnoverwifidirect.model.Peer;
import ag.ndn.ndnoverwifidirect.utils.IPAddress;
import ag.ndn.ndnoverwifidirect.utils.NDNController;
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
    public static final int CONNECT_SUCCESS = 0;      // marks successfuly connection
    public static Handler mHandler;                   // android handler to trigger UI update


    private WifiP2pManager mManager;
    private Channel mChannel;
    private WiFiDirectBroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;

    private NDNOverWifiDirect mController;      // handler for ndn over wifidirect

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

        mHandler = getHandler();

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


        // TODO this is new
        NDNController.getInstance().recordWifiP2pResources(mManager, mChannel, this);
        Log.d(TAG, "new discoverpeers");
        NDNController.getInstance().startDiscoveringPeers();
        NDNController.getInstance().startProbing();
        System.err.println("Connectacvitivty startProbing called");
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
        mHandler = getHandler();
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
        mHandler = null;
    }

    // returns a handler for connection success
    private Handler getHandler() {
        return new Handler(Looper.getMainLooper()) {

            public void handleMessage(Message msg) {
                if (msg.what == CONNECT_SUCCESS) {
                    Toast.makeText(ConnectActivity.this, "Successfully connected to group.", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /* implement Fragment listener(s) to allow fragment communications */
    @Override
    public void onListFragmentInteraction(Peer peer) {
        // when an item is clicked, this is ran

        mReceiver.connectToPeer(peer);
        Log.d(TAG, "Sending registration interest again on click");

        Face mFace = new Face("localhost");
        Name n = new Name("/ndn/wifid/register/" + WiFiDirectBroadcastReceiver.groupOwnerAddress + "/" +
        WiFiDirectBroadcastReceiver.myAddress + "/" + System.currentTimeMillis());

        Log.d(TAG, "manually (re)sending interest...");
        // on data callback
        OnData onDataCallback = new OnData() {
            @Override
            public void onData(Interest interest, Data data) {
                (new RegisterOnData()).doJob(interest, data);

                // TODO TEMP this is temporary code for notification purposes
                Message msg = new Message();
                msg.what = CONNECT_SUCCESS;
                mHandler.sendMessage(msg);
            }
        };
        Interest interest = new Interest(n);
        interest.setMustBeFresh(true);
        mController.sendInterest(interest, mFace, onDataCallback);
    }
}