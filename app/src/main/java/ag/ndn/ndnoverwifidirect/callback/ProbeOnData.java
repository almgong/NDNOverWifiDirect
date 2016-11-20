package ag.ndn.ndnoverwifidirect.callback;

import android.util.Log;

import com.intel.jndn.management.Nfdc;
import com.intel.jndn.management.types.FibEntry;
import com.intel.jndn.management.types.NextHopRecord;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import ag.ndn.ndnoverwifidirect.utils.NDNController;

/**
 * Handle OnData events for probe interests.
 *
 * Created by allengong on 11/12/16.
 */
public class ProbeOnData implements NDNCallbackOnData {

    private static final String TAG = "ProbeOnData";
    private NDNController mController = NDNController.getInstance();
    private Face mFace = mController.getLocalHostFace();

    @Override
    public void doJob(Interest interest, Data data) {
        // interest name = /localhop/wifidirect/<toIp>/<fromIp>/probe%timestamp
        Log.d(TAG, "Got data for interest: " + interest.getName().toString());

        String[] nameArr = interest.getName().toString().split("/");
        String peerIp = nameArr[nameArr.length-3];
        int peerFaceId = mController.getFaceIdForPeer(peerIp);

        // parse the data, update controller prefix map
        /**
         * Data is in form:
         * {numPrefixes}\n
         * prefix1\n
         * prefix2\n
         * ...
         */
        String[] responseArr = data.getContent().toString().split("\n");

        // validation
        if (peerFaceId == -1) {
            Log.e(TAG, "Undocumented peer.");
            return;
        }

        int numPrefixes = Integer.parseInt(responseArr[0]);
        HashSet<String> prefixesInResp = new HashSet<>(numPrefixes);
        for (int i = 1; i <= numPrefixes; i++) {
            prefixesInResp.add(responseArr[i]);
        }

        // enumerate FIB entries, and collect the set of data prefixes towards this peer
        HashSet<String> prefixesRegisteredForPeer = new HashSet<>();
        try {
            List<FibEntry> fibEntries = Nfdc.getFibList(mFace);
            for (FibEntry fibEntry : fibEntries) {
                if (fibEntry.getPrefix().toString().startsWith(NDNController.DATA_PREFIX)) {
                    List<NextHopRecord> nextHopRecords = fibEntry.getNextHopRecords();
                    for (NextHopRecord nextHopRecord : nextHopRecords) {
                        if (nextHopRecord.getFaceId() == peerFaceId) {
                            prefixesRegisteredForPeer.add(fibEntry.getPrefix().toString());
                        }
                    }
                }
            }

            Iterator<String> it = prefixesInResp.iterator();
            while (it.hasNext()) {
                String prefix = it.next();
                if (prefixesRegisteredForPeer.contains(prefix)) {
                    it.remove();
                    prefixesRegisteredForPeer.remove(prefix);
                }
            }

            // register new prefixes in response
            if (prefixesInResp.size() > 0) {
                Log.d(TAG, prefixesInResp.size() + " new prefixes to add.");
                mController.ribRegisterPrefix(peerFaceId, prefixesInResp.toArray(new String[0]));
            } else {
                Log.d(TAG, "No new prefixes to register.");
            }

            // unregister all prefixes that no longer are supported via this face
            for (String toRemovePrefix : prefixesRegisteredForPeer) {
                Log.d(TAG, "Removing from FIB: " + toRemovePrefix + " " + peerFaceId);
                Nfdc.unregister(mFace, new Name(toRemovePrefix), peerFaceId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
