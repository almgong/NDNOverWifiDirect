package ag.ndn.ndnoverwifidirect;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import ag.ndn.ndnoverwifidirect.fragment.ConnectFragment;
import ag.ndn.ndnoverwifidirect.utils.NDNController;
import ag.ndn.ndnoverwifidirect.utils.WDBroadcastReceiver;

/**
 * Logic flow:
 *
 * init() wifidirect and register this activity with it, attempt to discover peers, and allow
 * user to select via a list fragment, a peer to connect to.
 */
public class ConnectActivity extends AppCompatActivity implements ConnectFragment.OnFragmentInteractionListener {

    private static final String TAG = "ConnectActivity";
    public static final int CONNECT_SUCCESS = 0;      // marks successfuly connection
    public static Handler mHandler;                   // android handler to trigger UI update

    private WifiP2pManager mManager;
    private Channel mChannel;
    private WDBroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;

    private ConnectFragment mFragment;
    private String mFragmentTag = "connectFragmentTag";

    public WDBroadcastReceiver getReceiver() {
        return mReceiver;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NDNController.getInstance().setWifiDirectContext(this);
        mHandler = getHandler();

        // if there is saved state, don't recreate the fragment
        if (savedInstanceState == null) {
            Log.d(TAG, "There was no state to restore.");
            mFragment = ConnectFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.connectLayout, mFragment, mFragmentTag)
                    .commit();
        } else {
            Log.d(TAG, "There was STATE to restore!!");
            mFragment = (ConnectFragment) getSupportFragmentManager()
                    .findFragmentByTag(mFragmentTag);
        }
    }

    /* initialize manager and receiver for activity */
    /* moved to WDBroadcastReceiverService */
    @Deprecated
    private void initWifiP2p() {
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        //mReceiver = new WDBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        NDNController.getInstance().recordWifiP2pResources(mManager, mChannel);
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        //registerReceiver(mReceiver, mIntentFilter);
        mHandler = getHandler();
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(mReceiver);
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
    public void onFragmentInteraction(Uri uri) {
        Log.d(TAG, "Interaction with connectFragment");
    }
}