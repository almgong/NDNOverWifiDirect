package ag.ndn.ndnoverwifidirect;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;

import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.security.KeyChain;

import java.util.HashMap;
import java.util.jar.Manifest;

import ag.ndn.ndnoverwifidirect.callback.RegisterOnInterest;
import ag.ndn.ndnoverwifidirect.fragment.PeerFragment;
import ag.ndn.ndnoverwifidirect.model.Peer;
import ag.ndn.ndnoverwifidirect.model.PeerList;
import ag.ndn.ndnoverwifidirect.service.NfdService;
import ag.ndn.ndnoverwifidirect.utils.IPAddress;
import ag.ndn.ndnoverwifidirect.utils.WiFiDirectBroadcastReceiver;
import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;

import static android.app.PendingIntent.getActivity;

/**
 * Logic flow:
 *
 * init() wifidirect and register this activity with it -- test to see if device changes cause a
 * event to be caught
 *
 * Create a new Interface that bridges the gap between wifidirect and JNDN (NFDC) -- interface
 * should be analogous to the regular NFDC, but has changes that deals with wifidirect.
 *
 */
public class MainActivity extends AppCompatActivity implements PeerFragment.OnListFragmentInteractionListener {

    private static final String TAG = "MainActivity";

    private WifiP2pManager mManager;
    private Channel mChannel;
    private WiFiDirectBroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;

    private NDNOverWifiDirect mController;    // handler for ndn over wifidirect

    private Face testFace = new Face("localhost");

    // layout elements
    private Fragment peerFrag;
    //private Button sendRegistrationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // handle for the list fragment

        Log.d(TAG, "Init WifiP2P for this app");
        initWifiP2p();

        Log.d(TAG, "Start using NDNOverWifiDirect interface...");
        Log.d(TAG, "Discover peers");
        try {
            mController.addPrefixHandled("/ndn/wifid/lord-of-the-rings/8/22");
            mController.initialize();   // initializes this device for NDNOverWifid readiness
            mController.discoverPeers(mManager, mChannel);

            // call init() that will run in the background discover peers every so often
            // discoverPeers() should update FIB entries with the correct information
        } catch (Exception e) {
            e.printStackTrace();
        }


        // layout elements

       //  this button is temporary and for debugging
//        sendRegistrationButton = (Button) findViewById(R.id.send_reg_interest_button);
//
//        sendRegistrationButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mController.sendInterest(new Interest(new Name("/ndn/wifid/"
//                        + IPAddress.getDottedDecimalIP(IPAddress.getLocalIPAddress()))), new Face(WiFiDirectBroadcastReceiver.groupOwnerAddress));
//
//                Toast.makeText(MainActivity.this, "Sent interest via button", Toast.LENGTH_SHORT);
//            }
//        });

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
        //bindNfdService();
        registerReceiver(mReceiver, mIntentFilter);
    }

    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        super.onPause();
        //unbindNfdService();
        //Log.d(TAG, "UNBIND NFD SERVICES");
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbindNfdService();
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

        mController.sendInterest(new Interest(n), mFace);
    }
}