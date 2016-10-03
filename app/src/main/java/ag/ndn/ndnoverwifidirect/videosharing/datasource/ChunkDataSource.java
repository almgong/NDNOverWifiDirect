package ag.ndn.ndnoverwifidirect.videosharing.datasource;

import android.net.Uri;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

import java.io.IOException;
import java.io.InterruptedIOException;
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
    private byte[] current = new byte[0];
    private int currentCounter = 0;         // 0-means current points to first x bytes retrieved from buffer, etc.
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

        // if we have any bytes left in "cached" byte array
        if (current.length > 0) {

            // if there is enough bytes in current to satisfy this call
            if (readLength <= current.length) {

                System.arraycopy(current, 0, buffer, offset, readLength);
                current = Arrays.copyOfRange(current, readLength, current.length); // [readLength:]

            } else {
                // uh oh, need to grab a new tempBuffer and piece together the remaining bytes in current
                while ((tempBuffer = videoPlayerBuffer.getFromBuffer()) == null) {
                    try{
                        Thread.sleep(500);      // wait .5 seconds between each call as to not waste CPU cycles
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } // end while

                if (tempBuffer.length == 0) {   // no more bytes to retrieve, include last bytes in current
                    System.arraycopy(current, 0, buffer, offset, current.length);
                    eofReached = true;
                    return current.length;
                }

                System.arraycopy(current, 0, buffer, offset, current.length);
                System.arraycopy(tempBuffer, 0, buffer, offset+current.length, readLength-current.length);

                // update current
                current = Arrays.copyOfRange(tempBuffer, readLength-current.length, tempBuffer.length);
            }

        } else {        // else we do not have a cached chunk, or exactly all bytes have been used up

            while ((tempBuffer = videoPlayerBuffer.getFromBuffer()) == null) {
                try{
                    Thread.sleep(500);      // wait .5 seconds between each call as to not waste CPU cycles
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } // end while

            if (tempBuffer.length == 0) {
                return C.RESULT_END_OF_INPUT;
            }

            // at this point, tempBuffer points to a non-null byte[]
            current = Arrays.copyOfRange(tempBuffer, readLength, tempBuffer.length);    // splice array [offset:]
            System.arraycopy(tempBuffer, 0, buffer, offset, readLength);
        }

        return readLength;
    }

    @Override
    public Uri getUri() {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}
