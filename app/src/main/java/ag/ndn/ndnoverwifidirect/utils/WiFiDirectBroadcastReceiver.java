package ag.ndn.ndnoverwifidirect.utils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.util.Log;
import android.widget.Toast;

import com.intel.jndn.management.types.FaceStatus;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.util.Blob;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;

import ag.ndn.ndnoverwifidirect.callback.RegisterOnData;
import ag.ndn.ndnoverwifidirect.callback.RegisterOnInterest;
import ag.ndn.ndnoverwifidirect.fragment.PeerFragment;
import ag.ndn.ndnoverwifidirect.model.Peer;
import ag.ndn.ndnoverwifidirect.model.PeerList;
import ag.ndn.ndnoverwifidirect.task.FaceCreateTask;
import ag.ndn.ndnoverwifidirect.task.RegisterPrefixTask;
import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;

/**
 * Created by allengong on 7/5/16.
 *
 * Interface to initialize defaults and handle WifiP2P events.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "WifiP2PBR";

    // if you are the group owner
    private boolean isGroupOwner = false;

    private WifiP2pManager mManager;
    private Channel mChannel;
    private Activity mActivity;

    // maps PeerID -> Device
    private HashMap<String, WifiP2pDevice> activePeers;

    // singleton controller to access NFD, etc.
    private NDNOverWifiDirect mController;

    // wifid ip addresses of group owner and self
    public static String groupOwnerAddress = "";
    public static String myAddress = "";

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, Channel channel,
                                       Activity activity) { // was MyWifiActivity
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;

        this.mController = NDNOverWifiDirect.getInstance();

        this.activePeers = new HashMap<>();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

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

            Log.d(TAG, "peers changed!");

            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()
            if (mManager != null) {
                mManager.requestPeers(mChannel, new PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        Log.d(TAG,
                                String.format("woo peers available: %d", peers.getDeviceList().size()));

                        PeerList.resetPeerList();   // todo need to do extra processing to unregister faces
                        int id = 0;

                        // iterate through devices
                        for (WifiP2pDevice device : peers.getDeviceList()) {

                            // no need to check since we cleared the list before this
                            Peer peer = new Peer();
                            peer.setId("" + ++id);
                            peer.setDeviceAddress(device.deviceAddress);
                            peer.setName(device.deviceName);
                            PeerList.addPeer(peer);

                            // store mapping
                            activePeers.put(""+id, device);

                        }

                        PeerFragment.notifyDataChanged();   // tell fragment that list data has changed
                    }
                });
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections

            Log.d(TAG, "p2pconnection changed check");
            if (mManager == null) {
                Log.d(TAG, "mManager is null, skipping...");
                return;
            }

            NetworkInfo networkInfo = (NetworkInfo) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                // We are connected with the other device, request connection
                // info to find group owner IP
                mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                    @Override
                    public void onConnectionInfoAvailable(WifiP2pInfo info) {
                        Log.d(TAG, "connection info is available!!");

                        // group owner address
                        groupOwnerAddress = info.groupOwnerAddress.getHostAddress();

                        // this device's address
                        myAddress = IPAddress.getLocalIPAddress();

                        // Use localhost to express OR handle registration, can use mController.getFaceByUri("localhost") as well
                        Face mFace = new Face("localhost");

                        System.out.println("group owner address " + groupOwnerAddress);


                        Log.d(TAG, "Registering the global registration prefix...");

                        // register the registration prefix if it has not already been registered - all peers must do this
                        // done here to ensure WifiD IP exists
                        if (!mController.getRegistrationPrefixComplete()) {

                            Log.d(TAG, "Registering the registration prefix...");
                            try {
                                KeyChain keyChain = mController.getKeyChain();
                                mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
                            } catch (Exception e) {
                                e.printStackTrace();
                                return;
                            }

                            // all devices will register this prefix(/ndn/wifid/register/xxx.xxx.xxx) for basic info exchange
                            String prefixToRegister = "/ndn/wifid/register/" + IPAddress.getLocalIPAddress();
                            OnInterestCallback cb = new OnInterestCallback() {
                                @Override
                                public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
                                    Log.d(TAG, "On Global registration interest!!!");
                                    (new RegisterOnInterest()).doJob(prefix, interest, face, interestFilterId,
                                            filter);
                                }
                            };

                            mController.registerPrefix(mFace, prefixToRegister, cb, true, 5000);
                            mController.setRegistrationPrefixComplete(true);
                            Log.d(TAG, "Registering registration prefix complete.");

                        } else {
                            Log.d(TAG, "Registration prefix already registered, skipping...");
                        }

                        // move on to etc.

                        // After the group negotiation, we can determine the group owner.
                        if (info.groupFormed && info.isGroupOwner) {
                            // Do whatever tasks are specific to the group owner.
                            // One common case is creating a server thread and accepting
                            // incoming connections.

                            Log.d(TAG, "I am the group owner");
                            isGroupOwner = true;

                            // for testing
                            mController.addPrefixHandled("/ndn/wifid/big-buck-bunny");

                        } else if (info.groupFormed) {
                            // The other device acts as the client. In this case,
                            // you'll want to create a client thread that connects to the group
                            // owner.
                            Log.d(TAG, "I am not the group owner, and my ip is: " +
                                    myAddress);

                            Log.d(TAG, "handling dummy prefix at /ndn/wifid/megaman/8/22");
                            mController.addPrefixHandled("/ndn/wifid/megaman/8/28");

                            try {
                                // log initial faces, of group owner
                                mController.logFace(groupOwnerAddress, new Face(groupOwnerAddress));

                                Log.d(TAG, "Successfully logged direct face to group owner");

                                // recall mFace is defined above

                                // create a face with the registration prefix pointing to the group owner
                                String[] prefixesToRegister = {"/ndn/wifid/register/" + groupOwnerAddress};
                                mController.createFace(groupOwnerAddress, prefixesToRegister);

                                // on data callback
                                OnData onDataCallback = new OnData() {
                                    @Override
                                    public void onData(Interest interest, Data data) {
                                        (new RegisterOnData()).doJob(interest, data);
                                    }
                                };

                                // send registration interest to group owner
                                mController.sendInterest(new Interest(new Name("/ndn/wifid/register/" +
                                                groupOwnerAddress + "/" + myAddress)),
                                        mFace, onDataCallback);

                                Log.d(TAG, "Registration Interest sent.");

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing

            Log.d(TAG, "wifi state changed check");
        }
    }

    // connects to a peer
    public void connectToPeer(Peer p) {
        connect(activePeers.get(p.getId()));
    }

    private void connect(WifiP2pDevice peerDevice) {

        // config
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = peerDevice.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        // attempt to connect
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(mActivity, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}


