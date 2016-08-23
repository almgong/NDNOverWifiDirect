package ag.ndn.ndnoverwifidirect.utils;

import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.util.Log;

import com.intel.jndn.management.ManagementException;

import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn_xx.util.FaceUri;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;

import ag.ndn.ndnoverwifidirect.callback.RegisterOnInterest;
import ag.ndn.ndnoverwifidirect.task.FaceCreateTask;
import ag.ndn.ndnoverwifidirect.task.RegisterPrefixTask;
import ag.ndn.ndnoverwifidirect.task.RibRegisterPrefixTask;
import ag.ndn.ndnoverwifidirect.task.SendInterestTask;

/**
 * Interface implementation for the family of classes that
 * encapsulate WifiDirect logic under the familiar NFD
 * interface.
 *
 * Some desired functionality that this interface should expose:
 *
 * 1. Starting important WifiDirect logic (discovering peers for the first time, etc.)
 * 2. Allowing an application to register a prefix that it handles (and to de-register)
 * 3. Allowing an application to send NDN data and interest packets (for now, simply use defaults; keys, etc.)
 * 4. Allowing an application to register callback handlers to deal with subsequent interests to (3.).
 * 5. Allowing an application to retrieve the ndn prefixes that are currently available (FIB)
 *
 * Need to implement a custom class for OnInterest and OnData callbacks, perhaps simply an interface
 * with conventional method to run, of which, the new OnXXX in the tasks will call - this way it is customizable
 *
 * Created by allengong on 7/11/16.
 */
public class NDNOverWifiDirect extends NfdcHelper {

    // prevents instantiation
    private NDNOverWifiDirect() {
        super();
    }

    // singleton
    private static NDNOverWifiDirect singleton;

    // members
    private static final String TAG = "NDNOverWifiDirect";

    // { peerIP : Face }, any created faces using "new" logged here - good for MANUALLY selecting faces
    private HashMap<String, Face> faceMap = new HashMap<>();

    // { peerIp : faceId }, all faces created using Nfdc's faceCreate. one-to-one relationship
    private HashMap<String, Integer> peerToFaceMap = new HashMap<>();

    // set of prefixes that the running application handles - everyone is assumed to handle /ndn/wifid/register
    private Set<String> prefixes = new HashSet<String>();

    // flag to denote that registration prefix (ubiquitous) has been set up already
    private static boolean registrationPrefixComplete = false;

    public static NDNOverWifiDirect getInstance() {

        if (singleton == null) {
            singleton = new NDNOverWifiDirect();
        }

        return singleton;
    }

    public void setRegistrationPrefixComplete(boolean complete) {
        registrationPrefixComplete = complete;
    }

    public boolean getRegistrationPrefixComplete() {
        return registrationPrefixComplete;
    }

    /**
     * Logs the created face so no duplicates are made.
     * @param faceUri Unique faceuri (e.g. ip)
     * @param face NDN Face instance that uses the above faceUri
     * @return boolean indicating if the face was added to the map
     */
    public boolean logFace(String faceUri, Face face) {
        if (!faceMap.containsKey(faceUri) && !faceUri.equals(IPAddress.getLocalIPAddress())) {
            faceMap.put(faceUri, face);
            return true;
        }

        return false;
    }

    /**
     * Retrieve a registered/logged Face instance given its URI
     * @param uri
     * @return a Face instance with the given URI, or null
     */
    public Face getFaceByUri(String uri) {
        if (faceMap.containsKey(uri)) {
            return faceMap.get(uri);            // uri identifies the peer
        }
        return null;
    }

    /**
     * Retrieve set of peer Ips that have been registered/logged by the current device,
     * ignores any "locahost" faces.
     * @return
     */
    public Set<String> enumerateLoggedFaces() {
        Set<String> faces = faceMap.keySet();
        faces.remove("localhost");
        return faces;
    }

    /**
     * Logs peerIp to Face Id relationship. Each peer will have at most one
     * face created for him and logged here.
     * @param peerIp host/wifid ip
     * @param faceId integer face id
     * @return true if new record added to index, else false
     */
    public boolean logPeerToFaceId(String peerIp, int faceId) {

        if (!peerToFaceMap.containsKey(peerIp)) {
            peerToFaceMap.put(peerIp, faceId);
            return true;
        }

        return false;
    }

    /**
     * Given a peer's ip, removes the peer from all indexes.
     * @param peerIp the WifiDirect IP of the peer
     */
    public void removePeer(String peerIp) {

        // remove from faceMap
        faceMap.remove(peerIp);

        // remove from peer to face id index
        peerToFaceMap.remove(peerIp);

    }

    /**
     * Retrieves a set of NDN prefixes that is currently
     * registered as being handled by the upper-level application.
     * @return Set of string prefixes
     */
    public Set<String> getPrefixesHandled() {
        return this.prefixes;
    }

    /**
     * Registers a prefix to the controller that
     * indicates that the upper-level application
     * handles the given prefix. Only called internally, via registerPrefix().
     * @param prefix the prefix
     */
    public void addPrefixHandled(String prefix) {
        this.prefixes.add(prefix);
    }

    /**
     * Removes a registered prefix that the application
     * had previously registered with the controller.
     * @param prefix the prefix
     * @return true if removed, false if no matching prefix found
     */
    private boolean removePrefixHandled(String prefix) {
        return this.prefixes.remove(prefix);
    }

    // checks for peers, if new peers then broadcast will be sent for PEERS_CHANGED
    public void discoverPeers(WifiP2pManager mManager, WifiP2pManager.Channel mChannel) throws Exception {

        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
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

    // send interest, default OnData
    public void sendInterest(Interest interest, Face face) {
        SendInterestTask task = new SendInterestTask(interest, face);
        task.execute();
    }

    // send interest with custom OnData
    public void sendInterest(Interest interest, Face face, OnData onData) {
        SendInterestTask task = new SendInterestTask(interest, face, onData);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR); // parallel handling of async tasks
    }

    // catch-all-like send interest function, sends to all faces
    public void sendInterestToAllFaces(Interest interest) {
        for (String faceUri : faceMap.keySet()) {
           SendInterestTask task = new SendInterestTask(interest, faceMap.get(faceUri));
            task.execute();
        }
    }

    // uses TCP as transport protocol, creates face and registers RIB entry(ies)
    // let NFD automatically destroy faces
    public void createFace(String peerIp, String[] prefixesToRegister) {

        if (peerIp.equals(IPAddress.getLocalIPAddress())) {
            return; //never add yourself as a face
        }

        if (peerToFaceMap.containsKey(peerIp)) {
            try {
                for (String prefix : prefixesToRegister) {
                    RibRegisterPrefixTask task = new RibRegisterPrefixTask(prefix, peerToFaceMap.get(peerIp),
                            0, true, false);
                    task.execute();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        // else need to create a new face with these prefixes
        FaceCreateTask task = new FaceCreateTask(peerIp, prefixesToRegister);
        task.execute(String.format("tcp://%s", peerIp));
    }

    /**
     * Registers the given prefix to the specified face.
     * @param face NDN Face instance to register prefix on.
     * @param prefix string prefix, e.g. /ndn/wifid/register
     * @param handleForever boolean denoting whether the prefix should be advertised indefinitely
     */
    public void registerPrefix(Face face, String prefix, OnInterestCallback cb, boolean handleForever,
                               long repeatTimer) {

        RegisterPrefixTask registerPrefixTask = new RegisterPrefixTask(face,
                prefix, cb, handleForever);
        if (repeatTimer > 0) {
            registerPrefixTask.setProcessEventsTimer(repeatTimer);
        }

        // all processing of interests should be handled in parallel, as to not block each other
        registerPrefixTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

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

    // --- initialization logic, things that should be called by applications using this ---//
    public void initialize() {

        Log.d(TAG, "Initializing NDNOverWifiDirectController");
        logFace("localhost", new Face("localhost"));
        Face mFace = getFaceByUri("localhost");

        try {
            KeyChain keyChain = getKeyChain();
            mFace.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }
}