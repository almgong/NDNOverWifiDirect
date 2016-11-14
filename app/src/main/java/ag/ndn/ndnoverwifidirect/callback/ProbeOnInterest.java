package ag.ndn.ndnoverwifidirect.callback;

import android.util.Log;

import com.intel.jndn.management.Nfdc;
import com.intel.jndn.management.types.RibEntry;

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
 * Created by allengong on 11/12/16.
 */
public class ProbeOnInterest implements NDNCallBackOnInterest {

    private static final String TAG = "ProbeOnInterest";

    private Face mFace = new Face("localhost"); // localhost face for communicating with NFD, destroyed after usage
    private NDNController mController = NDNController.getInstance();

    @Override
    public void doJob(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
        Log.d(TAG, "Got an interest for: " + prefix.toString());

        // /localhop/wifidirect/192.168.49.x/192.168.49.y/probe?mustBeFresh=1
        String[] prefixArr = prefix.toString().split("/");

        // validate
        if (prefixArr.length != 5) {
            Log.e(TAG, "Error with this interest, skipping...");
        }

        final String peerIp = prefixArr[prefixArr.length - 2];

        // if not logged (a face created for this probing peer), should then create a face (mainly for GO)
        if (mController.getFaceIdForPeer(peerIp) == -1) {

            mController.createFace(peerIp, NDNController.PROBE_PREFIX + "/" + peerIp, new GenericCallback() {
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

            if (mController.getIsGroupOwner()) {
                prefixesToReturn.addAll(mController.getAllLoggedPrefixes());    // avoid consulting NFD
                num = mController.getAllLoggedPrefixes().size();
            } else {
                // need to consult NFD and find the data prefixes
                List<RibEntry> ribEntries = Nfdc.getRouteList(mFace);

                for (RibEntry ribEntry : ribEntries) {
                    String entryName = ribEntry.getName().toString();
                    if (entryName.startsWith(NDNController.DATA_PREFIX)) {
                        // TODO also need to check it was created at localhost
                        // see issue #18 on git, if the answer is yes, we can
                        // simply avoid NFD communication and grab from prefixMap
                        response += ribEntry.getName().toString();
                        num++;
                    }

                }
            }

            Data data = new Data();

            // format payload, for now ignore hopcount as it is not clear whether
            // it is useful
            for (String pre : prefixesToReturn) {
                response += (pre + "\n");
            }

            Blob payload = new Blob(num + "\n" + response);
            data.setContent(payload);

            face.putData(data);
            Log.d(TAG, "Responded to: " + prefix.toUri());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mFace.shutdown();
        }
    }
}
