package ag.ndn.ndnoverwifidirect.videosharing.task;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;

import com.intel.jndn.management.Nfdc;
import com.intel.jndn.management.types.FibEntry;

import java.util.ArrayList;
import java.util.List;

import ag.ndn.ndnoverwifidirect.VideoActivity;
import ag.ndn.ndnoverwifidirect.utils.NDNController;
import ag.ndn.ndnoverwifidirect.videosharing.model.VideoResource;
import ag.ndn.ndnoverwifidirect.videosharing.model.VideoResourceList;

import static android.content.ContentValues.TAG;

/**
 * Asynchronous task that will send a re-registration
 * interest to all currently registered peers.
 *
 * Currently daisy chains the calls to keep track of which have
 * returned.
 *
 * Created by allengong on 10/11/16.
 */

public class GetAvailableVideosTask extends AsyncTask <Integer, Void, Integer> {

    private List<String> prefixes = new ArrayList<>();
    private ArrayAdapter<VideoResource> adapter;
    private VideoResourceList videoResourceList;
    private ProgressBar progressBar;

    // prefixes is a list of NDN prefixes found by querying all peer
    public GetAvailableVideosTask(ArrayAdapter<VideoResource> adapter, VideoResourceList list, ProgressBar progressBar) {
        this.adapter = adapter;
        this.videoResourceList = list;
        this.progressBar = progressBar;
    }

    @Override
    protected Integer doInBackground(Integer... params) {

        try {
            // we are specifically looking for video data prefixes
            String relevantPrefix = VideoActivity.DATA_PREFIX + VideoActivity.NDN_VIDEO_PREFIX;

            List<FibEntry> fibEntries = Nfdc.getFibList(NDNController.getInstance().getLocalHostFace());

            for (FibEntry fibEntry : fibEntries) {
                if (fibEntry.getPrefix().toString().startsWith(relevantPrefix)) {
                    prefixes.add(fibEntry.getPrefix().toString());
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Something happened while looking up data prefixes");
            e.printStackTrace();
            return -1;
        }

        return 1;
    }

    /**
     * Updates the input VideoResourceList to contain the prefixes found,
     * as well as any UI.
     *
     * @param v
     */
    @Override
    protected void onPostExecute(Integer v) {

        if (v == -1) {
            // notify front end of error
        }

        int i = 0;
        for (String s : prefixes) {
            videoResourceList.addToList(new VideoResource(i++, s));
        }

        adapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);   // remove from UI
    }
}
