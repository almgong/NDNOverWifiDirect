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
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import ag.ndn.ndnoverwifidirect.ConnectActivity;
import ag.ndn.ndnoverwifidirect.callback.GenericCallback;

import static android.content.ContentValues.TAG;

/**
 * WiFi Direct Broadcast receiver. Does not deviate too much
 * from the standard broadcast receivers seen in the official
 * android docs, will attempt to connect to all peers in range.
 *
 * Once connected to a group, we will register a face to the GO
 * if we are non-GO.
 *
 * Created by allengong on 11/5/16.
 */

public class WDBroadcastReceiver extends BroadcastReceiver {

    public static String groupOwnerAddress;
    public static String myAddress;

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private Activity mActivity;

    private NDNController mController;

    private HashSet<String> connectedPeers;
    private int maxPeers = 5;// max number of peers per group

    public WDBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       Activity activity) { // was MyWifiActivity
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;

        this.mController = NDNController.getInstance();

        this.connectedPeers = new HashSet<>();
    }

    public List<String> getConnectedPeers() {
        ArrayList<String> ret = new ArrayList<>(connectedPeers.size());

        for (String peerMacAddr : connectedPeers) {
            ret.add(peerMacAddr);
        }

        return ret;
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
                mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        Log.d(TAG,
                                String.format("Peers available: %d", peers.getDeviceList().size()));

                        // TODO some diff algorithm to remove peers from list

                        // attempt to connect to all devices in range up to the max:
                        for (WifiP2pDevice device : peers.getDeviceList()) {
                            if (!connectedPeers.contains(device.deviceAddress) &&
                                    connectedPeers.size() <= maxPeers) {

                                connect(device);
                            }
                        }
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

                        // this device's address, which is now available
                        myAddress = IPAddress.getLocalIPAddress();

                        if (!mController.getHasRegisteredOwnLocalhop()) {
                            // do so now
                            mController.registerOwnLocalhop();
                            Log.d(TAG, "registerOwnLocalhop() called...");
                        }

                        System.out.println("group owner address " + groupOwnerAddress);


                        // After the group negotiation, we can determine the group owner.
                        if (info.groupFormed && info.isGroupOwner) {
                            // Do whatever tasks are specific to the group owner.
                            // One common case is creating a server thread and accepting
                            // incoming connections.

                            Log.d(TAG, "I am the group owner... do nothing.");
                            mController.setIsGroupOwner(true);

                        } else if (info.groupFormed) {
                            // The other device acts as the client. In this case,
                            // you'll want to create a client thread that connects to the group
                            // owner.
                            Log.d(TAG, "I am not the group owner, and my ip is: " +
                                    myAddress);

                            mController.setIsGroupOwner(false);

                            // skip if already part of this group
                            if (mController.getFaceIdForPeer(groupOwnerAddress) != -1) {
                                return;
                            }

                            // create a callback that will register the /localhop/wifidirect/<go-addr> prefix
                            GenericCallback cb = new GenericCallback() {
                                @Override
                                public void doJob() {
                                    Log.d(TAG, "registering " + NDNController.PROBE_PREFIX + "/" + groupOwnerAddress);
                                    String[] prefixes = new String[1];
                                    prefixes[0] = NDNController.PROBE_PREFIX + "/" + groupOwnerAddress;
                                    mController.ribRegisterPrefix(mController.getFaceIdForPeer(groupOwnerAddress),
                                            prefixes);
                                }
                            };

                            // create UDP face towards GO, with callback to register /localhop/... prefix
                            mController.createFace(groupOwnerAddress, NDNController.URI_TCP_PREFIX, cb);

                            (Toast.makeText(mActivity, "Successfully connected to group.", Toast.LENGTH_LONG)).show();
                        }
                    }
                });
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            Log.d(TAG, "wifi state changed check");
        }
    }

    private void connect(final WifiP2pDevice peerDevice) {

        // config
        final WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = peerDevice.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        // attempt to connect
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                // logic goes to onReceive()
                Log.d(TAG, "Connect successful for: " + config.deviceAddress);
                connectedPeers.add(config.deviceAddress);
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(mActivity, "Connect failed for " + config.deviceAddress,
                        Toast.LENGTH_SHORT).show();

                // remove log of this device from connectedPeers
                connectedPeers.remove(config.deviceAddress);
            }
        });
    }

    /**
     * Resets all persistent state accumulated through
     * normal operation.
     */
    public void resetState() {
        connectedPeers.clear();
        myAddress = null;
        groupOwnerAddress = null;
    }
}
