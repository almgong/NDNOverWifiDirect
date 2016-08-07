package ag.ndn.ndnoverwifidirect.utils;

import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.intel.jndn.management.ManagementException;

import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
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

import ag.ndn.ndnoverwifidirect.task.FaceCreateTask;
import ag.ndn.ndnoverwifidirect.task.RegisterPrefixTask;
import ag.ndn.ndnoverwifidirect.task.SendInterestTask;

/**
 * Interface specification for the family of classes that
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
    private Set<String> activePeerIps = new HashSet<>();   // holds ips of ALL peers currently registered with owner
    //private Set<String> activeFaceUris = new HashSet<>();
    private HashMap<String, Face> faceMap = new HashMap<>();// any created faces logged here

    public static NDNOverWifiDirect getInstance() {

        if (singleton == null) {
            singleton = new NDNOverWifiDirect();
        }

        return singleton;
    }

    /**
     * Logs the created face so no duplicates are made.
     * @param faceUri Unique faceuri (e.g. ip)
     * @param face NDN Face instance that uses the above faceUri
     */
    public void logFace(String faceUri, Face face) {
        if (!faceMap.containsKey(faceUri)) {
            faceMap.put(faceUri, face);
        }
    }

    /**
     * Retrieve a registered/logged Face instance given its URI
     * @param uri
     * @return
     */
    public Face getFaceByUri(String uri) {
        return faceMap.get(uri);            // uri identifies the peer
    }

    /**
     * Retrieve set of peer Ips that have been registered/logged by the current device
     * @return
     */
    public Set<String> enumerateLoggedFaces() {
        return faceMap.keySet();
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
        task.execute();
    }

    // catch-all-like send interest function, sends to all faces
    public void sendInterestToAllFaces(Interest interest) {
        for (String faceUri : faceMap.keySet()) {
           SendInterestTask task = new SendInterestTask(interest, faceMap.get(faceUri));
            task.execute();
        }
    }

    /**
     * Registers the given prefix to the specified face.
     * @param face NDN Face instance to register prefix on.
     * @param prefix string prefix, e.g. /ndn/wifid/register
     * @param handleForever boolean denoting whether the prefix should be advertised indefinitely
     */
    public void registerPrefix(Face face, String prefix, OnInterestCallback cb, boolean handleForever) {

        RegisterPrefixTask registerPrefixTask = new RegisterPrefixTask(face,
                prefix, cb, handleForever);
        registerPrefixTask.execute();
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
}