package ag.ndn.ndnoverwifidirect.utils;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.util.Log;

import net.named_data.jndn.Face;
import net.named_data.jndn.Name;

import java.util.HashMap;
import java.util.Set;

import ag.ndn.ndnoverwifidirect.task.DiscoverPeersTask;
import ag.ndn.ndnoverwifidirect.task.ProbeTask;

/**
 * New streamlined NDNOverWifiDirect controller. One instance exists
 * for the application.
 *
 * 1. No longer subclasses NfdcHelper class, instead uses delegation.
 * 2. GO's will keep track of Faces to peers (faceIds), while non-GO
 * simply have a single face to GO
 *
 * Created by allengong on 10/23/16.
 */

public class NDNController {

    public static final String URI_UDP_PREFIX = "udp://";
    public static final String URI_TCP_PREFIX = "tcp://";

    private static final String TAG = "NDNController";

    // singleton
    private static NDNController mController = null;

    private static NfdcHelper nfdcHelper;

    // WiFi Direct related resources
    private WifiP2pManager wifiP2pManager = null;
    private WifiP2pManager.Channel channel = null;
    private Context wifiDirectContext = null;

    // shared members (GO and Non-GO)
    private DiscoverPeersTask discoverPeersTask = null;
    private ProbeTask probeTask = null;
    private boolean isGroupOwner;           // set in broadcast receiver
    private static Face mFace;              // main face pointing to localhost for registering prefixes, etc.

    // GO specific members
    private HashMap<String, Integer> peersMap = new HashMap<>();    // { peerIp:faceId }
    private HashMap<String, Set<String>> peersPrefixMap = new HashMap<>(); // { peerIp:{data prefix names} }

    // NON-GO specific members
    private Face faceToGO;


    private NDNController() {}  // prevents outside instantiation

    public static NDNController getInstance() {
        if (mController == null) {
            nfdcHelper = new NfdcHelper();  // init
            mController = new NDNController();
            mFace = new Face("localhost");
        }

        return mController;
    }


    // shared methods

    /**
     * Initializes the WifiP2p context, channel and manager, for use with discovering peers.
     * This must be done before starting ConnectService.
     * @param wifiP2pManager
     * @param channel
     */
    public void recordWifiP2pResources(WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel,
                                  Context context) {
        this.wifiP2pManager = wifiP2pManager;
        this.channel = channel;
        this.wifiDirectContext = context;
    }

    public void setIsGroupOwner(boolean b) {
        this.isGroupOwner = b;
    }

    /**
     * creates a face using nfdchelper, with the given peerIp and protocol.
     *
     * @param peerIp
     * @param uriPrefix
     */
    public void createFace(String peerIp, String uriPrefix) {
        try {
            if (!peersMap.containsKey(peerIp)) {
                // need to create a new face for this peer
                nfdcHelper.faceCreate(uriPrefix+peerIp);

                // TODO create facecreate task and run it above will lead to network on main thread expcetion
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ribRegisterPrefix(int faceId, String[] prefixes) {
        // TODO create a new task that specifcially registers prefix to some face
    }

    /**
     * Begins periodically looking for peers, and connecting
     * to them.
     */
    public void startDiscoveringPeers() {
        if (discoverPeersTask == null) {
            Log.d(TAG, "startDiscoveringPeers()");
            discoverPeersTask = new DiscoverPeersTask();
            discoverPeersTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            Log.d(TAG, "Discovering peers task already running!");
        }
    }

    public void stopDiscoveringPeers() {
        if (discoverPeersTask != null) {
            discoverPeersTask.stop();
            discoverPeersTask = null;
        }
    }

    /**
     * Begins probing the network for data prefixes
     */
    public void startProbing() {
        if (probeTask != null) {
            probeTask = new ProbeTask();
            probeTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            Log.d(TAG, "Probing task already running!");
        }
    }

    public void stopProbing() {
        if (probeTask != null) {
            probeTask.stop();
            probeTask = null;
        }
    }

    public NfdcHelper getNfdcHelper() {
        return nfdcHelper;
    }   // I don't like this


    // GO methods

    /**
     * Logs the peer with the corresponding faceId
     * @param peerIp
     * @param faceId
     * @return true if new peer was added to the map, else false
     */
    public boolean logPeer(String peerIp, int faceId) {
        if (peersMap.containsKey(peerIp)) {
            return false;
        }

        peersMap.put(peerIp, faceId);

        return true;
    }



    // Non-GO methods

    public void setGroupOwnerFace(Face face) {
        this.faceToGO = face;
    }




    // everything below here is for convenience, send interest, create face, register prefixes

    public AsyncTask sendInterest() { return null; }

    // checks for peers, if new peers then broadcast will be sent for PEERS_CHANGED
    // will connect to up to 5 peers(?)
    // can be called as a one-off op
    public void discoverPeers() throws Exception {

        if (wifiP2pManager == null || channel == null) {
            Log.e(TAG, "Unable to discover peers, did you recordWifiP2pResources() yet?");
            return;
        }

        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // WIFI_P2P_PEERS_CHANGED_ACTION intent sent!!
                Log.d(TAG, "Success on discovering peers");
            }

            @Override
            public void onFailure(int reasonCode) {

                Log.d(TAG, "Fail discover peers, reasoncode: " + reasonCode);
            }
        });
    }
}
