package ag.ndn.ndnoverwifidirect.videosharing.callback;

import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;

import java.util.Arrays;

import ag.ndn.ndnoverwifidirect.callback.NDNCallbackOnData;
import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayer;
import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayerBuffer;
import ag.ndn.ndnoverwifidirect.videosharing.task.GetVideoTask;

import static android.content.ContentValues.TAG;

/**
 * Created by allengong on 10/8/16.
 */

public class GetVideoOnData implements NDNCallbackOnData {

    private String tag;

    private int taskNum;    // really which index to put data into for transmission window
    private GetVideoTask.TransmissionWindow window;
    private VideoPlayerBuffer buffer;

    public GetVideoOnData(int taskNumber, GetVideoTask.TransmissionWindow window, VideoPlayerBuffer buffer) {
        this.taskNum = taskNumber;
        this.window = window;
        this.buffer = buffer;   // we don't directly access buffer here, just need a reference to notify EOF
    }

    @Override
    public void doJob(Interest interest, Data data) {

        byte[] payload = data.getContent().getImmutableArray();

        if (payload[0] == VideoPlayer.EOF_FLAG) { // no more data

            // there was no data in resp, meaning all data sent for resource
            // looping stops here
            buffer.notifyEofReached();          // probably should be done a different way
            buffer.addToBuffer(new byte[0]);    // added precaution
            Log.d(TAG, String.format("All data from %s has been processed from peer, or stop was set.", tag));

        } else if (payload[0] == VideoPlayer.DATA_FLAG) { // packet contains data

            // parse custom header
            byte[] dataToBuffer = Arrays.copyOfRange(payload, 1, payload.length);

            window.setSlot(taskNum, dataToBuffer);
        }

        Log.d(TAG, "Task " + taskNum + " completed.");
    }


}