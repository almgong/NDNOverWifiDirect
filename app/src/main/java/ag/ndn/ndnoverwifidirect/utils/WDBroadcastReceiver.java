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
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.intel.jndn.management.Nfdc;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;

import java.util.HashMap;

import ag.ndn.ndnoverwifidirect.ConnectActivity;
import ag.ndn.ndnoverwifidirect.callback.NDNCallbackOnData;
import ag.ndn.ndnoverwifidirect.callback.RegisterOnData;
import ag.ndn.ndnoverwifidirect.callback.RegisterOnInterest;
import ag.ndn.ndnoverwifidirect.fragment.PeerFragment;
import ag.ndn.ndnoverwifidirect.model.Peer;
import ag.ndn.ndnoverwifidirect.model.PeerList;
import ag.ndn.ndnoverwifidirect.task.SendInterestTask;

import static ag.ndn.ndnoverwifidirect.utils.WiFiDirectBroadcastReceiver.groupOwnerAddress;
import static ag.ndn.ndnoverwifidirect.utils.WiFiDirectBroadcastReceiver.myAddress;
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

    public WDBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       Activity activity) { // was MyWifiActivity
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;

        this.mController = NDNController.getInstance();
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

                        // attempt to connect to all devices in range:
                        // TODO limit to just 5 later on
                        for (WifiP2pDevice device : peers.getDeviceList()) {
                            connect(device);
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

                            // create UDP face towards GO
                            mController.createFace(groupOwnerAddress, NDNController.URI_UDP_PREFIX);

                            // register the /localhop/wifidirect/<go-address> to this face
                            String[] prefixes = new String[1];
                            prefixes[0] = NDNController.PROBE_PREFIX + "/" + groupOwnerAddress;
                            mController.ribRegisterPrefix(mController.getFaceIdForPeer(groupOwnerAddress),
                                    prefixes);
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
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(mActivity, "Connect failed for " + config.deviceAddress,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
