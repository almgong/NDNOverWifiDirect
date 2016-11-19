package ag.ndn.ndnoverwifidirect.videosharing.callback;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import ag.ndn.ndnoverwifidirect.callback.NDNCallBackOnInterest;
import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayer;
import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayerBuffer;

import static android.R.attr.data;
import static android.os.Environment.getExternalStorageDirectory;

/**
 * On receiving an interest for media data, return the requested
 * media. Max of media in response is assumed to be VideoPlayerBuffer.MAX_ITEM_SIZE.
 *
 * Created by allengong on 10/1/16.
 */

public class GetVideoOnInterest implements NDNCallBackOnInterest {

    private final String TAG = "GetOnVideoInterest";

    private static boolean test = false;

    private RandomAccessFile ras = null;

    // the calling activity should be aware of the file to open,
    // to avoid re-opening files, pass the stream as a mandatory argument
    // note that this task does not take care of closing the stream, that is
    // left to the calling activity
    public GetVideoOnInterest(RandomAccessFile ras) {
        this.ras = ras;
    }

    @Override
    public void doJob(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {

        Log.d(TAG, "Got interest: " + interest.getName().toUri());
        String[] nameArr = interest.getName().toString().split("/");
        int sequenceNumber = Integer.parseInt(nameArr[nameArr.length - 1]);

        Data data = new Data();
        data.setName(interest.getName());

        // holds the media bytes to send back (or just the header if no bytes)
        byte[] dataBytes;
        int headerSize = 1;

        try {
            int size = VideoPlayerBuffer.MAX_ITEM_SIZE;

            // choose lesser of 2 (in case JNDN gets updated, etc.)
            // see http://www.lists.cs.ucla.edu/pipermail/ndn-interest/2015-August/000780.html
            // for why we limit to a fraction of the practical limit
            int limit = (face.getMaxNdnPacketSize()*95)/100;   // 95% of practical limit
            if (limit < VideoPlayerBuffer.MAX_ITEM_SIZE) {
                size = limit-headerSize;
            }

            byte[] temp = new byte[size-headerSize];

            // seek (skip) to the desired byte offset
            long skipAmount = 0;
            if (sequenceNumber > 0) {
                skipAmount = sequenceNumber*(size-headerSize);
            }

            ras.seek(skipAmount);       // seek to desired position

            // read bytes to temporary array (temp has length = max size already)
            Log.d(TAG, "Read from: " + sequenceNumber*(size-headerSize) + "-" + (skipAmount+temp.length));
            int bytesRead = ras.read(temp);
            if (bytesRead == -1) {
                dataBytes = new byte[1];
                dataBytes[0] = VideoPlayer.EOF_FLAG;
            } else {
                Log.d(TAG, "[ " + sequenceNumber + " ] read : " + bytesRead + " bytes.");
                dataBytes = new byte[bytesRead+headerSize];      // +1 for the header byte
                dataBytes[0] = VideoPlayer.DATA_FLAG;
                System.arraycopy(temp, 0, dataBytes, 1, bytesRead);
            }
        } catch(Exception e) {
            e.printStackTrace();
            return;
        } finally {

        }

        Blob payload = new Blob(dataBytes);
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
