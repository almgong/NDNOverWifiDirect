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
import net.named_data.jndn.Name;

import java.util.HashMap;
import java.util.jar.Manifest;

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
            mController.discoverPeers(mManager, mChannel);

            // call init() that will run in the background discover peers every so often
            // discoverPeers() should update FIB entries with the correct information
        } catch (Exception e) {
            //
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
    }


    // temp

//    /**
//     * Method that binds the current activity to the NfdService.
//     */
//    private void
//    bindNfdService() {
//        if (!m_isNfdServiceConnected) {
//            // Bind to Service
//            this.bindService(new Intent(this, NfdService.class),
//                    m_ServiceConnection, Context.BIND_AUTO_CREATE);
//            Log.d(TAG, "MainFragment::bindNfdService()");
//        }
//    }
//
//    /**
//     * Method that unbinds the current activity from the NfdService.
//     */
//    private void
//    unbindNfdService() {
//        if (m_isNfdServiceConnected) {
//            // Unbind from Service
//            this.unbindService(m_ServiceConnection);
//            m_isNfdServiceConnected = false;
//
//            Log.d(TAG, "MainFragment::unbindNfdService()");
//        }
//    }
//
//    /**
//     * Client ServiceConnection to NfdService.
//     */
//    private final ServiceConnection m_ServiceConnection = new ServiceConnection() {
//        @Override
//        public void
//        onServiceConnected(ComponentName className, IBinder service) {
//            // Establish Messenger to the Service
//            m_nfdServiceMessenger = new Messenger(service);
//            m_isNfdServiceConnected = true; // onServiceConnected runs on the main thread
//
//            // Check if NFD Service is running
//            try {
//                boolean shouldServiceBeOn = m_sharedPreferences.getBoolean(PREF_NFD_SERVICE_STATUS, true);
//
//                Message msg = Message.obtain(null, shouldServiceBeOn ? NfdService.START_NFD_SERVICE : NfdService.STOP_NFD_SERVICE);
//                msg.replyTo = m_clientMessenger;
//                m_nfdServiceMessenger.send(msg);
//            } catch (RemoteException e) {
//                // If Service crashes, nothing to do here
//                Log.d(TAG, "onServiceConnected(): " + e);
//            }
//
//            Log.d(TAG, "m_ServiceConnection::onServiceConnected()");
//        }
//
//        @Override
//        public void
//        onServiceDisconnected(ComponentName componentName) {
//            // In event of unexpected disconnection with the Service; Not expecting to get here.
//            Log.d(TAG, "m_ServiceConnection::onServiceDisconnected()");
//
//            // Update UI
//            //setNfdServiceDisconnected();
//
//            m_isNfdServiceConnected = false; // onServiceDisconnected runs on the main thread
//            //m_handler.postDelayed(m_retryConnectionToNfdService, 1000);
//        }
//    };
//
//    /**
//     * Client Message Handler.
//     *
//     * This handler is used to handle messages that are being sent back
//     * from the NfdService to the current application.
//     */
//    private class ClientHandler extends Handler {
//        @Override
//        public void handleMessage(Message msg) {
//            switch (msg.what) {
//                case NfdService.NFD_SERVICE_RUNNING:
//                    //setNfdServiceRunning();
//                    Log.d(TAG, "ClientHandler: NFD is Running.");
//
//                    //m_handler.postDelayed(m_statusUpdateRunnable, 500);
//                    break;
//
//                case NfdService.NFD_SERVICE_STOPPED:
//                    //setNfdServiceStopped();
//                    Log.d(TAG, "ClientHandler: NFD is Stopped.");
//                    break;
//
//                default:
//                    super.handleMessage(msg);
//                    break;
//            }
//        }
//    }
//
//    /** Flag that marks that application is connected to the NfdService */
//    private boolean m_isNfdServiceConnected = false;
//
//    /** Client Message Handler */
//    private final Messenger m_clientMessenger = new Messenger(new ClientHandler());
//
//    /** Messenger connection to NfdService */
//    private Messenger m_nfdServiceMessenger = null;
//
//    private SharedPreferences m_sharedPreferences;
//
//    private static final String PREF_NFD_SERVICE_STATUS = "NFD_SERVICE_STATUS";
}