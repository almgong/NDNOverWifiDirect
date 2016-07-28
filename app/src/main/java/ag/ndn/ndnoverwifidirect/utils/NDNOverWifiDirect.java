package ag.ndn.ndnoverwifidirect.utils;

import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.util.Log;

import com.intel.jndn.management.ManagementException;

import junit.framework.Test;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn_xx.util.FaceUri;

import java.util.HashSet;
import java.util.Set;

/**
 * Interface specification for the family of classes that
 * encapsulate WifiDirect logic under the familiar NFD
 * interface.
 *
 * Some desired functionality that this interface should expose:
 *
 * 1. Starting important WifiDirect logic (discovering peers for the first time, etc.)
 * 2. Allowing an application to register a prefix that it handles (and to de-register)
 * 3. Allowing an application to send NDN data packets (for now, simply use defaults; keys, etc.)
 * 4. Allowing an application to register callback handlers to deal with subsequent interests to (3.).
 * 5. Allowing an application to retrieve the ndn prefixes that are currently available (FIB)
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
    private Set<String> activeFaceUris = new HashSet<String>(); // O(1) access to registered faces

    public static NDNOverWifiDirect getInstance() {

        if (singleton == null) {
            singleton = new NDNOverWifiDirect();
        }

        return singleton;
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

    public void createFace(String faceUri) {
        FaceCreateTask task = new FaceCreateTask();
        task.execute(faceUri);
    }

    // overrides, should not be called outside of this class

    @Override
    public int
    faceCreate(String faceUri) throws ManagementException, FaceUri.Error, FaceUri.CanonizeError {

        if (!activeFaceUris.contains(faceUri)) {
            activeFaceUris.add(faceUri);
        }

        return super.faceCreate(faceUri);
    }

    // Misc.

    // task to create a network face without using main thread
    private class FaceCreateTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String... faceUris) {
            int faceId = -1;

            try {
                System.out.println("--------Inside face create task--------");
                System.out.println("num faces before: " + faceList().size());
                faceId = faceCreate(faceUris[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(TAG, "!!!Created face with face id: " + faceId);
            return faceId;
        }

//        @Override
//        protected Integer onPostExecute(Integer faceId) {
//            return faceId;
//        }

    }


    public void sendPing() {
        TestInterestTask task =  new TestInterestTask();
        task.execute();
    }

    // temp task that will send out an interest, todo: CHANGE TO A THREAD
    private class TestInterestTask extends AsyncTask<String, Void, Integer> {

        protected Integer doInBackground(String... stuff) {

            Face mFace = new Face("localhost");
            Name name = new Name("/test/name/fakekekekeke");
            Log.d(TAG, "attempting to send out interest " + name.toUri());
            try {
                mFace.expressInterest(name, new OnData() {
                    @Override
                    public void onData(Interest interest, Data data) {
                        Log.d(TAG,"WHATTATATAT I GOT DATA BACK?? " + interest.getName().toUri());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

            return 0;
        }
    }

}
