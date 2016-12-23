package ag.ndn.ndnoverwifidirect.runnable;

import android.util.Log;

import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.Nfdc;
import com.intel.jndn.management.types.FibEntry;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;

import java.io.IOException;
import java.util.List;

import ag.ndn.ndnoverwifidirect.callback.ProbeOnData;
import ag.ndn.ndnoverwifidirect.model.Peer;
import ag.ndn.ndnoverwifidirect.utils.NDNController;
import ag.ndn.ndnoverwifidirect.utils.WDBroadcastReceiver;

/**
 * Probes network for data prefixes, as specified in protocol.
 * Created by allengong on 12/9/16.
 */
public class ProbeRunnable implements Runnable {
    private static final String TAG = "ProbeRunnable";
    private final int MAX_TIMEOUTS_ALLOWED = 5;

    private Face mFace = NDNController.getInstance().getLocalHostFace();

    @Override
    public void run() {
        try {
            if (WDBroadcastReceiver.myAddress == null) {
                Log.d(TAG, "Skip this iteration due to null WD ip.");
            } else {
                // enumerate FIB entries
                List<FibEntry> fibEntries = Nfdc.getFibList(mFace);

                // look only for the ones related to /localhop/wifidirect/xxx
                for (FibEntry entry : fibEntries) {
                    String prefix = entry.getPrefix().toString();
                    final String[] prefixArr = prefix.split("/");
                    if (prefix.startsWith(NDNController.PROBE_PREFIX) && !prefixArr[prefixArr.length - 1].equals(WDBroadcastReceiver.myAddress)) {
                        Log.d(TAG, "Someone else's localhop prefix found!");
                        Log.d(TAG, entry.getPrefix().toString());

                        // send interest to this peer
                        Interest interest = new Interest(new Name(prefix + "/" + WDBroadcastReceiver.myAddress + "/probe?" + System.currentTimeMillis()));
                        interest.setMustBeFresh(true);
                        Log.d(TAG, "Sending interest: " + interest.getName().toString());
                        mFace.expressInterest(interest, new OnData() {
                            @Override
                            public void onData(Interest interest, Data data) {
                                (new ProbeOnData()).doJob(interest, data);
                                Peer peer = NDNController.getInstance().getPeerByIp(prefixArr[prefixArr.length - 1]);
                                peer.setNumProbeTimeouts(0);    // peer responded, so reset timeout counter
                            }
                        }, new OnTimeout() {
                            @Override
                            public void onTimeout(Interest interest) {
                                Peer peer = NDNController.getInstance().getPeerByIp(prefixArr[prefixArr.length - 1]);
                                if (peer == null) {
                                    Log.d(TAG, "No peer information available to track timeout.");
                                    return;
                                }

                                Log.d(TAG, "Timeout for interest: " + interest.getName().toString() +
                                        " Attempts: " + (peer.getNumProbeTimeouts() + 1));

                                if (peer.getNumProbeTimeouts() + 1 >= MAX_TIMEOUTS_ALLOWED) {
                                    // declare peer as disconnected from group
                                    NDNController.getInstance().removePeer(prefixArr[prefixArr.length - 1]);
                                } else {
                                    peer.setNumProbeTimeouts(peer.getNumProbeTimeouts() + 1);
                                }
                            }
                        });
                    }
                }
            }
        } catch (ManagementException me) {
            Log.e(TAG, "Something went wrong with acquiring the FibList.");
            me.printStackTrace();
        } catch (IOException ioe) {
            Log.e(TAG, "Something went wrong with sending a probe interest.");
            ioe.printStackTrace();
        }
    }
}
