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

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;

import java.net.InetAddress;
import java.util.HashMap;

import ag.ndn.ndnoverwifidirect.fragment.PeerFragment;
import ag.ndn.ndnoverwifidirect.model.Peer;
import ag.ndn.ndnoverwifidirect.model.PeerList;
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

                        PeerList.resetPeerList();   // need to do extra processing to unregister faces
                        int id = 0;
                        // ideally want to check if peers are new, and add to some rolling list
                        // use PeerList, which should expose diff methods, etc., to know what faces
                        // to actually create, and which to destroy
                        for (WifiP2pDevice device : peers.getDeviceList()) {

                            // no need to check since we cleared the list before this
                            Peer peer = new Peer();
                            peer.setId("" + ++id);
                            peer.setDeviceAddress(device.deviceAddress);
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
                        String groupOwnerAddress = info.groupOwnerAddress.getHostAddress();
                        System.out.println("group owner address " + groupOwnerAddress);
                        // After the group negotiation, we can determine the group owner.
                        if (info.groupFormed && info.isGroupOwner) {
                            // Do whatever tasks are specific to the group owner.
                            // One common case is creating a server thread and accepting
                            // incoming connections.

                            Log.d(TAG, "I am the group owner");
                            isGroupOwner = true;
                            // start thread to listen for incoming connections and people telling you
                            // their ips

                        } else if (info.groupFormed) {
                            // The other device acts as the client. In this case,
                            // you'll want to create a client thread that connects to the group
                            // owner.
                            Log.d(TAG, "I am not the group owner, and my ip is: " +
                                    IPAddress.getDottedDecimalIP(IPAddress.getLocalIPAddress()));

                            // create unicast tcp face to group owner
                            String faceUri = "tcp://%s";

                            try {
                                mController.createFace(String.format(faceUri, groupOwnerAddress));
                                Log.d(TAG, String.format("Successfully created a new face [ %s ]", groupOwnerAddress));
                                mController.sendPing();
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
        System.out.println(activePeers.size());
        System.out.println("Peer device:" + peerDevice);
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


