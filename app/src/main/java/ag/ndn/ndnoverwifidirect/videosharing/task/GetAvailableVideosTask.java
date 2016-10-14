package ag.ndn.ndnoverwifidirect.videosharing.task;

import android.os.AsyncTask;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ag.ndn.ndnoverwifidirect.task.SendInterestTask;
import ag.ndn.ndnoverwifidirect.utils.IPAddress;
import ag.ndn.ndnoverwifidirect.utils.NDNOverWifiDirect;
import ag.ndn.ndnoverwifidirect.videosharing.model.VideoResource;
import ag.ndn.ndnoverwifidirect.videosharing.model.VideoResourceList;

import static android.R.attr.startOffset;

/**
 * Asynchronous task that will send a re-registration
 * interest to all currently registered peers.
 *
 * Currently daisy chains the calls to keep track of which have
 * returned.
 *
 * Created by allengong on 10/11/16.
 */

public class GetAvailableVideosTask extends AsyncTask <Integer, Void, Void> {

    private NDNOverWifiDirect mController = NDNOverWifiDirect.getInstance();
    private OnData onData;
    private Face mFace = new Face("localhost");
    private SendInterestTask currTask;

    private List<String> prefixes = new ArrayList<>();
    private ArrayAdapter<VideoResource> adapter;
    private VideoResourceList videoResourceList;
    private ProgressBar progressBar;


    private String myIp;
    private String[] peers;
    private int i;

    private int processEventsTimer = 500;
    private boolean loop = true;

    // prefixes is a list of NDN prefixes found by querying all peer
    public GetAvailableVideosTask(ArrayAdapter<VideoResource> adapter, VideoResourceList list, ProgressBar progressBar) {
        this.adapter = adapter;
        this.videoResourceList = list;
        this.progressBar = progressBar;
    }

    @Override
    protected Void doInBackground(Integer... params) {

        // init
        myIp = IPAddress.getLocalIPAddress();
        peers = mController.enumerateLoggedFaces().toArray(new String[0]);
        i = 0;

        // start the daisy chain
        if (peers.length > 0 && myIp != null) {

            onData = new OnData() {
                @Override
                public void onData(Interest interest, Data data) {

                    currTask.setStopProcessing(true);   // stop previous processing

                    /* response format:
                         <ARBITRARY MESSAGE>\n
                         <NUMBER OF PEERS>
                         <LIST OF PEER IPs\n...>
                         <NUMBER OF PREFIXES YOU HANDLE>
                         <LIST OF PREFIXES\n...>
                     */

                    String[] resp = data.getContent().toString().split("\n");

                    // skip the arbitrary message
                    int offset = 1;

                    // skip peers list and num prefixes
                    int numPeers = Integer.parseInt(resp[offset]);
                    offset += (numPeers + 1 + 1);

                    // rest of response is the list of prefixes
                    for (int i = offset; i < resp.length; i++) {
                        prefixes.add(resp[i]);
                    }

                    // re-associate peer with these prefixes
                    mController.createFace(peers[i-1], prefixes.toArray(new String[0]));

                    // now chain for next
                    if (i < peers.length) {
                        Interest nextInterest = new Interest(new Name("/ndn/wifid/register/" + peers[i++] + "/" + myIp + "/" + System.currentTimeMillis()));
                        currTask = (SendInterestTask) mController.sendInterest(nextInterest, mFace,
                                onData, processEventsTimer);
                    } else {
                        // we are done, do nothing
                        loop = false;
                    }
                }

            };

            // send first interest
            Interest interest = new Interest(new Name("/ndn/wifid/register/" + peers[i++] + "/" + myIp + "/" + System.currentTimeMillis()));
            currTask = (SendInterestTask) mController.sendInterest(interest, mFace,
                    onData, processEventsTimer);

            while (loop) {
                try {
                    Thread.sleep(processEventsTimer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.err.println("Could not find any video resources or IP null.");
        }

        return null;
    }

    /**
     * Updates the input VideoResourceList to contain the prefixes found,
     * as well as any UI.
     *
     * @param v
     */
    @Override
    protected void onPostExecute(Void v) {

        int i = 0;
        for (String s : prefixes) {
            videoResourceList.addToList(new VideoResource(i++, s));
        }

        adapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);   // remove from UI
    }
}
