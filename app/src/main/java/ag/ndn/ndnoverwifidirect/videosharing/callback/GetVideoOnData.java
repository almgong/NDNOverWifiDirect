package ag.ndn.ndnoverwifidirect.videosharing.callback;

import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;

import ag.ndn.ndnoverwifidirect.callback.NDNCallbackOnData;
import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayerBuffer;

/**
 * On data received from peer, parse content and add data to the video player
 * buffer.
 *
 * Created by allengong on 10/1/16.
 */

public class GetVideoOnData implements NDNCallbackOnData {

    private final String TAG = "GetVideoOnData";
    private VideoPlayerBuffer buffer;

    public GetVideoOnData(VideoPlayerBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void doJob(Interest interest, Data data) {
        Log.d(TAG, "Got data for interest: " + interest.getName().toString());
        Log.d(TAG, new String(data.getContent().getImmutableArray()));
    }
}
