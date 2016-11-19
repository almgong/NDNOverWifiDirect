package ag.ndn.ndnoverwifidirect.callback;

import android.util.Log;

import com.intel.jndn.management.Nfdc;
import com.intel.jndn.management.enums.FaceScope;
import com.intel.jndn.management.types.FaceStatus;
import com.intel.jndn.management.types.FibEntry;
import com.intel.jndn.management.types.NextHopRecord;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

import java.util.HashSet;
import java.util.List;

import ag.ndn.ndnoverwifidirect.utils.NDNController;

/**
 * Handle OnInterest events for probe interests.
 *
 * Created by allengong on 11/12/16.
 */
public class ProbeOnInterest implements NDNCallBackOnInterest {

    private static final String TAG = "ProbeOnInterest";

    private NDNController mController = NDNController.getInstance();
    private Face mFace = mController.getLocalHostFace(); // localhost face for communicating with NFD


    @Override
    public void doJob(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
        Log.d(TAG, "Got an interest for: " + interest.getName().toString());

        // /localhop/wifidirect/192.168.49.x/192.168.49.y/probe?mustBeFresh=1
        String[] prefixArr = interest.getName().toString().split("/");

        // validate
        if (prefixArr.length != 6) {
            Log.e(TAG, "Error with this interest, skipping...");
        }

        final String peerIp = prefixArr[prefixArr.length - 2];

        // if not logged (a face created for this probing peer), should then create a face (mainly for GO)
        if (mController.getFaceIdForPeer(peerIp) == -1) {

            mController.createFace(peerIp, NDNController.URI_TCP_PREFIX, new GenericCallback() {
                @Override
                public void doJob() {
                    Log.d(TAG, "Registering localhop for: " + peerIp);
                    String[] prefixes = new String[1];
                    prefixes[0] = NDNController.PROBE_PREFIX + "/" + peerIp;
                    mController.ribRegisterPrefix(mController.getFaceIdForPeer(peerIp),
                            prefixes);
                }
            });
        }

        // enumerate RIB, look for all /ndn/wifidirect/* data prefixes, return to user as described in slides
        try {
            // set of prefixes to return to interest sender
            HashSet<String> prefixesToReturn = new HashSet<>();
            String response = "";
            int num = 0;

            // consult NFD to get all entries in FIB
            List<FibEntry> fibEntries = Nfdc.getFibList(mFace);

            if (mController.getIsGroupOwner()) {
                // if GO, return all data prefixes
                for (FibEntry fibEntry : fibEntries) {
                    if (fibEntry.getPrefix().toString().startsWith(NDNController.DATA_PREFIX)) {
                        prefixesToReturn.add(fibEntry.getPrefix().toString());
                        num++;
                    }
                }
            } else {
                // enumerate local faces
                List<FaceStatus> faceStatuses = Nfdc.getFaceList(mFace);
                HashSet<Integer> localFaceIds = new HashSet<>();
                for (FaceStatus faceStatus : faceStatuses) {
                    if (faceStatus.getFaceScope().toInteger() == FaceScope.LOCAL.toInteger()) {
                        localFaceIds.add(faceStatus.getFaceId());
                    }
                }
                faceStatuses = null;    // gc

                // return only those prefixes that can be handled locally
                for (FibEntry fibEntry : fibEntries) {
                    if (fibEntry.getPrefix().toString().startsWith(NDNController.DATA_PREFIX)) {

                        // added constraint that the prefix can be served from this device (e.g.
                        // by an upper layer application)
                        List<NextHopRecord> nextHopRecords = fibEntry.getNextHopRecords();
                        for (NextHopRecord nextHopRecord : nextHopRecords) {
                            if (localFaceIds.contains(nextHopRecord.getFaceId())) {
                                prefixesToReturn.add(fibEntry.getPrefix().toString());
                                num++;
                                break;
                            }
                        }
                    }
                }
            }

            Data data = new Data();
            data.setName(new Name(interest.getName().toUri()));

            // format payload, for now ignore hopcount as it is not clear whether
            // it is useful
            for (String pre : prefixesToReturn) {
                response += ("\n" + pre);
            }

            Blob payload = new Blob(num + response); // num + ("\nprefix1\nprefix2...")
            data.setContent(payload);

            face.putData(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
