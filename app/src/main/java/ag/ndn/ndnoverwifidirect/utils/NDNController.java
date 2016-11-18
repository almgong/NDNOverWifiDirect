package ag.ndn.ndnoverwifidirect.utils;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.util.Log;

import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ag.ndn.ndnoverwifidirect.callback.GenericCallback;
import ag.ndn.ndnoverwifidirect.callback.ProbeOnInterest;
import ag.ndn.ndnoverwifidirect.task.DiscoverPeersTask;
import ag.ndn.ndnoverwifidirect.task.FaceCreateTask;
import ag.ndn.ndnoverwifidirect.task.ProbeTask;
import ag.ndn.ndnoverwifidirect.task.RegisterPrefixTask;
import ag.ndn.ndnoverwifidirect.task.RibRegisterPrefixTask;

/**
 * New streamlined NDNOverWifiDirect controller. One instance exists
 * for the application.
 *
 * 1. No longer subclasses NfdcHelper class, instead uses delegation.
 * 2. GO's will keep track of Faces to peers (faceIds), while non-GO
 * effectively have a single face to GO
 *
 * Created by allengong on 10/23/16.
 */

public class NDNController {

    public static final String URI_UDP_PREFIX = "udp://";
    public static final String URI_TCP_PREFIX = "tcp://";
    public static final String PROBE_PREFIX = "/localhop/wifidirect";   // prefix used in probe handling
    public static final String DATA_PREFIX = "/ndn/wifidirect";

    private static final String TAG = "NDNController";

    // singleton
    private static NDNController mController = null;

    // internal delegated Nfdc handle
    private static NfdcHelper nfdcHelper;

    // WiFi Direct related resources
    private WifiP2pManager wifiP2pManager = null;
    private WifiP2pManager.Channel channel = null;
    private Context wifiDirectContext = null;

    // shared members (GO and Non-GO)
    private DiscoverPeersTask discoverPeersTask = null;
    private ProbeTask probeTask = null;
    private boolean hasRegisteredOwnLocalhop = false;
    private boolean isGroupOwner = false;   // set in broadcast receiver, defaulted to false, used primarily in ProbeOnInterest

    private final Face mFace = new Face("localhost"); // single face instance at localhost, not to be used outside of this class

    //private HashMap<String, Set<String>> peersPrefixMap = new HashMap<>(); // { peerIp:{data prefix names},... }
    private HashMap<String, Integer> peersMap = new HashMap<>();    // { peerIp:faceId }

    // GO specific members

    // NON-GO specific members


    private NDNController() {
        try {
            KeyChain kc = buildTestKeyChain();
            mFace.setCommandSigningInfo(kc, kc.getDefaultCertificateName());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }  // prevents outside instantiation

    public static NDNController getInstance() {
        if (mController == null) {
            nfdcHelper = new NfdcHelper();  // init
            mController = new NDNController();
        }

        return mController;
    }


    // shared methods

    // returns faceId of face towards the given peer
    // returns -1 if no mapping exists for this peer
    public int getFaceIdForPeer(String peerIp) {
        if (peersMap.containsKey(peerIp)) {
            return peersMap.get(peerIp);
        }

        return -1;
    }

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

    /**
     * Returns whether this device is the group owner
     * @return
     */
    public boolean getIsGroupOwner() {
        return isGroupOwner;
    }

    /**
     * Sets whether this device is the group owner
     * @param b
     */
    public void setIsGroupOwner(boolean b) {
        isGroupOwner = b;
    }

    /**
     * Initializes the WifiP2p context, channel and manager, for use with discovering peers.
     * This must be done before starting DiscoverPeersTask.
     * @param wifiP2pManager
     * @param channel
     */
    public void recordWifiP2pResources(WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel,
                                  Context context) {
        this.wifiP2pManager = wifiP2pManager;
        this.channel = channel;
        this.wifiDirectContext = context;
    }


    /**
     * Creates a face to the specified peer (IP), with the
     * uriPrefix (e.g. tcp://). Optional callback parameter
     * for adding a callback function to be called after successful
     * face creation. Passing in null for callback means no callback.
     *
     * @param peerIp
     * @param uriPrefix
     * @param callback An implementation of GenericCallback, or null.
     */
    public void createFace(String peerIp, String uriPrefix, GenericCallback callback) {

        if (peerIp.equals(IPAddress.getLocalIPAddress())) {
            return; //never add yourself as a face
        }

        try {
            if (!peersMap.containsKey(peerIp)) {
                // need to create a new face for this peer
                FaceCreateTask task = new FaceCreateTask(peerIp, new String[0]);

                if (callback != null) {
                    task.setCallback(callback);
                }

                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, uriPrefix+peerIp);
            } else {
                Log.d(TAG, "Face to " + peerIp + " already exists. Skipping createFace()");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Registers the array of prefixes with the given Face, denoted by
     * its face id.
     * @param faceId
     * @param prefixes
     */
    public void ribRegisterPrefix(int faceId, String[] prefixes) {
        if (peersMap.containsValue(faceId)) {
            try {
                for (String prefix : prefixes) {
                    RibRegisterPrefixTask task = new RibRegisterPrefixTask(prefix, faceId,
                            0, true, false);
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR); // no blocking of other async tasks
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }


    // enumerates all currently logged data prefixes, across all faces
    // used in ProbeOnInterest
//    public Set<String> getAllLoggedPrefixes() {
//        Set<String> prefixes = new HashSet<>();
//        for (String key : peersPrefixMap.keySet()) {
//            prefixes.addAll(peersPrefixMap.get(key));
//        }
//
//        return prefixes;
//    }

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

    /**
     * Stops discovering peers periodically.
     */
    public void stopDiscoveringPeers() {
        if (discoverPeersTask != null) {
            discoverPeersTask.stop();
            discoverPeersTask = null;
        }
    }

    /**
     * Begins probing the network for data prefixes.
     */
    public void startProbing() {
        if (probeTask == null) {
            probeTask = new ProbeTask();
            probeTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            Log.d(TAG, "Probing task already running!");
        }
    }

    /**
     * Stops probing the network for data prefixes.
     */
    public void stopProbing() {
        if (probeTask != null) {
            probeTask.stop();
            probeTask = null;
        }
    }

    /**
     * Whether or not /localhop/wifidirect/xxx.xxx.xxx.xxx has
     * been registered. Here, the ip is specifically that of this device.
     * @return
     */
    public boolean getHasRegisteredOwnLocalhop() {
        return this.hasRegisteredOwnLocalhop;
    }

    public void registerOwnLocalhop() {
        if (!hasRegisteredOwnLocalhop) {
            this.hasRegisteredOwnLocalhop = true;

            // register /localhop/wifidirect/<this-devices-ip> to localhost
            registerPrefix(mFace, PROBE_PREFIX + "/" + IPAddress.getLocalIPAddress(), new OnInterestCallback() {
                @Override
                public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
                    (new ProbeOnInterest()).doJob(prefix, interest, face, interestFilterId, filter);
                }
            }, true, 100);  // process event every 100 ms
        }
    }

    public NfdcHelper getNfdcHelper() {
        return nfdcHelper;
    }   // I don't like this// TODO not needed, can use Nfdc directly (all static methods)

    // checks for peers, if new peers then broadcast will be sent for PEERS_CHANGED
    // can be called as a one-off operation
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

    /**
     * Returns a face to localhost, to prevent creation/destruction of
     * faces.
     * @return
     */
    public Face getLocalHostFace() {
        return mFace;
    }
    // GO methods


    // Non-GO methods


    // TODO

    // everything below here is for convenience (can be done manually otherwise)

    // registers a prefix to the given face (usually localhost)
    public AsyncTask registerPrefix(Face face, String prefix, OnInterestCallback cb, boolean handleForever,
                                    long repeatTimer) {

        RegisterPrefixTask task  = new RegisterPrefixTask(face, prefix, cb, handleForever);
        task.setProcessEventsTimer(repeatTimer);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        return task;
    }

    /** misc **/

    private KeyChain buildTestKeyChain() throws net.named_data.jndn.security.SecurityException {
        MemoryIdentityStorage identityStorage = new MemoryIdentityStorage();
        MemoryPrivateKeyStorage privateKeyStorage = new MemoryPrivateKeyStorage();
        IdentityManager identityManager = new IdentityManager(identityStorage, privateKeyStorage);
        KeyChain keyChain = new KeyChain(identityManager);
        try {
            keyChain.getDefaultCertificateName();
        } catch (net.named_data.jndn.security.SecurityException e) {
            keyChain.createIdentity(new Name("/test/identity"));
            keyChain.getIdentityManager().setDefaultIdentity(new Name("/test/identity"));
        }
        return keyChain;

    }

    /* In the future, we should allow users to implement this method so they can provide their own keychain */
    public KeyChain getKeyChain() throws net.named_data.jndn.security.SecurityException {

        return buildTestKeyChain();
    }
}
