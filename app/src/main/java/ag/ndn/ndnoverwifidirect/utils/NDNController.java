package ag.ndn.ndnoverwifidirect.utils;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.util.Log;

import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.Nfdc;

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
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import ag.ndn.ndnoverwifidirect.callback.GenericCallback;
import ag.ndn.ndnoverwifidirect.callback.ProbeOnInterest;
import ag.ndn.ndnoverwifidirect.model.Peer;
import ag.ndn.ndnoverwifidirect.runnable.DiscoverPeersRunnable;
import ag.ndn.ndnoverwifidirect.runnable.ProbeRunnable;
import ag.ndn.ndnoverwifidirect.service.WDBroadcastReceiverService;
import ag.ndn.ndnoverwifidirect.task.FaceCreateTask;
import ag.ndn.ndnoverwifidirect.task.FaceDestroyTask;
import ag.ndn.ndnoverwifidirect.task.RegisterPrefixTask;
import ag.ndn.ndnoverwifidirect.task.RibRegisterPrefixTask;

/**
 * New streamlined NDNOverWifiDirect controller. One instance exists
 * for the application.
 *
 * 1. No longer subclasses NfdcHelper class.
 * 2. GO's will keep track of Faces to peers (faceIds), while non-GO
 * effectively have a single face to GO
 *
 * To integrate:
 * First, make sure to add a line in the Manifest declaring WDBroadcastReceiverService (see this project's manifest for example).
 * While you are at the Manifest file, add the permissions wrapped in "wifid" comments found in this project's manifest.
 *
 * 1. Import ag.ndn.ndnoverwifidirect.utils.NDNController (this) as necessary.
 * 2. call setWifiDirectContext(), and set the context in which you call start/stop (e.g. the fragment with the switch)
 * 3. call NDNController.getInstance().start/stopBroadcastReceiverService() as necessary
 * 4. call NDNController.getInstance().start/stopDiscoverAndProbe() as necessary.
 *
 * Created by allengong on 10/23/16.
 */

public class NDNController {

    public static final String URI_UDP_PREFIX = "udp://";
    public static final String URI_TCP_PREFIX = "tcp://";
    public static final String PROBE_PREFIX = "/localhop/wifidirect";   // prefix used in probing
    //public static final String DATA_PREFIX = "/ndn/wifidirect";

    private static final String TAG = "NDNController";
    private static final int DISCOVER_PEERS_DELAY = 30000;  // in ms
    private static final int PROBE_DELAY = 12000;           // in ms
    private static final int MAX_PEERS = 5;

    // singleton
    private static NDNController mController = null;

    // WiFi Direct related resources
    private WifiP2pManager wifiP2pManager = null;
    private WifiP2pManager.Channel channel = null;
    private Context wifiDirectContext = null;       // context in which WiFi direct operations begin (the activity/fragment)

    // shared members (GO and Non-GO)
    private WDBroadcastReceiverService brService = null;
    private Future discoverPeersFuture = null;
    private Future probeFuture = null;
    private ScheduledThreadPoolExecutor discoverProbeExecutor;

    private boolean hasRegisteredOwnLocalhop = false;
    private boolean isGroupOwner;                   // set in broadcast receiver, used primarily in ProbeOnInterest
    private boolean protocolRunning = false;        // whether the tasks/services of this protocol are reported running

    // we have some redundancy here in data, but difficult to avoid given WFDirect API
    // exposes only MAC addresses at the connect stage
    private HashMap<String, Peer> connectedPeers;                   // { deviceAddress(MAC) : PeerInstance, ... }, contains MAC and device name
    private HashMap<String, Peer> peersMap;                         // { peerIp : PeerInstance }, contains at least Face id info

    private final Face mFace = new Face("localhost"); // single face instance at localhost, not to be used outside of this class

    private NDNController() {
        try {
            KeyChain kc = buildTestKeyChain();
            mFace.setCommandSigningInfo(kc, kc.getDefaultCertificateName());
            connectedPeers = new HashMap<>(MAX_PEERS);
            peersMap = new HashMap<>(MAX_PEERS);
            discoverProbeExecutor = new ScheduledThreadPoolExecutor(1); // need at least one thread
        } catch (Exception e) {
            e.printStackTrace();
        }

    }  // prevents outside instantiation

    public static NDNController getInstance() {
        if (mController == null) {
            mController = new NDNController();
        }

        return mController;
    }

    /**
     * Attempts to add a peer to a rolling list of connected peers, up to MAX_PEERS amount.
     * @param peer
     * @return true if addition was successful, false otherwise (max peers number reached).
     */
    public boolean logConnectedPeer(Peer peer) {
        if (connectedPeers.size() < MAX_PEERS) {
            if (!connectedPeers.containsKey(peer.getDeviceAddress())) {
                connectedPeers.put(peer.getDeviceAddress(), peer);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the MAC addresses of all currently connected peers.
     * @return Set of MAC addresses
     */
    public Set<String> getConnectedPeers() {
        return connectedPeers.keySet();
    }

    /**
     * Given a MAC address, return all logged peer information, if any.
     * @param deviceAddress MAC address of peer
     * @return Peer instance containing information of this peer.
     */
    public Peer getPeerByDeviceAddress(String deviceAddress) {
        return connectedPeers.get(deviceAddress);
    }

    // shared methods

    /**
     * Returns the Face id associated with the given peer, denoted by IP address.
     * @param peerIp The WiFi Direct IP address of the peer
     * @return the Face id of the peer or -1 if no mapping exists.
     */
    public int getFaceIdForPeer(String peerIp) {
        if (peersMap.containsKey(peerIp)) {
            return peersMap.get(peerIp).getFaceId();
        }

        return -1;
    }

    /**
     * Logs the peer with the corresponding faceId
     * @param peerIp The peer's WD IP address
     * @param peer A Peer instance with at least FaceId set.
     * @return true if new peer was added to the map, else false
     */
    public boolean logPeer(String peerIp, Peer peer) {
        if (peersMap.containsKey(peerIp)) {
            return false;
        }

        peersMap.put(peerIp, peer);
        return true;
    }

    /**
     * Returns the logged peer instance (via logPeer()) by its
     * WiFi Direct IP address.
     * @param ip WiFi Direct IP address of peer
     * @return the peer instance logged earlier by a call to logPeer(), or null
     * if none.
     */
    public Peer getPeerByIp(String ip) {
        return peersMap.get(ip);
    }

    /**
     * Removes mapping to the logged peer, and destroys any state of that peer (e.g. any created
     * faces and registered prefixes).
     * @param ip WiFi Direct IP address of peer
     */
    public void removePeer(String ip) {
        FaceDestroyTask task = new FaceDestroyTask();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, peersMap.get(ip).getFaceId());
        peersMap.remove(ip);
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
     * This must be done before ever calling discoverPeers().
     * @param wifiP2pManager
     * @param channel
     */
    public void recordWifiP2pResources(WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel) {
        this.wifiP2pManager = wifiP2pManager;
        this.channel = channel;
    }

    /**
     * Must be done before we can start the Broadcast Receiver Service -  in the future we can also
     * move the starting to somewhere within an activity or fragment in order to avoid this.
     * @param context A valid Android context
     */
    public void setWifiDirectContext(Context context) {
        this.wifiDirectContext = context;
    }

    /**
     * Creates a face to the specified peer (IP), with the
     * uriPrefix (e.g. tcp://). Optional callback parameter
     * uriPrefix (e.g. tcp://). Optional callback parameter
     * for adding a callback function to be called after successful
     * face creation. Passing in null for callback means no callback.
     *
     * @param peerIp the peer's WiFi Direct IP
     * @param uriPrefix uri prefix
     * @param callback An implementation of GenericCallback, or null. Is called AFTER face
     *                 creation succeeds.
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
        Log.d(TAG, "ribRegisterPrefix called with: " + faceId + " and " + prefixes.length + " prefixes");

        HashSet<Integer> faceIds = new HashSet<>(peersMap.size());
        for (Peer p : peersMap.values()) {
            faceIds.add(p.getFaceId());
        }

        if (faceIds.contains(faceId)) {
            try {
                for (String prefix : prefixes) {
                    Log.d(TAG, "actually creating the task and running it now: " + prefix);
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

    /**
     * Begins periodically looking for peers, and connecting
     * to them.
     */
    public void startDiscoveringPeers() {
        if (discoverPeersFuture == null) {
            Log.d(TAG, "Start discovering peers every " + DISCOVER_PEERS_DELAY + "ms");
            discoverPeersFuture = discoverProbeExecutor.scheduleWithFixedDelay(new DiscoverPeersRunnable(), 100, DISCOVER_PEERS_DELAY, TimeUnit.MILLISECONDS);
        } else {
            Log.d(TAG, "Discovering peers already running!");
        }
    }

    /**
     * Stops discovering peers periodically.
     */
    public void stopDiscoveringPeers() {
        if (discoverPeersFuture != null) {
            discoverPeersFuture.cancel(false);  // don't interrupt the execution, but stop further discovery
            discoverPeersFuture = null;
            Log.d(TAG, "Stopped discovering peers.");
        }
    }

    /**
     * Begins probing the network for data prefixes.
     */
    public void startProbing() {
        if (probeFuture == null) {
            Log.d(TAG, "Start probing for data prefixes every " + PROBE_DELAY + "ms");
            probeFuture = discoverProbeExecutor.scheduleWithFixedDelay(new ProbeRunnable(), 200, PROBE_DELAY, TimeUnit.MILLISECONDS);
        } else {
            Log.d(TAG, "Probing task already running!");
        }
    }

    /**
     * Stops probing the network for data prefixes.
     */
    public void stopProbing() {
        if (probeFuture != null) {
            probeFuture.cancel(false);  // don't interrupt execution, but stop further probing
            probeFuture = null;
            Log.d(TAG, "Stopped probing.");
        }
    }

    /**
     * Starts service that registers the broadcast receiver for handling peer discovery
     */
    public void startBroadcastReceiverService() {
        if (brService == null) {
            Log.d(TAG, "Starting WDBR service...");
            brService = new WDBroadcastReceiverService();
            Intent intent = new Intent(wifiDirectContext, WDBroadcastReceiverService.class);
            wifiDirectContext.startService(intent);
        } else {
            Log.d(TAG, "BroadcastReceiverService already started.");
        }
    }

    /**
     * Stops the service that registers the broadcast recevier for handling peer discovery.
     */
    public void stopBroadcastReceiverService() {
        if (brService == null) {
            Log.d(TAG, "BroadcastReceiverService not running, no need to stop.");
        } else {
            if (wifiDirectContext != null) {
                Log.d(TAG, "Stopping WDBR service...");
                wifiDirectContext.stopService(new Intent(wifiDirectContext, WDBroadcastReceiverService.class));
                // also can try: brService.onStop()
            }

            brService = null;
            Log.d(TAG, "Stopped WDBR service.");
        }
    }

    /**
     * Convenience wrapper method to start all background
     * tasks/services for this protocol.
     */
    public void startDiscoverAndProbe() {
        // we will treat this as meaning both are not active
        startDiscoveringPeers();
        startProbing();
        startBroadcastReceiverService();

        protocolRunning = true;
    }

    /**
     * Convenience wrapper method to stop all background
     * tasts/services for this protocol.
     */
    public void stopDiscoverAndProbe() {
        stopDiscoveringPeers();
        stopProbing();
        stopBroadcastReceiverService();

        protocolRunning = false;
    }

    /**
     * Temporary method, really just used for the demo: see ConnectFragment.java.
     * @return whether the last call was a call to startDiscoverAndProbe() as opposed
     * to stopDiscoverAndProbe().
     */
    public boolean isProtocolRunning() {
        return protocolRunning;
    }

    /**
     * Whether or not /localhop/wifidirect/xxx.xxx.xxx.xxx has
     * been registered. Here, the ip is specifically that of this device.
     * @return
     */
    public boolean getHasRegisteredOwnLocalhop() {
        return this.hasRegisteredOwnLocalhop;
    }

    public void setHasRegisteredOwnLocalhop(boolean set) { this.hasRegisteredOwnLocalhop = set; }

    public void registerOwnLocalhop() {
        if (!hasRegisteredOwnLocalhop) {
            // register /localhop/wifidirect/<this-devices-ip> to localhost
            registerPrefix(mFace, PROBE_PREFIX + "/" + IPAddress.getLocalIPAddress(), new OnInterestCallback() {
                @Override
                public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
                    (new ProbeOnInterest()).doJob(prefix, interest, face, interestFilterId, filter);
                }
            }, true, 100);  // process event every 100 ms
        }
    }

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

    /**
     * Resets all state accumulated through normal operation.
     */
    public void cleanUp() {

        // if you are not a group owner, need to remove yourself from the WifiP2p group.
        // group owners will automatically destroy the group when appropriate
        if (!isGroupOwner && (wifiP2pManager != null)) {
            wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Successfully removed self from WifiP2p group.");
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Unable to remove self from WifiP2p group, reason: " + reason);
                }
            });
        }

        // remove all faces created to peers, make use of our handy executor
        for (final String peerIp : peersMap.keySet()) {
            discoverProbeExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Log.d(TAG, "Cleaning up face towards peer: " + peerIp);
                        Nfdc.destroyFace(mFace, peersMap.get(peerIp).getFaceId());
                    } catch (ManagementException me) {
                        me.printStackTrace();
                    }
                }
            });
        }

        // destroy the localhost face used for NFD communication
        mFace.shutdown();

        // finally remove all state of controller singleton
        mController = null;
    }
}
