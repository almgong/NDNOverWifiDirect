package ag.ndn.ndnoverwifidirect.videosharing.callback;

import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

import java.io.IOException;

import ag.ndn.ndnoverwifidirect.callback.NDNCallBackOnInterest;

/**
 * On receiving an interest for media data, return the requested
 * media. Max of media in response is assumed to be VideoPlayerBuffer.MAX_ITEM_SIZE.
 *
 * Created by allengong on 10/1/16.
 */

public class GetVideoOnInterest implements NDNCallBackOnInterest {

    private final String TAG = "GetOnVideoInterest";

    @Override
    public void doJob(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {

        Log.d(TAG, "Got interest: " + interest.getName().toUri());

        Data data = new Data();
        data.setName(interest.getName());

        // for testing, return EOF byte
        byte[] temp = new byte[1];
        temp[0] = (byte)0;

        Blob payload = new Blob(temp);

        data.setContent(payload);

        try {
            face.putData(data);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
