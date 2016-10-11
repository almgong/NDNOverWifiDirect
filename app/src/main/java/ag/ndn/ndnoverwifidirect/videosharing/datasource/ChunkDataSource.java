package ag.ndn.ndnoverwifidirect.videosharing.datasource;

import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

import java.io.IOException;
import java.util.Arrays;

import ag.ndn.ndnoverwifidirect.videosharing.VideoPlayerBuffer;

/**
 * Custom DataSource to use in a VideoPlayer/ExoPlayer. Specifically
 * implemented for use with NDNOverWifiDirect.
 *
 * Created by allengong on 9/29/16.
 */
public class ChunkDataSource implements DataSource {

    private VideoPlayerBuffer videoPlayerBuffer;
    private final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private byte[] current = EMPTY_BYTE_ARRAY;                  // initial value
    private int waitTime = VideoPlayerBuffer.POLITENESS_DELAY; // ms time to wait in between calls to videoplayer buffer
    private boolean eofReached = false;

    public ChunkDataSource(VideoPlayerBuffer buffer) {
        this.videoPlayerBuffer = buffer;
    }


    @Override
    public long open(DataSpec dataSpec) throws IOException {
        return C.LENGTH_UNSET;  // we don't know how many bytes can be retrieved by network
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {

        // flag is set below
        if (eofReached) {
            return C.RESULT_END_OF_INPUT;
        }

        // used to temporarily store each response from VideoPlayerBuffer
        byte[] tempBuffer;
        int bytesRead = 0;

        // if there is something left in the "cached" byte array
        if (current.length > 0) {

            // if there is enough bytes to satisfy this call
            if (readLength <= current.length) {

                System.arraycopy(current, 0, buffer, offset, readLength);
                current = Arrays.copyOfRange(current, readLength, current.length); // [readLength:]
                bytesRead = readLength;
            } else {

                // uh oh, not enough, for now let's just return what we have
                System.arraycopy(current, 0, buffer, offset, current.length);
                bytesRead = current.length;
                current = EMPTY_BYTE_ARRAY;
            }

        } else {

            while ((tempBuffer = videoPlayerBuffer.getFromBuffer()) == null) {
                try{
                    Thread.sleep(waitTime);      // wait x seconds between each call as to not waste CPU cycles
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } // end while

            // at this point, tempBuffer points to a non-null byte[]
            if (tempBuffer.length == 0) {
                eofReached = true;
                return C.RESULT_END_OF_INPUT;       // signals EOF
            }

            // do a similar check, see if there are enough bytes in current buffer item to satisfy call
            if (readLength <= tempBuffer.length) {
                current = Arrays.copyOfRange(tempBuffer, readLength, tempBuffer.length);    // splice array [readLength:]
                System.arraycopy(tempBuffer, 0, buffer, offset, readLength);
                bytesRead = readLength;
            } else {
                // else not enough to satisfy, return what we have now
                System.arraycopy(tempBuffer, 0, buffer, offset, tempBuffer.length);
                bytesRead = tempBuffer.length;
                current = EMPTY_BYTE_ARRAY;
            }
        }

        return bytesRead;       // return number of bytes returned by this call to read()
    }

    @Override
    public Uri getUri() {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}
