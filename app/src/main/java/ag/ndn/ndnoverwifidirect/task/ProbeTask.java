package ag.ndn.ndnoverwifidirect.task;

import android.os.AsyncTask;
import android.util.Log;

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
import ag.ndn.ndnoverwifidirect.utils.WDBroadcastReceiver;

import static android.content.ContentValues.TAG;

/**
 * Periodically probes registered peers for available
 * data prefixes.
 *
 * Created by allengong on 11/5/16.
 */

public class ProbeTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "ProbeTask";
    private static final int REPEAT_TIMER_MS = 5000;

    private boolean loop = true;

    private NDNController mController = NDNController.getInstance();
    private Face mFace = mController.getLocalHostFace();

    public void stop() {
        loop = true;
    }

    @Override
    protected Void doInBackground(Void... params) {

        while (loop) {
            try {

                if (WDBroadcastReceiver.myAddress == null) {
                    Log.d(TAG, "Skip this iteration due to null WD ip.");
                } else {
                    // enumerate FIB entries
                    List<FibEntry> fibEntries = Nfdc.getFibList(mFace);

                    // look only for the ones related to /localhop/wifidirect/xxx
                    for (FibEntry entry : fibEntries) {
                        String prefix = entry.getPrefix().toString();
                        String[] prefixArr = prefix.split("/");

                        if (prefix.startsWith(NDNController.PROBE_PREFIX) && !prefixArr[prefixArr.length - 1].equals(WDBroadcastReceiver.myAddress)) {
                            System.out.println("someone else's localhop prefix found!");
                            System.out.println(entry.getPrefix().toString());

                            // send interest to this peer
                            Interest interest = new Interest(new Name(prefix + "/" + WDBroadcastReceiver.myAddress + "/probe?" + System.currentTimeMillis()));
                            interest.setMustBeFresh(true);
                            System.err.println("Sending interest: " + interest.getName().toString());
                            mFace.expressInterest(interest, new OnData() {
                                @Override
                                public void onData(Interest interest, Data data) {
                                    (new ProbeOnData()).doJob(interest, data);
                                }
                            }); // no timeout handling
                        }
                    }
                }

                Thread.sleep(REPEAT_TIMER_MS);  // wait a little before doing again

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
