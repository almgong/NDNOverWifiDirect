package ag.ndn.ndnoverwifidirect.runnable;

import android.util.Log;

import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.Nfdc;
import com.intel.jndn.management.types.FaceStatus;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import ag.ndn.ndnoverwifidirect.utils.NDNController;

/**
 * Checks for the consistency between NDNController's view of
 * the logged peers and the NFD's. Specifically, this is carried
 * out by comparing views on active Faces.
 * Created by allengong on 12/21/16.
 */
public class FaceConsistencyRunnable implements Runnable {
    private static final String TAG = "FaceConsistencyRunnable";

    @Override
    public void run() {

        Log.d(TAG, "Running periodic Face consistency check...");

        // first, let's retrieve a set of active FaceIds from NFD.
        // then, let's compare this set with what NDNController has
        try {
            List<FaceStatus> faceStatuses =
                    Nfdc.getFaceList(NDNController.getInstance().getLocalHostFace());

            // put face ids in an easy to access manner
            HashSet<Integer> nfdActiveFaceIds = new HashSet<>(faceStatuses.size());
            for (FaceStatus faceStatus : faceStatuses) {
                nfdActiveFaceIds.add(faceStatus.getFaceId());
            }

            // iterate through keyset of peersMap
            Iterator<String> iterator = NDNController.getInstance().getIpsOfLoggedPeers().iterator();
            while(iterator.hasNext()) {
                int peerFaceId = NDNController.getInstance().getFaceIdForPeer(iterator.next());
                if ((peerFaceId != -1) && (!nfdActiveFaceIds.contains(peerFaceId))) {

                    // then this face should be un-logged (NFD does not report this face as existing)
                    Log.d(TAG, "Removing inconsistent mapping to Face: " + peerFaceId);
                    iterator.remove();
                }
            }

        } catch (ManagementException me) {
            Log.e(TAG, "There was an issue retrieving FaceList from NFD");
        }
    }
}
