package ag.ndn.ndnoverwifidirect.task;

import android.os.AsyncTask;

import com.intel.jndn.management.Nfdc;
import com.intel.jndn.management.types.FibEntry;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;

import java.util.List;

import ag.ndn.ndnoverwifidirect.callback.ProbeOnData;
import ag.ndn.ndnoverwifidirect.utils.IPAddress;
import ag.ndn.ndnoverwifidirect.utils.NDNController;

/**
 * Periodically probes registered peers for available
 * data prefixes.
 *
 * Created by allengong on 11/5/16.
 */

public class ProbeTask extends AsyncTask<Void, Void, Void> {

    private static final int REPEAT_TIMER_MS = 5000;
    private boolean loop = true;

    private Face mFace = null;
    private NDNController mController = NDNController.getInstance();

    public void stop() {
        loop = true;
    }

    @Override
    protected Void doInBackground(Void... params) {

        String myIp = IPAddress.getLocalIPAddress();

        while (loop) {
            System.err.println("Probe for data prefixes...");
            try {
                mFace = new Face("localhost"); // localhost face used to contact nfd, destroyed at end
                // enumerate FIB entries
                List<FibEntry> fibEntries = Nfdc.getFibList(mFace);

                // look only for the ones related to /localhop/wifidirect/xxx
                for (FibEntry entry : fibEntries) {
                    String prefix = entry.getPrefix().toString();
                    String[] prefixArr = prefix.split("/");

                    if (prefix.startsWith(NDNController.PROBE_PREFIX) && !prefixArr[prefixArr.length-1].equals(myIp)) {
                        System.out.println("someone else's localhop prefix found!");
                        System.out.println(entry.getPrefix().toString());

                        // send interest to this peer
                        Interest interest = new Interest(new Name(prefix + "/" + myIp + "/probe"));
                        interest.setMustBeFresh(true);
                        mFace.expressInterest(interest, new OnData() {
                            @Override
                            public void onData(Interest interest, Data data) {
                                (new ProbeOnData()).doJob(interest, data);
                            }
                        }); // no timeout handling
                    }

                    System.out.println(entry.getPrefix().toString());
                }

                //TODO exclude localhost bound faces????

                mFace.shutdown();
                Thread.sleep(REPEAT_TIMER_MS);  // wait a little before doing again

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
